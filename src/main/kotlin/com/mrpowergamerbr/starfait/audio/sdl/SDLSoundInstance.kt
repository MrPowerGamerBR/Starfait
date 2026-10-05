package com.mrpowergamerbr.starfait.audio.sdl

import org.lwjgl.sdl.SDLAudio.SDL_DestroyAudioStream
import org.lwjgl.sdl.SDLStdinc.SDL_free
import org.lwjgl.stb.STBVorbisInfo
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memFree
import java.nio.ByteBuffer

sealed class SDLSoundInstance(
    val sdlStreamId: Long,
    val instanceId: Int,
    val soundIndex: Int,
    val loop: Boolean
) {
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
        val stbVorbisInfo: STBVorbisInfo,
        val stack: MemoryStack
    ) : SDLSoundInstance(sdlStreamId, instanceId, soundIndex, loop) {
        override fun free() {
            this.freed = true
            this.stack.close()
            SDL_DestroyAudioStream(this.sdlStreamId)
        }
    }

    class UncompressedSDLSoundInstance(
        sdlStreamId: Long,
        instanceId: Int,
        soundIndex: Int,
        loop: Boolean,
        val byteBuffer: ByteBuffer,
        val stack: MemoryStack
    ) : SDLSoundInstance(sdlStreamId, instanceId, soundIndex, loop) {
        override fun free() {
            this.freed = true
            this.stack.close()
            SDL_free(this.byteBuffer)
            SDL_DestroyAudioStream(this.sdlStreamId)
        }
    }
}