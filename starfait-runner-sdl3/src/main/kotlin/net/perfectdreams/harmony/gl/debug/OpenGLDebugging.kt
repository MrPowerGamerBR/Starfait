package net.perfectdreams.harmony.gl.debug

import org.lwjgl.opengl.GL11.glEnable
import org.lwjgl.opengl.GL43
import org.lwjgl.opengl.GL43.GL_DEBUG_SEVERITY_HIGH
import org.lwjgl.opengl.GL43.GL_DEBUG_SEVERITY_LOW
import org.lwjgl.opengl.GL43.GL_DEBUG_SEVERITY_MEDIUM
import org.lwjgl.opengl.GL43.GL_DEBUG_SEVERITY_NOTIFICATION
import org.lwjgl.opengl.GL43.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR
import org.lwjgl.opengl.GL43.GL_DEBUG_TYPE_ERROR
import org.lwjgl.opengl.GL43.GL_DEBUG_TYPE_MARKER
import org.lwjgl.opengl.GL43.GL_DEBUG_TYPE_OTHER
import org.lwjgl.opengl.GL43.GL_DEBUG_TYPE_PERFORMANCE
import org.lwjgl.opengl.GL43.GL_DEBUG_TYPE_POP_GROUP
import org.lwjgl.opengl.GL43.GL_DEBUG_TYPE_PORTABILITY
import org.lwjgl.opengl.GL43.GL_DEBUG_TYPE_PUSH_GROUP
import org.lwjgl.opengl.GL43.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.NULL

object OpenGLDebugging {
    // Inspired by
    // - https://github.com/fendevel/Guide-to-Modern-OpenGL-Functions#detailed-messages-with-debug-output
    // - https://github.com/meemknight/ourCraft/blob/master/src/platform/errorReporting.cpp
    fun enableReportGLErrors() {
        glEnable(GL43.GL_DEBUG_OUTPUT)
        glEnable(GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS)
        GL43.glDebugMessageCallback({ source: Int, type: Int, id: Int, severity: Int, length: Int, messagePointer: Long, userParamPointer: Long ->
            val debugMessage = MemoryUtil.memUTF8(messagePointer, length)

            val loggerSuffix = when (source) {
                GL43.GL_DEBUG_SOURCE_API -> "API"
                GL43.GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "Window System"
                GL43.GL_DEBUG_SOURCE_SHADER_COMPILER -> "Shader Compiler"
                GL43.GL_DEBUG_SOURCE_THIRD_PARTY -> "Third Party"
                GL43.GL_DEBUG_SOURCE_APPLICATION -> "Application"
                GL43.GL_DEBUG_SOURCE_OTHER -> "Other"
                else -> "Unknown ($source)"
            }

            val loggerName = "OpenGL $loggerSuffix"

            val typeAsString = when (type) {
                GL_DEBUG_TYPE_ERROR -> "Error"
                GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "Deprecated Behaviour"
                GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "Undefined Behaviour"
                GL_DEBUG_TYPE_PORTABILITY -> "Portability"
                GL_DEBUG_TYPE_PERFORMANCE -> "Performance"
                GL_DEBUG_TYPE_MARKER -> "Marker"
                GL_DEBUG_TYPE_PUSH_GROUP -> "Push Group"
                GL_DEBUG_TYPE_POP_GROUP -> "Pop Group"
                GL_DEBUG_TYPE_OTHER -> "Other"
                else -> "Unknown ($type)"
            }

            val severityAsString = when (severity) {
                GL_DEBUG_SEVERITY_HIGH -> "High"
                GL_DEBUG_SEVERITY_MEDIUM -> "Medium"
                GL_DEBUG_SEVERITY_LOW -> "Low"
                GL_DEBUG_SEVERITY_NOTIFICATION -> "Notification"
                else -> "Unknown ($severity)"
            }

            try {
                error("OpenGL debug message callback")
            } catch (e: Exception) {
                println("OpenGL ($severityAsString): [${typeAsString}] $debugMessage\n${e.stackTraceToString()}")
                e.printStackTrace()
            }
        }, NULL)
    }
}