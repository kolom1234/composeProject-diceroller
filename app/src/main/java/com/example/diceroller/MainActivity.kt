package com.example.diceroller

import android.os.Bundle
import android.view.animation.BounceInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.diceroller.ui.theme.DiceRollerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DiceRollerTheme {
                DiceRollerApp()
            }
        }
    }
}

@Preview
@Composable
fun DiceRollerApp() {
    DiceWithButtonAndImage(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    )
}

@Composable
fun DiceWithButtonAndImage(modifier: Modifier = Modifier) {
    var result by remember { mutableStateOf(1) }
    var isRolling by remember { mutableStateOf(false) }

    // 주사위의 Y축 위치 애니메이션을 위한 초기 값 설정
    val offsetY = remember { Animatable(0f) }

//    // 애니메이션을 위해 이미지 크기 설정
//    val imageSize by animateDpAsState(
//        targetValue = if (isRolling) 80.dp else 100.dp,
//        animationSpec = spring(
//            dampingRatio = Spring.DampingRatioMediumBouncy,
//            stiffness = Spring.StiffnessLow
//        )
//    )

    // 이미지 리소스를 결정하는 변수
    val imageResource = when (result) {
        1 -> R.drawable.dice_1
        2 -> R.drawable.dice_2
        3 -> R.drawable.dice_3
        4 -> R.drawable.dice_4
        5 -> R.drawable.dice_5
        else -> R.drawable.dice_6
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 이미지의 위치를 애니메이션으로 조정
        Image(
            painter = painterResource(imageResource),
            contentDescription = result.toString(),
            modifier = Modifier
                .offset { IntOffset(0, offsetY.value.toInt()) } // Y축 위치 변경
                .size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            isRolling = true // 애니메이션 시작 플래그 설정
        }) {
            Text(stringResource(R.string.roll))
        }

        // 애니메이션 처리
        if (isRolling) {
            // 애니메이션 실행
            LaunchedEffect(Unit) {
                // 주사위를 위로 던지는 애니메이션
                offsetY.animateTo(
                    targetValue = -300f,
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = LinearOutSlowInEasing
                    )
                )
                // 주사위를 아래로 떨어뜨리는 애니메이션
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 500,
                        easing = EaseInBounce
                    )
                )
                // 주사위 회전 애니메이션 구현
                repeat(20) { i ->
                    result = (1..6).random()
                    delay(50L + i * 10) // 지연 시간을 늘려가며 멈추는 효과 구현
                }
                isRolling = false // 애니메이션 종료
            }
        }
    }
}
// EaseInBounce Easing 정의
val EaseInBounce: Easing = Easing { fraction ->
    when {
        fraction < 4f / 11.0f -> {
            (121f * fraction * fraction) / 16.0f
        }
        fraction < 8f / 11.0f -> {
            (363f / 40.0f * fraction * fraction) - (99f / 10.0f * fraction) + 17f / 5.0f
        }
        fraction < 9f / 10.0f -> {
            (4356f / 361.0f * fraction * fraction) - (35442f / 1805.0f * fraction) + 16061f / 1805.0f
        }
        else -> {
            (54f / 5.0f * fraction * fraction) - (513f / 25.0f * fraction) + 268f / 25.0f
        }
    }
}