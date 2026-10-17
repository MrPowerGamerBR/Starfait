package com.mrpowergamerbr.starfait.web

import js.buffer.ArrayBuffer
import web.blob.Blob
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.wasmMemory
import kotlin.wasm.unsafe.withScopedMemoryAllocator

// A lot of fast copy variants for Kotlin/Wasm
// All of these are essentially "copying from JS-land to Wasm-land is slow, so let's do it in bulk"

@JsFun("(memory, src, dst) => new Uint8Array(memory.buffer, dst, src.byteLength).set(new Uint8Array(src))")
private external fun copyArrayBufferToLinearMemory(memory: JsAny, src: ArrayBuffer, dst: Int)

@JsFun("(memory, src, size, type) => new Blob([new Uint8Array(memory.buffer, src, size)], { type: type })")
private external fun createBlobFromLinearMemory(memory: JsAny, src: Int, size: Int, type: String): Blob

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
fun ByteArray.toBlobFast(type: String): Blob {
    withScopedMemoryAllocator { allocator ->
        val pointer = allocator.allocate(this.size)

        for (i in this.indices) {
            (pointer + i).storeByte(this[i])
        }

        return createBlobFromLinearMemory(wasmMemory, pointer.address.toInt(), this.size, type)
    }
}