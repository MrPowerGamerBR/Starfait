package com.mrpowergamerbr.starfait

import kotlin.time.Clock

object TimeUtils {
    fun currentTimeMillis() = Clock.System.now().toEpochMilliseconds()
}