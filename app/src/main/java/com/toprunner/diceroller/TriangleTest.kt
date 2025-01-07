// TriangleTest.kt
package com.toprunner.diceroller

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun TriangleViewTest() {
    AndroidView(
        factory = { ctx ->
            TriangleGLSurfaceView(ctx)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}
