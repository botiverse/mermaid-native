package build.raft.mermaid.core

/** Raw lines preserve semicolons and hashes in Gantt labels. Dates use an exclusive end. */
internal class GanttParser(private val source:String) {
    private data class Pending(val name:String,val id:String,val start:String?,val end:String,val section:Int,val tags:Set<String>,val line:Int)
    private val pending=mutableListOf<Pending>()
    private val sectionNames=mutableListOf<String>()
    private var title:String?=null
    private var format="YYYY-MM-DD"
    private var accTitle:String?=null
    private var accDescription:String?=null
    private val excludes=mutableListOf<String>()
    private val includes=mutableListOf<String>()
    private var inclusive=false
    private var today:String?=null
    private var axis:String?=null
    private var tick:String?=null
    private var weekday="sunday"
    private var weekend="saturday"
    private var display:String?=null
    private val interactions=mutableListOf<FlowInteraction>()
    private var line=1
    private var generated=0
    private val days=listOf("sunday","monday","tuesday","wednesday","thursday","friday","saturday")
    fun parse():MermaidParseResult = try {
        val lines=source.lines();var index=0;var header=false
        while(index<lines.size){line=index+1;val text=lines[index++].trim();if(text.isEmpty() || text.startsWith("%%"))continue
            if(!header){requireGantt(text.equals("gantt",true),"Expected gantt header");header=true;continue}
            fun value()=text.substringAfter(' ').trim()
            when {
                text.startsWith("title ")->title=value()
                text.startsWith("dateFormat ")->format=value()
                text.startsWith("accTitle:")->accTitle=text.substringAfter(':').trim()
                text.startsWith("accDescr:")->accDescription=text.substringAfter(':').trim()
                text.startsWith("accDescr") && '{' in text->{
                    val content=mutableListOf<String>();var current=text.substringAfter('{');var closed=false
                    while(true){if('}' in current){content+=current.substringBefore('}');closed=true;break};content+=current;if(index>=lines.size)break;current=lines[index++]}
                    requireGantt(closed,"Unclosed accessibility description");accDescription=content.joinToString("\n").trim().lines().joinToString("\n"){it.trim()}
                }
                text.startsWith("section ")->sectionNames+=value()
                text=="inclusiveEndDates"->inclusive=true
                text.startsWith("excludes ")->excludes+=value().split(Regex("[,\\s]+")).filter(String::isNotEmpty)
                text.startsWith("includes ")->includes+=value().split(Regex("[,\\s]+")).filter(String::isNotEmpty)
                text.startsWith("todayMarker ")->today=value()
                text.startsWith("axisFormat ")->axis=value()
                text.startsWith("tickInterval ")->tick=value()
                text.startsWith("weekday ")->{weekday=value();requireGantt(weekday in days,"Invalid weekday")}
                text.startsWith("weekend ")->{weekend=value();requireGantt(weekend in days,"Invalid weekend")}
                text.startsWith("displayMode ")->display=value()
                text.startsWith("click ")->click(text.drop(6))
                else -> task(text)
            }
        }
        val byId=pending.associateBy { it.id };requireGantt(byId.size==pending.size,"Duplicate Gantt task id")
        val resolved=mutableMapOf<String,GanttTask>();val active=mutableSetOf<String>()
        fun resolve(id:String):GanttTask {
            resolved[id]?.let { return it };val task=byId[id] ?: fail("Unknown Gantt dependency $id")
            line=task.line;requireGantt(active.add(id),"Cyclic Gantt dependency")
            fun reference(text:String,end:Boolean):Long {
                val ids=text.substringAfter(' ').trim().split(Regex("\\s+"));requireGantt(ids.isNotEmpty(),"Expected Gantt dependency")
                return ids.map { dependency -> val d=resolve(dependency);if(end)d.startEpochMillis+d.durationMillis else d.startEpochMillis }.let { if(end)it.max()else it.min() }
            }
            val start=when {
                task.start==null -> pending.indexOf(task).takeIf { it>0 }?.let { val p=resolve(pending[it-1].id);p.startEpochMillis+p.durationMillis } ?: fail("First Gantt task requires start date")
                task.start.startsWith("after ")->reference(task.start,true)
                else -> date(task.start) ?: fail("Invalid Gantt start date")
            }
            val duration=GanttDuration.parse(task.end).takeIf { it.amount.isFinite() }
            var end=when {
                task.end.startsWith("until ")->reference(task.end,false)
                duration!=null->{
                    val unitMillis = when(duration.unit) { "ms" -> 1L; "s" -> 1000L; "m" -> 60_000L; "h" -> 3_600_000L; "d" -> GANTT_DAY_MILLIS; "w" -> 7 * GANTT_DAY_MILLIS; else -> fail("Calendar month/year durations are not supported") }
                    val millis = when(duration.unit) {
                        "d" -> kotlin.math.floor(duration.amount + .5) * GANTT_DAY_MILLIS
                        "w" -> kotlin.math.floor(duration.amount * 7 + .5) * GANTT_DAY_MILLIS
                        else -> duration.amount * unitMillis
                    }
                    requireGantt(millis.isFinite() && millis >= 0 && millis <= 100000.0 * GANTT_DAY_MILLIS,"Gantt duration out of range")
                    start + millis.toLong()
                }
                else -> (date(task.end) ?: fail("Invalid Gantt end date")) + if(inclusive)GANTT_DAY_MILLIS else 0L
            }
            // Upstream advances from the day after start, including the end boundary.
            // Explicit ISO end dates stay fixed; computed duration/until ends may extend.
            var renderEnd:Long?=null
            if(excludes.isNotEmpty() && (duration != null || task.end.startsWith("until "))){
                var cursor=start+GANTT_DAY_MILLIS;val limit=end+10000*GANTT_DAY_MILLIS;var previousExcluded=false
                while(cursor<=end){if(!previousExcluded)renderEnd=end;previousExcluded=excluded(ganttFloorDay(cursor));if(previousExcluded)end+=GANTT_DAY_MILLIS;requireGantt(end<=limit,"Excluded calendar has no working days");cursor+=GANTT_DAY_MILLIS}
            }
            requireGantt(end>=start,"Gantt end precedes start")
            val statuses=task.tags.mapNotNull { when(it){"done"->GanttTaskStatus.DONE;"active"->GanttTaskStatus.ACTIVE;"crit"->GanttTaskStatus.CRITICAL;else->null} }.toSet()
            val status=listOf(GanttTaskStatus.CRITICAL,GanttTaskStatus.DONE,GanttTaskStatus.ACTIVE).firstOrNull { it in statuses } ?: GanttTaskStatus.TODO
            val durationMillis = end - start
            val renderDurationMillis = (renderEnd ?: end) - start
            val result = GanttTask(
                name = task.name, id = task.id, startDay = ganttFloorDay(start),
                durationDays = (durationMillis / GANTT_DAY_MILLIS).toInt(),
                status = status, statuses = statuses, milestone = "milestone" in task.tags,
                renderDurationDays = (renderDurationMillis / GANTT_DAY_MILLIS).toInt(),
                startEpochMillis = start, durationMillis = durationMillis,
                renderDurationMillis = renderDurationMillis,
            )
            active.remove(id);resolved[id]=result;return result
        }
        val tasks=pending.map { resolve(it.id) }
        MermaidParseResult.Success(GanttDiagram(title,format,sectionNames.mapIndexed { i,name->GanttSection(name,pending.mapIndexedNotNull { n,p->tasks[n].takeIf { p.section==i } }) },accTitle,accDescription,excludes.toList(),includes.toList(),inclusive,today,axis,tick,weekday,weekend,display,interactions.toList()))
    }catch(e:GanttSyntaxError){MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,e.message ?: "Invalid Gantt syntax",SourceLocation(line,1))))}
    private fun task(text:String) {
        val colon=text.indexOf(':');requireGantt(colon>0,"Expected Gantt task")
        val tokens=text.drop(colon+1).split(',').map { it.trim() }.toMutableList();val tags=mutableSetOf<String>()
        while(tokens.firstOrNull() in setOf("done","active","crit","milestone"))tags+=tokens.removeAt(0)
        requireGantt(tokens.size in 1..3 && tokens.none(String::isEmpty),"Invalid Gantt task fields")
        val id=if(tokens.size==3)tokens.removeAt(0)else "task${++generated}"
        if(sectionNames.isEmpty())sectionNames+=""
        pending+=Pending(text.take(colon).trim(),id,if(tokens.size==2)tokens[0]else null,tokens.last(),sectionNames.lastIndex,tags,line)
    }
    private fun date(text:String):Long? = when(format){
        "YYYY-MM-DD","yyyy-mm-dd" -> parseIsoDay(text)?.let { (it.toLong() - GANTT_EPOCH_DAY) * GANTT_DAY_MILLIS }
        "DD-MM-YYYY" -> text.split('-').takeIf { it.size==3 }?.let { parseIsoDay("${it[2]}-${it[1]}-${it[0]}") }?.let { (it.toLong() - GANTT_EPOCH_DAY) * GANTT_DAY_MILLIS }
        "x", "X" -> text.toDoubleOrNull()?.let { it * if(format == "X") 1000 else 1 }?.takeIf { it.isFinite() && it >= -62167219200000.0 && it <= 253402300799999.0 }?.toLong()
        else -> null
    }
    private fun calendarDay(text: String): Int? = date(text)?.let(::ganttFloorDay) ?: parseIsoDay(text)
    private fun excluded(day:Int):Boolean {
        if(includes.any { calendarDay(it)==day })return false
        val weekdayIndex=(day+6)%7
        return excludes.any { token->when(token){"weekends"->weekdayIndex==days.indexOf(weekend) || weekdayIndex==(days.indexOf(weekend)+1)%7;in days->weekdayIndex==days.indexOf(token);else->calendarDay(token)==day} }
    }
    private fun click(text:String) {
        val id=text.takeWhile { !it.isWhitespace() };val rest=text.drop(id.length).trim()
        if(rest.startsWith("href ")){val target=rest.drop(5).trim();requireGantt(target.length>=2 && target.first()=='"' && target.last()=='"',"Expected quoted Gantt link");interactions+=FlowInteraction(id,target.substring(1,target.lastIndex));return}
        requireGantt(rest.startsWith("call "),"Expected Gantt click action");val call=rest.drop(5).trim();val open=call.indexOf('(');requireGantt(open>0 && call.endsWith(')'),"Invalid Gantt callback")
        interactions+=FlowInteraction(id,call.take(open),true,call.substring(open+1,call.lastIndex).takeIf { it.isNotBlank() })
    }
    private fun requireGantt(value:Boolean,message:String){if(!value)fail(message)}
    private fun fail(message:String):Nothing=throw GanttSyntaxError(message)
    private class GanttSyntaxError(message:String):Exception(message)
}
