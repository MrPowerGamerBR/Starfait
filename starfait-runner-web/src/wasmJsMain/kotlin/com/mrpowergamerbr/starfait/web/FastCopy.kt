package com.mrpowergamerbr.starfait.web

import js.buffer.ArrayBuffer
import js.buffer.ArrayBufferLike
import js.typedarrays.Float32Array
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
// By default the .toByteArray and others do a for each loop on each element, so they are stupidly slow

@JsFun("(memory, src, dst) => new Uint8Array(memory.buffer, dst, src.byteLength).set(new Uint8Array(src))")
private external fun copyArrayBufferToLinearMemory(memory: JsAny, src: ArrayBufferLike, dst: Int)

// Slice without any parameters = copy
@JsFun("(memory, src, size) => new Uint8Array(memory.buffer, src, size).slice().buffer")
private external fun createArrayBufferFromLinearMemory(memory: JsAny, src: Int, size: Int): ArrayBuffer

// Slice without any parameters = copy
@JsFun("(memory, src, size) => new Float32Array(memory.buffer, src, size).slice()")
private external fun createFloat32ArrayFromLinearMemory(memory: JsAny, src: Int, size: Int): Float32Array<ArrayBufferLike>

@OptIn(UnsafeWasmMemoryApi::class, ExperimentalWasmJsInterop::class)
fun ArrayBufferLike.toByteArrayFast(): ByteArray {
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

@OptIn(UnsafeWasmMemoryApi::class, ExperimentalWasmJsInterop::class)
fun FloatArray.toFloat32ArrayFast(): Float32Array<ArrayBufferLike> {
    withScopedMemoryAllocator { allocator ->
        val pointer = allocator.allocate(this.size * 4)

        for (i in this.indices) {
            (pointer + i * 4).storeInt(this[i].toRawBits())
        }

        return createFloat32ArrayFromLinearMemory(wasmMemory, pointer.address.toInt(), this.size)
    }
}