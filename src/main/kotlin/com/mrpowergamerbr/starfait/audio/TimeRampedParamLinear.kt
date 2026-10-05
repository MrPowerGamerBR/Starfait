package com.mrpowergamerbr.starfait.audio

// Ported from GameMaker-HTML5/scripts/utils/TimeRampedParamLinear.js
class TimeRampedParamLinear(initialVal: Float) {
    private var value = initialVal
    private var origin = initialVal
    private var target = initialVal
    private var diff = 0f
    private var rampStart = 0L
    private var rampEnd = 0L
    private var rampScalar = 0.0

    fun get() = value

    fun set(target: Float, timeMs: Int = 0) {
        val timeMs = maxOf(0, timeMs)

        if (timeMs == 0) {
            this.value = target
            this.target = target
            this.rampEnd = System.nanoTime()
        } else {
            this.origin = this.value
            this.target = target
            this.diff = this.target - this.origin

            this.rampStart = System.nanoTime()
            this.rampEnd = this.rampStart + timeMs * 1_000_000L
            this.rampScalar = 1.0 / (this.rampEnd - this.rampStart)
        }
    }

    fun update(): Float {
        val currentTime = System.nanoTime()

        if (currentTime >= rampEnd) {
            value = target
        } else {
            val frac = ((currentTime - rampStart) * rampScalar).coerceIn(0.0, 1.0)
            value = origin + (diff * frac).toFloat()
        }

        return value
    }
}
