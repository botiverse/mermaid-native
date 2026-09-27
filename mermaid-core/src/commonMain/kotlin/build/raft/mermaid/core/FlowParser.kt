package build.raft.mermaid.core

/** Flow tokens retain quoted labels instead of splitting arrows inside node text. */
internal class FlowParser(private val source:String) {
    private val nodes=linkedMapOf<String,FlowNode>()
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
                    requireFlow(id.isNotBlank(),"Expected subgraph name");requireFlow(stack.none { it.id==id } && groups.none { it.id==id },"Duplicate or cyclic subgraph id");stack+=Group(id,label,if(unquote(if(bracket>=0)raw.substring(bracket+1,raw.length-1)else raw).second=="markdown")"markdown"else"text")
                }
                text=="end" -> {
                    requireFlow(stack.isNotEmpty(),"Unexpected end")
                    val group=stack.removeAt(stack.lastIndex)
                    groups+=FlowSubgraph(group.id,group.label,group.members.distinct(),group.direction,stack.lastOrNull()?.id,group.labelType)
                    stack.lastOrNull()?.members?.add(group.id)
                }
                text.startsWith("direction ") -> {val value=parseDirection(text.drop(10).trim());if(stack.isEmpty())direction=value else stack.last().direction=value}
                text.startsWith("accTitle:") -> accTitle=text.substringAfter(':').trim()
                text.startsWith("accDescr:") -> accDescription=text.substringAfter(':').trim()
                text.startsWith("accDescr") && text.contains('{') -> {requireFlow(text.endsWith('}'),"Unclosed accessibility description");accDescription=text.substringAfter('{').dropLast(1).trim()}
                text.startsWith("style ") || text.startsWith("class ") || text.startsWith("classDef ") || text.startsWith("click ") || text.startsWith("linkStyle ") -> fail("Unsupported flowchart style or interaction")
                else -> Chain(text).parse()
            }
        }
        requireFlow(stack.isEmpty(),"Unclosed subgraph")
        MermaidParseResult.Success(FlowchartDiagram(direction,nodes.values.toList(),edges.toList(),groups.toList(),accTitle,accDescription))
    } catch(e:FlowSyntaxError) {
        val prefix=source.take(offset)
        MermaidParseResult.Failure(listOf(MermaidDiagnostic(if(parsingHeader)MermaidDiagnosticCode.INVALID_HEADER else MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,e.message?:"Unsupported flowchart syntax",SourceLocation(prefix.count { it=='\n' }+1,offset-prefix.lastIndexOf('\n')))))
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
        requireFlow(!quote && closes.isEmpty(),"Unclosed flowchart label");emit(source.length);return out
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
                for((i,a) in from.withIndex())for((j,b) in to.withIndex())edges+=edge.copy(sourceId=a,targetId=b,id=edge.id.takeIf { i==from.lastIndex && j==0 })
                members.addAll(0,to);from=to
            }
            stack.lastOrNull()?.members?.addAll(members)
        }
        fun nodeList():List<String>{val values=mutableListOf(node());while(true){skip();if(text.getOrNull(at)!='&')break;at++;values+=node()};return values}
        fun node():String {
            skip();val start=at
            while(at<text.length && !text[at].isWhitespace() && text[at] !in "[](){}<>|@\"" && !text.startsWith(":::",at) && !arrowStart(at))at++
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
                nodes[id]=FlowNode(id,label.first,shape.third,label.second,borders)
            } else if(id !in nodes)nodes[id]=FlowNode(id,id)
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
