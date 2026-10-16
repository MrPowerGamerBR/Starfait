package com.mrpowergamerbr.starfait.runner

class StdoutGameOutput : GameOutput {
    override fun showDebugMessage(message: String) {
        println("Game: $message")
    }
}