package com.toprunner.diceroller

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.toprunner.diceroller.TextureHelper.vibrateShort
import kotlin.random.Random

class DiceGLSurfaceView(context: Context) : GLSurfaceView(context) {

    val renderer: DiceRenderer

    // 진동 on/off 상태

    var vibrateEnabled = true



    // **추가: "모두 멈춤" 콜백을 외부에 전달하기 위해서**
    var onAllDiceStopped: ((Int) -> Unit)? = null

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        //setZOrderOnTop(true)

        renderer = DiceRenderer(context)

        // **추가: renderer에 콜백 세팅**
        renderer.setOnAllDiceStoppedListener { sum ->
            // 주사위가 모두 멈춘 시점에 sum을 콜백
            onAllDiceStopped?.invoke(sum)
        }

        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    // 원하는 시점(Compose에서) 주사위 리스트를 통째로 교체하고 싶다면 이 함수를 제공
    fun setDiceList(newList: List<Dice3D>) {
        renderer.diceList = newList.toMutableList()
        // "이미 멈춤" 플래그를 재설정해야 한다면,
        // renderer.alreadyNotified = false  // 필요 시
    }


    // 흔들림 감지 시 호출 (주사위 속도,회전 부여)
    fun rollDice() {
        // [1] Vibrator 진동 → vibrateEnabled가 true일 때만
        if (vibrateEnabled) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        }

        var rollCount = 0

        // 이미 멈춰 있던 주사위도 다시 움직이게
        for (dice in renderer.diceList) {
            dice.finalResult = 0  // 다시 face 미결정 상태
            dice.velX   = Random.nextFloat() * 2f - 1f   // -1..1
            dice.velY   = Random.nextFloat() * 2f +  2f  // 2..4  (위로 살짝 던지기)
            dice.velZ   = Random.nextFloat() * 2f - 1f
            dice.speedX = Random.nextFloat() * 20f - 10f  // 회전 속도
            dice.speedY = Random.nextFloat() * 20f - 10f
            dice.speedZ = Random.nextFloat() * 20f - 10f
        }
        // 새로 굴릴 때마다 "모두 멈춤" 이벤트 한 번만 더 나오도록 초기화
        renderer.alreadyNotified = false

        rollCount++

        // 20번마다 전면광고
        if (rollCount % 20 == 0) {
            // showInterstitialAd() 같은 함수
            // 전면 광고 로딩 & 표시 로직
//            if (mInterstitialAd != null) {
//                mInterstitialAd?.show(activity)
//            } else {
//                loadInterstitialAdAgain()
//            }

        }
    }

    fun updateVibrateEnabled(newValue: Boolean) {
        this.vibrateEnabled = newValue
        renderer.vibrateEnabled = newValue
    }


    // 주사위 개수 변경 시 호출 (UI에서 슬라이더 등)
    fun setDiceCount(n: Int) {
        val currentSize = renderer.diceList.size

        if (n > currentSize) {
            val toAdd = n - currentSize
            repeat(toAdd) {
                val randX = (Random.nextFloat() * 20f) - 10f
                val randZ = (Random.nextFloat() * 20f) - 10f
                // posY = 10f 등, 위에서 떨어지도록
                // velY = - (0.5..2.0) 정도 주면 자연스러운 낙하
                val randVelY = -(0.5f + Random.nextFloat() * 1.5f)
                renderer.diceList.add(
                    Dice3D(
                        posX = randX,
                        posY = 10f,  // 높이 10에서 떨어짐
                        posZ = randZ,

                        velX = 0f,  // 좌우 속도는 필요에 따라
                        velY = randVelY,
                        velZ = 0f,

                        // 회전도 약간 주고 싶다면:
                        speedX = Random.nextFloat() * 10f - 5f,
                        speedY = Random.nextFloat() * 10f - 5f,
                        speedZ = Random.nextFloat() * 10f - 5f,

                        finalResult = 0 // 아직 윗면 미정
                    )
                )
            }
        } else if (n < currentSize) {
            val toRemove = currentSize - n
            repeat(toRemove) {
                renderer.diceList.removeLast()
            }
        }

    }

    fun setDiceCountSafe(n: Int) {
        this@DiceGLSurfaceView.queueEvent {
            renderer.setDiceCount(n)
        }
    }



    // DiceGLSurfaceView에서:
    fun cycleTableTextureSafe() {
        queueEvent {
            renderer.cycleTableTexture()
        }
    }


    fun computeSum(): Int {
        // renderer.totalSum을 그대로 반환하거나,
        // 모든 dice.finalResult를 다시 합산해도 됨
        return renderer.diceList.sumOf { it.finalResult }
    }
    // 주사위 정렬 → 모든 dice에 snapDiceRotationByAngles
    fun alignAllDice() {
        for (dice in renderer.diceList) {
            renderer.snapDiceRotationByAngles(dice)
        }
    }
}
