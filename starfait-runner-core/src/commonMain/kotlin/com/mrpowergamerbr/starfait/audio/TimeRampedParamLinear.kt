package com.mrpowergamerbr.starfait.audio

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

// Ported from GameMaker-HTML5/scripts/utils/TimeRampedParamLinear.js
class TimeRampedParamLinear(initialVal: Float) {
    private var value = initialVal
    private var origin = initialVal
    private var target = initialVal
    private var diff = 0f
    private var rampStart = TimeSource.Monotonic.markNow()
    private var rampDuration = Duration.ZERO

    fun get() = value

    fun set(target: Float, timeMs: Int = 0) {
        val timeMs = maxOf(0, timeMs)

        if (timeMs == 0) {
            this.value = target
            this.target = target
            this.rampDuration = 0.milliseconds
        } else {
            this.origin = this.value
            this.target = target
            this.diff = this.target - this.origin

            this.rampStart = TimeSource.Monotonic.markNow()
            this.rampDuration = timeMs.milliseconds
        }
    }

    fun update(): Float {
        val currentTime = TimeSource.Monotonic.markNow()

        if (currentTime >= (rampStart + rampDuration)) {
            value = target
        } else {
            val frac = (currentTime - rampStart) / rampDuration
            value = origin + (diff * frac).toFloat()
        }

        return value
    }
}
