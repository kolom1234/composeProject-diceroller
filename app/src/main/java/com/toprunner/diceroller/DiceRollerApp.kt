package com.toprunner.diceroller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.delay

@Composable
fun DiceRollerApp(shakeEvent: State<Int>) {
    val diceGLViewState = remember { mutableStateOf<DiceGLSurfaceView?>(null) }

    var numberOfDice by remember { mutableStateOf(1) }
    var vibrateEnabledLocal by remember { mutableStateOf(true) }


    // 입체감 주는 Elevation 설정
    val customElevation = ButtonDefaults.elevatedButtonElevation(
        defaultElevation = 8.dp,
        pressedElevation = 12.dp
    )

    // 전체 레이아웃을 Box로 감싸,
    // 1) 아래에 Dice3DView가 깔리고
    // 2) 위에 Column(Rows of buttons)와 BannerAdView를 Overlay로 올리는 방법
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds() // 3D View 넘어가면 잘라내기
    ) {
        // (1) 주사위 3D 화면
        Dice3DView(
            modifier = Modifier.matchParentSize(), // 전체 화면 차지
            onRollFinished = { /* sum -> ... */ },
            shakeCount = shakeEvent.value,
            onGLViewCreated = { glView ->
                diceGLViewState.value = glView
                glView.setDiceCountSafe(numberOfDice)
                glView.updateVibrateEnabled(vibrateEnabledLocal)
            }
        )

        // (2) 버튼들과 주사위 개수 표시 (상단)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 첫 번째 Row: 정렬 / 텍스처 / 진동
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 정렬 버튼
                ElevatedButton(
                    onClick = { diceGLViewState.value?.alignAllDice() },
                    //elevation = customElevation,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xBEFFFFFF),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sort")
                }

                // 텍스처 변경 버튼
                ElevatedButton(
                    onClick = { diceGLViewState.value?.cycleTableTextureSafe() },
                    //elevation = customElevation,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xBEFFFFFF),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Change Texture")
                }

                val icon = if (vibrateEnabledLocal) Icons.Filled.Vibration else Icons.Filled.NotificationsOff

                // 진동 ON/OFF 버튼
                ElevatedButton(
                    onClick = {
                        vibrateEnabledLocal = !vibrateEnabledLocal
                        diceGLViewState.value?.updateVibrateEnabled(vibrateEnabledLocal)
                    },
                    //elevation = customElevation,

                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xBEFFFFFF),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = icon, contentDescription = "Vibration")
                    Spacer(Modifier.width(4.dp))
                    //Text(if (vibrateEnabledLocal) "ON" else "OFF")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 두 번째 Row: 주사위 개수 변경 (Set=1 / - / Dice:X / +)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Set=1" 버튼
                ElevatedButton(
                    onClick = {
                        numberOfDice = 1
                        diceGLViewState.value?.setDiceCountSafe(1)
                    },
                    //elevation = customElevation,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xBEFFFFFF),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Set=1")
                }

                // "-" 버튼
                ElevatedButton(
                    onClick = {
                        val newCount = (numberOfDice - 1).coerceAtLeast(1)
                        numberOfDice = newCount
                        diceGLViewState.value?.setDiceCountSafe(newCount)
                    },
                    //elevation = customElevation,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xBEFFFFFF),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("-")
                }

                // "Dice: X" -> 작은 Surface 안의 텍스트
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "Dice: $numberOfDice",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.Black
                    )
                }

                // "+" 버튼
                ElevatedButton(
                    onClick = {
                        val newCount = (numberOfDice + 1).coerceAtMost(500)
                        numberOfDice = newCount
                        diceGLViewState.value?.setDiceCountSafe(newCount)
                    },
                    //elevation = customElevation,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xBEFFFFFF),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("+")
                }
            }
        }

        // (3) 하단 배너 광고 (맨 아래)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BannerAdView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                adUnitId = "ca-app-pub-5022915725486354/3289851274"
            )
        }
    }
}

// 배너 광고 Composable
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String
) {
    AndroidView(
        factory = {
            AdView(it).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = modifier
    )
}
