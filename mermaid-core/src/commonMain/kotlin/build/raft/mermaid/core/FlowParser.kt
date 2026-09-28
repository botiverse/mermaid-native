package build.raft.mermaid.core

/** Flow tokens retain quoted labels instead of splitting arrows inside node text. */
internal class FlowParser(private val source:String) {
    private val nodes=linkedMapOf<String,FlowNode>()
    private val definitions=linkedMapOf<String,List<String>>()
    private val interactions=mutableListOf<FlowInteraction>()
    private var defaultEdgeStyles=emptyList<String>()
    private var defaultInterpolate:String?=null
    private val edges=mutableListOf<FlowEdge>()
    private val groups=mutableListOf<FlowSubgraph>()
    private data class Group(val id:String,val label:String,val labelType:String="text",val members:MutableList<String> = mutableListOf(),var direction:FlowDirection?=null)
    private val stack=mutableListOf<Group>()
    private var offset=0
    private var parsingHeader=true
    private var direction=FlowDirection.TB
    private var accTitle:String?=null
    private var accDescription:String?=null
    fun parse():MermaidParseResult = try {
        val statements=statements()
        requireFlow(statements.isNotEmpty(),"Expected flowchart header")
        val header=statements.first().second.trim().split(Regex("\\s+"))
        requireFlow(header.first().lowercase() in setOf("graph","flowchart","flowchart-elk"),"Expected flowchart header")
        requireFlow(header.size<=2,"Invalid flowchart header")
        direction=if(header.size==1)FlowDirection.TB else parseDirection(header[1])
        parsingHeader=false
        statements.drop(1).forEach { (at,text) ->
            offset=at
            when {
                text.startsWith("subgraph ") -> {
                    val raw=text.drop(9).trim();val bracket=raw.indexOf('[')
                    val label=if(bracket>=0){requireFlow(raw.endsWith(']'),"Unclosed subgraph title");unquote(raw.substring(bracket+1,raw.length-1)).first}else unquote(raw).first
                    val id=if(bracket>=0)raw.take(bracket).trim() else if(label.any { it.isWhitespace() })"subGraph${groups.size+stack.size}" else label
                    requireFlow(id.isNotBlank(),"Expected subgraph name");requireFlow(stack.none { it.id==id },"Duplicate or cyclic subgraph id");stack+=Group(id,label,if(unquote(if(bracket>=0)raw.substring(bracket+1,raw.length-1)else raw).second=="markdown")"markdown"else"text")
                }
                text=="end" -> {
                    requireFlow(stack.isNotEmpty(),"Unexpected end")
                    val group=stack.removeAt(stack.lastIndex)
                    val existing=groups.firstOrNull { it.id==group.id };groups.removeAll { it.id==group.id }
                    groups+=FlowSubgraph(group.id,group.label,((existing?.nodeIds ?: emptyList())+group.members).distinct(),group.direction ?: existing?.direction,stack.lastOrNull()?.id,group.labelType,classes=existing?.classes.orEmpty())
                    stack.lastOrNull()?.members?.add(group.id)
                }
                text.startsWith("direction ") -> {val value=parseDirection(text.drop(10).trim());if(stack.isEmpty())direction=value else stack.last().direction=value}
                text.startsWith("accTitle:") -> accTitle=text.substringAfter(':').trim()
                text.startsWith("accDescr:") -> accDescription=text.substringAfter(':').trim()
                text.startsWith("accDescr") && text.contains('{') -> {requireFlow(text.endsWith('}'),"Unclosed accessibility description");accDescription=text.substringAfter('{').dropLast(1).trim()}
                text.startsWith("classDef ") -> styleDefinition(text.drop(9).trim())
                text.startsWith("style ") -> nodeStyle(text.drop(6).trim())
                text.startsWith("class ") -> cssClass(text.drop(6).trim())
                text.startsWith("click ") -> interaction(text.drop(6).trim())
                text.startsWith("linkStyle ") -> edgeStyle(text.drop(10).trim())
                else -> Chain(text).parse()
            }
        }
        requireFlow(stack.isEmpty(),"Unclosed subgraph")
        MermaidParseResult.Success(FlowchartDiagram(direction,nodes.values.toList(),edges.toList(),groups.toList(),accTitle,accDescription,definitions.toMap(),interactions.toList(),defaultEdgeStyles,defaultInterpolate))
    } catch(e:FlowSyntaxError) {
        val prefix=source.take(offset)
        MermaidParseResult.Failure(listOf(MermaidDiagnostic(if(parsingHeader)MermaidDiagnosticCode.INVALID_HEADER else MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,e.message?:"Unsupported flowchart syntax",SourceLocation(prefix.count { it=='\n' }+1,offset-prefix.lastIndexOf('\n')))))
    }
    private fun styles(text:String):List<String> {
        val values=mutableListOf<String>();var start=0;var level=0
        text.forEachIndexed { i,c->if(c=='(')level++ else if(c==')')level--;if(c==',' && level==0){values+=text.substring(start,i).trim();start=i+1} }
        values+=text.drop(start).trim();requireFlow(level==0,"Unbalanced style value")
        values.forEach { requireFlow(it.isNotBlank() && (!it.contains(':') || it.substringAfter(':').isNotBlank()),"Empty flowchart style property") }
        return values
    }
    private fun styleDefinition(text:String){val names=text.takeWhile { !it.isWhitespace() };val values=styles(text.drop(names.length).trim());names.split(',').forEach { requireFlow(it.isNotBlank(),"Expected style name");definitions[it]=definitions[it].orEmpty()+values }}
    private fun nodeStyle(text:String){val names=text.takeWhile { !it.isWhitespace() };val values=styles(text.drop(names.length).trim());names.split(',').forEach { id->val old=nodes[id] ?: FlowNode(id,id,createdByStyle=true);nodes[id]=old.copy(styles=old.styles+values) }}
    private fun cssClass(text:String){val names=text.takeWhile { !it.isWhitespace() };val css=text.drop(names.length).trim();requireFlow(css.isNotBlank(),"Expected CSS class");names.split(',').forEach { assignClass(it,css) }}
    private fun assignClass(id:String,css:String) {
        nodes[id]?.let { nodes[id]=it.copy(classes=it.classes+css) }
        groups.indexOfFirst { it.id==id }.takeIf { it>=0 }?.let { i->groups[i]=groups[i].copy(classes=groups[i].classes+css) }
        edges.indexOfFirst { it.id==id }.takeIf { it>=0 }?.let { i->edges[i]=edges[i].copy(classes=edges[i].classes+css) }
    }
    private fun edgeStyle(text:String) {
        val targets=text.takeWhile { !it.isWhitespace() };var rest=text.drop(targets.length).trim()
        var curve:String?=null
        if(rest.startsWith("interpolate ")){rest=rest.drop(12).trim();curve=rest.takeWhile { !it.isWhitespace() };rest=rest.drop(curve.length).trim();requireFlow(curve.isNotEmpty(),"Expected interpolation")}
        val values=if(rest.isEmpty())emptyList()else styles(rest)
        requireFlow(curve!=null || values.isNotEmpty(),"Expected edge style")
        targets.split(',').forEach { value->if(value=="default"){defaultEdgeStyles+=values;if(curve!=null)defaultInterpolate=curve}
            else {val index=value.toIntOrNull()?:fail("Expected edge index");requireFlow(index in edges.indices,"Link style index out of bounds");edges[index]=edges[index].copy(styles=edges[index].styles+values,interpolate=curve ?: edges[index].interpolate)} }
    }
    private fun metadata(id:String,raw:String) {
        val data=try { flowMetadata(raw) }catch(e:IllegalArgumentException){fail(e.message ?: "Invalid metadata")}
        val edgeIndex=edges.indexOfFirst { it.id==id }
        if(edgeIndex>=0){val edge=edges[edgeIndex];val animate=data["animate"]?.let { requireFlow(it=="true" || it=="false","Expected animate boolean");it=="true" };edges[edgeIndex]=edge.copy(interpolate=data["curve"] ?: edge.interpolate,animate=animate ?: edge.animate,animation=data["animation"] ?: edge.animation);return}
        val groupIndex=groups.indexOfFirst { it.id==id }
        if(groupIndex>=0){data["view"]?.let { requireFlow(it in setOf("collapsed","expanded"),"Unsupported group view");groups[groupIndex]=groups[groupIndex].copy(collapsed=it=="collapsed") };return}
        val node=nodes[id] ?: FlowNode(id,id)
        val shape=data["shape"]?.let { name->
            requireFlow(name==name.lowercase() && '_' !in name,"No such shape: $name. Shape names should be lowercase.")
            when(name){"rect","rectangle","process"->FlowNodeShape.RECTANGLE;"rounded","event"->FlowNodeShape.ROUNDED;"circle"->FlowNodeShape.CIRCLE;"dbl-circ","double-circle"->FlowNodeShape.DOUBLE_CIRCLE;"stadium","terminal"->FlowNodeShape.STADIUM;"diamond","diam","decision"->FlowNodeShape.DIAMOND;"hex","hexagon","prepare"->FlowNodeShape.HEXAGON;"cyl","cylinder","database"->FlowNodeShape.CYLINDER;"subproc","subprocess","subroutine"->FlowNodeShape.SUBROUTINE;"odd"->FlowNodeShape.ASYMMETRIC;"lean-r","lean-right","in-out"->FlowNodeShape.PARALLELOGRAM;"lean-l","lean-left","out-in"->FlowNodeShape.PARALLELOGRAM_ALT;"trap-b","trapezoid","priority"->FlowNodeShape.TRAPEZOID;"trap-t","inv-trapezoid","manual"->FlowNodeShape.TRAPEZOID_ALT;else->fail("No such shape: $name.")}
        } ?: node.shape
        nodes[id]=node.copy(shape=shape,label=data["label"]?.takeIf { it.isNotEmpty() } ?: node.label,labelType=data["labelType"] ?: node.labelType,metadata=node.metadata+data)
    }
    private fun interaction(text:String){
        val id=text.takeWhile { !it.isWhitespace() };var rest=text.drop(id.length).trim();requireFlow(id.isNotBlank() && rest.isNotEmpty(),"Expected click target")
        val callback=!(rest.startsWith('"') || rest.startsWith("href "));if(rest.startsWith("href "))rest=rest.drop(5).trim()
        var arguments:String?=null;val value:String
        fun quoted():String {requireFlow(rest.startsWith('"'),"Expected quoted value");val end=rest.indexOf('"',1);requireFlow(end>0,"Unclosed quoted value");val result=rest.substring(1,end);rest=rest.drop(end+1).trim();return result}
        if(!callback)value=quoted()
        else if(rest.startsWith("call ")) {
            rest=rest.drop(5).trim();val open=rest.indexOf('(');requireFlow(open>0,"Expected callback arguments");value=rest.take(open).trim();var end=open+1;var quote=false
            while(end<rest.length){if(rest[end]=='"')quote=!quote;if(rest[end]==')' && !quote)break;end++};requireFlow(end<rest.length,"Unclosed callback arguments")
            arguments=rest.substring(open+1,end).takeIf { it.isNotBlank() };rest=rest.drop(end+1).trim()
        }else {value=rest.takeWhile { !it.isWhitespace() };rest=rest.drop(value.length).trim()}
        val tooltip=if(rest.startsWith('"'))quoted()else null
        requireFlow(rest.isEmpty() || (!callback && rest in setOf("_self","_blank","_parent","_top")),"Unsupported click suffix")
        interactions+=FlowInteraction(id,value,callback,arguments,tooltip,rest.takeIf { it.isNotEmpty() })
    }
    private fun parseDirection(value:String)=when(value){">"->FlowDirection.LR;"<"->FlowDirection.RL;"^"->FlowDirection.BT;"v"->FlowDirection.TB;else->FlowDirection.entries.firstOrNull { it.name==value.uppercase() }?:fail("Invalid flowchart direction")}
    private fun statements():List<Pair<Int,String>> {
        val out=mutableListOf<Pair<Int,String>>();var start=0;var at=0;var quote=false;val closes=mutableListOf<Char>()
        fun emit(end:Int){val raw=source.substring(start,end);val leading=raw.indexOfFirst { !it.isWhitespace() };if(leading>=0)out+=(start+leading) to raw.trim()}
        while(at<source.length){val c=source[at]
            if(c=='"')quote=!quote
            if(!quote){
                if(closes.isEmpty() && source.startsWith("%%",at)){emit(at);while(at<source.length && source[at]!='\n')at++;start=at+1;at++;continue}
                if(c in "[({")closes+=when(c){'['->']';'('->')';else->'}'}
                else if(closes.lastOrNull()==c)closes.removeAt(closes.lastIndex)
                if(closes.isEmpty() && (c=='\n' || c==';')){emit(at);start=at+1}
            };at++
        }
        if(quote || closes.isNotEmpty()){val leading=source.drop(start).indexOfFirst { !it.isWhitespace() };offset=start+maxOf(0,leading);parsingHeader=out.isEmpty();fail("Unclosed flowchart label")};emit(source.length);return out
    }
    private fun unquote(raw:String):Pair<String,String> {
        val text=raw.trim()
        if(text.startsWith("\"`") && text.endsWith("`\""))return text.substring(2,text.length-2) to "markdown"
        if(text.contains('"')) {
            requireFlow(text.startsWith('"') && text.endsWith('"'),"Unexpected token: got 'STR'")
            requireFlow(text.count { it=='"' }==2,"Expecting 'SQE' after quoted label")
        }
        return (if(text.length>=2 && text.startsWith('"') && text.endsWith('"'))text.substring(1,text.length-1)else text) to if(text.startsWith('"'))"string"else"text"
    }
    private inner class Chain(private val text:String){
        var at=0
        fun skip(){while(at<text.length && text[at].isWhitespace())at++}
        fun parse(){
            var from=nodeList();val members=from.toMutableList()
            while(true){skip();if(at==text.length)break
                val edge=link();val to=nodeList()
                for((i,a) in from.withIndex())for((j,b) in to.withIndex())edges+=edge.copy(sourceId=a,targetId=b,id=edge.id.takeIf { i==from.lastIndex && j==0 && edges.none { existing->existing.id==edge.id } })
                members.addAll(0,to);from=to
            }
            stack.lastOrNull()?.members?.addAll(members)
        }
        fun nodeList():List<String>{val values=mutableListOf(node());while(true){skip();if(text.getOrNull(at)!='&')break;at++;values+=node()};return values}
        fun node():String {
            skip();val start=at
            while(at<text.length && !text[at].isWhitespace() && text[at] !in "[](){}<>|@\"" && !text.startsWith(":::",at) && !(arrowStart(at) && text[at] !in "ox"))at++
            requireFlow(at>start,"Expected flowchart node")
            val id=text.substring(start,at);requireFlow(id.substringBefore('.').substringBefore('-').substringBefore('/') !in RESERVED,"Reserved flowchart keyword")
            skip();val candidates=SHAPES.filter { text.startsWith(it.first,at) };val longest=candidates.maxOfOrNull { it.first.length };val shape=candidates.filter { it.first.length==longest }.minByOrNull { text.indexOf(it.second,at+it.first.length).let { end -> if(end<0)Int.MAX_VALUE else end } }
            if(shape!=null){at+=shape.first.length;val labelStart=at;var quote=false
                while(at<text.length){if(text[at]=='"')quote=!quote;if(!quote && text.startsWith(shape.second,at))break;at++}
                requireFlow(at<text.length,"Unclosed node label");var raw=text.substring(labelStart,at);var borders:String?=null
                if(shape.third==FlowNodeShape.RECTANGLE && raw.startsWith('|')) {
                    val end=raw.indexOf('|',1);requireFlow(end>1,"Expected node properties")
                    val property=raw.substring(1,end).split(':',limit=2)
                    requireFlow(property.size==2 && property[0]=="borders" && property[1].all { it in "ltrb" },"Unsupported rectangle property")
                    borders=property[1];raw=raw.drop(end+1)
                }
                val label=unquote(raw)
                if(label.second=="text") {requireFlow('(' !in raw,"Unexpected token: got 'PS'");requireFlow(')' !in raw,"Unexpected token: got 'PE'")}
                at+=shape.second.length
                nodes[id]=(nodes[id] ?: FlowNode(id,id)).copy(label=label.first,shape=shape.third,labelType=label.second,borders=borders)
            } else if(id !in nodes && groups.none { it.id==id } && edges.none { it.id==id })nodes[id]=FlowNode(id,id)
            skip()
            if(text.startsWith("@{",at)){at+=2;val begin=at;var quote:Char?=null;var escaped=false
                while(at<text.length){val c=text[at];if(escaped){escaped=false;at++;continue};if(c=='\\' && quote=='"'){escaped=true;at++;continue};if(quote!=null){if(c==quote)quote=null}else if(c=='"' || c=='\'')quote=c else if(c=='}')break;at++}
                requireFlow(at<text.length,"Unclosed metadata");metadata(id,text.substring(begin,at++));skip()
            }
            while(text.startsWith(":::",at)) {
                at+=3;val begin=at;while(at<text.length && !text[at].isWhitespace() && !(arrowStart(at) && text[at] !in "ox") && text[at]!='&')at++
                requireFlow(at>begin,"Expected CSS class name");assignClass(id,text.substring(begin,at))
            }
            return id
        }
        fun arrowStart(index:Int):Boolean = text.startsWith("--",index)||text.startsWith("==",index)||text.startsWith("-.",index)||text.startsWith("~~~",index)||((text.getOrNull(index) in listOf('<','o','x')) && (text.startsWith("--",index+1)||text.startsWith("==",index+1)||text.startsWith("-.",index+1)))
        fun token():String {
            val start=at
            if(text.getOrNull(at) in listOf('<','o','x'))at++
            while(at<text.length && text[at] in "-=.~")at++
            if(text.getOrNull(at) in listOf('>','o','x'))at++
            requireFlow(at>start,"Expected flowchart edge");return text.substring(start,at)
        }
        fun link():FlowEdge {
            skip();var id:String?=null
            if(!arrowStart(at)) {val begin=at;while(at<text.length && text[at]!='@' && !text[at].isWhitespace())at++;requireFlow(text.getOrNull(at)=='@',"Expected edge operator");id=text.substring(begin,at++);requireFlow(id.isNotEmpty(),"Empty edge id")}
            val first=token();var last=first;var label:String?=null;var labelType="text";var left=FlowMarker.NONE
            val full=first.endsWith('>')||first.endsWith('o')||first.endsWith('x')||first.count { it=='-' || it=='=' || it=='~' }>=3||first.contains(".-")
            if(!full){
                left=marker(first.first());val begin=at;var quote=false
                while(at<text.length){if(text[at]=='"')quote=!quote;if(!quote && (if(first.contains('='))text.startsWith("==",at)else if(first.contains('.'))(text[at]=='.' && text.drop(at).takeWhile { it=='.' }.let { dots -> text.getOrNull(at+dots.length)=='-' })else text.startsWith("--",at)))break;at++}
                requireFlow(at<text.length,"Unclosed edge label");val parsed=unquote(text.substring(begin,at));label=parsed.first;labelType=parsed.second;last=token()
            }else left=marker(first.first())
            val right=marker(last.last());if(full && left!=right)left=FlowMarker.NONE;val style=when{last.contains('.') -> FlowEdgeStyle.DOTTED;last.contains('=')->FlowEdgeStyle.THICK;last.contains('~')->FlowEdgeStyle.INVISIBLE;else->FlowEdgeStyle.NORMAL}
            if(!full){val startStyle=when{first.contains('.') -> FlowEdgeStyle.DOTTED;first.contains('=')->FlowEdgeStyle.THICK;else->FlowEdgeStyle.NORMAL};requireFlow(startStyle==style && (left==FlowMarker.NONE || left==right),"Incompatible edge endpoints")}
            skip();if(text.getOrNull(at)=='|'){at++;val begin=at;var quote=false;while(at<text.length){if(text[at]=='"')quote=!quote;if(text[at]=='|' && !quote)break;at++};requireFlow(at<text.length,"Unclosed edge label");val parsed=unquote(text.substring(begin,at++));label=parsed.first;labelType=parsed.second}
            val body=last.dropLast(1).let { if(full && left!=FlowMarker.NONE)it.drop(1)else it }
            val length=if(style==FlowEdgeStyle.DOTTED)body.count { it=='.' }else body.length-1
            requireFlow(length>=1,"Incomplete flowchart edge")
            return FlowEdge("","",style,label,left,right,length,id,labelType)
        }
    }
    private fun marker(c:Char)=when(c){'<','>'->FlowMarker.POINT;'x'->FlowMarker.CROSS;'o'->FlowMarker.CIRCLE;else->FlowMarker.NONE}
    private fun requireFlow(value:Boolean,message:String){if(!value)fail(message)}
    private fun fail(message:String):Nothing=throw FlowSyntaxError(message)
    private class FlowSyntaxError(message:String):Exception(message)
    companion object {
        private val RESERVED=setOf("graph","flowchart","style","linkStyle","interpolate","classDef","class","_self","_blank","_parent","_top","end","subgraph")
        private val SHAPES=listOf(
            Triple("(((",")))",FlowNodeShape.DOUBLE_CIRCLE),Triple("((","))",FlowNodeShape.CIRCLE),Triple("(-","-)",FlowNodeShape.ELLIPSE),Triple("([","])",FlowNodeShape.STADIUM),
            Triple("[[","]]",FlowNodeShape.SUBROUTINE),Triple("[(",")]",FlowNodeShape.CYLINDER),Triple("{{","}}",FlowNodeShape.HEXAGON),
            Triple("[/","\\]",FlowNodeShape.TRAPEZOID),Triple("[\\","/]",FlowNodeShape.TRAPEZOID_ALT),
            Triple("[/","/]",FlowNodeShape.PARALLELOGRAM),Triple("[\\","\\]",FlowNodeShape.PARALLELOGRAM_ALT),
            Triple("[","]",FlowNodeShape.RECTANGLE),Triple("(",")",FlowNodeShape.ROUNDED),Triple("{","}",FlowNodeShape.DIAMOND),Triple(">","]",FlowNodeShape.ASYMMETRIC)
        )
    }
}
