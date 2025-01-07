package com.toprunner.diceroller

import android.content.Context
import android.opengl.GLSurfaceView

class TriangleGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer: TriangleRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = TriangleRenderer(context)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}
