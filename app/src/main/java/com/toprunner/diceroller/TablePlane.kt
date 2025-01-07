package com.toprunner.diceroller

import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * 간단한 Plane(정사각형) 메시:
 *  (x,z) -1~-1 -> +1~+1 범위, y=0  로 정의
 *  텍스처 좌표 (u,v) 전체 0~1
 */
class TablePlane {

    // 평면 4개 꼭지점 (x, y, z)
    private val vertexCoords = floatArrayOf(
        // (-1,0,-1)
        -1f, 0f, -1f,
        // (+1,0,-1)
        +1f, 0f, -1f,
        // (+1,0,+1)
        +1f, 0f, +1f,
        // (-1,0,+1)
        -1f, 0f, +1f
    )

    // 텍스처 좌표 (각 꼭짓점에 대응)
    // 전체를 0~1로 매핑
    private val texCoords = floatArrayOf(
        0f, 1f,   // (-1,0,-1)
        1f, 1f,   // (+1,0,-1)
        1f, 0f,   // (+1,0,+1)
        0f, 0f    // (-1,0,+1)
    )

    // 인덱스 (사각형 → 2개 삼각형)
    private val indices = shortArrayOf(
        0,1,2,  0,2,3
    )

    // 버퍼
    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer

    // OpenGL 핸들
    private var program = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var mvpMatrixHandle = 0

    init {
        // 버퍼 준비
        vertexBuffer = ByteBuffer.allocateDirect(vertexCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(vertexCoords)
                position(0)
            }

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(texCoords)
                position(0)
            }

        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer().apply {
                put(indices)
                position(0)
            }

        // 셰이더 작성 (Cube와 유사)
        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            attribute vec2 aTexCoord;

            varying vec2 vTexCoord;

            void main() {
                gl_Position = uMVPMatrix * vPosition;
                vTexCoord   = aTexCoord;
            }
        """

        val fragmentShaderCode = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;

            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """

        val vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glLinkProgram(it)
        }
    }

    fun draw(mvpMatrix: FloatArray, textureId: Int) {
        GLES20.glUseProgram(program)

        // 정점 위치
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            3*4,
            vertexBuffer
        )

        // 텍스처 좌표
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(
            texCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            2*4,
            texCoordBuffer
        )

        // MVP
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // 텍스처 바인딩
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uTexture"), 0)

        // 드로우
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            indices.size,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )

        // Disable
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    companion object {
        fun loadShader(type: Int, code: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, code)
            GLES20.glCompileShader(shader)
            return shader
        }
    }
}
