package com.toprunner.diceroller

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class TriangleRenderer(private val context: Context) : GLSurfaceView.Renderer {

    // 간단한 삼각형 정점 (x,y,z)
    private val triangleCoords = floatArrayOf(
        // in normalized device space
        0f,  0.5f, 0f,   // top
        -0.5f, -0.5f, 0f, // bottom left
        0.5f, -0.5f, 0f  // bottom right
    )

    // 정점 버퍼
    private val vertexBuffer: FloatBuffer

    // 셰이더 코드: 간단 버전 (MVP행렬 적용, 단색 빨강)
    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
        }
    """

    // 단색 빨강
    private val fragmentShaderCode = """
        precision mediump float;
        void main() {
            gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
        }
    """

    // OpenGL program
    private var program = 0

    // 행렬들
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    init {
        // 정점 버퍼 준비
        val bb = ByteBuffer.allocateDirect(triangleCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(triangleCoords)
        vertexBuffer.position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d("TriangleRenderer", "onSurfaceCreated called")
        GLES20.glClearColor(0.6f, 0.6f, 0.6f, 1f)

        // 깊이 테스트 (삼각형 2D라 필요 없을 수도 있지만, 습관적으로 켜둠)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // 컬링 비활성 (혹시 면이 뒤집혀도 보이게)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        // 셰이더 컴파일 & 링크
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d("TriangleRenderer", "onSurfaceChanged: $width x $height")
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height
        // 간단히 frustum
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 10f)

        // 카메라 z=-3, 원점(0,0,0)을 봄
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // 모델 행렬(간단히 단위행렬)
        Matrix.setIdentityM(modelMatrix, 0)
        // view x model
        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        // projection x temp
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        GLES20.glUseProgram(program)

        // vPosition 핸들
        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            3 * 4,
            vertexBuffer
        )

        // uMVPMatrix 핸들
        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // 그리기
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)

        // 마무리
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}
