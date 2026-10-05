package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.MathUtils
import com.mrpowergamerbr.starfait.wad.Path
import kotlin.math.sqrt

// See GameMaker-HTML5's yyPath.js
class RunnerPath(path: Path) {
    class IntPoint(val x: Float, val y: Float, val speed: Float) {
        // its length from the start (not used in waypoints)
        var l = 0.0f
    }

    private val kind = path.kind
    private val closed = path.closed
    private val precision = path.precision
    private val points = path.points
    private val intpoints = mutableListOf<IntPoint>()
    // length of the path
    var length = 0.0f
        private set

    init {
        computeInternal()
    }

    // computes the length fields for the internal path
    private fun computeLength() {
        this.length = 0.0f
        if (this.intpoints.isEmpty()) return
        this.intpoints[0].l = 0.0f
        for (i in 1 until this.intpoints.size) {
            this.length += sqrt(MathUtils.squared(this.intpoints[i].x - this.intpoints[i - 1].x) + MathUtils.squared(this.intpoints[i].y - this.intpoints[i - 1].y))
            this.intpoints[i].l = this.length
        }
    }

    // Adds an internal point to the set
    private fun addInternalPoint(xx: Float, yy: Float, ss: Float) {
        this.intpoints.add(IntPoint(xx, yy, ss))
    }

    // computes a linear internal path
    private fun computeLinear() {
        this.intpoints.clear()
        if (this.points.isEmpty()) return
        for (point in this.points) {
            addInternalPoint(point.x, point.y, point.speed)
        }
        if (this.closed) {
            addInternalPoint(this.points[0].x, this.points[0].y, this.points[0].speed)
        }
    }

    private fun handlePiece(depth: Int, x1: Float, y1: Float, s1: Float, x2: Float, y2: Float, s2: Float, x3: Float, y3: Float, s3: Float) {
        if (depth == 0) return

        val mx = (x1 + x2 + x2 + x3) / 4.0f
        val my = (y1 + y2 + y2 + y3) / 4.0f
        val ms = (s1 + s2 + s2 + s3) / 4.0f

        if (MathUtils.squared(x2 - x1) + MathUtils.squared(y2 - y1) > 16.0f) {
            handlePiece(depth - 1, x1, y1, s1, (x2 + x1) / 2.0f, (y2 + y1) / 2.0f, (s2 + s1) / 2.0f, mx, my, ms)
        }

        addInternalPoint(mx, my, ms)
        if (MathUtils.squared(x2 - x3) + MathUtils.squared(y2 - y3) > 16.0f) {
            handlePiece(depth - 1, mx, my, ms, (x3 + x2) / 2.0f, (y3 + y2) / 2.0f, (s3 + s2) / 2.0f, x3, y3, s3)
        }
    }

    // computes a curved internal path
    private fun computeCurved() {
        this.intpoints.clear()

        val count = this.points.size
        if (0 >= count) return
        if (!this.closed) {
            addInternalPoint(this.points[0].x, this.points[0].y, this.points[0].speed)
        }
        val n = if (this.closed) count - 1 else count - 3

        for (i in 0..n) {
            val point1 = this.points[i % count]
            val point2 = this.points[(i + 1) % count]
            val point3 = this.points[(i + 2) % count]
            handlePiece(
                this.precision,
                (point1.x + point2.x) / 2.0f, (point1.y + point2.y) / 2.0f, (point1.speed + point2.speed) / 2.0f,
                point2.x, point2.y, point2.speed,
                (point2.x + point3.x) / 2.0f, (point2.y + point3.y) / 2.0f, (point2.speed + point3.speed) / 2.0f
            )
        }

        if (!this.closed) {
            addInternalPoint(this.points[count - 1].x, this.points[count - 1].y, this.points[count - 1].speed)
        } else {
            addInternalPoint(this.intpoints[0].x, this.intpoints[0].y, this.intpoints[0].speed)
        }
    }

    private fun computeInternal() {
        if (this.kind == Path.PathKind.CURVED) {
            computeCurved()
        } else {
            computeLinear()
        }
        computeLength()
    }

    fun getPosition(ind: Float): IntPoint {
        val intcount = this.intpoints.size
        // easy cases
        if (0 >= intcount) {
            return IntPoint(0.0f, 0.0f, 0.0f)
        }
        if ((intcount == 1) || (this.length == 0.0f) || (0.0f >= ind)) {
            return this.intpoints[0]
        }

        if (ind >= 1.0f) {
            return this.intpoints[intcount - 1]
        }

        // get the right interval
        var l = this.length * ind
        var pos = 0

        while ((intcount - 2 > pos) && (l >= this.intpoints[pos + 1].l)) {
            pos++
        }

        // find the right coordinate
        val pNode = this.intpoints[pos]
        l -= pNode.l
        val w = this.intpoints[pos + 1].l - pNode.l

        if (w != 0.0f) {
            pos++
            return IntPoint(
                pNode.x + l * (this.intpoints[pos].x - pNode.x) / w,
                pNode.y + l * (this.intpoints[pos].y - pNode.y) / w,
                pNode.speed + l * (this.intpoints[pos].speed - pNode.speed) / w
            )
        }
        return pNode
    }
}
