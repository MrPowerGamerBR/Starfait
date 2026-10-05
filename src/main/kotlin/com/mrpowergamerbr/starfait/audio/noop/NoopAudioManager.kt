package com.mrpowergamerbr.starfait.audio.noop

import com.mrpowergamerbr.starfait.audio.AudioManager
import com.mrpowergamerbr.starfait.wad.Sound

class NoopAudioManager : AudioManager() {
    override fun playSound(soundIndex: Int, sound: Sound, loop: Boolean): Int {
        return 400_000
    }

    override fun stopSound(soundIndexOrInstanceId: Int) {}
    override fun pauseSound(soundIndexOrInstanceId: Int) {}
    override fun resumeSound(soundIndexOrInstanceId: Int) {}
    override fun stopAllSounds() {}
    override fun isAudioPlaying(soundIndexOrInstanceId: Int) = false
    override fun setAudioPitch(soundIndexOrInstanceId: Int, pitch: Float) {}
    override fun setAudioGain(soundIndexOrInstanceId: Int, gain: Float, timeMs: Int) {}
    override fun tick() {}
}