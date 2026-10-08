package dev.mermaid.examples

import android.app.Activity
import android.os.Bundle
import com.tencent.kuikly.core.render.android.expand.KuiklyBaseView

class MainActivity : Activity() {
    private lateinit var canvas: KuiklyBaseView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        canvas = KuiklyBaseView(this)
        setContentView(canvas)
        canvas.onAttach("", "mermaid", mapOf("sample" to (intent.getStringExtra("sample") ?: "flow")))
    }
    override fun onResume() { super.onResume(); if (::canvas.isInitialized) canvas.onResume() }
    override fun onPause() { if (::canvas.isInitialized) canvas.onPause(); super.onPause() }
    override fun onDestroy() { if (::canvas.isInitialized) canvas.onDetach(); super.onDestroy() }
}
