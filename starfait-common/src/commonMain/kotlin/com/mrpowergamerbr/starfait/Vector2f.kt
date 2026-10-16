package com.mrpowergamerbr.starfait

// From JOML
class Vector2f(var x: Float, var y: Float) {
    constructor() : this(0f, 0f)
    constructor(vector: Vector2f) : this(vector.x, vector.y)

    fun set(x: Float, y: Float) {
        this.x = x
        this.y = y
    }


    /**
     * Add `v` to this vector.
     *
     * @param v
     * the vector to add
     * @return this
     */
    fun add(v: Vector2f): Vector2f {
        return add(v, this)
    }

    fun add(v: Vector2f, dest: Vector2f): Vector2f {
        dest.x = x + v.x
        dest.y = y + v.y
        return dest
    }

    /**
     * Increment the components of this vector by the given values.
     *
     * @param x
     * the x component to add
     * @param y
     * the y component to add
     * @return this
     */
    fun add(x: Float, y: Float): Vector2f {
        return add(x, y, this)
    }

    fun add(x: Float, y: Float, dest: Vector2f): Vector2f {
        dest.x = this.x + x
        dest.y = this.y + y
        return dest
    }

    /**
     * Subtract `v` from this vector.
     *
     * @param v
     * the vector to subtract
     * @return this
     */
    fun sub(v: Vector2f): Vector2f {
        return sub(v, this)
    }

    fun sub(v: Vector2f, dest: Vector2f): Vector2f {
        dest.x = x - v.x
        dest.y = y - v.y
        return dest
    }

    /**
     * Subtract `(x, y)` from this vector.
     *
     * @param x
     * the x component to subtract
     * @param y
     * the y component to subtract
     * @return this
     */
    fun sub(x: Float, y: Float): Vector2f {
        return sub(x, y, this)
    }

    fun sub(x: Float, y: Float, dest: Vector2f): Vector2f {
        dest.x = this.x - x
        dest.y = this.y - y
        return dest
    }

    /**
     * Multiply the components of this vector by the given scalar.
     *
     * @param scalar
     * the value to multiply this vector's components by
     * @return this
     */
    fun mul(scalar: Float): Vector2f {
        return mul(scalar, this)
    }

    fun mul(scalar: Float, dest: Vector2f): Vector2f {
        dest.x = x * scalar
        dest.y = y * scalar
        return dest
    }

    /**
     * Multiply the components of this vector by the given scalar values and store the result in `this`.
     *
     * @param x
     * the x component to multiply this vector by
     * @param y
     * the y component to multiply this vector by
     * @return this
     */
    fun mul(x: Float, y: Float): Vector2f {
        return mul(x, y, this)
    }

    fun mul(x: Float, y: Float, dest: Vector2f): Vector2f {
        dest.x = this.x * x
        dest.y = this.y * y
        return dest
    }

    /**
     * Normalize this vector.
     *
     *
     * If this vector has zero length, the result will contain `NaN` or `Infinity` components.
     * Use [.isFinite] on the result to check for this.
     *
     * @return this
     */
    fun normalize(): Vector2f {
        return normalize(this)
    }

    fun normalize(dest: Vector2f): Vector2f {
        val invLength = MathUtils.invsqrt(x * x + y * y)
        dest.x = x * invLength
        dest.y = y * invLength
        return dest
    }
}