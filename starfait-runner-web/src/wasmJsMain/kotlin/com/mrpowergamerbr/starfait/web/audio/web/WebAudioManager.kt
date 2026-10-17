package com.mrpowergamerbr.starfait.web.audio.web

import com.mrpowergamerbr.starfait.audio.AudioManager
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.Sound
import js.buffer.toArrayBuffer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import web.audio.AudioContext
import web.audio.decodeAudioData

class WebAudioManager(val gameWAD: GameWAD) : AudioManager() {
    val audioContext = AudioContext()

    override fun playSound(soundIndex: Int, sound: Sound, loop: Boolean): Int {
        val audio = gameWAD.audo.blobs[sound.audoIndex]

        GlobalScope.launch {
            val buffer = audio.toArrayBuffer()
            val audioBuffer = audioContext.decodeAudioData(buffer)
            val source = audioContext.createBufferSource()
            source.buffer = audioBuffer
            source.connect(audioContext.destination)
            source.start()
        }

        return 400000
    }

    override fun stopSound(soundIndexOrInstanceId: Int) {
        // TODO("Not yet implemented")
    }

    override fun pauseSound(soundIndexOrInstanceId: Int) {
        // TODO("Not yet implemented")
    }

    override fun resumeSound(soundIndexOrInstanceId: Int) {
        // TODO("Not yet implemented")
    }

    override fun stopAllSounds() {
        // TODO("Not yet implemented")
    }

    override fun isAudioPlaying(soundIndexOrInstanceId: Int): Boolean {
        // TODO("Not yet implemented")
        return false
    }

    override fun setAudioPitch(soundIndexOrInstanceId: Int, pitch: Float) {
        // TODO("Not yet implemented")
    }

    override fun setAudioGain(soundIndexOrInstanceId: Int, gain: Float, timeMs: Int) {
        // TODO("Not yet implemented")
    }

    override fun tick() {
        // TODO("Not yet implemented")
    }
}