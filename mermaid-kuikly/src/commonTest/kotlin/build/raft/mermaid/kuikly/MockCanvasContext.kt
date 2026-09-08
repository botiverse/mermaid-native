package build.raft.mermaid.kuikly

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.module.ImageRef
import com.tencent.kuikly.core.views.CanvasContext
import com.tencent.kuikly.core.views.CanvasLinearGradient
import com.tencent.kuikly.core.views.ContextApi
import com.tencent.kuikly.core.views.FontStyle
import com.tencent.kuikly.core.views.FontWeight
import com.tencent.kuikly.core.views.TextAlign
import com.tencent.kuikly.core.views.TextMetrics

class MockCanvasContext : ContextApi {
    val log = mutableListOf<String>()
    var batchDraw: Boolean = false

    override fun beginPath() {
        log.add("beginPath")
    }

    override fun closePath() {
        log.add("closePath")
    }

    override fun moveTo(x: Float, y: Float) {
        log.add("moveTo($x, $y)")
    }

    override fun lineTo(x: Float, y: Float) {
        log.add("lineTo($x, $y)")
    }

    override fun arc(
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
        endAngle: Float,
        counterclockwise: Boolean
    ) {
        log.add("arc($centerX, $centerY, $radius, $startAngle, $endAngle, $counterclockwise)")
    }

    override fun quadraticCurveTo(controlPointX: Float, controlPointY: Float, pointX: Float, pointY: Float) {
        log.add("quadraticCurveTo($controlPointX, $controlPointY, $pointX, $pointY)")
    }

    override fun bezierCurveTo(
        controlPoint1X: Float,
        controlPoint1Y: Float,
        controlPoint2X: Float,
        controlPoint2Y: Float,
        pointX: Float,
        pointY: Float
    ) {
        log.add("bezierCurveTo($controlPoint1X, $controlPoint1Y, $controlPoint2X, $controlPoint2Y, $pointX, $pointY)")
    }

    override fun fill() {
        log.add("fill")
    }

    override fun stroke() {
        log.add("stroke")
    }

    override fun fillStyle(color: Color) {
        log.add("fillStyle(${color.hexColor})")
    }

    override fun fillStyle(linearGradient: CanvasLinearGradient) {
        log.add("fillStyle(gradient)")
    }

    override fun strokeStyle(color: Color) {
        log.add("strokeStyle(${color.hexColor})")
    }

    override fun strokeStyle(linearGradient: CanvasLinearGradient) {
        log.add("strokeStyle(gradient)")
    }

    override fun lineWidth(width: Float) {
        log.add("lineWidth($width)")
    }

    override fun setLineDash(intervals: List<Float>) {
        log.add("setLineDash(${intervals.joinToString()})")
    }

    override fun lineCapRound() {
        log.add("lineCapRound")
    }

    override fun lineCapButt() {
        log.add("lineCapButt")
    }

    override fun lineCapSquare() {
        log.add("lineCapSquare")
    }

    override fun fillText(text: String, x: Float, y: Float) {
        log.add("fillText($text, $x, $y)")
    }

    override fun strokeText(text: String, x: Float, y: Float) {
        log.add("strokeText($text, $x, $y)")
    }

    override fun textAlign(textAlign: TextAlign) {
        log.add("textAlign($textAlign)")
    }

    override fun font(size: Float, family: String) {
        log.add("font($size, $family)")
    }

    override fun font(style: FontStyle, weight: FontWeight, size: Float, family: String) {
        log.add("font($style, $weight, $size, $family)")
    }

    override fun measureText(value: String): TextMetrics {
        return TextMetrics(width = value.length * 10f, 0f, 0f, 0f, 0f)
    }

    override fun save() {
        log.add("save")
    }

    override fun saveLayer(x: Float, y: Float, width: Float, height: Float) {
        log.add("saveLayer($x, $y, $width, $height)")
    }

    override fun restore() {
        log.add("restore")
    }

    override fun clip(intersect: Boolean) {
        log.add("clip($intersect)")
    }

    override fun clipPathIntersect() {
        log.add("clipPathIntersect")
    }

    override fun clipPathDifference() {
        log.add("clipPathDifference")
    }

    override fun scale(x: Float, y: Float) {
        log.add("scale($x, $y)")
    }

    override fun translate(x: Float, y: Float) {
        log.add("translate($x, $y)")
    }

    override fun rotate(angle: Float) {
        log.add("rotate($angle)")
    }

    override fun skew(x: Float, y: Float) {
        log.add("skew($x, $y)")
    }

    override fun transform(array: FloatArray) {
        log.add("transform(${array.joinToString()})")
    }

    override fun createLinearGradient(x0: Float, y0: Float, x1: Float, y1: Float): CanvasLinearGradient {
        throw UnsupportedOperationException()
    }

    override fun createRadialGradient(
        x0: Float,
        y0: Float,
        r0: Float,
        x1: Float,
        y1: Float,
        r1: Float,
        alpha: Float,
        vararg colors: Color
    ) {
        throw UnsupportedOperationException()
    }

    override fun drawImage(image: ImageRef, dx: Float, dy: Float) {}

    override fun drawImage(image: ImageRef, dx: Float, dy: Float, dWidth: Float, dHeight: Float) {}

    override fun drawImage(
        image: ImageRef,
        sx: Float,
        sy: Float,
        sWidth: Float,
        sHeight: Float,
        dx: Float,
        dy: Float,
        dWidth: Float,
        dHeight: Float
    ) {}
}
