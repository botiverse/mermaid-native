package build.raft.mermaid.layout.simple

import kotlin.math.*

/** Bounded D3 comma/general display (12 significant digits), with Mermaid's $0,0 prefix. */
internal fun formatTreemapValue(value: Double, format: String): String {
    require(value.isFinite())
    require(format == "," || format == "$0,0")
    val prefix=if(format.startsWith('$')) "$" else ""
    if(value==0.0)return prefix+"0"
    var exponent=floor(log10(abs(value))).toInt()
    val mantissa=if(exponent < -300) abs(value)*1e300/10.0.pow(exponent+300) else abs(value)/10.0.pow(exponent)
    var rounded=floor(mantissa*1e11+0.5).toLong()
    if(rounded>=1000000000000L){rounded/=10;exponent++}
    val digits=rounded.toString().padStart(12,'0').trimEnd('0')
    val sign=if(value<0) "−" else ""
    if(exponent < -6 || exponent >= 12) {
        val fraction=digits.drop(1).takeIf { it.isNotEmpty() }?.let { "."+it }.orEmpty()
        return prefix+sign+digits.first()+fraction+"e"+(if(exponent>=0) "+" else "")+exponent
    }
    val decimal=exponent+1
    val integer=if(decimal<=0) "0" else digits.take(decimal).padEnd(decimal,'0')
    val fraction=if(decimal<=0) "0".repeat(-decimal)+digits else digits.drop(decimal)
    val grouped=integer.reversed().chunked(3).joinToString(",").reversed()
    return prefix+sign+grouped+(if(fraction.isEmpty()) "" else "."+fraction)
}
