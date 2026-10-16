package com.mrpowergamerbr.starfait

import kotlin.math.cos
import kotlin.math.sin

// From JOML
class Matrix3x2f {
    var m00 = 0f
    var m01 = 0f
    var m10 = 0f
    var m11 = 0f
    var m20 = 0f
    var m21 = 0f

    /**
     * Create a new [Matrix3x2f] and set it to [identity][.identity].
     */
    constructor() {
        this.m00 = 1.0f
        this.m11 = 1.0f
    }

    /**
     * Apply scaling to this matrix by scaling the unit axes by the given x and y and store the result in `dest`.
     *
     *
     * If `M` is `this` matrix and `S` the scaling matrix,
     * then the new matrix will be `M * S`. So when transforming a
     * vector `v` with the new matrix by using `M * S * v`, the scaling will be applied first!
     *
     * @param x
     * the factor of the x component
     * @param y
     * the factor of the y component
     * @param dest
     * will hold the result
     * @return dest
     */
    fun scale(x: Float, y: Float, dest: Matrix3x2f): Matrix3x2f {
        dest.m00 = m00 * x
        dest.m01 = m01 * x
        dest.m10 = m10 * y
        dest.m11 = m11 * y
        dest.m20 = m20
        dest.m21 = m21
        return dest
    }

    /**
     * Apply scaling to this matrix by scaling the base axes by the given x and y factors.
     *
     *
     * If `M` is `this` matrix and `S` the scaling matrix,
     * then the new matrix will be `M * S`. So when transforming a
     * vector `v` with the new matrix by using `M * S * v`, the scaling will be applied first!
     *
     * @param x
     * the factor of the x component
     * @param y
     * the factor of the y component
     * @return this
     */
    fun scale(x: Float, y: Float): Matrix3x2f {
        return scale(x, y, this)
    }

    /**
     * Apply a translation to this matrix by translating by the given number of units in x and y and store the result
     * in `dest`.
     *
     *
     * If `M` is `this` matrix and `T` the translation
     * matrix, then the new matrix will be `M * T`. So when
     * transforming a vector `v` with the new matrix by using
     * `M * T * v`, the translation will be applied first!
     *
     *
     * In order to set the matrix to a translation transformation without post-multiplying
     * it, use [.translation].
     *
     * @see .translation
     * @param x
     * the offset to translate in x
     * @param y
     * the offset to translate in y
     * @param dest
     * will hold the result
     * @return dest
     */
    fun translate(x: Float, y: Float, dest: Matrix3x2f): Matrix3x2f {
        val rm20 = x
        val rm21 = y
        dest.m20 = m00 * rm20 + m10 * rm21 + m20
        dest.m21 = m01 * rm20 + m11 * rm21 + m21
        dest.m00 = m00
        dest.m01 = m01
        dest.m10 = m10
        dest.m11 = m11
        return dest
    }

    /**
     * Apply a translation to this matrix by translating by the given number of units in x and y.
     *
     *
     * If `M` is `this` matrix and `T` the translation
     * matrix, then the new matrix will be `M * T`. So when
     * transforming a vector `v` with the new matrix by using
     * `M * T * v`, the translation will be applied first!
     *
     *
     * In order to set the matrix to a translation transformation without post-multiplying
     * it, use [.translation].
     *
     * @see .translation
     * @param x
     * the offset to translate in x
     * @param y
     * the offset to translate in y
     * @return this
     */
    fun translate(x: Float, y: Float): Matrix3x2f {
        return translate(x, y, this)
    }

    /**
     * Apply a rotation transformation to this matrix by rotating the given amount of radians.
     *
     *
     * If `M` is `this` matrix and `R` the rotation matrix,
     * then the new matrix will be `M * R`. So when transforming a
     * vector `v` with the new matrix by using `M * R * v`
     * , the rotation will be applied first!
     *
     * @param ang
     * the angle in radians
     * @return this
     */
    fun rotate(ang: Float): Matrix3x2f {
        return rotate(ang, this)
    }

    /**
     * Apply a rotation transformation to this matrix by rotating the given amount of radians and store the result in `dest`.
     *
     *
     * If `M` is `this` matrix and `R` the rotation matrix,
     * then the new matrix will be `M * R`. So when transforming a
     * vector `v` with the new matrix by using `M * R * v`, the rotation will be applied first!
     *
     * @param ang
     * the angle in radians
     * @param dest
     * will hold the result
     * @return dest
     */
    fun rotate(ang: Float, dest: Matrix3x2f): Matrix3x2f {
        val cos = cos(ang.toDouble()).toFloat()
        val sin = sin(ang.toDouble()).toFloat()
        val rm00 = cos
        val rm01 = sin
        val rm10 = -sin
        val rm11 = cos
        val nm00 = m00 * rm00 + m10 * rm01
        val nm01 = m01 * rm00 + m11 * rm01
        dest.m10 = m00 * rm10 + m10 * rm11
        dest.m11 = m01 * rm10 + m11 * rm11
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m20 = m20
        dest.m21 = m21
        return dest
    }

    /**
     * Transform/multiply the given 2D-vector, as if it was a 3D-vector with z=1, by
     * this matrix and store the result in that vector.
     *
     *
     * The given 2D-vector is treated as a 3D-vector with its z-component being 1.0, so it
     * will represent a position/location in 2D-space rather than a direction.
     *
     *
     * In order to store the result in another vector, use [.transformPosition].
     *
     * @see .transformPosition
     * @see .transform
     * @param v
     * the vector to transform and to hold the final result
     * @return v
     */
    fun transformPosition(v: Vector2f): Vector2f {
        v.set(m00 * v.x + m10 * v.y + m20, m01 * v.x + m11 * v.y + m21)
        return v
    }
}