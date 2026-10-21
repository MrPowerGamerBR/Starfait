@file:OptIn(ExperimentalJsExport::class, ExperimentalWasmJsInterop::class)

package com.mrpowergamerbr.starfait.web

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.StarfaitUtils
import com.mrpowergamerbr.starfait.TimeUtils
import com.mrpowergamerbr.starfait.filesystem.LayeredFileSystem
import com.mrpowergamerbr.starfait.filesystem.virtual.VirtualFileSystem
import com.mrpowergamerbr.starfait.renderer.opengl.GLRenderer
import com.mrpowergamerbr.starfait.renderer.opengl.StarfaitGLConstants.GL_COLOR_BUFFER_BIT
import com.mrpowergamerbr.starfait.renderer.opengl.shaders.MinimalShader
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.runner.StdoutGameOutput
import com.mrpowergamerbr.starfait.runner.resources.RunnerResources
import com.mrpowergamerbr.starfait.vm.StarfaitVM
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.web.audio.web.WebAudioManager
import com.mrpowergamerbr.starfait.web.filesystem.opfs.OPFSFileSystem
import com.mrpowergamerbr.starfait.web.renderer.BrowserImageDecoder
import com.mrpowergamerbr.starfait.web.renderer.StarfaitWebGL2
import js.buffer.ArrayBuffer
import js.buffer.ArrayBufferLike
import js.coroutines.asPromise
import js.iterable.asFlow
import js.objects.unsafeJso
import js.promise.Promise
import js.reflect.unsafeCast
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import web.blob.arrayBuffer
import web.dom.ElementId
import web.dom.document
import web.events.addEventHandler
import web.file.File
import web.fs.FileSystemDirectoryHandle
import web.fs.FileSystemFileHandle
import web.fs.FileSystemGetDirectoryOptions
import web.fs.FileSystemHandleKind
import web.fs.file
import web.fs.getDirectoryHandle
import web.fs.getFile
import web.gl.GLbitfield
import web.gl.ID
import web.gl.WebGL2RenderingContext
import web.gl.WebGLContextAttributes
import web.html.HTMLCanvasElement
import web.html.HTMLElement
import web.keyboard.KEY_DOWN
import web.keyboard.KEY_UP
import web.keyboard.KeyboardEvent
import web.navigator.navigator
import web.storage.getDirectory
import web.window.window

private suspend fun getNestedFolder(path: String, baseDirectory: FileSystemDirectoryHandle): FileSystemDirectoryHandle {
    val elements = path.split("/")
    var handle = baseDirectory

    for (element in elements) {
        handle = handle.getDirectoryHandle(element, unsafeJso<FileSystemGetDirectoryOptions>() { create = true })
    }

    return handle
}

lateinit var gRunner: StarfaitRunner

