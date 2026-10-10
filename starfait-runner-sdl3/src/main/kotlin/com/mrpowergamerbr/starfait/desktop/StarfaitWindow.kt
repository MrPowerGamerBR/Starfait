package com.mrpowergamerbr.starfait.desktop

import com.mrpowergamerbr.starfait.RecordedFrame
import com.mrpowergamerbr.starfait.audio.sdl.SDLAudioManager
import com.mrpowergamerbr.starfait.filesystem.LayeredFileSystem
import com.mrpowergamerbr.starfait.filesystem.disk.DiskFileSystem
import com.mrpowergamerbr.starfait.filesystem.virtual.VirtualFileSystem
import com.mrpowergamerbr.starfait.renderer.opengl.OpenGLRenderer
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_ADD
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_DECIMAL
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_DELETE
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_DIVIDE
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_DOWN
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_END
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_ENTER
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_F1
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_HOME
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_INSERT
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_LALT
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_LCONTROL
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_LEFT
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_LSHIFT
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_MULTIPLY
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_NUMPAD0
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_PAGEDOWN
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_PAGEUP
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_PAUSE
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_PRINTSCREEN
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_RALT
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_RCONTROL
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_RIGHT
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_RSHIFT
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_SUBTRACT
import com.mrpowergamerbr.starfait.runner.RunnerKeyboard.Companion.VK_UP
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.runner.StdoutGameOutput
import com.mrpowergamerbr.starfait.statedumper.StateDumper
import com.mrpowergamerbr.starfait.vm.RValue
import com.mrpowergamerbr.starfait.vm.StarfaitVM
import com.mrpowergamerbr.starfait.vm.VariableInstanceType
import kotlinx.serialization.json.Json
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.sdl.SDLAudio.SDL_AUDIO_DEVICE_DEFAULT_PLAYBACK
import org.lwjgl.sdl.SDLAudio.SDL_OpenAudioDevice
import org.lwjgl.sdl.SDLAudio.SDL_SetAudioDeviceGain
import org.lwjgl.sdl.SDLError.SDL_GetError
import org.lwjgl.sdl.SDLEvents.*
import org.lwjgl.sdl.SDLInit.*
import org.lwjgl.sdl.SDLKeycode.*
import org.lwjgl.sdl.SDLVideo.*
import org.lwjgl.sdl.SDL_Event
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil

