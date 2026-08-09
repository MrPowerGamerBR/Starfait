package com.mrpowergamerbr.starfait.desktop

import com.mrpowergamerbr.starfait.runner.PendingRoom
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
import com.mrpowergamerbr.starfait.vm.StarfaitVM
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.opengl.GL11.glClearColor
import org.lwjgl.sdl.SDLError.SDL_GetError
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_KEY_DOWN
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_KEY_UP
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_QUIT
import org.lwjgl.sdl.SDLEvents.SDL_PollEvent
import org.lwjgl.sdl.SDLInit.SDL_INIT_VIDEO
import org.lwjgl.sdl.SDLInit.SDL_Init
import org.lwjgl.sdl.SDLInit.SDL_Quit
import org.lwjgl.sdl.SDLKeycode.SDLK_BACKSPACE
import org.lwjgl.sdl.SDLKeycode.SDLK_DELETE
import org.lwjgl.sdl.SDLKeycode.SDLK_DOWN
import org.lwjgl.sdl.SDLKeycode.SDLK_END
import org.lwjgl.sdl.SDLKeycode.SDLK_ESCAPE
import org.lwjgl.sdl.SDLKeycode.SDLK_F1
import org.lwjgl.sdl.SDLKeycode.SDLK_F12
import org.lwjgl.sdl.SDLKeycode.SDLK_HOME
import org.lwjgl.sdl.SDLKeycode.SDLK_INSERT
import org.lwjgl.sdl.SDLKeycode.SDLK_KP_0
import org.lwjgl.sdl.SDLKeycode.SDLK_KP_1
import org.lwjgl.sdl.SDLKeycode.SDLK_KP_9
import org.lwjgl.sdl.SDLKeycode.SDLK_KP_DIVIDE
import org.lwjgl.sdl.SDLKeycode.SDLK_KP_ENTER
import org.lwjgl.sdl.SDLKeycode.SDLK_KP_MINUS
import org.lwjgl.sdl.SDLKeycode.SDLK_KP_MULTIPLY
import org.lwjgl.sdl.SDLKeycode.SDLK_KP_PERIOD
import org.lwjgl.sdl.SDLKeycode.SDLK_KP_PLUS
import org.lwjgl.sdl.SDLKeycode.SDLK_LALT
import org.lwjgl.sdl.SDLKeycode.SDLK_LCTRL
import org.lwjgl.sdl.SDLKeycode.SDLK_LEFT
import org.lwjgl.sdl.SDLKeycode.SDLK_LSHIFT
import org.lwjgl.sdl.SDLKeycode.SDLK_PAGEDOWN
import org.lwjgl.sdl.SDLKeycode.SDLK_PAGEUP
import org.lwjgl.sdl.SDLKeycode.SDLK_PAUSE
import org.lwjgl.sdl.SDLKeycode.SDLK_PRINTSCREEN
import org.lwjgl.sdl.SDLKeycode.SDLK_RALT
import org.lwjgl.sdl.SDLKeycode.SDLK_RCTRL
import org.lwjgl.sdl.SDLKeycode.SDLK_RETURN
import org.lwjgl.sdl.SDLKeycode.SDLK_RIGHT
import org.lwjgl.sdl.SDLKeycode.SDLK_RSHIFT
import org.lwjgl.sdl.SDLKeycode.SDLK_SPACE
import org.lwjgl.sdl.SDLKeycode.SDLK_TAB
import org.lwjgl.sdl.SDLKeycode.SDLK_UP
import org.lwjgl.sdl.SDLVideo.SDL_CreateWindow
import org.lwjgl.sdl.SDLVideo.SDL_DestroyWindow
import org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_MAJOR_VERSION
import org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_MINOR_VERSION
import org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_PROFILE_CORE
import org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_PROFILE_MASK
import org.lwjgl.sdl.SDLVideo.SDL_GL_CreateContext
import org.lwjgl.sdl.SDLVideo.SDL_GL_DestroyContext
import org.lwjgl.sdl.SDLVideo.SDL_GL_MakeCurrent
import org.lwjgl.sdl.SDLVideo.SDL_GL_SetAttribute
import org.lwjgl.sdl.SDLVideo.SDL_GL_SetSwapInterval
import org.lwjgl.sdl.SDLVideo.SDL_GL_SwapWindow
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_OPENGL
import org.lwjgl.sdl.SDL_Event
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import kotlin.use

class StarfaitWindow(val vm: StarfaitVM, val seed: Long) {
    fun start() {
        check(SDL_Init(SDL_INIT_VIDEO)) { "SDL_Init failed: ${SDL_GetError()}" }

        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 3)
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 3)
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE)

        val window = SDL_CreateWindow("Starfait", vm.gameWAD.gen8.initialApplicationWidth, vm.gameWAD.gen8.initialApplicationHeight, SDL_WINDOW_OPENGL)
        check(window != MemoryUtil.NULL) { "SDL_CreateWindow failed: ${SDL_GetError()}" }

        val context = SDL_GL_CreateContext(window)
        check(context != MemoryUtil.NULL) { "SDL_GL_CreateContext failed: ${SDL_GetError()}" }
        SDL_GL_MakeCurrent(window, context)
        GL.createCapabilities() // required before any GL call
        SDL_GL_SetSwapInterval(1) // vsync

        val renderer = OpenGLRenderer(vm.gameWAD)
        val runner = StarfaitRunner(vm.gameWAD, vm, renderer, seed)
        renderer.runner = runner
        vm.runner = runner
        runner.start()

        glClearColor(1.0f, 0.0f, 0.0f, 1.0f)

        MemoryStack.stackPush().use { stack ->
            val event = SDL_Event.malloc(stack)
            var running = true
            while (running) {
                while (SDL_PollEvent(event)) {
                    if (event.type() == SDL_EVENT_QUIT)
                        running = false

                    if (event.type() == SDL_EVENT_KEY_DOWN) {
                        runner.keyboard.onKeyDown(convertSDLKeycodeToGMLVK(event.key().key()))
                    }

                    if (event.type() == SDL_EVENT_KEY_UP) {
                        if (event.key().key() == SDLK_PAGEUP) {
                            runner.pendingRoom = PendingRoom(
                                runner.currentRoom.roomId - 1,
                                vm.gameWAD.room.rooms[runner.currentRoom.roomId - 1]
                            )
                        }

                        if (event.key().key() == SDLK_PAGEDOWN) {
                            runner.pendingRoom = PendingRoom(
                                runner.currentRoom.roomId + 1,
                                vm.gameWAD.room.rooms[runner.currentRoom.roomId + 1]
                            )
                        }

                        runner.keyboard.onKeyUp(convertSDLKeycodeToGMLVK(event.key().key()))
                    }
                }

                renderer.frame {
                    runner.step()
                }

                SDL_GL_SwapWindow(window)
            }
        }

        SDL_GL_DestroyContext(context)
        SDL_DestroyWindow(window)
        SDL_Quit()
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
}