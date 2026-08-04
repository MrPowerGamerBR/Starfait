package com.mrpowergamerbr.starfait.runner

object EventType {
    const val CREATE = 0
    const val DESTROY = 1
    const val ALARM = 2
    const val STEP = 3
    const val OTHER = 7

    object StepSubType {
        const val NORMAL = 0
        const val BEGIN = 1
        const val END = 2
    }

    object OtherSubType {
        const val GAME_START = 2
    }
}