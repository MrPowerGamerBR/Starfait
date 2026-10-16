package com.mrpowergamerbr.starfait

// From JOML
class Vector3f(var x: Float, var y: Float, var z: Float) {
    constructor() : this(0f, 0f, 0f)

    fun set(x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }
}