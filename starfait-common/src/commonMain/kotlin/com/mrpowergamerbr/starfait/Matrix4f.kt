package com.mrpowergamerbr.starfait

import kotlin.math.cos
import kotlin.math.sin

// From JOML, only the generic (non-properties) paths
class Matrix4f {
    var m00 = 0f
    var m01 = 0f
    var m02 = 0f
    var m03 = 0f
    var m10 = 0f
    var m11 = 0f
    var m12 = 0f
    var m13 = 0f
    var m20 = 0f
    var m21 = 0f
    var m22 = 0f
    var m23 = 0f
    var m30 = 0f
    var m31 = 0f
    var m32 = 0f
    var m33 = 0f

    /**
     * Create a new [Matrix4f] and set it to identity.
     */
    constructor() {
        this.m00 = 1.0f
        this.m11 = 1.0f
        this.m22 = 1.0f
        this.m33 = 1.0f
    }

    /**
     * Apply a translation to this matrix by translating by the given number of
     * units in x, y and z and store the result in `dest`.
     *
     *
     * If `M` is `this` matrix and `T` the translation
     * matrix, then the new matrix will be `M * T`. So when
     * transforming a vector `v` with the new matrix by using
     * `M * T * v`, the translation will be applied first!
     *
     * @param x
     * the offset to translate in x
     * @param y
     * the offset to translate in y
     * @param z
     * the offset to translate in z
     * @param dest
     * will hold the result
     * @return dest
     */
    fun translate(x: Float, y: Float, z: Float, dest: Matrix4f): Matrix4f {
        val nm30 = m00 * x + (m10 * y + (m20 * z + m30))
        val nm31 = m01 * x + (m11 * y + (m21 * z + m31))
        val nm32 = m02 * x + (m12 * y + (m22 * z + m32))
        val nm33 = m03 * x + (m13 * y + (m23 * z + m33))
        dest.m00 = m00
        dest.m01 = m01
        dest.m02 = m02
        dest.m03 = m03
        dest.m10 = m10
        dest.m11 = m11
        dest.m12 = m12
        dest.m13 = m13
        dest.m20 = m20
        dest.m21 = m21
        dest.m22 = m22
        dest.m23 = m23
        dest.m30 = nm30
        dest.m31 = nm31
        dest.m32 = nm32
        dest.m33 = nm33
        return dest
    }

    /**
     * Apply a translation to this matrix by translating by the given number of
     * units in x, y and z.
     *
     *
     * If `M` is `this` matrix and `T` the translation
     * matrix, then the new matrix will be `M * T`. So when
     * transforming a vector `v` with the new matrix by using
     * `M * T * v`, the translation will be applied first!
     *
     * @param x
     * the offset to translate in x
     * @param y
     * the offset to translate in y
     * @param z
     * the offset to translate in z
     * @return this
     */
    fun translate(x: Float, y: Float, z: Float): Matrix4f {
        return translate(x, y, z, this)
    }

    /**
     * Apply rotation about the Z axis to this matrix by rotating the given amount of radians
     * and store the result in `dest`.
     *
     *
     * When used with a right-handed coordinate system, the produced rotation will rotate a vector
     * counter-clockwise around the rotation axis, when viewing along the negative axis direction towards the origin.
     * When used with a left-handed coordinate system, the rotation is clockwise.
     *
     *
     * If `M` is `this` matrix and `R` the rotation matrix,
     * then the new matrix will be `M * R`. So when transforming a
     * vector `v` with the new matrix by using `M * R * v`, the
     * rotation will be applied first!
     *
     * @param ang
     * the angle in radians
     * @param dest
     * will hold the result
     * @return dest
     */
    fun rotateZ(ang: Float, dest: Matrix4f): Matrix4f {
        val sin = sin(ang.toDouble()).toFloat()
        val cos = cos(ang.toDouble()).toFloat()
        return rotateTowardsXY(sin, cos, dest)
    }

    /**
     * Apply rotation about the Z axis to this matrix by rotating the given amount of radians.
     *
     *
     * When used with a right-handed coordinate system, the produced rotation will rotate a vector
     * counter-clockwise around the rotation axis, when viewing along the negative axis direction towards the origin.
     * When used with a left-handed coordinate system, the rotation is clockwise.
     *
     *
     * If `M` is `this` matrix and `R` the rotation matrix,
     * then the new matrix will be `M * R`. So when transforming a
     * vector `v` with the new matrix by using `M * R * v`, the
     * rotation will be applied first!
     *
     * @param ang
     * the angle in radians
     * @return this
     */
    fun rotateZ(ang: Float): Matrix4f {
        return rotateZ(ang, this)
    }

    /**
     * Apply rotation about the Z axis to align the local `+X` towards `(dirX, dirY)` and store the result in `dest`.
     *
     *
     * If `M` is `this` matrix and `R` the rotation matrix,
     * then the new matrix will be `M * R`. So when transforming a
     * vector `v` with the new matrix by using `M * R * v`, the
     * rotation will be applied first!
     *
     *
     * The vector `(dirX, dirY)` must be a unit vector.
     *
     * @param dirX
     * the x component of the normalized direction
     * @param dirY
     * the y component of the normalized direction
     * @param dest
     * will hold the result
     * @return dest
     */
    fun rotateTowardsXY(dirX: Float, dirY: Float, dest: Matrix4f): Matrix4f {
        val nm00 = m00 * dirY + m10 * dirX
        val nm01 = m01 * dirY + m11 * dirX
        val nm02 = m02 * dirY + m12 * dirX
        val nm03 = m03 * dirY + m13 * dirX
        dest.m10 = m00 * -dirX + m10 * dirY
        dest.m11 = m01 * -dirX + m11 * dirY
        dest.m12 = m02 * -dirX + m12 * dirY
        dest.m13 = m03 * -dirX + m13 * dirY
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m02 = nm02
        dest.m03 = nm03
        dest.m20 = m20
        dest.m21 = m21
        dest.m22 = m22
        dest.m23 = m23
        dest.m30 = m30
        dest.m31 = m31
        dest.m32 = m32
        dest.m33 = m33
        return dest
    }

