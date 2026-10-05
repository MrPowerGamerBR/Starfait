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
import org.lwjgl.sdl.SDLAudio.SDL_SetAudioStreamGain
import org.lwjgl.sdl.SDLAudio.SDL_UnbindAudioStream
import org.lwjgl.sdl.SDLError.SDL_GetError
import org.lwjgl.sdl.SDLIOStream.SDL_IOFromConstMem
import org.lwjgl.sdl.SDL_AudioSpec
import org.lwjgl.stb.STBVorbis.stb_vorbis_get_info
import org.lwjgl.stb.STBVorbis.stb_vorbis_get_samples_short_interleaved
import org.lwjgl.stb.STBVorbis.stb_vorbis_open_memory
import org.lwjgl.stb.STBVorbis.stb_vorbis_seek_start
import org.lwjgl.stb.STBVorbis.stb_vorbis_stream_length_in_samples
import org.lwjgl.stb.STBVorbisInfo
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memByteBuffer
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.system.MemoryUtil.memShortBuffer
import java.io.File
import java.nio.ByteBuffer
import kotlin.check

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
        val audo = gameWAD.audo.blobs[sound.audoIndex]

        // You SHOULD NOT use the MemoryStack for long lived allocations, because the MemoryStack is shared per thread!

        when (sound.kind) {
            Sound.Kind.COMPRESSED_STREAMED -> {
                // TECHNICALLY sounds can be something that aren't OGGs, but for now, this shall do
                val songBytes = File("/home/mrpowergamerbr/Projects/ButterGMGames/Undertale_108/", gameWAD.strg.getString(sound.fileName)).readBytes()

                MemoryStack.stackPush().use { stack ->
                    // memAlloc is just like a ByteBuffer
                    val byteBuffer = memAlloc(songBytes.size)
                    byteBuffer.put(songBytes).position(0)

                    val pError = stack.mallocInt(1)

                    // This keeps a "position" internally
                    val stbVorbisId = stb_vorbis_open_memory(byteBuffer, pError, null)
                    // TODO: How can we check if the OGG was opened correctly?

                    val stbVorbisInfo = STBVorbisInfo.calloc()

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
                        )
                    )

                    return instanceId
                }

            }
            Sound.Kind.UNCOMPRESSED_NOT_STREAMED, Sound.Kind.COMPRESSED_NOT_STREAMED -> {
                MemoryStack.stackPush().use { stack ->
                    val directByteBuffer = ByteBuffer.allocateDirect(audo.size)
                    directByteBuffer.put(audo).flip()

                    val spec = SDL_AudioSpec.calloc(stack)

                    if (sound.kind == Sound.Kind.UNCOMPRESSED_NOT_STREAMED) {
                        val pBuf = stack.mallocPointer(1)
                        val pLen = stack.mallocInt(1)

                        val io = SDL_IOFromConstMem(directByteBuffer)

                        val successfullyDecoded = SDL_LoadWAV_IO(io, true, spec, pBuf, pLen)
                        check(successfullyDecoded) { "Failed to decode WAV file!" }

                        val sdlStreamId = SDL_CreateAudioStream(spec, null)
                        check(sdlStreamId != MemoryUtil.NULL) { "Something went wrong when opening the audio device stream! ${SDL_GetError()}" }
                        val successfullyBound = SDL_BindAudioStream(this.sdlDeviceId, sdlStreamId)
                        check(successfullyBound) { "Failed to bind SDL stream to SDL device! ${SDL_GetError()}" }

                        val byteBuffer = memByteBuffer(pBuf.get(), pLen.get(0))

                        val instanceId = this.currentSoundInstanceId++
                        soundInstances.add(
                            SDLSoundInstance.UncompressedSDLSoundInstance(
                                sdlStreamId,
                                instanceId,
                                soundIndex,
                                loop,
                                true,
                                byteBuffer
                            )
                        )
                        return instanceId
                    } else {
                        // This keeps a "position" internally
                        val pError = stack.mallocInt(1)
                        val stbVorbisId = stb_vorbis_open_memory(directByteBuffer, pError, null)
                        // TODO: How can we check if the OGG was opened correctly?

                        val stbVorbisInfo = STBVorbisInfo.calloc(stack)

                        stb_vorbis_get_info(stbVorbisId, stbVorbisInfo)

                        spec.format(SDL_AUDIO_S16)
                        spec.channels(stbVorbisInfo.channels())
                        spec.freq(stbVorbisInfo.sample_rate())

                        val sdlStreamId = SDL_CreateAudioStream(spec, null)
                        check(sdlStreamId != MemoryUtil.NULL) { "Something went wrong when opening the audio device stream! ${SDL_GetError()}" }
                        val successfullyBound = SDL_BindAudioStream(this.sdlDeviceId, sdlStreamId)
                        check(successfullyBound) { "Failed to bind SDL stream to SDL device! ${SDL_GetError()}" }

                        val samples = stb_vorbis_stream_length_in_samples(stbVorbisId)
                        println("Samples is $samples")
                        val capacity = samples * stbVorbisInfo.channels() * Short.SIZE_BYTES
                        println("Audio capacity is $capacity")
                        val byteBuffer = memAlloc(capacity)

                        // This is honestly a hack of the century
                        val shortBuffer = byteBuffer.asShortBuffer()
                        while (true) {
                            val frames = stb_vorbis_get_samples_short_interleaved(stbVorbisId, stbVorbisInfo.channels(), shortBuffer)
                            if (frames == 0)
                                break
                        }

                        val instanceId = this.currentSoundInstanceId++
                        soundInstances.add(
                            SDLSoundInstance.UncompressedSDLSoundInstance(
                                sdlStreamId,
                                instanceId,
                                soundIndex,
                                loop,
                                false,
                                byteBuffer
                            )
                        )
                        return instanceId
                    }
                }
            }
            Sound.Kind.COMPRESSED_UNCOMPRESS_ON_LOAD_NOT_STREAMED -> TODO()
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

    override fun setAudioGain(soundIndexOrInstanceId: Int, gain: Float, timeMs: Int) {
        for (soundInstance in this.filterSoundInstancesBySoundIndexOrInstanceId(soundIndexOrInstanceId)) {
            soundInstance.gain.set(gain, timeMs)
            SDL_SetAudioStreamGain(soundInstance.sdlStreamId, soundInstance.gain.update())
        }
    }

    override fun tick() {
        for (soundInstance in soundInstances) {
            if (soundInstance.markedToBeFreed) {
                soundInstance.free()
                continue
            }

            val previousGain = soundInstance.gain.get()
            val currentGain = soundInstance.gain.update()
            if (previousGain != currentGain)
                SDL_SetAudioStreamGain(soundInstance.sdlStreamId, currentGain)

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

                    // println("stb vorbis ID: ${soundInstance.stbVorbisId}")
                    // println("I have $bytesInTheQueue bytes in the queue and my sample rate is ${soundInstance.stbVorbisInfo.sample_rate()}, in theory I have ${audioTimeRemainingOnTheQueue} seconds remaining")
                    if (0.1 >= audioTimeRemainingOnTheQueue) {
                        // We refill every ~0.25s
                        val size = audioSecondInBytes / 4
                        val bufferBytes = memAlloc(size)

                        try {
                            // println("Refilling... chan: ${soundInstance.stbVorbisInfo.channels()}")
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
                                SDL_PutAudioStreamData(soundInstance.sdlStreamId, soundInstance.buffer)
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