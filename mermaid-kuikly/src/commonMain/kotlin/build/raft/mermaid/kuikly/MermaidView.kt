package build.raft.mermaid.kuikly

import build.raft.mermaid.layout.LayoutScene
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas

/**
 * Attributes for [MermaidView].
 */
public class MermaidViewAttr : ComposeAttr() {
    /** The prepared layout scene to render. */
    public var scene: LayoutScene? by observable(null)

    /** Scale factor applied to the diagram canvas. */
    public var scale: Float by observable(1.0f)

    /** Whether to enable Kuikly batchDraw buffering (recommended true). */
    public var batchDraw: Boolean by observable(true)
}

/**
 * Kuikly DSL Component for rendering Mermaid [LayoutScene] via high-performance Canvas.
 */
public class MermaidView : ComposeView<MermaidViewAttr, ComposeEvent>() {

    override fun createAttr(): MermaidViewAttr = MermaidViewAttr()

    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val self = this
        return {
            val scene = self.attr.scene
            if (scene != null && scene.width > 0 && scene.height > 0) {
                Canvas({
                    attr {
                        width(scene.width.toFloat() * self.attr.scale)
                        height(scene.height.toFloat() * self.attr.scale)
                    }
                }) { context, _, _ ->
                    MermaidKuiklyRenderer.render(
                        scene = scene,
                        context = context,
                        scale = self.attr.scale,
                        batchDraw = self.attr.batchDraw,
                    )
                }
            }
        }
    }
}

/**
 * Kuikly DSL extension function to add a [MermaidView] to any container.
 */
public fun ViewContainer<*, *>.MermaidView(init: MermaidView.() -> Unit) {
    addChild(MermaidView(), init)
}
