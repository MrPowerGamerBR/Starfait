package com.mrpowergamerbr.starfait.web.audio.web

import com.mrpowergamerbr.starfait.audio.TimeRampedParamLinear
import web.audio.AudioBuffer
import web.audio.AudioBufferSourceNode
import web.audio.AudioContext
import web.audio.GainNode

class WebSoundInstance(
    val audioContext: AudioContext,
    val instanceId: Int,
    val soundIndex: Int,
    val loop: Boolean,
    val gainNode: GainNode
) {
    val gain = TimeRampedParamLinear(1f)
    var pitch = 1f
        private set
    // Null while the AudioBuffer is still being decoded
    var buffer: AudioBuffer? = null
        private set
    // AudioBufferSourceNodes can only be started once, so pausing throws the node away and resuming creates a new one
    private var sourceNode: AudioBufferSourceNode? = null
    // Position in the buffer (in seconds) when the current source node started playing
    private var offset = 0.0
    private var startedAt = 0.0
    var paused = false
        private set
    var freed = false
        private set

    val isPlaying get() = sourceNode != null

    fun attachBuffer(buffer: AudioBuffer) {
        if (freed)
            return

        this.buffer = buffer
        if (!paused)
            startSourceNode()
    }

    fun position(): Double {
        val buffer = buffer ?: return 0.0
        val sourceNode = sourceNode ?: return offset

        val position = offset + (audioContext.currentTime - startedAt) * pitch
        return if (loop) position % buffer.duration else position
    }

    fun hasReachedEnd(): Boolean {
        val buffer = buffer ?: return false
        return !loop && !paused && position() >= buffer.duration
    }

    fun pause() {
        if (paused || freed)
            return

        offset = position()
        stopSourceNode()
        paused = true
    }

    fun resume() {
        if (!paused || freed)
            return

        paused = false
        if (buffer != null)
            startSourceNode()
    }

    fun setPitch(pitch: Float) {
        // Rebase the position, because the time elapsed since startedAt was played at the old pitch
        offset = position()
        startedAt = audioContext.currentTime
        this.pitch = pitch
        sourceNode?.playbackRate?.value = pitch
    }

    fun free() {
        if (freed)
            return

        stopSourceNode()
        gainNode.disconnect()
        freed = true
    }

    private fun startSourceNode() {
        val node = audioContext.createBufferSource()
        node.buffer = buffer
        node.loop = loop
        node.playbackRate.value = pitch
        node.connect(gainNode)
        node.start(0.0, offset)

        startedAt = audioContext.currentTime
        sourceNode = node
    }

    private fun stopSourceNode() {
        val node = sourceNode ?: return
        node.stop()
        node.disconnect()
        sourceNode = null
    }
}
