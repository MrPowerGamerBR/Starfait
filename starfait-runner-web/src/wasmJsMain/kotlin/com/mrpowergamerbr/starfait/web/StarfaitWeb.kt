package com.mrpowergamerbr.starfait.web

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.StarfaitUtils
import com.mrpowergamerbr.starfait.TimeUtils
import com.mrpowergamerbr.starfait.audio.noop.NoopAudioManager
import com.mrpowergamerbr.starfait.filesystem.LayeredFileSystem
import com.mrpowergamerbr.starfait.filesystem.virtual.VirtualFileSystem
import com.mrpowergamerbr.starfait.renderer.opengl.GLRenderer
import com.mrpowergamerbr.starfait.renderer.opengl.StarfaitGLConstants.GL_COLOR_BUFFER_BIT
import com.mrpowergamerbr.starfait.runner.EventType
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.runner.StdoutGameOutput
import com.mrpowergamerbr.starfait.runner.resources.RunnerResources
import com.mrpowergamerbr.starfait.vm.StarfaitVM
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.web.renderer.BrowserImageDecoder
import com.mrpowergamerbr.starfait.web.renderer.StarfaitWebGL2
import js.objects.unsafeJso
import js.reflect.unsafeCast
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import web.blob.byteArray
import web.dom.ElementId
import web.dom.document
import web.events.addEventHandler
import web.gl.GLbitfield
import web.gl.ID
import web.gl.WebGL2RenderingContext
import web.gl.WebGLContextAttributes
import web.html.HTMLCanvasElement
import web.html.HTMLInputElement
import web.input.INPUT
import web.input.InputEvent
import web.keyboard.KEY_DOWN
import web.keyboard.KEY_PRESS
import web.keyboard.KEY_UP
import web.keyboard.KeyboardEvent
import web.window.window
import kotlin.js.unsafeCast

class StarfaitWeb {
    fun start() {
        println("Howdy from Kotlin/WASM! :3")

        document.getElementById(ElementId("file-input"))!!
            .addEventHandler(InputEvent.INPUT) {
                println("File changed!")

                val element = it.currentTarget as HTMLInputElement
                val wadFile = element.files!!.item(0)!!

                GlobalScope.launch(CoroutineExceptionHandler { _, e -> println(e.stackTraceToString()) }) {
                    val gameWAD = GameWAD.read(StarfaitByteBuffer.fromByteArray(wadFile.byteArray(), 0))
                    println(gameWAD.strg.getString(gameWAD.gen8.gameDisplayName))

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
                    val gameCanvas = document.getElementById(ElementId("game-canvas"))!! as HTMLCanvasElement
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

                            // For batching, we store the position of each quad directly on the position, so we don't actually *need* to have a uModel
                            layout(location = 0) in vec2 aPosition;
                            layout(location = 1) in vec2 aTexCoord;
                            layout(location = 2) in vec4 aBlendColor;

                            uniform vec4 uColor;
                            uniform mat4 uProjection;

                            out vec2 vTexCoord;
                            out vec4 vBlendColor;

                            void main() {
                                vTexCoord = aTexCoord;
                                vBlendColor = aBlendColor;
                                gl_Position =  uProjection * vec4(aPosition, 0.0, 1.0);
                            }
                        """.trimIndent(),
                        """
                            #version 300 es
                            precision highp float;
                            precision highp int;
                            precision highp sampler2D;

                            in vec2 vTexCoord;
                            in vec4 vBlendColor;

                            uniform sampler2D uTexture;

                            out vec4 fragColor;

                            void main() {
                                fragColor = texture(uTexture, vTexCoord) * vBlendColor;
                            }
                        """.trimIndent(),
                        BrowserImageDecoder()
                    )
                    val layeredFileSystem = LayeredFileSystem(VirtualFileSystem(), VirtualFileSystem())
                    val runner = StarfaitRunner(
                        RunnerResources.create(vm.gameWAD, renderer),
                        vm,
                        renderer,
                        NoopAudioManager(),
                        layeredFileSystem,
                        StdoutGameOutput(),
                        TimeUtils.currentTimeMillis(),
                        emptyMap()
                    )
                    renderer.runner = runner
                    vm.runner = runner
                    // vm.runner.enableCollisionRendering = enableCollisionRendering
                    // vm.runner.drawInvisibleInstances = drawInvisibleInstances
                    runner.start()

                    window.addEventHandler(KeyboardEvent.KEY_DOWN) { event ->
                        runner.keyboard.onKeyDown(convertWebKeyCodeToGMLVK(event.code.toString()))
                    }

                    window.addEventHandler(KeyboardEvent.KEY_UP) { event ->
                        runner.keyboard.onKeyUp(convertWebKeyCodeToGMLVK(event.code.toString()))
                    }

                    while (true) {
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
                }
            }
    }

    fun convertWebKeyCodeToGMLVK(code: String) = when {
        code.length == 4 && code.startsWith("Key") -> code[3].code // "KeyA" -> VK_A (65)
        code.length == 6 && code.startsWith("Digit") -> code[5].code // "Digit0" -> 48
        code.length == 7 && code.startsWith("Numpad") && code[6].isDigit() -> RunnerKeyboard.VK_NUMPAD0 + (code[6] - '0')
        code.startsWith("F") && code.drop(1).toIntOrNull() in 1..12 -> RunnerKeyboard.VK_F1 + (code.drop(1).toInt() - 1)
        else -> when (code) {
            "Backspace" -> RunnerKeyboard.VK_BACKSPACE
            "Tab" -> RunnerKeyboard.VK_TAB
            "Enter" -> RunnerKeyboard.VK_ENTER
            "Escape" -> RunnerKeyboard.VK_ESCAPE
            "Space" -> RunnerKeyboard.VK_SPACE
            "ArrowLeft" -> RunnerKeyboard.VK_LEFT
            "ArrowRight" -> RunnerKeyboard.VK_RIGHT
            "ArrowUp" -> RunnerKeyboard.VK_UP
            "ArrowDown" -> RunnerKeyboard.VK_DOWN
            "ShiftLeft" -> RunnerKeyboard.VK_LSHIFT
            "ShiftRight" -> RunnerKeyboard.VK_RSHIFT
            "ControlLeft" -> RunnerKeyboard.VK_LCONTROL
            "ControlRight" -> RunnerKeyboard.VK_RCONTROL
            "AltLeft" -> RunnerKeyboard.VK_LALT
            "AltRight" -> RunnerKeyboard.VK_RALT
            "Home" -> RunnerKeyboard.VK_HOME
            "End" -> RunnerKeyboard.VK_END
            "Insert" -> RunnerKeyboard.VK_INSERT
            "Delete" -> RunnerKeyboard.VK_DELETE
            "PageUp" -> RunnerKeyboard.VK_PAGEUP
            "PageDown" -> RunnerKeyboard.VK_PAGEDOWN
            "Pause" -> RunnerKeyboard.VK_PAUSE
            "PrintScreen" -> RunnerKeyboard.VK_PRINTSCREEN
            "NumpadMultiply" -> RunnerKeyboard.VK_MULTIPLY
            "NumpadAdd" -> RunnerKeyboard.VK_ADD
            "NumpadSubtract" -> RunnerKeyboard.VK_SUBTRACT
            "NumpadDecimal" -> RunnerKeyboard.VK_DECIMAL
            "NumpadDivide" -> RunnerKeyboard.VK_DIVIDE
            "NumpadEnter" -> RunnerKeyboard.VK_ENTER
            else -> -1
        }
    }
}