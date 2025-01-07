package com.toprunner.diceroller

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.toprunner.diceroller.TextureHelper.vibrateShort
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

class DiceRenderer(private val context: Context) : GLSurfaceView.Renderer {

    var diceList = mutableListOf<Dice3D>()
    var vibrateEnabled = true



    // 카메라·행렬
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    private var cube: Cube? = null
    private var texture: Int = 0

    // 물리 상수
    private val gravity = 0.09f
    private val friction = 0.97f
    private val restitution = 0.8f       // 바닥 반발
    private val collisionDist = 2.0f
    private val floorY = 0f             // 바닥 y=0
    private var totalSum = 0            // 최종 합(멈춘 주사위들의 윗면 합)
    private var tablePlane: TablePlane? = null
    private var tableTexture: Int = 0

    private val tableTextureIds = listOf(
        R.drawable.table_texture,
        R.drawable.table_texture2,
        R.drawable.table_texture3,
        R.drawable.table_texture4,
    )
    private var currentTextureIndex = 0
    val LERP_FACTOR = 0.2f


    fun cycleTableTexture() {
        currentTextureIndex = (currentTextureIndex + 1) % tableTextureIds.size
        tableTexture = TextureHelper.loadTexture(context, tableTextureIds[currentTextureIndex])
        Log.d("DiceRenderer", "Table texture changed to index=$currentTextureIndex")
    }

    // "모두 멈춤" 이벤트를 한 번만 보내기 위한 플래그 + 콜백
    var alreadyNotified = false

    private var onAllDiceStopped: ((Int) -> Unit)? = null
    override fun onSurfaceCreated(gl: GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        tablePlane = TablePlane()
        tableTexture = TextureHelper.loadTexture(context, R.drawable.table_texture)

        Log.d("DiceRenderer", "onSurfaceCreated!")
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        cube = Cube()
        texture = TextureHelper.loadTexture(context, R.drawable.dice_texture)
        // 1) Plane 생성
        //tablePlane = TablePlane()
        // 2) 책상 텍스처 로드 (drawable/table_texture.png 등)
        //tableTexture = TextureHelper.loadTexture(context, R.drawable.table_texture)
        // 화면 새로 생성 시 이벤트 플래그 초기화
        alreadyNotified = false
    }

