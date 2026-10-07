package build.raft.mermaid.web

import build.raft.mermaid.layout.*
import build.raft.mermaid.render.svg.SvgRenderer
import kotlin.test.*

class TicketConsumerTest {
    @Test fun linkTransportPreservesEscapingAndCoordinatesForBothConsumers() {
        val s=LayoutScene(200.0,100.0,emptyList(),links=listOf(SceneLink(SceneRect(10.0,20.0,30.0,12.0),"https://example.com/123?q=\"&n=1","Ticket \"123\"")))
        val svg=SvgRenderer.render(s)
        assertTrue("href=\"https://example.com/123?q=&quot;&amp;n=1\"" in svg)
        assertTrue("rel=\"noopener noreferrer\"" in svg)
        assertTrue("pointer-events=\"all\"" in svg)
        val canvas=MermaidCanvasRenderer.render(s)
        assertTrue("\"links\":[{\"x\":10,\"y\":20,\"w\":30,\"h\":12" in canvas)
        assertTrue("Ticket \\\"123\\\"" in canvas)
        assertFailsWith<IllegalArgumentException> { SceneLink(SceneRect(0.0,0.0,1.0,1.0),"javascript:alert(1)","bad") }
    }
}
