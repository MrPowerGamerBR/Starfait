package com.mrpowergamerbr.starfait.web.audio.web

import com.mrpowergamerbr.starfait.audio.AudioManager
import com.mrpowergamerbr.starfait.filesystem.FileSystem
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.Sound
import com.mrpowergamerbr.starfait.web.toArrayBufferFast
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import web.audio.AudioBuffer
import web.audio.AudioContext
import web.audio.decodeAudioData

class WebAudioManager(val gameWAD: GameWAD, val fileSystem: FileSystem, masterVolume: Float = 1f) : AudioManager() {
    private val soundInstances = mutableListOf<WebSoundInstance>()
    // TODO: Move this somewhere else - This is from GameMaker
    private var currentSoundInstanceId = 400_000

    val audioContext = AudioContext()
    private val masterGainNode = audioContext.createGain().apply {
        gain.value = masterVolume
        connect(audioContext.destination)
    }

    // Deferreds instead of AudioBuffers so that playing the same sound twice while it is still decoding doesn't decode it twice
    private val decodedEmbeddedAudioBuffers = mutableMapOf<Int, Deferred<AudioBuffer>>()
    private val decodedStreamedAudioBuffers = mutableMapOf<String, Deferred<AudioBuffer>>()

    fun filterSoundInstancesBySoundIndexOrInstanceId(soundIndexOrInstanceId: Int): List<WebSoundInstance> {
        return this.soundInstances.filter {
            if (it.freed)
                return@filter false

            if (400_000 > soundIndexOrInstanceId) {
                return@filter it.soundIndex == soundIndexOrInstanceId
            } else {
                return@filter it.instanceId == soundIndexOrInstanceId
            }
        }
    }

    // Browsers create AudioContexts as "suspended" until the user interacts with the page (autoplay policy), so this must be called from a user gesture
    fun resumeAudioContext() {
        audioContext.resumeAsync()
    }

    override fun playSound(soundIndex: Int, sound: Sound, loop: Boolean): Int {
        val audioBuffer = when (sound.kind) {
            Sound.Kind.COMPRESSED_STREAMED -> {
                // TECHNICALLY sounds can be something that aren't OGGs, but for now, this shall do
                val fileName = sound.fileName

                decodedStreamedAudioBuffers.getOrPut(fileName) {
                    decodeAsync { fileSystem.readFile(fileName)!! }
                }
            }
            Sound.Kind.UNCOMPRESSED_NOT_STREAMED, Sound.Kind.COMPRESSED_NOT_STREAMED -> {
                decodedEmbeddedAudioBuffers.getOrPut(sound.audoIndex) {
                    decodeAsync { gameWAD.audo.blobs[sound.audoIndex] }
                }
            }
            Sound.Kind.COMPRESSED_UNCOMPRESS_ON_LOAD_NOT_STREAMED -> TODO()
        }

        val gainNode = audioContext.createGain()
        gainNode.connect(masterGainNode)

        val soundInstance = WebSoundInstance(audioContext, this.currentSoundInstanceId++, soundIndex, loop, gainNode)
        soundInstances.add(soundInstance)

        if (audioBuffer.isCompleted) {
            soundInstance.attachBuffer(audioBuffer.getCompleted())
        } else {
            GlobalScope.launch {
                try {
                    soundInstance.attachBuffer(audioBuffer.await())
                } catch (e: Throwable) {
                    println("Failed to decode sound $soundIndex: $e")
                    soundInstance.free()
                }
            }
        }

        return soundInstance.instanceId
    }

    private fun decodeAsync(bytesProvider: () -> ByteArray) = GlobalScope.async {
        audioContext.decodeAudioData(bytesProvider().toArrayBufferFast())
    }

    override fun stopSound(soundIndexOrInstanceId: Int) {
        for (soundInstance in this.filterSoundInstancesBySoundIndexOrInstanceId(soundIndexOrInstanceId)) {
            soundInstance.free()
        }
    }

    override fun pauseSound(soundIndexOrInstanceId: Int) {
        for (soundInstance in this.filterSoundInstancesBySoundIndexOrInstanceId(soundIndexOrInstanceId)) {
            soundInstance.pause()
        }
    }

    override fun resumeSound(soundIndexOrInstanceId: Int) {
        for (soundInstance in this.filterSoundInstancesBySoundIndexOrInstanceId(soundIndexOrInstanceId)) {
            soundInstance.resume()
        }
    }

    override fun stopAllSounds() {
        for (soundInstance in this.soundInstances) {
            soundInstance.free()
        }
        this.soundInstances.clear()
    }

    override fun isAudioPlaying(soundIndexOrInstanceId: Int): Boolean {
        return this.filterSoundInstancesBySoundIndexOrInstanceId(soundIndexOrInstanceId).isNotEmpty()
    }

    override fun setAudioPitch(soundIndexOrInstanceId: Int, pitch: Float) {
        for (soundInstance in this.filterSoundInstancesBySoundIndexOrInstanceId(soundIndexOrInstanceId)) {
            soundInstance.setPitch(pitch)
        }
    }

    override fun setAudioGain(soundIndexOrInstanceId: Int, gain: Float, timeMs: Int) {
        for (soundInstance in this.filterSoundInstancesBySoundIndexOrInstanceId(soundIndexOrInstanceId)) {
            soundInstance.gain.set(gain, timeMs)
            soundInstance.gainNode.gain.value = soundInstance.gain.update()
        }
    }

    override fun tick() {
        for (soundInstance in soundInstances) {
            if (soundInstance.freed)
                continue

            // Ramped on the game side (instead of linearRampToValueAtTime) to match the SDL backend and so that the gain can be read back
            val previousGain = soundInstance.gain.get()
            val currentGain = soundInstance.gain.update()
            if (previousGain != currentGain)
                soundInstance.gainNode.gain.value = currentGain

            if (soundInstance.hasReachedEnd())
                soundInstance.free()
        }

        this.soundInstances.removeAll { it.freed }
    }
}