    /**
     * Apply an orthographic projection transformation for a right-handed coordinate system
     * using the given NDC z range to this matrix and store the result in `dest`.
     *
     *
     * If `M` is `this` matrix and `O` the orthographic projection matrix,
     * then the new matrix will be `M * O`. So when transforming a
     * vector `v` with the new matrix by using `M * O * v`, the
     * orthographic projection transformation will be applied first!
     *
     *
     * Reference: [http://www.songho.ca](http://www.songho.ca/opengl/gl_projectionmatrix.html#ortho)
     *
     * @param left
     * the distance from the center to the left frustum edge
     * @param right
     * the distance from the center to the right frustum edge
     * @param bottom
     * the distance from the center to the bottom frustum edge
     * @param top
     * the distance from the center to the top frustum edge
     * @param zNear
     * near clipping plane distance
     * @param zFar
     * far clipping plane distance
     * @param zZeroToOne
     * whether to use Vulkan's and Direct3D's NDC z range of `[0..+1]` when `true`
     * or whether to use OpenGL's NDC z range of `[-1..+1]` when `false`
     * @param dest
     * will hold the result
     * @return dest
     */
    fun ortho(left: Float, right: Float, bottom: Float, top: Float, zNear: Float, zFar: Float, zZeroToOne: Boolean, dest: Matrix4f): Matrix4f {
        // calculate right matrix elements
        val rm00 = 2.0f / (right - left)
        val rm11 = 2.0f / (top - bottom)
        val rm22 = (if (zZeroToOne) 1.0f else 2.0f) / (zNear - zFar)
        val rm30 = (left + right) / (left - right)
        val rm31 = (top + bottom) / (bottom - top)
        val rm32 = (if (zZeroToOne) zNear else (zFar + zNear)) / (zNear - zFar)
        // perform optimized multiplication
        // compute the last column first, because other columns do not depend on it
        dest.m30 = m00 * rm30 + m10 * rm31 + m20 * rm32 + m30
        dest.m31 = m01 * rm30 + m11 * rm31 + m21 * rm32 + m31
        dest.m32 = m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
        dest.m33 = m03 * rm30 + m13 * rm31 + m23 * rm32 + m33
        dest.m00 = m00 * rm00
        dest.m01 = m01 * rm00
        dest.m02 = m02 * rm00
        dest.m03 = m03 * rm00
        dest.m10 = m10 * rm11
        dest.m11 = m11 * rm11
        dest.m12 = m12 * rm11
        dest.m13 = m13 * rm11
        dest.m20 = m20 * rm22
        dest.m21 = m21 * rm22
        dest.m22 = m22 * rm22
        dest.m23 = m23 * rm22
        return dest
    }

    /**
     * Apply an orthographic projection transformation for a right-handed coordinate system
     * using OpenGL's NDC z range of `[-1..+1]` to this matrix.
     *
     *
     * If `M` is `this` matrix and `O` the orthographic projection matrix,
     * then the new matrix will be `M * O`. So when transforming a
     * vector `v` with the new matrix by using `M * O * v`, the
     * orthographic projection transformation will be applied first!
     *
     *
     * Reference: [http://www.songho.ca](http://www.songho.ca/opengl/gl_projectionmatrix.html#ortho)
     *
     * @param left
     * the distance from the center to the left frustum edge
     * @param right
     * the distance from the center to the right frustum edge
     * @param bottom
     * the distance from the center to the bottom frustum edge
     * @param top
     * the distance from the center to the top frustum edge
     * @param zNear
     * near clipping plane distance
     * @param zFar
     * far clipping plane distance
     * @return this
     */
    fun ortho(left: Float, right: Float, bottom: Float, top: Float, zNear: Float, zFar: Float): Matrix4f {
        return ortho(left, right, bottom, top, zNear, zFar, false, this)
    }

    /**
     * Transform/multiply the given 3D-vector, as if it was a 4D-vector with w=1, by
     * this matrix and store the result in `dest`.
     *
     *
     * The given 3D-vector is treated as a 4D-vector with its w-component being 1.0, so it
     * will represent a position/location in 3D-space rather than a direction. This method is therefore
     * not suited for perspective projection transformations as it will not save the
     * `w` component of the transformed vector.
     *
     * @param v
     * the vector to transform
     * @param dest
     * will hold the result
     * @return dest
     */
    fun transformPosition(v: Vector3f, dest: Vector3f): Vector3f {
        val x = v.x
        val y = v.y
        val z = v.z
        dest.x = m00 * x + (m10 * y + (m20 * z + m30))
        dest.y = m01 * x + (m11 * y + (m21 * z + m31))
        dest.z = m02 * x + (m12 * y + (m22 * z + m32))
        return dest
    }

    /**
     * Store this matrix in column-major order into the given float array.
     *
     * @param arr
     * the array to write the matrix values into
     * @return the passed in array
     */
    fun get(arr: FloatArray): FloatArray {
        arr[0] = m00
        arr[1] = m01
        arr[2] = m02
        arr[3] = m03
        arr[4] = m10
        arr[5] = m11
        arr[6] = m12
        arr[7] = m13
        arr[8] = m20
        arr[9] = m21
        arr[10] = m22
        arr[11] = m23
        arr[12] = m30
        arr[13] = m31
        arr[14] = m32
        arr[15] = m33
        return arr
    }
}
