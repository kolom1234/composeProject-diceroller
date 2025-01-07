package com.toprunner.diceroller

import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class Cube {

    // (1) 24개의 정점(6면 × 4꼭지점).
    //     각 면은 (X, Y, Z)가 4개씩.
    private val vertexCoords = floatArrayOf(
        // === 면1 (Front) ===
        -1f, -1f, +1f,  // 0
        +1f, -1f, +1f,  // 1
        +1f, +1f, +1f,  // 2
        -1f, +1f, +1f,  // 3

        // === 면2 (Right) ===
        +1f, -1f, +1f,  // 4
        +1f, -1f, -1f,  // 5
        +1f, +1f, -1f,  // 6
        +1f, +1f, +1f,  // 7

        // === 면3 (Back) ===
        +1f, -1f, -1f,  // 8
        -1f, -1f, -1f,  // 9
        -1f, +1f, -1f,  //10
        +1f, +1f, -1f,  //11

        // === 면4 (Left) ===
        -1f, -1f, -1f,  //12
        -1f, -1f, +1f,  //13
        -1f, +1f, +1f,  //14
        -1f, +1f, -1f,  //15

        // === 면5 (Top) ===
        -1f, +1f, +1f,  //16
        +1f, +1f, +1f,  //17
        +1f, +1f, -1f,  //18
        -1f, +1f, -1f,  //19

        // === 면6 (Bottom) ===
        -1f, -1f, -1f,  //20
        +1f, -1f, -1f,  //21
        +1f, -1f, +1f,  //22
        -1f, -1f, +1f   //23
    )

    // (2) 24개의 텍스처 좌표(각 면마다 4개씩).
    //     dice_texture.png가 2행×3열로 배치됐다고 가정:
    //     u 폭: 1/3=0.3333f, 2/3=0.6667f
    //     v 높이: 1/2=0.5f
    private val texCoords = floatArrayOf(
        // === 면1 (Front) → "면1" 위치 (0,0.5)~(0.3333,1.0)
        0f, 1f,        // (-1,-1,+1)
        0.3333f, 1f,   // (+1,-1,+1)
        0.3333f, 0.5f, // (+1,+1,+1)
        0f,      0.5f, // (-1,+1,+1)

        // === 면2 (Right) → "면2" 위치 (0.3333,0.5)~(0.6667,1.0)
        0.3333f, 1f,
        0.6667f, 1f,
        0.6667f, 0.5f,
        0.3333f, 0.5f,

        // === 면3 (Back) → "면3" 위치 (0.6667,0.5)~(1.0,1.0)
        0.6667f, 1f,
        1.0f,    1f,
        1.0f,    0.5f,
        0.6667f, 0.5f,

        // === 면4 (Left) → "면4" 위치 (0,0)~(0.3333,0.5)
        0f,      0.5f,
        0.3333f, 0.5f,
        0.3333f, 0f,
        0f,      0f,

        // === 면5 (Top) → "면5" 위치 (0.3333,0)~(0.6667,0.5)
        0.3333f, 0.5f,
        0.6667f, 0.5f,
        0.6667f, 0f,
        0.3333f, 0f,

        // === 면6 (Bottom) → "면6" 위치 (0.6667,0)~(1.0,0.5)
        0.6667f, 0.5f,
        1.0f,    0.5f,
        1.0f,    0f,
        0.6667f, 0f
    )

    // (3) 36개 인덱스(6면 × 2삼각형 × 3인덱스 = 36).
    //     각 면은 “사각형을 두 삼각형”으로 구성.
    //     → 면마다 4정점, 예) (n, n+1, n+2), (n, n+2, n+3)
    private val indices = shortArrayOf(
        // 면1 (0,1,2,3)
        0,1,2,  0,2,3,
        // 면2 (4,5,6,7)
        4,5,6,  4,6,7,
        // 면3 (8,9,10,11)
        8,9,10, 8,10,11,
        // 면4 (12,13,14,15)
        12,13,14, 12,14,15,
        // 면5 (16,17,18,19)
        16,17,18, 16,18,19,
        // 면6 (20,21,22,23)
        20,21,22, 20,22,23
    )

    // (4) 셰이더 코드 (단순 색상+텍스처)
    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        attribute vec2 aTexCoord;

        varying vec2 vTexCoord;

        void main() {
            gl_Position = uMVPMatrix * vPosition;
            vTexCoord   = aTexCoord;
        }
    """

    // 주사위 눈(텍스처)을 표시할 경우:
    //   gl_FragColor = texture2D(uTexture, vTexCoord);
    // 혹은 단색 테스트하려면 vec4(1,0,0,1) 등.
    private val fragmentShaderCode = """
        precision mediump float;

        varying vec2 vTexCoord;
        uniform sampler2D uTexture;

        void main() {
            gl_FragColor = texture2D(uTexture, vTexCoord);
        }
    """

    // 버퍼들
    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer

    private var program = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var mvpMatrixHandle = 0

    init {
        Log.d("Cube", "Cube init block START")

        // 24버텍스
        vertexBuffer = ByteBuffer.allocateDirect(vertexCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(vertexCoords)
                position(0)
            }

        // 24 UV
        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(texCoords)
                position(0)
            }

        // 인덱스 36개
        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer().apply {
                put(indices)
                position(0)
            }

        // 셰이더 컴파일
        val vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        checkShaderCompileError(vs, "VertexShader")
        val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        checkShaderCompileError(fs, "FragmentShader")

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glLinkProgram(it)
            checkProgramLinkError(it)
        }

        Log.d("Cube", "Cube init block END")
    }

    private fun checkShaderCompileError(shaderId: Int, name: String) {
        val status = IntArray(1)
        GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val infoLog = GLES20.glGetShaderInfoLog(shaderId)
            Log.e("Cube", "$name compile error:\n$infoLog")
            GLES20.glDeleteShader(shaderId)
            throw RuntimeException("$name compilation failed:\n$infoLog")
        }
        else {
            Log.d("Cube", "$name compilation success")
        }
    }

    private fun checkProgramLinkError(program: Int) {
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val infoLog = GLES20.glGetProgramInfoLog(program)
            Log.e("Cube", "Program link error:\n$infoLog")
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Program link failed:\n$infoLog")
        }
    }

    // (5) 그리기
    fun draw(mvpMatrix: FloatArray, textureId: Int) {
        GLES20.glUseProgram(program)

        // 정점 위치
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            3,                 // xyz
            GLES20.GL_FLOAT,
            false,
            3 * 4,
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
            2 * 4,
            texCoordBuffer
        )

        // MVP 행렬
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

        // 정리
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    companion object {
        // 셰이더 로더
        fun loadShader(type: Int, code: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, code)
            GLES20.glCompileShader(shader)
            return shader
        }
    }
}
