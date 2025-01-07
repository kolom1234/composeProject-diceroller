package com.toprunner.diceroller

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@Composable
fun Dice3DView(
    modifier: Modifier = Modifier,
    onRollFinished: (Int) -> Unit,
    shakeCount: Int,
    onGLViewCreated: (DiceGLSurfaceView) -> Unit
) {
    var diceGLView by remember { mutableStateOf<DiceGLSurfaceView?>(null) }
    // 흔들림 발생 시 -> rollDice()
    LaunchedEffect(shakeCount) {

        if (shakeCount > 0) {
            diceGLView?.rollDice()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color(0xFFCD853F))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { diceGLView?.rollDice() }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                DiceGLSurfaceView(ctx).apply {
                    diceGLView = this
                    onAllDiceStopped = { sum -> onRollFinished(sum) }
                    // 알려주기
                    onGLViewCreated(this)
                }
            },
            // update는 굳이 없음
            modifier = Modifier.matchParentSize()
        )
    }
}
