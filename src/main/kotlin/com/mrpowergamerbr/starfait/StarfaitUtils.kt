package com.mrpowergamerbr.starfait

import com.mrpowergamerbr.starfait.wad.DumpNested
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.StringPointer
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

object StarfaitUtils {
    fun dump(gameWAD: GameWAD, starfaitObject: Any, depth: Int) {
        // We want to preserve the constructor order
        val constructorParametersName = starfaitObject::class
            .primaryConstructor!!
            .parameters
            .map { it.name }

        starfaitObject::class
            .memberProperties
            .sortedBy {
                val index = constructorParametersName.indexOf(it.name)
                if (index != -1) index else Int.MAX_VALUE
            }
            .forEach {
                var value = it.call(starfaitObject)
                if (it.hasAnnotation<DumpNested>()) {
                    println("  ".repeat(depth) + it.name + "[]: ")
                    for (element in value as List<*>) {
                        dump(gameWAD, element!!, depth + 1)
                    }
                } else {
                    if (value is StringPointer) {
                        value = gameWAD.strg.strings[value]
                    }

                    println("  ".repeat(depth) + it.name + ": " + value)
                }
            }
    }
}