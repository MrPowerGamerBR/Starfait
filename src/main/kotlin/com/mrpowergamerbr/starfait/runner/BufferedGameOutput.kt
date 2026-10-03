package com.mrpowergamerbr.starfait.runner

class BufferedGameOutput : GameOutput {
    val output = StringBuilder()

    override fun showDebugMessage(message: String) {
        output.appendLine(message)
    }
}