class StarfaitWindow(
    val wadFile: File,
    val saveFolder: SaveFolderKind,
    val vm: StarfaitVM,
    val seed: Long,
    val enableCollisionRendering: Boolean,
    val drawInvisibleInstances: Boolean,
    val recordInputsFile: File?,
    val playbackInputFrames: Map<Int, RecordedFrame>?,
    val screenshot: File?,
    val screenshotFrames: Set<Int>,
    val exitAtFrame: Int?,
    val pauseAtFrames: Set<Int>,
    val headless: Boolean,
    val replaceRooms: Map<Int, Int>,
    var dumpStateFile: File?,
    val dumpStateAtFrames: Set<Int>,
    val traceFrameDuration: List<IntRange>,
    val speed: Double,
    val runFullSpeedUntilFrame: Int?,
    val masterVolume: Float
) {
    companion object {
        private val JsonPrettyPrint = Json {
            prettyPrint = true
        }
    }

    var frameCount = 0

    fun start() {
        check(SDL_Init(SDL_INIT_VIDEO or SDL_INIT_AUDIO)) { "SDL_Init failed: ${SDL_GetError()}" }

        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 3)
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 3)
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE)

        var windowFlags = SDL_WINDOW_OPENGL or SDL_WINDOW_HIGH_PIXEL_DENSITY
        if (headless) windowFlags = windowFlags or SDL_WINDOW_HIDDEN
        val window = SDL_CreateWindow("Starfait", vm.gameWAD.gen8.initialApplicationWidth, vm.gameWAD.gen8.initialApplicationHeight, windowFlags)
        val scale = SDL_GetWindowDisplayScale(window)
        // Thanks Wayland!
        SDL_SetWindowSize(window, (ceil(vm.gameWAD.gen8.initialApplicationWidth / scale)).toInt(), (ceil(vm.gameWAD.gen8.initialApplicationHeight / scale)).toInt())
        check(window != MemoryUtil.NULL) { "SDL_CreateWindow failed: ${SDL_GetError()}" }

        val context = SDL_GL_CreateContext(window)
        check(context != MemoryUtil.NULL) { "SDL_GL_CreateContext failed: ${SDL_GetError()}" }
        SDL_GL_MakeCurrent(window, context)
        GL.createCapabilities() // required before any GL call
        SDL_GL_SetSwapInterval(1) // vsync

        val device = SDL_OpenAudioDevice(SDL_AUDIO_DEVICE_DEFAULT_PLAYBACK, null)
        SDL_SetAudioDeviceGain(device, masterVolume)
        if (headless) {
            SDL_SetAudioDeviceGain(device, 0.0f)
        }

        val saveFileSystem = when (saveFolder) {
            is SaveFolderKind.Folder -> DiskFileSystem(saveFolder.file)
            SaveFolderKind.Memory -> VirtualFileSystem()
        }
        val bundleFileSystem = DiskFileSystem(wadFile.parentFile)
        val layeredFileSystem = LayeredFileSystem(saveFileSystem, bundleFileSystem)

        val renderer = OpenGLRenderer(vm.gameWAD)
        val runner = StarfaitRunner(
            vm.gameWAD,
            vm,
            renderer,
            SDLAudioManager(vm.gameWAD, bundleFileSystem, device),
            layeredFileSystem,
            StdoutGameOutput(),
            seed,
            replaceRooms
        )
        renderer.runner = runner
        vm.runner = runner
        vm.runner.enableCollisionRendering = enableCollisionRendering
        vm.runner.drawInvisibleInstances = drawInvisibleInstances
        runner.start()

        glClearColor(1.0f, 0.0f, 0.0f, 1.0f)

        var isPaused = false

        recordInputsFile?.delete()

        MemoryStack.stackPush().use { stack ->
            val event = SDL_Event.malloc(stack)
            var running = true
            while (running) {
                if (frameCount == exitAtFrame) {
                    running = false
                    continue
                }

                if (runner.gameEnded) {
                    running = false
                    continue
                }

                val started = System.currentTimeMillis()
                var stepOneFrame = false

                if (!isPaused && frameCount in pauseAtFrames) {
                    println("Platform: Automatically paused at frame $frameCount")
                    isPaused = true
                }

                while (SDL_PollEvent(event)) {
                    if (event.type() == SDL_EVENT_QUIT)
                        running = false

                    if (event.type() == SDL_EVENT_KEY_DOWN) {
                        val gmlVkKey = convertSDLKeycodeToGMLVK(event.key().key())
                        if (gmlVkKey != -1)
                            runner.keyboard.onKeyDown(gmlVkKey)
                    }

                    if (event.type() == SDL_EVENT_KEY_UP) {
                        if (event.key().key() == SDLK_PAGEUP) {
                            runner.setPendingRoom(runner.currentRoom.roomId - 1)
                            runner.audioManager.stopAllSounds()
                        }

                        if (event.key().key() == SDLK_PAGEDOWN) {
                            runner.setPendingRoom(runner.currentRoom.roomId + 1)
                            runner.audioManager.stopAllSounds()
                        }

                        if (event.key().key() == SDLK_F4) {
                            runner.pendingRestart = true
                        }

                        if (event.key().key() == SDLK_F5) {
                            runner.enableCollisionRendering = !runner.enableCollisionRendering
                        }

                        if (event.key().key() == SDLK_F6) {
                            runner.drawInvisibleInstances = !runner.drawInvisibleInstances
                        }

                        if (event.key().key() == SDLK_F11) {
                            if (dumpStateFile == null) {
                                println("Platform: You haven't configured a dump state file!")
                                val selectedFile = TinyFileDialogs.tinyfd_saveFileDialog(
                                    "Where the dump state file should be saved?",
                                    null,
                                    null,
                                    null
                                )

                                dumpStateFile = selectedFile?.let { File(it) }
                            }

                            if (dumpStateFile != null) {
                                dumpRunnerStateAsJsonToFile(runner, dumpStateFile!!)
                            }
                        }

                        if (event.key().key() == SDLK_F10) {
                            val variableSlot = runner.vm.getVariableSlotFromVariableName(VariableInstanceType.GLOBAL, "interact")
                            runner.vm.global.setVariable(variableSlot, RValue.Int32(0), null)
                            println("Platform: Changed global.interact [$variableSlot] = 0")
                        }

                        if (event.key().key() == 'p'.code) {
                            isPaused = !isPaused
                            println("Platform: Toggled pause")
                        }

                        if (event.key().key() == 'o'.code) {
                            println("Platform: Stepped one frame")
                            stepOneFrame = true
                        }

                        val gmlVkKey = convertSDLKeycodeToGMLVK(event.key().key())
                        if (gmlVkKey != -1)
                            runner.keyboard.onKeyUp(gmlVkKey)
                    }
                }

                if (!isPaused || stepOneFrame) {
                    for (range in traceFrameDuration) {
                        if (frameCount in range) {
                            println("Platform: Frame $frameCount Start")
                        }
                    }

                    // Before we execute it, track whatever keys were pressed/released
                    if (playbackInputFrames != null) {
                        // If we have frames set...
                        val playbackInputFrame = playbackInputFrames[frameCount]

                        if (playbackInputFrame != null) {
                            playbackInputFrame.keyDown.forEach { runner.keyboard.onKeyDown(it) }
                            playbackInputFrame.keyUp.forEach { runner.keyboard.onKeyUp(it) }
                        }
                    }

                    if (recordInputsFile != null) {
                        recordInputsFile.appendText(
                            Json.encodeToString(
                                RecordedFrame(
                                    // What an horrible hack!
                                    runner.keyboard.keyPressed.mapIndexed { index, bool -> if (bool) index else -1 }.filter { it != -1 }.toSet(),
                                    runner.keyboard.keyReleased.mapIndexed { index, bool -> if (bool) index else -1 }.filter { it != -1 }.toSet(),
                                )
                            ) + "\n"
                        )
                    }

                    renderer.frame {
                        runner.tick()
                    }

                    val finished = System.currentTimeMillis()
                    if (!headless && (runFullSpeedUntilFrame == null || frameCount > runFullSpeedUntilFrame)) {
                        val targetFramesPerSecond = 1000 / (runner.currentRoom.speed.toDouble() * speed)
                        val waitTime = (targetFramesPerSecond - (finished - started)).toLong()
                        if (waitTime > 0)
                            Thread.sleep(waitTime)
                    }

                    if (frameCount in screenshotFrames && screenshot != null) {
                        val output = File(screenshot.parentFile, screenshot.name.format(frameCount))

                        val pixels = IntArray(640 * 480)
                        glPixelStorei(GL_PACK_ALIGNMENT, 1)
                        glReadBuffer(GL_BACK)
                        glReadPixels(0, 0, 640, 480, GL_RGBA, GL_UNSIGNED_BYTE, pixels)

                        val image = BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB)

                        for ((index, pixel) in pixels.withIndex()) {
                            val x = index % 640
                            // Yes it needs to be 479 because the pixels range from 0 to 479
                            val y = 479 - (index / 640)

                            val a = (pixel shr 24) and 0xFF
                            val b = (pixel shr 16) and 0xFF
                            val g = (pixel shr 8) and 0xFF
                            val r = pixel and 0xFF
                            val argb = (a shl 24) or (r shl 16) or (g shl 8) or b

                            image.setRGB(x, y, argb)
                        }

                        ImageIO.write(image, "png", output)
                    }

                    if (frameCount in dumpStateAtFrames && dumpStateFile != null) {
                        dumpRunnerStateAsJsonToFile(runner, dumpStateFile!!)
                    }

                    for (range in traceFrameDuration) {
                        if (frameCount in range) {
                            println("Platform: Frame $frameCount End (${finished - started}ms)")
                        }
                    }

                    frameCount++

                    if (renderer.doSwap)
                        SDL_GL_SwapWindow(window)
                }
            }
        }

        SDL_GL_DestroyContext(context)
        SDL_DestroyWindow(window)
        SDL_Quit()

        println("Bye! :3 (Executed $frameCount frames)")
    }

    fun dumpRunnerStateAsJsonToFile(runner: StarfaitRunner, dumpStateFile: File) {
        val output = File(dumpStateFile.parentFile, dumpStateFile.name.format(frameCount))
        output.writeText(JsonPrettyPrint.encodeToString(StateDumper.dumpRunnerStateAsJson(runner)))
        println("Platform: Dumped runner state!")
    }

    fun convertSDLKeycodeToGMLVK(key: Int): Int = when (key) {
        in 'a'.code..'z'.code -> key - 32 // 'a' -> VK_A
        in '0'.code..'9'.code -> key // identity
        SDLK_BACKSPACE, SDLK_TAB, SDLK_RETURN, SDLK_ESCAPE, SDLK_SPACE -> key
        SDLK_LEFT -> VK_LEFT
        SDLK_RIGHT -> VK_RIGHT
        SDLK_UP -> VK_UP
        SDLK_DOWN -> VK_DOWN
        SDLK_LSHIFT -> VK_LSHIFT
        SDLK_RSHIFT -> VK_RSHIFT
        SDLK_LCTRL -> VK_LCONTROL
        SDLK_RCTRL -> VK_RCONTROL
        SDLK_LALT -> VK_LALT
        SDLK_RALT -> VK_RALT
        SDLK_HOME -> VK_HOME
        SDLK_END -> VK_END
        SDLK_INSERT -> VK_INSERT
        SDLK_DELETE -> VK_DELETE
        SDLK_PAGEUP -> VK_PAGEUP
        SDLK_PAGEDOWN -> VK_PAGEDOWN
        SDLK_PAUSE -> VK_PAUSE
        SDLK_PRINTSCREEN -> VK_PRINTSCREEN
        in SDLK_F1..SDLK_F12 -> VK_F1 + (key - SDLK_F1)
        in SDLK_KP_1..SDLK_KP_9 -> VK_NUMPAD0 + 1 + (key - SDLK_KP_1) // SDL orders 1-9 then 0
        SDLK_KP_0 -> VK_NUMPAD0
        SDLK_KP_MULTIPLY -> VK_MULTIPLY
        SDLK_KP_PLUS -> VK_ADD
        SDLK_KP_MINUS -> VK_SUBTRACT
        SDLK_KP_PERIOD -> VK_DECIMAL
        SDLK_KP_DIVIDE -> VK_DIVIDE
        SDLK_KP_ENTER -> VK_ENTER
        else -> -1 // unmapped, ignore
    }

    sealed class SaveFolderKind {
        data class Folder(val file: File) : SaveFolderKind()
        data object Memory : SaveFolderKind()
    }
}