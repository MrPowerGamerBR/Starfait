package com.mrpowergamerbr.starfait.desktop

import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.vm.StarfaitVM
import net.perfectdreams.harmony.gl.shaders.bind
import org.joml.Matrix4f
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import org.lwjgl.opengl.GL11.GL_UNSIGNED_INT
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.opengl.GL11.glClearColor
import org.lwjgl.opengl.GL11.glDrawElements
import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_STATIC_DRAW
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL15.glBufferData
import org.lwjgl.opengl.GL15.glGenBuffers
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glGenVertexArrays
import org.lwjgl.sdl.SDLError.SDL_GetError
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_QUIT
import org.lwjgl.sdl.SDLEvents.SDL_PollEvent
import org.lwjgl.sdl.SDLInit.SDL_INIT_VIDEO
import org.lwjgl.sdl.SDLInit.SDL_Init
import org.lwjgl.sdl.SDLInit.SDL_Quit
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

class StarfaitWindow(val vm: StarfaitVM) {
    fun start() {
        check(SDL_Init(SDL_INIT_VIDEO)) { "SDL_Init failed: ${SDL_GetError()}" }

        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 3)
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 3)
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE)

        val window = SDL_CreateWindow("Starfait", 640, 480, SDL_WINDOW_OPENGL)
        check(window != MemoryUtil.NULL) { "SDL_CreateWindow failed: ${SDL_GetError()}" }

        val context = SDL_GL_CreateContext(window)
        check(context != MemoryUtil.NULL) { "SDL_GL_CreateContext failed: ${SDL_GetError()}" }
        SDL_GL_MakeCurrent(window, context)
        GL.createCapabilities() // required before any GL call
        SDL_GL_SetSwapInterval(1) // vsync

        val renderer = OpenGLRenderer()
        val runner = StarfaitRunner(vm.gameWAD, vm, renderer)
        vm.runner = runner
        runner.start()

        glClearColor(1.0f, 0.0f, 0.0f, 1.0f)

        val vao = glGenVertexArrays()
        val vbo = glGenBuffers()
        val ebo = glGenBuffers()

        glBindVertexArray(vao)

        // four vertices: x, y, u, v, r, g, b, a
        val vertices = floatArrayOf(
            //   x       y     u    v     r    g    b    a
            0f,     0f,   0f,  0f,   1f,  1f,  1f,  1f,   // top-left
            100f,     0f,   1f,  0f,   1f,  1f,  1f,  1f,   // top-right
            100f,   100f,   1f,  1f,   1f,  1f,  1f,  1f,   // bottom-right
            0f,   100f,   0f,  1f,   1f,  1f,  1f,  1f,   // bottom-left
        )
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)

        // two triangles sharing the 0-2 edge
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, intArrayOf(0, 1, 2, 2, 3, 0), GL_STATIC_DRAW)

        val stride = 8 * Float.SIZE_BYTES // 32
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0L)                        // aPosition
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, (2 * Float.SIZE_BYTES).toLong()) // aTexCoord
        glEnableVertexAttribArray(1)
        glVertexAttribPointer(2, 4, GL_FLOAT, false, stride, (4 * Float.SIZE_BYTES).toLong()) // aColor
        glEnableVertexAttribArray(2)

        glBindVertexArray(0)

        MemoryStack.stackPush().use { stack ->
            val event = SDL_Event.malloc(stack)
            var running = true
            while (running) {
                while (SDL_PollEvent(event)) {
                    if (event.type() == SDL_EVENT_QUIT)
                        running = false
                }

                runner.step()

                glClear(GL_COLOR_BUFFER_BIT)

                renderer.minimalShader.uProjection.set(Matrix4f().ortho(0.0f, 640.0f, 0.0f, 480.0f, 1.0f, -1.0f).get(FloatArray(16)))

                renderer.minimalShader.bind {
                    glBindVertexArray(vao)
                    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0L)
                }

                SDL_GL_SwapWindow(window)
            }
        }

        SDL_GL_DestroyContext(context)
        SDL_DestroyWindow(window)
        SDL_Quit()
    }
}