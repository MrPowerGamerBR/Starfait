package com.mrpowergamerbr.starfait.desktop

import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.vm.StarfaitVM
import net.perfectdreams.harmony.gl.shaders.bind
import org.joml.Matrix4f
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.GL_FALSE
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL11.GL_NEAREST
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP
import org.lwjgl.opengl.GL11.GL_UNSIGNED_INT
import org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.opengl.GL11.glClearColor
import org.lwjgl.opengl.GL11.glDrawArrays
import org.lwjgl.opengl.GL11.glDrawElements
import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW
import org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_STATIC_DRAW
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL15.glBufferData
import org.lwjgl.opengl.GL15.glBufferSubData
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

        val window = SDL_CreateWindow("Starfait", vm.gameWAD.gen8.initialApplicationWidth, vm.gameWAD.gen8.initialApplicationHeight, SDL_WINDOW_OPENGL)
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

        val textures = vm.gameWAD.txtr.textures.map { renderer.textureManager.loadTexture(it.blob, GL_NEAREST) }

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

                for (instance in runner.currentRoom.instances) {
                    // Only attempt to render instances that DO have a sprite index
                    if (instance.spriteIndex >= 0) {
                        val sprite = runner.gameWAD.sprt.sprites[instance.spriteIndex.toInt()]
                        val framePointer = sprite.frames[instance.imageIndex.toInt()]
                        val tpag = runner.gameWAD.tpag.entries[framePointer]!!

                        println("I ${instance.objectName} want to draw with texture ${tpag.textureIndex} at ${instance.x}, ${instance.y}")

                        val texture = textures[tpag.textureIndex.toInt()]
                        renderer.drawQuad(
                            instance.x + tpag.targetX.toFloat(),
                            instance.y + tpag.targetY.toFloat(),
                            tpag.sourceX.toFloat() / texture.width,
                            tpag.sourceY.toFloat() / texture.height,
                            (tpag.sourceX.toFloat() + tpag.sourceWidth.toFloat()) / texture.width,
                            (tpag.sourceY.toFloat() + tpag.sourceHeight.toFloat()) / texture.height,
                            sprite.width.toFloat() - tpag.targetX.toFloat(),
                            sprite.height.toFloat() - tpag.targetY.toFloat(),
                            textures[tpag.textureIndex.toInt()]
                        )
                    } else {
                        println("Instance ${instance.objectName} has no sprite index!")
                    }
                }

                renderer.flush()

                SDL_GL_SwapWindow(window)
            }
        }

        SDL_GL_DestroyContext(context)
        SDL_DestroyWindow(window)
        SDL_Quit()
    }
}