    fun setOnAllDiceStoppedListener(listener: (Int) -> Unit) {
        onAllDiceStopped = listener
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        // 카메라를 좀 더 멀리(near=1, far=50)
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 50f)
    }

    override fun onDrawFrame(gl: GL10?) {
        // 카메라: 더 멀리(예: x=-20, y=8, z=0)

        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, 40f, 3f,
            0f, 1f, 0f,
            0f, 1f, 0f
        )

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        /// (A) 테이블 Plane 그리기
        Matrix.setIdentityM(modelMatrix, 0)
        // 바닥 y=0 위치에 plane을 놓는다
        Matrix.translateM(modelMatrix, 0, 0f, 0f, 0f)
        // plane 정의상 y=0, x,z=-1..+1에 놓임 → -90°로 눕혀서 XZ평면
        Matrix.rotateM(modelMatrix, 0, 0f, 1f, 0f, 0f)
        // x,z축으로 30배 확대 -> -30..+30 범위, 주사위가 -10..+10 내부면 충분
        Matrix.scaleM(modelMatrix, 0, 58f, 100f, 58f)

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
        tablePlane?.draw(mvpMatrix, tableTexture)

        // 매번 합산을 다시 구할 수 있으나, 여기서는 예시로 매 프레임마다 0으로 만들고
        // 멈춘 주사위들만 합쳐서 totalSum에 넣도록 예시 구현
        totalSum = 0

        var allStopped = true

        for (i in diceList.indices) {
            val dice = diceList[i]

            // 1) 중력
            dice.velY -= gravity

            // 2) 이동
            dice.posX += dice.velX
            dice.posY += dice.velY
            dice.posZ += dice.velZ




            // 3) 바닥 충돌
            if (dice.posY < floorY ) {
                dice.posY = floorY
                dice.velY = -dice.velY * restitution
                // 바닥에 닿으면 속도와 회전 크게 감소
                dice.velX *= 0.7f
                dice.velZ *= 0.7f
                dice.speedX *= 0.7f
                dice.speedY *= 0.7f
                dice.speedZ *= 0.7f
                if (abs(dice.velY) > 0.1f && vibrateEnabled ) {
                    vibrateShort(context, 50)
                }
            }

            // 4) 주사위끼리 충돌
            for (j in i + 1 until diceList.size) {
                val other = diceList[j]
                val dx = other.posX - dice.posX
                val dy = other.posY - dice.posY
                val dz = other.posZ - dice.posZ
                val dist = sqrt(dx * dx + dy * dy + dz * dz)
                if (dist < collisionDist && dist > 0.0001f) {
                    val overlap = collisionDist - dist
                    val nx = dx / dist
                    val ny = dy / dist
                    val nz = dz / dist
                    // 반씩 보정
                    dice.posX -= nx * overlap * 0.5f
                    dice.posY -= ny * overlap * 0.5f
                    dice.posZ -= nz * overlap * 0.5f
                    other.posX += nx * overlap * 0.5f
                    other.posY += ny * overlap * 0.5f
                    other.posZ += nz * overlap * 0.5f

                    // 속도 반사
                    val rvx = other.velX - dice.velX
                    val rvy = other.velY - dice.velY
                    val rvz = other.velZ - dice.velZ
                    val relVelDot = rvx * nx + rvy * ny + rvz * nz
                    val impulse = 0.5f * relVelDot
                    dice.velX += impulse * nx
                    dice.velY += impulse * ny
                    dice.velZ += impulse * nz
                    other.velX -= impulse * nx
                    other.velY -= impulse * ny
                    other.velZ -= impulse * nz
                }
            }

            // 5) 경계 처리 (화면 밖으로 넘어가지 않게)
            val minX = -15f
            val maxX = 15f
            val minZ = -20f
            val maxZ = 20f

            if (dice.posX < minX) {
                dice.posX = minX
                dice.velX = -dice.velX * restitution
            } else if (dice.posX > maxX) {
                dice.posX = maxX
                dice.velX = -dice.velX * restitution
            }
            if (dice.posZ < minZ) {
                dice.posZ = minZ
                dice.velZ = -dice.velZ * restitution
            } else if (dice.posZ > maxZ) {
                dice.posZ = maxZ
                dice.velZ = -dice.velZ * restitution
            }

            // 6) 회전 각도
            dice.rotationX += dice.speedX*2f
            dice.rotationY += dice.speedY*2f
            dice.rotationZ += dice.speedZ*2f

            // 7) 마찰 감속
            dice.velX *= friction
            dice.velY *= friction
            dice.velZ *= friction
            dice.speedX *= friction
            dice.speedY *= friction
            dice.speedZ *= friction

            // 8) 멈춤 체크
            val isOnGround = (dice.posY <= floorY + 0.001f)
            val threshold = 0.05f
            val isStopped = isOnGround && (
                    abs(dice.velX) < threshold &&
                            abs(dice.velY) < threshold &&
                            abs(dice.velZ) < threshold &&
                            abs(dice.speedX) < threshold &&
                            abs(dice.speedY) < threshold &&
                            abs(dice.speedZ) < threshold
                    )

            // 예: DiceRenderer.onDrawFrame() 안
            if (isStopped && dice.finalResult == 0) {
                // (A) 더 이상 face 번호를 안 써도 된다면,
                //     굳이 computeTopFace(dice) 없이 그냥 finalResult=1 정도로 세팅하거나
                //     혹은 "face는 안 쓰겠다" 하고 넘어가도 됨.
                dice.finalResult = 1  // 임의값


                // (B) 각도 스냅
                //snapDiceRotationByAngles(dice)
                val rx = normalizeAngle(dice.rotationX)
                val ry = normalizeAngle(dice.rotationY)
                val rz = normalizeAngle(dice.rotationZ)


                dice.rotationX = snapToNearest90(rx)
                dice.rotationZ = snapToNearest90(rz)
                //dice.rotationY = snapToNearest90(ry)

                // (C) posY=0으로 바닥에 붙임
//                dice.posY = 0f
//                if(dice.rotationY == 45f || dice.rotationY == -45f)
//                    dice.rotationY = 0f
//                if(dice.rotationY == 135f || dice.rotationY == 225f)
//                    dice.rotationY = 180f
//                if(dice.rotationY == 45f || dice.rotationY == -45f)
//                    dice.rotationY = 0f

                // (D) 필요하다면 속도/회전속도 전부 0으로 세팅
//                dice.velX = 0f
//                dice.velY = 0f
//                dice.velZ = 0f
//                dice.speedX = 0f
//                dice.speedY = 0f
//                dice.speedZ = 0f

                Log.d("DiceRenderer", "Dice stopped => angle-snapped!")
            }




            // 모두 멈췄는지 추적
            if (dice.finalResult == 0) {
                allStopped = false
            } else {
                totalSum += dice.finalResult
            }

            // 모델 행렬
            Matrix.setIdentityM(modelMatrix, 0)
            // posY + 1f → 주사위 밑면이 바닥(y=0)에 닿도록
            Matrix.translateM(modelMatrix, 0, dice.posX, dice.posY + 1f, dice.posZ)
            Matrix.rotateM(modelMatrix, 0, dice.rotationX, 1f, 0f, 0f)
            Matrix.rotateM(modelMatrix, 0, dice.rotationY, 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, dice.rotationZ, 0f, 0f, 1f)

            Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

            // 실제 그리기
            cube?.draw(mvpMatrix, texture)
        }

        Log.d("DiceRenderer", "totalSum=$totalSum")

        // "모두 멈춤" 시점에 한 번만 콜백
        if (allStopped && !alreadyNotified) {
            alreadyNotified = true
            onAllDiceStopped?.invoke(totalSum)
        }
    }

    /**
     * 윗면 판정
     */
    private val AXIS_POS_X = floatArrayOf(1f, 0f, 0f)
    private val AXIS_NEG_X = floatArrayOf(-1f, 0f, 0f)
    private val AXIS_POS_Y = floatArrayOf(0f, 1f, 0f)
    private val AXIS_NEG_Y = floatArrayOf(0f, -1f, 0f)
    private val AXIS_POS_Z = floatArrayOf(0f, 0f, 1f)
    private val AXIS_NEG_Z = floatArrayOf(0f, 0f, -1f)

    /**
     * finalResult (윗면) 가 결정된 뒤,
     * "밑면(=7 - finalResult)을 local -Y"에 오도록 회전 각도를 강제
     */


    /**
     * 회전 각도를 딱 90도 단위로 스냅
     */
    /**
     * 현재 dice.finalResult (=topFace)를 윗면(+Y)에 두고,
     * 원하면 특정 face(예: 2)를 정면(-Z)으로 놓는 식으로
     * rotationX/Y/Z를 강제 세팅한다.
     */


    /**
     * 주어진 angle(각도, 보통 -∞~+∞)을
     * 0° ~ 360° 사이로 조정
     */
    fun normalizeAngle(angle: Float): Float {
        var result = angle % 360f
        if (result < 0f) {
            result += 360f
        }
        return result
    }

    /**
     * angle을 가장 가까운 0°, 90°, 180°, 270°로 반올림
     */
    fun snapToNearest90(angle: Float): Float {
        val n = Math.round(angle / 90f)
        val snapped = (n * 90) % 360
        return snapped.toFloat()
    }

    /**
     * Dice3D 객체의 rotationX, rotationY, rotationZ를
     * 90° 단위로 반올림하여 '축정렬' 상태로 만든다.
     */
    fun snapDiceRotationByAngles(dice: Dice3D) {
        val rx = normalizeAngle(dice.rotationX)
        val ry = normalizeAngle(dice.rotationY)
        val rz = normalizeAngle(dice.rotationZ)


        dice.rotationX = snapToNearest90(rx)
        dice.rotationZ = snapToNearest90(rz)
        dice.rotationY = snapToNearest90(ry)
    }
    fun setDiceCount(n: Int) {
        val currentSize = diceList.size

        if (n > currentSize) {
            val toAdd = n - currentSize
            repeat(toAdd) {
                val randX = (Random.nextFloat() * 20f) - 10f
                val randZ = (Random.nextFloat() * 20f) - 10f
                // posY = 10f 등, 위에서 떨어지도록
                // velY = - (0.5..2.0) 정도 주면 자연스러운 낙하
                val randVelY = -(0.5f + Random.nextFloat() * 1.5f)
                diceList.add(
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
                diceList.removeLast()
            }
        }

    }


    /**
     * "Pitch/Roll=0, Yaw만 유지" 형태로
     * dice.rotationX,Y,Z를 재설정하여,
     * 밑면(로컬 Y축)이 세계 Y축과 평행하도록 만든다.
     */


}