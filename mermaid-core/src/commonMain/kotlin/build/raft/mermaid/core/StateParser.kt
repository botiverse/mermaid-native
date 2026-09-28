package build.raft.mermaid.core

/** State grammar keeps composite boundaries and multiline notes before tokenization. */
internal class StateParser(private val source: String) {
    private val nodes = linkedMapOf<String, StateNode>()
    private val definitions = linkedMapOf<String,List<String>>()
    private val transitions = mutableListOf<StateTransition>()
    private val notes = mutableListOf<StateNote>()
    private val stack = mutableListOf<String>()
    private var direction = FlowDirection.TB
    private var pseudoIndex = 0
    private var at = 0
    private var statementAt = 0
    private var title: String? = null
    private var description: String? = null

    fun parse(): MermaidParseResult = try {
        var header = read()
        while (header.isEmpty() && at < source.length) header = read()
        requireState(header.lowercase() in listOf("statediagram", "statediagram-v2"), "Expected stateDiagram header")
        while (at < source.length) {
            val text = read()
            if (text.isEmpty()) continue
            when {
                text == "}" -> { requireState(stack.isNotEmpty(), "Unexpected composite end"); stack.removeAt(stack.lastIndex) }
                text.startsWith("accTitle:") -> title = text.substringAfter(':').trim()
                text.startsWith("accDescr:") -> description = text.substringAfter(':').trim()
                text.startsWith("accDescr") && text.endsWith('{') -> {
                    val end = source.indexOf('}', at); requireState(end >= at, "Unclosed accessibility description")
                    description = source.substring(at,end).trim().lines().joinToString("\n") { it.trim() };at=end+1
                }
                text.startsWith("direction ") -> {
                    val value=text.substringAfter(' ').trim();val parsed=FlowDirection.entries.firstOrNull { it.name==value } ?: fail("Invalid state direction")
                    if(stack.isEmpty()) direction=parsed else nodes[stack.last()]=nodes.getValue(stack.last()).copy(direction=parsed)
                }
                text == "hide empty description" -> Unit
                text.startsWith("scale ") -> requireState(text.removePrefix("scale ").trim().removeSuffix("width").trim().let { it.isNotEmpty() && it.all(Char::isDigit) },"Expected numeric scale width") // Upstream grammar emits no state statement for these directives.
                text.startsWith("classDef ") -> {
                    val rest=text.drop(9).trim();val name=rest.takeWhile { !it.isWhitespace() }
                    requireState(name.isNotBlank(),"Expected class name");definitions[name]=styleValues(rest.drop(name.length).trim())
                }
                text.startsWith("class ") -> {
                    val rest=text.drop(6).trim();val split=rest.lastIndexOf(' ');requireState(split>0,"Expected state class")
                    val css=rest.drop(split+1).trim();rest.take(split).split(',').forEach { id->val key=id.trim();validateId(key,false);register(key);nodes[key]=nodes.getValue(key).copy(classes=nodes.getValue(key).classes+css) }
                }
                text.startsWith("style ") -> {
                    val rest=text.drop(6).trim();val split=rest.indexOf(' ');requireState(split>0,"Expected state style")
                    val values=styleValues(rest.drop(split+1));rest.take(split).split(',').forEach { id->validateId(id,false);register(id);nodes[id]=nodes.getValue(id).copy(styles=values) }
                }
                text.startsWith("note ") -> note(text)
                text.endsWith('{') -> {
                    val declaration=text.dropLast(1).trim();requireState(declaration.startsWith("state "),"Expected composite state")
                    val id=state(declaration.drop(6).trim(),composite=true)
                    requireState(id !in stack,"Cyclic composite state");stack+=id
                }
                "-->" in text -> {
                    val split=text.indexOf("-->");val from=endpoint(text.take(split).trim(),true)
                    val target=text.drop(split+3).trim();val colon=descriptionColon(target);val targetText=if(colon<0)target else target.take(colon).trim();val tokens=targetText.split(Regex("\\s+"));val to=endpoint(tokens.first(),false);tokens.drop(1).forEach { state(it) }
                    transitions+=StateTransition(from,to,if(colon<0)""else target.drop(colon+1).trim())
                }
                else -> if(!text.startsWith("state ") && descriptionColon(text)<0 && !text.startsWith('"'))text.split(Regex("\\s+")).forEach { state(it) } else state(text.removePrefix("state "))
            }
        }
        requireState(stack.isEmpty(),"Unclosed composite state")
        MermaidParseResult.Success(StateDiagram(direction,nodes.values.toList(),transitions.toList(),notes.toList(),title,description,definitions.toMap()))
    } catch(e:StateSyntaxError) {
        val prefix=source.take(statementAt)
        MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,e.message ?: "Unsupported state syntax",SourceLocation(prefix.count { it=='\n' }+1,statementAt-prefix.lastIndexOf('\n')))))
    }

    private fun register(id:String,label:String?=null,kind:StateNodeKind=StateNodeKind.STATE,declared:Boolean=false):String {
        val old=nodes[id]
        nodes[id]=(old ?: StateNode(id,id,kind)).copy(label=label ?: old?.label ?: if(kind==StateNodeKind.STATE)id else "",kind=if(old?.kind!=null && kind==StateNodeKind.STATE)old.kind else kind,explicitLabel=old?.explicitLabel==true || (label!=null && kind==StateNodeKind.STATE),declared=old?.declared==true || declared)
        stack.lastOrNull()?.let { parent -> val node=nodes.getValue(parent);if(id!=parent && id !in node.childIds)nodes[parent]=node.copy(childIds=node.childIds+id) }
        return id
    }
    private fun endpoint(text:String,sourceEndpoint:Boolean):String {
        val raw=text.substringBefore(":::");val css=text.substringAfter(":::","")
        val id=if(raw=="[*]") {
            val kind=if(sourceEndpoint)StateNodeKind.START else StateNodeKind.END
            register("__${kind.name.lowercase()}_${pseudoIndex++}","",kind)
        }else state(raw,declared=false)
        if(css.isNotEmpty())nodes[id]=nodes.getValue(id).copy(classes=nodes.getValue(id).classes+css)
        return id
    }
    private fun descriptionColon(text:String)=text.indices.firstOrNull { text[it]==':' && text.getOrNull(it-1)!=':' && text.getOrNull(it+1)!=':' } ?: -1
    private fun styleValues(text:String):List<String> {
        val values=text.split(',').map { it.trim() };requireState(values.all { it.contains(':') && it.substringAfter(':').isNotBlank() },"Invalid state style");return values
    }
    private fun state(raw:String,composite:Boolean=false,declared:Boolean=true):String {
        val text=raw.trim()
        if(":::" in text && !text.startsWith('"')) {
            val id=state(text.substringBefore(":::"),composite,declared)
            val css=text.substringAfter(":::").trim();requireState(css.isNotEmpty(),"Expected state class")
            nodes[id]=nodes.getValue(id).copy(classes=nodes.getValue(id).classes+css);return id
        }
        if(text.startsWith('"')) {
            val end=text.indexOf('"',1);requireState(end>0,"Unclosed state label")
            val rest=text.drop(end+1).trim();requireState(rest.startsWith("as "),"Expected state alias")
            val id=rest.drop(3).trim();validateId(id,composite);return register(id,text.substring(1,end),declared=declared)
        }
        val pseudo=text.indexOf("<<")
        if(pseudo>=0){val id=text.take(pseudo).trim();validateId(id,composite);val type=text.substring(pseudo).trim();return register(id,declared=declared,kind=when(type){"<<choice>>"->StateNodeKind.CHOICE;"<<fork>>"->StateNodeKind.FORK;"<<join>>"->StateNodeKind.JOIN;else->fail("Unsupported state stereotype")})}
        val colon=text.indexOf(':');val id=if(colon<0)text else text.take(colon).trim();validateId(id,composite);register(id,declared=declared)
        if(colon>=0){val value=text.drop(colon+1).trim();val old=nodes.getValue(id);nodes[id]=old.copy(description=listOfNotNull(old.description,value).joinToString("\n"))}
        return id
    }
    private fun validateId(id:String,composite:Boolean) {
        requireState(id.isNotBlank() && id.none { it.isWhitespace() || it in "{};:\"" },if(composite)"Error: State name must be a single word."else"Expected single state identifier")
        requireState(id !in setOf("classDef","class","style","note","end","--","=="),"Unsupported state statement")
    }
    private fun note(text:String) {
        val left=text.startsWith("note left of ");val right=text.startsWith("note right of ")
        requireState(left || right,"Unsupported floating note")
        val rest=text.drop(if(left)13 else 14).trim();val colon=rest.indexOf(':');val id=if(colon<0)rest else rest.take(colon).trim();validateId(id,false);register(id)
        val value=if(colon>=0)rest.drop(colon+1).trim()else {
            val begin=at;var end=-1
            while(at<source.length){val lineStart=at;val newline=source.indexOf('\n',at).let { if(it<0)source.length else it };val line=source.substring(at,newline).trim();at=newline+if(newline<source.length)1 else 0;if(line=="end note"){end=lineStart;break}}
            requireState(end>=0,"Unclosed state note");source.substring(begin,end).trim()
        }
        notes+=StateNote(id,if(left)StateNotePosition.LEFT_OF else StateNotePosition.RIGHT_OF,value)
    }
    private fun read():String {
        while(at<source.length && (source[at].isWhitespace() || source[at]==';'))at++
        statementAt=at
        if(at>=source.length)return ""
        if(source[at]=='}'){at++;return "}"}
        val begin=at;var quote=false
        while(at<source.length){val c=source[at]
            if(c=='"')quote=!quote
            if(!quote){
                if(at>begin && source.startsWith("class ",at) && source[at-1].isWhitespace() && "-->" in source.substring(begin,at) && descriptionColon(source.substring(begin,at))<0)return source.substring(begin,at).trim()
                if(source.startsWith("%%",at)){val result=source.substring(begin,at).trim();while(at<source.length && source[at]!='\n')at++;return result}
                if(c=='{' ){at++;return source.substring(begin,at).trim()}
                if(c=='}')return source.substring(begin,at).trim()
                if(c=='\n' || c==';'){val result=source.substring(begin,at).trim();at++;return result}
            };at++
        }
        requireState(!quote,"Unclosed state string");return source.substring(begin,at).trim()
    }
    private fun requireState(value:Boolean,message:String){if(!value)fail(message)}
    private fun fail(message:String):Nothing=throw StateSyntaxError(message)
    private class StateSyntaxError(message:String):Exception(message)
}
