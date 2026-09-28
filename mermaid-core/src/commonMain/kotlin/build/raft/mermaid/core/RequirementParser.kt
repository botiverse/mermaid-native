package build.raft.mermaid.core

/** Requirement blocks retain optional fields and styling; names need not be identifiers. */
internal class RequirementParser(private val source: String) {
    private val requirements=linkedMapOf<String,RequirementDefinition>()
    private val elements=linkedMapOf<String,RequirementElement>()
    private val edges=mutableListOf<RequirementRelationship>()
    private val definitions=linkedMapOf<String,List<String>>()
    private var direction:FlowDirection?=null
    private var accTitle:String?=null
    private var accDescription:String?=null
    private var line=1
    fun parse():MermaidParseResult = try {
        val lines=source.lines();var index=0;var header=false
        while(index<lines.size){line=index+1;val text=lines[index++].trim();if(text.isBlank()||text.startsWith("%%")||text.startsWith('#'))continue
            if(!header){check(text.equals("requirementDiagram",true),"Expected requirementDiagram");header=true;continue}
            when {
                text.startsWith("accTitle:",true)->accTitle=text.substringAfter(':').trim()
                text.startsWith("accDescr:",true)->accDescription=text.substringAfter(':').trim()
                text.startsWith("accDescr",true)&&'{' in text->{
                    val parts=mutableListOf<String>();var part=text.substringAfter('{');var closed=false
                    while(true){if('}' in part){parts+=part.substringBefore('}');closed=true;break};parts+=part;if(index>=lines.size)break;part=lines[index++]}
                    check(closed,"Unclosed accessibility description");accDescription=parts.joinToString("\n").trim()
                }
                text.startsWith("direction ",true)->direction=runCatching{FlowDirection.valueOf(text.substringAfter(' ').trim().uppercase())}.getOrNull() ?: fail("Invalid requirement direction")
                text.startsWith("classDef ",true)->{val (ids,styles)=arguments(text.substringAfter(' '));for(id in ids.split(','))definitions[id]=definitions[id].orEmpty()+styles.split(',').filter(String::isNotBlank)}
                text.startsWith("class ",true)->{val (ids,classes)=arguments(text.substringAfter(' '));for(id in ids.split(','))assign(id,classes.split(','),true)}
                text.startsWith("style ",true)->{val (ids,styles)=arguments(text.substringAfter(' '));for(id in ids.split(','))assign(id,styles.split(',').filter(String::isNotBlank),false)}
                text.endsWith('{')->{
                    val type=text.substringBefore(' ').lowercase();val raw=text.substringAfter(' ').dropLast(1).trim();val name=unquote(raw.substringBefore(":::"));val classes=if(":::" in raw)raw.substringAfter(":::").split(',')else emptyList()
                    check(name.isNotEmpty(),"Empty requirement name");val fields=linkedMapOf<String,String>();var closed=false
                    while(index<lines.size){line=index+1;val field=lines[index++].trim();if(field=="}"){closed=true;break};if(field.isEmpty()||field.startsWith("%%")||field.startsWith('#'))continue
                        val colon=field.indexOf(':');check(colon>0,"Expected requirement field");val key=field.take(colon).lowercase();val value=unquote(field.drop(colon+1).trim());val allowed=if(type=="element")setOf("type","docref")else setOf("id","text","risk","verifymethod");check(key in allowed,"Unknown requirement field");if(key !in fields)fields[key]=value
                    }
                    check(closed,"Unclosed requirement block")
                    if(type=="element"){if(name !in elements)elements[name]=RequirementElement(name,fields["type"].orEmpty(),fields["docref"].orEmpty(),listOf("default")+classes)}else{
                        val kind=when(type){"requirement"->RequirementType.REQUIREMENT;"functionalrequirement"->RequirementType.FUNCTIONAL_REQUIREMENT;"interfacerequirement"->RequirementType.INTERFACE_REQUIREMENT;"performancerequirement"->RequirementType.PERFORMANCE_REQUIREMENT;"physicalrequirement"->RequirementType.PHYSICAL_REQUIREMENT;"designconstraint"->RequirementType.DESIGN_CONSTRAINT;else->fail("Unknown requirement type")}
                        val risk=fields["risk"]?.let{runCatching{RequirementRisk.valueOf(it.uppercase())}.getOrNull() ?: fail("Invalid requirement risk")} ?: RequirementRisk.UNSPECIFIED
                        val method=fields["verifymethod"]?.let{runCatching{RequirementVerifyMethod.valueOf(it.uppercase())}.getOrNull() ?: fail("Invalid verification method")} ?: RequirementVerifyMethod.UNSPECIFIED
                        if(name !in requirements)requirements[name]=RequirementDefinition(name,fields["id"].orEmpty(),fields["text"].orEmpty(),risk,method,kind,listOf("default")+classes)
                    }
                }
                else->{
                    val forward=Regex("^(.+?)\\s*-\\s*(contains|copies|derives|satisfies|verifies|refines|traces)\\s*->\\s*(.+)$",RegexOption.IGNORE_CASE).matchEntire(text)
                    val backward=Regex("^(.+?)\\s*<-\\s*(contains|copies|derives|satisfies|verifies|refines|traces)\\s*-\\s*(.+)$",RegexOption.IGNORE_CASE).matchEntire(text)
                    val match=forward?:backward
                    if(match!=null){val a=unquote(match.groupValues[1].trim());val b=unquote(match.groupValues[3].trim());edges+=RequirementRelationship(if(forward!=null)a else b,if(forward!=null)b else a,RequirementRelationshipKind.valueOf(match.groupValues[2].uppercase()))}
                    else if(":::" in text)assign(unquote(text.substringBefore(":::").trim()),text.substringAfter(":::").split(','),true)
                    else fail("Unsupported requirementDiagram syntax")
                }
            }
        }
        MermaidParseResult.Success(RequirementDiagram(requirements.values.toList(),elements.values.toList(),edges.toList(),accTitle,accDescription,direction,definitions.toMap()))
    }catch(e:RequirementSyntaxError){MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,e.message?:"Invalid requirement",SourceLocation(line,1))))}
    private fun arguments(text:String):Pair<String,String>{val split=text.indexOfFirst(Char::isWhitespace);return if(split<0)text to "" else text.take(split) to text.drop(split).trim()}
    private fun assign(id:String,values:List<String>,classes:Boolean){val name=id.trim();requirements[name]?.let{requirements[name]=if(classes)it.copy(classes=it.classes+values)else it.copy(styles=it.styles+values)};elements[name]?.let{elements[name]=if(classes)it.copy(classes=it.classes+values)else it.copy(styles=it.styles+values)}}
    private fun unquote(value:String):String {if(value.startsWith('"')){check(value.length>=2&&value.endsWith('"'),"Unclosed requirement string");return value.substring(1,value.lastIndex)};return value}
    private fun check(ok:Boolean,message:String){if(!ok)fail(message)}
    private fun fail(message:String):Nothing=throw RequirementSyntaxError(message)
    private class RequirementSyntaxError(message:String):Exception(message)
}
