package com.mrpowergamerbr.starfait.runner

import kotlin.math.max
import kotlin.math.min

@ConsistentCopyVisibility
data class CollisionPair private constructor(val objectIndex1: Int, val objectIndex2: Int) {
    companion object {
        fun of(objectIndex1: Int, objectIndex2: Int) = CollisionPair(
            min(objectIndex1, objectIndex2),
            max(objectIndex1, objectIndex2)
        )
    }
}