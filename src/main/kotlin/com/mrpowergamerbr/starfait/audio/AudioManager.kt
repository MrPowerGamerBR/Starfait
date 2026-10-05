package com.mrpowergamerbr.starfait.audio

import com.mrpowergamerbr.starfait.wad.Sound

abstract class AudioManager {
    abstract fun playSound(soundIndex: Int, sound: Sound, loop: Boolean): Int
    abstract fun stopSound(soundIndexOrInstanceId: Int)
    abstract fun pauseSound(soundIndexOrInstanceId: Int)
    abstract fun resumeSound(soundIndexOrInstanceId: Int)
    abstract fun stopAllSounds()
    abstract fun isAudioPlaying(soundIndexOrInstanceId: Int): Boolean
    abstract fun setAudioPitch(soundIndexOrInstanceId: Int, pitch: Float)
    abstract fun setAudioGain(soundIndexOrInstanceId: Int, gain: Float)
    abstract fun tick()
}