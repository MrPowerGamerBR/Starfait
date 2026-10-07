package com.mrpowergamerbr.starfait.runner.textfiles

import com.mrpowergamerbr.starfait.MathUtils

/**
 * Abstraction over [com.mrpowergamerbr.starfait.filesystem.FileSystem]'s files
 */
sealed class TextFile(val textFileId: Int, val path: String) {
    abstract fun writeString(value: String)
    abstract fun readString(): String

    // I HAVE NO IDEA WHY GAMEMAKER ADDS A SPACE AFTER THE REAL???
    fun writeReal(value: Double) = writeString(MathUtils.formatReal(value) + " ")
    abstract fun readReal(): Double

    // Same thing as readString but reads up until the end of the line
    abstract fun readLine(): String

    abstract fun hasRemaining(): Boolean

    fun writeNewLine() = writeString("\n")
}