@JsExport
fun createRunner(bundlePath: String, savePath: String, gameWADName: String, gameCanvas: HTMLCanvasElement): Promise<JsBoolean> {
    return GlobalScope.async(CoroutineExceptionHandler { _, e -> println(e.stackTraceToString()) }) {
        val baseDirectory = navigator.storage.getDirectory()

        val bundleFS = VirtualFileSystem()
        val bundleDirectory = getNestedFolder(bundlePath, baseDirectory)

        bundleDirectory.values().asFlow().collect { handle ->
            if (handle.kind == FileSystemHandleKind.file) {
                val file = (handle as FileSystemFileHandle).getFile()
                bundleFS.writeFile(file.name, file.arrayBuffer().toByteArrayFast())
            }
        }

        val gameWAD = GameWAD.read(StarfaitByteBuffer.fromByteArray(bundleFS.readFile(gameWADName)!!, 0))
        println(gameWAD.gen8.gameDisplayName)

        val vm = StarfaitVM(
            gameWAD,
            gmlScriptsOpcodesToBeTraced = emptySet(),
            functionCallsToBeTraced = emptySet(),
            alarmsToBeTraced = emptySet(),
            eventsToBeTraced = emptySet(),
            traceVariableReads = emptySet(),
            traceVariableWrites = emptySet(),
            traceCodeExecution = emptySet()
        )
        vm.associateBuiltins(StarfaitUtils.remapReferences(gameWAD))

        println("Getting game canvas")
        // val gameCanvas = document.getElementById(ElementId("game-canvas"))!! as HTMLCanvasElement
        println("Getting WebGL2 context")
        val context = gameCanvas.getContext(WebGL2RenderingContext.ID, unsafeJso<WebGLContextAttributes>() {
            this.alpha = false
        })!!

        println("Clearing screen")
        context.clearColor(1.0f, 0.0f, 0.0f, 1.0f)
        context.clear(unsafeCast<GLbitfield>(GL_COLOR_BUFFER_BIT))
        println("Creating renderer")

        val renderer = GLRenderer(
            StarfaitWebGL2(context),
            """
                            #version 300 es
                            precision highp float;
                            precision highp int;
                            precision highp sampler2D;
                            
                            ${MinimalShader.VERTEX_SHADER_CODE}
                        """.trimIndent(),
            """
                            #version 300 es
                            precision highp float;
                            precision highp int;
                            precision highp sampler2D;
                            
                            ${MinimalShader.FRAGMENT_SHADER_CODE}
                        """.trimIndent(),
            BrowserImageDecoder()
        )

        val savesDirectory = getNestedFolder(savePath, baseDirectory)

        val saveFS = VirtualFileSystem()
        savesDirectory.values().asFlow().collect { handle ->
            if (handle.kind == FileSystemHandleKind.file) {
                val file = (handle as FileSystemFileHandle).getFile()
                saveFS.writeFile(file.name, file.arrayBuffer().toByteArrayFast())
            }
        }

        val layeredFileSystem = LayeredFileSystem(OPFSFileSystem(saveFS, savesDirectory), bundleFS)
        val audioManager = WebAudioManager(vm.gameWAD, bundleFS)
        val runner = StarfaitRunner(
            RunnerResources.create(vm.gameWAD, renderer),
            vm,
            renderer,
            audioManager,
            layeredFileSystem,
            StdoutGameOutput(),
            TimeUtils.currentTimeMillis(),
            mapOf()
        )
        renderer.runner = runner
        vm.runner = runner
        // vm.runner.enableCollisionRendering = enableCollisionRendering
        // vm.runner.drawInvisibleInstances = drawInvisibleInstances
        runner.start()

        gRunner = runner

        return@async true.toJsBoolean()
    }.asPromise()
}

@JsExport
fun onKeyDown(key: Int) {
    gRunner.keyboard.onKeyDown(key)
}

@JsExport
fun onKeyUp(key: Int) {
    gRunner.keyboard.onKeyUp(key)
}

// Hacky!
@JsExport
fun startRunner(onExit: () -> (Unit)) {
    val renderer = gRunner.renderer as GLRenderer
    val runner = gRunner

    GlobalScope.launch(CoroutineExceptionHandler { _, e -> println(e.stackTraceToString()) }) {
        while (!runner.gameEnded) {
            val started = TimeUtils.currentTimeMillis()
            renderer.frame {
                runner.tick()
            }
            val finished = TimeUtils.currentTimeMillis()

            val targetFramesPerSecond = 1000 / (runner.currentRoom.speed.toDouble())
            val waitTime = (targetFramesPerSecond - (finished - started)).toLong()
            if (waitTime > 0)
                delay(waitTime)

            // If the game is stuck on a infinite loop, let's at least yield so we can give control back to the browser
            yield()
        }

        println("Exiting...")
        onExit()
    }
}

@JsExport
fun parseGameWAD(arrayBuffer: ArrayBuffer): JsReference<GameWAD> {
    return GameWAD.read(StarfaitByteBuffer.fromByteArray(arrayBuffer.toByteArrayFast(), 0)).toJsReference()
}

@JsExport
fun getGameDisplayName(gameWAD: JsReference<GameWAD>): String {
    return gameWAD.get().gen8.gameDisplayName
}

@JsExport
fun getWADVersion(gameWAD: JsReference<GameWAD>): Byte {
    return gameWAD.get().gen8.wadVersion
}