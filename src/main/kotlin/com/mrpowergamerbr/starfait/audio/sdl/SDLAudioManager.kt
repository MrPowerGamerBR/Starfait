package com.mrpowergamerbr.starfait.audio.sdl

import com.mrpowergamerbr.starfait.audio.AudioManager
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.Sound
import org.lwjgl.sdl.SDLAudio.SDL_AUDIO_S16
import org.lwjgl.sdl.SDLAudio.SDL_BindAudioStream
import org.lwjgl.sdl.SDLAudio.SDL_CreateAudioStream
import org.lwjgl.sdl.SDLAudio.SDL_FlushAudioStream
import org.lwjgl.sdl.SDLAudio.SDL_GetAudioStreamQueued
import org.lwjgl.sdl.SDLAudio.SDL_LoadWAV_IO
import org.lwjgl.sdl.SDLAudio.SDL_PutAudioStreamData
import org.lwjgl.sdl.SDLAudio.SDL_SetAudioStreamFrequencyRatio
import org.lwjgl.sdl.SDLAudio.SDL_UnbindAudioStream
import org.lwjgl.sdl.SDLError.SDL_GetError
import org.lwjgl.sdl.SDLIOStream.SDL_IOFromConstMem
import org.lwjgl.sdl.SDL_AudioSpec
import org.lwjgl.stb.STBVorbis.stb_vorbis_get_info
import org.lwjgl.stb.STBVorbis.stb_vorbis_get_samples_short_interleaved
import org.lwjgl.stb.STBVorbis.stb_vorbis_open_memory
import org.lwjgl.stb.STBVorbis.stb_vorbis_seek_start
import org.lwjgl.stb.STBVorbisInfo
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memByteBuffer
import org.lwjgl.system.MemoryUtil.memFree
import java.io.File
import java.nio.ByteBuffer
import kotlin.check
import kotlin.error

class SDLAudioManager(val gameWAD: GameWAD, val sdlDeviceId: Int) : AudioManager() {
    private val soundInstances = mutableListOf<SDLSoundInstance>()
    // TODO: Move this somewhere else - This is from GameMaker
    private var currentSoundInstanceId = 400_000

    fun filterSoundInstancesBySoundIndexOrInstanceId(soundIndexOrInstanceId: Int): List<SDLSoundInstance> {
        return this.soundInstances.filter {
            if (400_000 > soundIndexOrInstanceId) {
                return@filter it.soundIndex == soundIndexOrInstanceId
            } else {
                return@filter it.instanceId == soundIndexOrInstanceId
            }
        }
    }

    override fun playSound(soundIndex: Int, sound: Sound, loop: Boolean): Int {
        val extension = gameWAD.strg.getString(sound.fileType)
        // println("Trying to play sound ${gameWAD.strg.getString(sound.name)} with priority $priority and loop $loop and ${sound.kind} ${runner.gameWAD.strg.getString(sound.fileType)}")

        val audo = gameWAD.audo.blobs[sound.audoIndex]

        // TECHNICALLY we should handle it based on the audio kind, but it is what it is, so let's keep it like this for now
        when (extension) {
            ".ogg" -> {
                val stack = MemoryStack.stackPush()
                val song = File("/home/mrpowergamerbr/Projects/ButterGMGames/Undertale_108/", gameWAD.strg.getString(sound.name) + ".ogg")
                val songBytes = song.readBytes()

                // memAlloc is just like a ByteBuffer
                val byteBuffer = memAlloc(songBytes.size)
                byteBuffer.put(songBytes).position(0)

                val pError = stack.mallocInt(1)

                // This keeps a "position" internally
                val stbVorbisId = stb_vorbis_open_memory(byteBuffer, pError, null)
                checkNotNull(stbVorbisId) { "Failed to decode OGG!" }

                val stbVorbisInfo = STBVorbisInfo.calloc(stack)

                stb_vorbis_get_info(stbVorbisId, stbVorbisInfo)

                val spec = SDL_AudioSpec.malloc(stack)
                    .format(SDL_AUDIO_S16)
                    .channels(stbVorbisInfo.channels())
                    .freq(stbVorbisInfo.sample_rate())

                val sdlStreamId = SDL_CreateAudioStream(spec, null)
                check(sdlStreamId != MemoryUtil.NULL) { "Something went wrong when opening the audio device stream! ${SDL_GetError()}" }
                val successfullyBound = SDL_BindAudioStream(this.sdlDeviceId, sdlStreamId)
                check(successfullyBound) { "Failed to bind SDL stream to SDL device! ${SDL_GetError()}" }

                val instanceId = this.currentSoundInstanceId++
                soundInstances.add(
                    SDLSoundInstance.StreamedSDLSoundInstance(
                        sdlStreamId,
                        instanceId,
                        soundIndex,
                        loop,
                        stbVorbisId,
                        stbVorbisInfo,
                        stack
                    )
                )

                return instanceId
            }
            ".wav" -> {
                val stack = MemoryStack.stackPush()

                val directByteBuffer = ByteBuffer.allocateDirect(audo.size)
                directByteBuffer.put(audo).flip()

                val pBuf = stack.mallocPointer(1)
                val pLen = stack.mallocInt(1)

                val spec = SDL_AudioSpec.calloc(stack)

                val io = SDL_IOFromConstMem(directByteBuffer)

                val successfullyDecoded = SDL_LoadWAV_IO(io, true, spec, pBuf, pLen)
                check(successfullyDecoded) { "Failed to decode WAV file!" }

                val sdlStreamId = SDL_CreateAudioStream(spec, null)
                check(sdlStreamId != MemoryUtil.NULL) { "Something went wrong when opening the audio device stream! ${SDL_GetError()}" }
                val successfullyBound = SDL_BindAudioStream(this.sdlDeviceId, sdlStreamId)
                check(successfullyBound) { "Failed to bind SDL stream to SDL device! ${SDL_GetError()}" }

                val buffer = memByteBuffer(pBuf.get(), pLen.get(0))

                val instanceId = this.currentSoundInstanceId++
                soundInstances.add(
                    SDLSoundInstance.UncompressedSDLSoundInstance(
                        sdlStreamId,
                        instanceId,
                        soundIndex,
                        loop,
                        buffer,
                        stack
                    )
                )

                return instanceId
            }
            else -> error("Unsupported sound file type! $extension")
        }
    }

