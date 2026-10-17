package com.mrpowergamerbr.starfait.web

import js.buffer.ArrayBuffer
import web.audio.AudioContext
import web.blob.Blob
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.wasmMemory
import kotlin.wasm.unsafe.withScopedMemoryAllocator

// A lot of fast copy variants for Kotlin/Wasm
// All of these are essentially "copying from JS-land to Wasm-land is slow, so let's do it in bulk"
// The tl;dr is that
// ByteArrays: WasmGC-land (Kotlin/Wasm)
// ArrayBuffers: JS-land
// By default the .toByteArray and others do a individual copy and they are (comparatively) slow

@JsFun("(memory, src, dst) => new Uint8Array(memory.buffer, dst, src.byteLength).set(new Uint8Array(src))")
private external fun copyArrayBufferToLinearMemory(memory: JsAny, src: ArrayBuffer, dst: Int)

// Slice without any parameters = copy
@JsFun("(memory, src, size) => new Uint8Array(memory.buffer, src, size).slice().buffer")
private external fun createArrayBufferFromLinearMemory(memory: JsAny, src: Int, size: Int): ArrayBuffer

@OptIn(UnsafeWasmMemoryApi::class, ExperimentalWasmJsInterop::class)
fun ArrayBuffer.toByteArrayFast(): ByteArray {
    val size = this.byteLength
    val result = ByteArray(size)

    // Essentially manual memory management (woo)
    withScopedMemoryAllocator { allocator ->
        val pointer = allocator.allocate(size)
        copyArrayBufferToLinearMemory(wasmMemory, this, pointer.address.toInt())

        for (i in 0 until size) {
            result[i] = (pointer + i).loadByte()
        }
    }

    return result
}

@OptIn(UnsafeWasmMemoryApi::class, ExperimentalWasmJsInterop::class)
fun ByteArray.toArrayBufferFast(): ArrayBuffer {
    withScopedMemoryAllocator { allocator ->
        val pointer = allocator.allocate(this.size)

        for (i in this.indices) {
            (pointer + i).storeByte(this[i])
        }

        return createArrayBufferFromLinearMemory(wasmMemory, pointer.address.toInt(), this.size)
    }
}