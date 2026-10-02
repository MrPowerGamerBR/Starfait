package com.mrpowergamerbr.starfait

import kotlinx.serialization.Serializable

@Serializable
data class RecordedFrame(
    val keyDown: Set<Int>,
    val keyUp: Set<Int>,
)