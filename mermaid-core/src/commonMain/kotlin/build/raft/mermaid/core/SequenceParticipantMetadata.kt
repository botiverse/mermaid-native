package build.raft.mermaid.core

/** The string-valued participant type/alias subset of upstream's flow mapping. */
internal class SequenceParticipantMetadata(private val source: String, private val jsonStringsOnly: Boolean = false) {
    private var offset=0
    fun parse(): Map<String,String>? = try {
        val result=linkedMapOf<String,String>()
        whitespace()
        while(offset<source.length) {
            if(jsonStringsOnly) require(source.getOrNull(offset)=='"')
            val key=token(':')
            whitespace(); require(take(':'))
            whitespace(); if(jsonStringsOnly) require(source.getOrNull(offset)=='"')
            val value=token(',')
            require(jsonStringsOnly || key.isNotBlank() && key !in result)
            result[key]=value
            whitespace()
            if(offset==source.length) break
            require(take(',')); whitespace()
        }
        result
    } catch (_: IllegalArgumentException) { null }
    private fun token(delimiter: Char): String {
        whitespace()
        val quote=source.getOrNull(offset)
        if(quote=='"' || quote=='\'') {
            offset++
            val out=StringBuilder()
            while(offset<source.length) {
                val c=source[offset++]
                if(c==quote) {
                    if(quote=='\'' && source.getOrNull(offset)=='\'') { out.append(c); offset++; continue }
                    return out.toString()
                }
                if(c=='\\' && quote=='"') {
                    require(offset<source.length)
                    out.append(when(val escape=source[offset++]) { 'n'->'\n'; 'r'->'\r'; 't'->'\t'; 'b'->'\b'; 'f'->'\u000c'; 'u'->{ require(offset+4<=source.length); val digits=source.substring(offset,offset+4); require(digits.all { it in "0123456789abcdefABCDEF" }); offset+=4; digits.toInt(16).toChar() }; '"'->'"'; '\\'->'\\'; '/'->'/'; else->throw IllegalArgumentException("Unsupported string escape") })
                } else out.append(c)
            }
            throw IllegalArgumentException("Unclosed string")
        }
        val start=offset
        while(offset<source.length && source[offset]!=delimiter) {
            require(source[offset] !in "{}[]\\\"'\n\r")
            offset++
        }
        return source.substring(start,offset).trim().also { require(it.isNotEmpty()) }
    }
    private fun whitespace() { while(source.getOrNull(offset)?.isWhitespace()==true) offset++ }
    private fun take(c: Char)= (source.getOrNull(offset)==c).also { if(it) offset++ }
}
