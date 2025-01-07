package com.toprunner.diceroller

// Dice3D 객체: 위치( posX, posY, posZ ) 필드를 추가.
// Dice3D에 이동 속도(velX, velY, velZ) + position
data class Dice3D(
    var posX: Float = 0f,    // 위치
    var posY: Float = 0f,
    var posZ: Float = -5f,

    var velX: Float = 0f,    // 이동 속도
    var velY: Float = 0f,
    var velZ: Float = 0f,

    var rotationX: Float = 0f, // 회전 각도
    var rotationY: Float = 0f,
    var rotationZ: Float = 0f,

    var speedX: Float = 0f,  // 회전 속도
    var speedY: Float = 0f,
    var speedZ: Float = 0f,

    // Dice3D에 추가: "멈출 때의 목표 회전각"
    var targetRotationX: Float = 0f,
    var targetRotationY: Float = 0f,
    var targetRotationZ: Float = 0f,


    var finalResult: Int = 1 // 윗면 번호(1~6)
)