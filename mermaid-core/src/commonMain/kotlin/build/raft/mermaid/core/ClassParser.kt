package build.raft.mermaid.core

/** A cursor preserves class-body lines, quoted identifiers and nested namespaces. */
internal class ClassParser(private val source: String) {
    private var at = 0
    private var statementAt = 0
    private val classes = linkedMapOf<String, ClassDefinition>()
    private val namespaces = linkedMapOf<String, ClassNamespace>()
    private val namespaceStack = mutableListOf<String>()
    private val notes = mutableListOf<ClassNote>()
    private val relationships = mutableListOf<ClassRelationship>()
    private var direction = FlowDirection.TB
    private var accTitle: String? = null
    private var accDescription: String? = null

    fun parse(): MermaidParseResult = try {
        skip()
        val header = readOuter()
        requireSyntax(header.equals("classDiagram", true) || header.equals("classDiagram-v2", true), "Expected classDiagram")
        while (true) {
            skip()
            if (at == source.length) break
            statementAt = at
            if (source[at] == '}') {
                requireSyntax(namespaceStack.isNotEmpty(), "Unexpected namespace terminator")
                namespaceStack.removeAt(namespaceStack.lastIndex); at++; continue
            }
            val text = readOuter()
            when {
                text == "note" || text.startsWith("note ") -> note(text.drop(4).trim())
                text.startsWith("namespace ") -> namespace(text.drop(10).trim())
                text.startsWith("class ") -> declaration(text.drop(6).trim())
                text.startsWith("direction ") -> direction = FlowDirection.entries.firstOrNull { it.name == text.drop(10).trim() }
                    ?: fail("Unsupported class direction")
                text.startsWith("accTitle:") -> accTitle = text.substringAfter(':').trim()
                text.startsWith("accDescr:") -> accDescription = text.substringAfter(':').trim()
                text == "accDescr" && source.getOrNull(at) == '{' -> {
                    at++; val start=at
                    while(at<source.length && source[at]!='}') at++
                    requireSyntax(at<source.length,"Unclosed accessibility description")
                    accDescription=source.substring(start,at++).trim()
                }
                text.startsWith("<<") -> {
                    val end=text.indexOf(">>")
                    requireSyntax(end>2,"Invalid annotation")
                    val id=reference(text.drop(end+2).trim())
                    addMember(id,"<<${text.substring(2,end)}>>")
                }
                else -> relationOrMember(text)
            }
        }
        requireSyntax(namespaceStack.isEmpty(), "Unclosed class namespace")
        MermaidParseResult.Success(ClassDiagram(classes.values.toList(), relationships.toList(), notes.toList(), namespaces.values.toList(), direction, accTitle, accDescription))
    } catch(error: ClassSyntaxError) {
        val prefix=source.take(statementAt)
        MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,error.message ?: "Unsupported class syntax",
            SourceLocation(prefix.count { it=='\n' }+1,statementAt-prefix.lastIndexOf('\n')))))
    }

    private fun note(text:String) {
        var body=text
        var owner:String?=null
        if(body.startsWith("for ")) {
            body=body.drop(4).trim()
            val name=readName(body); owner=name.first; body=body.drop(name.second).trim()
        }
        requireSyntax(body.length>=2 && body.startsWith('"') && body.endsWith('"'),"Expecting 'STR' after note")
        notes += ClassNote(body.substring(1,body.length-1),owner,namespaceStack.lastOrNull())
    }

    private fun namespace(text: String) {
        val name=readName(text)
        val suffix=text.drop(name.second).trim()
        val label=if(suffix.isEmpty()) null else quotedLabel(suffix)
        requireSyntax(source.getOrNull(at)=='{',"Expected namespace body")
        at++
        val id=namespaceStack.lastOrNull()?.let { "$it.${name.first}" } ?: name.first
        val parts=id.split('.')
        parts.indices.forEach { index ->
            val current=parts.take(index+1).joinToString(".")
            val leaf=index==parts.lastIndex
            val old=namespaces[current]
            namespaces[current]=old?.copy(label=if(leaf && label!=null) label else old.label,explicit=old.explicit || leaf)
                ?: ClassNamespace(current,if(leaf && label!=null) label else parts[index],if(index==0) null else parts.take(index).joinToString("."),leaf)
        }
        namespaceStack+=id
    }

    private fun declaration(text: String) {
        val name=readName(text)
        val id=ensure(name.first)
        var suffix=text.drop(name.second).trim()
        if(suffix.startsWith('[')) {
            val end=suffix.lastIndexOf(']')
            requireSyntax(end>=0,"Unclosed class label")
            classes[id]=classes.getValue(id).copy(label=quotedLabel(suffix.take(end+1)))
            suffix=suffix.drop(end+1).trim()
        }
        if(suffix.startsWith("<<") && suffix.endsWith(">>")) {
            addMember(id,suffix); suffix=""
        }
        requireSyntax(suffix.isEmpty(),"Unsupported class declaration suffix")
        namespaceStack.lastOrNull()?.let { classes[id]=classes.getValue(id).copy(namespaceName=it) }
        if(source.getOrNull(at)=='{') {
            at++
            while(true) {
                requireSyntax(at<source.length,"Unclosed class member block")
                if(source[at]=='}') { at++; break }
                val start=at
                while(at<source.length && source[at]!='\n' && source[at]!='}') {
                    requireSyntax(source[at]!='{',"Unexpected class member block")
                    at++
                }
                val member=source.substring(start,at).trim()
                if(member.isNotEmpty() && !member.startsWith("%%")) addMember(id,member)
                if(source.getOrNull(at)=='\n') at++
            }
        }
    }

    private fun relationOrMember(text: String) {
        val name=readName(text)
        var rest=text.drop(name.second).trim()
        if(rest.isEmpty()) return // Bare names are no-op statements in the upstream grammar.
        val fromName=name.first
        if(rest.startsWith(':') && !rest.startsWith(":::")) { addMember(ensure(fromName),rest.drop(1).trim()); return }
        var fromCard: String?=null
        if(rest.startsWith('"')) { val end=rest.indexOf('"',1); requireSyntax(end>=0,"Unclosed cardinality"); fromCard=rest.substring(1,end);rest=rest.drop(end+1).trim() }
        val match=RELATION.find(rest) ?: fail("Unsupported class relationship")
        val left=marker(match.groupValues[1]);val right=marker(match.groupValues[3]);val dashed=match.groupValues[2]==".."
        rest=rest.drop(match.value.length).trim()
        var toCard: String?=null
        if(rest.startsWith('"')) { val end=rest.indexOf('"',1); requireSyntax(end>=0,"Unclosed cardinality");toCard=rest.substring(1,end);rest=rest.drop(end+1).trim() }
        val target=readName(rest)
        rest=rest.drop(target.second).trim()
        requireSyntax(rest.isEmpty() || rest.startsWith(':'),"Unexpected relationship suffix")
        val kind=when {
            left==ClassMarker.INHERITANCE || right==ClassMarker.INHERITANCE -> if(dashed) ClassRelationshipKind.REALIZATION else ClassRelationshipKind.INHERITANCE
            left==ClassMarker.COMPOSITION || right==ClassMarker.COMPOSITION -> ClassRelationshipKind.COMPOSITION
            left==ClassMarker.AGGREGATION || right==ClassMarker.AGGREGATION -> ClassRelationshipKind.AGGREGATION
            left==ClassMarker.ARROW || right==ClassMarker.ARROW -> if(dashed) ClassRelationshipKind.DEPENDENCY else ClassRelationshipKind.ASSOCIATION
            else -> if(dashed) ClassRelationshipKind.DASHED_ASSOCIATION else ClassRelationshipKind.LINK
        }
        relationships+=ClassRelationship(ensure(fromName),ensure(target.first),kind,rest.takeIf { it.isNotEmpty() }?.drop(1)?.trim(),fromCard,toCard,left,right,dashed)
    }

    private fun addMember(id: String, value: String) {
        requireSyntax(value.isNotEmpty(),"Expected class member")
        val klass=classes.getValue(id)
        if(value.startsWith("<<") && value.endsWith(">>")) {
            classes[id]=klass.copy(annotations=klass.annotations+value.substring(2,value.length-2));return
        }
        val marker=value.first()
        val explicit=marker in "+-#~"
        val signature=if(explicit) value.drop(1).trim() else value
        requireSyntax(signature.isNotEmpty(),"Class member visibility requires a signature")
        val visibility=when(marker) {'-'->ClassVisibility.PRIVATE;'#'->ClassVisibility.PROTECTED;'~'->ClassVisibility.PACKAGE;else->ClassVisibility.PUBLIC}
        classes[id]=klass.copy(members=klass.members+ClassMember(signature,visibility,explicit))
    }
    private fun ensure(raw: String): String {
        val id=raw.substringBefore('~')
        if(id !in classes) classes[id]=ClassDefinition(id,genericType=raw.takeIf { '~' in it }?.substringAfter('~')?.substringBeforeLast('~'))
        return id
    }
    private fun reference(text: String): String { val name=readName(text); requireSyntax(text.drop(name.second).isBlank(),"Unexpected class identifier suffix");return ensure(name.first) }
    private fun readName(text: String): Pair<String,Int> {
        requireSyntax(text.isNotEmpty(),"Expected class identifier")
        var end=0
        val id=if(text[0]=='`') {
            end=text.indexOf('`',1);requireSyntax(end>1,"Unclosed class identifier")
            text.substring(1,end++)
        } else {
            while(end<text.length && (text[end].isLetterOrDigit() || text[end] in "_.-")) {
                if(text.startsWith("--",end) || text.startsWith("..",end)) break
                end++
            }
            requireSyntax(end>0,"Invalid class identifier")
            text.take(end)
        }
        if(text.getOrNull(end)=='~') {
            val start=++end
            while(end<text.length && text[end]!='~')end++
            requireSyntax(end<text.length,"Unclosed generic type")
            return "$id~${text.substring(start,end++)}~" to end
        }
        return id to end
    }
    private fun quotedLabel(text: String): String {
        requireSyntax(text.startsWith("[\"") && text.endsWith("\"]"),"Expected quoted class label")
        return text.substring(2,text.length-2)
    }
    private fun readOuter(): String {
        val start=at;var quote:Char?=null
        val metadata=source.startsWith("accTitle:",at) || source.startsWith("accDescr:",at)
        while(at<source.length) {
            val c=source[at]
            if(quote!=null) { if(c==quote)quote=null }
            else if(c=='`' || c=='"') quote=c
            else if(c=='\n' || (!metadata && (c in ";{}" || source.startsWith("%%",at)))) break
            at++
        }
        requireSyntax(quote==null,"Unclosed quoted text")
        return source.substring(start,at).trim()
    }
    private fun skip() {
        while(at<source.length) {
            if(source[at].isWhitespace() || source[at]==';')at++
            else if(source.startsWith("%%",at)) { while(at<source.length && source[at]!='\n')at++ }
            else break
        }
    }
    private fun marker(text: String)=when(text.trim()) { "<|","|>"->ClassMarker.INHERITANCE;"*"->ClassMarker.COMPOSITION;"o"->ClassMarker.AGGREGATION;"<",">"->ClassMarker.ARROW;"()"->ClassMarker.LOLLIPOP;else->ClassMarker.NONE }
    private fun requireSyntax(condition:Boolean,message:String){if(!condition)fail(message)}
    private fun fail(message:String):Nothing=throw ClassSyntaxError(message)
    private class ClassSyntaxError(message:String):Exception(message)
    companion object { private val RELATION=Regex("^(<\\||\\|>|[o*<>]|\\(\\))?\\s*(--|\\.\\.)\\s*(<\\||\\|>|[o*<>]|\\(\\))?") }
}