    override fun stopSound(soundIndexOrInstanceId: Int) {
        for (soundInstance in this.filterSoundInstancesBySoundIndexOrInstanceId(soundIndexOrInstanceId)) {
            soundInstance.markedToBeFreed = true
        }
    }

    override fun pauseSound(soundIndexOrInstanceId: Int) {
        for (soundInstance in this.filterSoundInstancesBySoundIndexOrInstanceId(soundIndexOrInstanceId)) {
            if (soundInstance.paused)
                continue

            soundInstance.paused = true
            SDL_UnbindAudioStream(soundInstance.sdlStreamId)
        }
    }

    override fun resumeSound(soundIndexOrInstanceId: Int) {
        for (soundInstance in this.filterSoundInstancesBySoundIndexOrInstanceId(soundIndexOrInstanceId)) {
            if (!soundInstance.paused)
                continue

            soundInstance.paused = false
            SDL_BindAudioStream(sdlDeviceId, soundInstance.sdlStreamId)
        }
    }

    override fun stopAllSounds() {
        for (soundInstance in this.soundInstances) {
            soundInstance.markedToBeFreed = true
        }
    }

    override fun isAudioPlaying(soundIndexOrInstanceId: Int): Boolean {
        for (soundInstance in this.filterSoundInstancesBySoundIndexOrInstanceId(soundIndexOrInstanceId)) {
            if (!soundInstance.markedToBeFreed) {
                return true
            }
        }
        return false
    }

    override fun setAudioPitch(soundIndexOrInstanceId: Int, pitch: Float) {
        for (soundInstance in this.filterSoundInstancesBySoundIndexOrInstanceId(soundIndexOrInstanceId)) {
            SDL_SetAudioStreamFrequencyRatio(soundInstance.sdlStreamId, pitch)
        }
    }

    override fun tick() {
        for (soundInstance in soundInstances) {
            if (soundInstance.markedToBeFreed) {
                soundInstance.free()
                continue
            }

            if (soundInstance.paused)
                continue

            val bytesInTheQueue = SDL_GetAudioStreamQueued(soundInstance.sdlStreamId)

            if (soundInstance.reachedEnd) {
                if (bytesInTheQueue == 0)
                    soundInstance.free()

                continue
            }

            when (soundInstance) {
                is SDLSoundInstance.StreamedSDLSoundInstance -> {
                    val audioSecondInBytes = (soundInstance.stbVorbisInfo.sample_rate() * soundInstance.stbVorbisInfo.channels() * Short.SIZE_BYTES)
                    val audioTimeRemainingOnTheQueue = bytesInTheQueue / audioSecondInBytes.toDouble()

                    // println("I have $bytesInTheQueue bytes in the queue and my sample rate is ${soundInstance.stbVorbisInfo.sample_rate()}, in theory I have ${audioTimeRemainingOnTheQueue} seconds remaining")
                    if (0.1 >= audioTimeRemainingOnTheQueue) {
                        // We refill every ~0.25s
                        val size = audioSecondInBytes / 4
                        val bufferBytes = memAlloc(size)

                        try {
                            // println("Refilling...")
                            var frames = stb_vorbis_get_samples_short_interleaved(soundInstance.stbVorbisId, soundInstance.stbVorbisInfo.channels(), bufferBytes.asShortBuffer())
                            // println("Received frames: $frames, size: $size")

                            if (frames == 0) {
                                if (soundInstance.loop) {
                                    stb_vorbis_seek_start(soundInstance.stbVorbisId)
                                    frames = stb_vorbis_get_samples_short_interleaved(soundInstance.stbVorbisId, soundInstance.stbVorbisInfo.channels(), bufferBytes.asShortBuffer())
                                } else {
                                    soundInstance.reachedEnd = true
                                    continue
                                }
                            }

                            bufferBytes.limit(frames * soundInstance.stbVorbisInfo.channels() * Short.SIZE_BYTES)
                            bufferBytes.position(0)

                            SDL_PutAudioStreamData(soundInstance.sdlStreamId, bufferBytes)
                            SDL_FlushAudioStream(soundInstance.sdlStreamId)

                            soundInstance.hasPlayedAtLeastOnce = true
                        } finally {
                            memFree(bufferBytes)
                        }
                    }
                }
                is SDLSoundInstance.UncompressedSDLSoundInstance -> {
                    if (bytesInTheQueue == 0) {
                        if (!soundInstance.hasPlayedAtLeastOnce || soundInstance.loop) {
                            // This seems a bit funky, but the reason we do this is to avoid the queue being exhausted while we are in the middle of a frame
                            // So we push it twice if it is for a loop
                            repeat(if (soundInstance.loop) 2 else 1) {
                                SDL_PutAudioStreamData(soundInstance.sdlStreamId, soundInstance.byteBuffer)
                                SDL_FlushAudioStream(soundInstance.sdlStreamId)
                            }
                            soundInstance.hasPlayedAtLeastOnce = true
                        } else {
                            soundInstance.reachedEnd = true
                        }
                    }
                }
            }
        }

        this.soundInstances.removeIf { it.freed }
    }
}