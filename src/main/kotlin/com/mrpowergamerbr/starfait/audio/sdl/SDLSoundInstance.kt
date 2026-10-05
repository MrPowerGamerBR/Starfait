package com.mrpowergamerbr.starfait.audio.sdl

import com.mrpowergamerbr.starfait.audio.TimeRampedParamLinear
import org.lwjgl.sdl.SDLAudio.SDL_DestroyAudioStream
import org.lwjgl.sdl.SDLStdinc.SDL_free
import org.lwjgl.stb.STBVorbisInfo
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memFree
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ShortBuffer

sealed class SDLSoundInstance(
    val sdlStreamId: Long,
    val instanceId: Int,
    val soundIndex: Int,
    val loop: Boolean
) {
    val gain = TimeRampedParamLinear(1f)
    var hasPlayedAtLeastOnce = false
    var markedToBeFreed = false
    var freed = false
    var paused = false
    var reachedEnd = false

    abstract fun free()

    class StreamedSDLSoundInstance(
        sdlStreamId: Long,
        instanceId: Int,
        soundIndex: Int,
        loop: Boolean,
        val stbVorbisId: Long,
        val stbVorbisInfo: STBVorbisInfo
    ) : SDLSoundInstance(sdlStreamId, instanceId, soundIndex, loop) {
        override fun free() {
            this.markedToBeFreed = true
            this.freed = true
            stbVorbisInfo.close()
            SDL_DestroyAudioStream(this.sdlStreamId)
        }
    }

    class UncompressedSDLSoundInstance(
        sdlStreamId: Long,
        instanceId: Int,
        soundIndex: Int,
        loop: Boolean,
        val bufferAllocatedBySDL: Boolean,
        val buffer: ByteBuffer
    ) : SDLSoundInstance(sdlStreamId, instanceId, soundIndex, loop) {
        override fun free() {
            this.markedToBeFreed = true
            this.freed = true
            if (bufferAllocatedBySDL) {
                SDL_free(this.buffer)
            } else {
                memFree(this.buffer)
            }
            SDL_DestroyAudioStream(this.sdlStreamId)
        }
    }
}