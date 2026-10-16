package com.mrpowergamerbr.starfait

import com.mrpowergamerbr.starfait.runner.GlobalObject
import com.mrpowergamerbr.starfait.runner.RunnerRoom
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.statedumper.DumpRunnerField
import com.mrpowergamerbr.starfait.vm.RValue
import com.mrpowergamerbr.starfait.vm.VariableContainer
import com.mrpowergamerbr.starfait.wad.DumpNested
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.OPTNFlags
import com.mrpowergamerbr.starfait.wad.RoomFlags
import com.mrpowergamerbr.starfait.wad.StringPointer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

// The Last Word in Dumping Things:tm:
// We keep it split up because if we decide to port Starfait to Kotlin/JS, we can't use Reflection to get the dumpable fields
object ReflectionUtils {
    fun transformIntoJsonElement(value: Any?, markedAsDumpable: Boolean): JsonElement {
        if (value == null) return JsonNull

        return when (value) {
            is Float -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is GMLColor -> JsonPrimitive(value.value)
            is Array<*> -> buildJsonArray {
                for (element in value) {
                    add(transformIntoJsonElement(element, markedAsDumpable))
                }
            }
            is List<*> -> buildJsonArray {
                for (element in value) {
                    add(transformIntoJsonElement(element, markedAsDumpable))
                }
            }
            is RValue -> {
                buildJsonObject {
                    put("type", value::class.simpleName)
                    put(
                        "value",
                        when (value) {
                            is RValue.Array -> buildJsonObject {
                                for ((key, value) in value.value.entries) {
                                    put(key.toString(), transformIntoJsonElement(value, false))
                                }
                            }
                            is RValue.Boolean -> JsonPrimitive(value.value)
                            is RValue.Int32 -> JsonPrimitive(value.value)
                            is RValue.Int64 -> JsonPrimitive(value.value)
                            is RValue.Real -> JsonPrimitive(value.value)
                            is RValue.String -> JsonPrimitive(value.value)
                            RValue.Undefined -> JsonNull
                        }
                    )
                }
            }
            else -> {
                if (markedAsDumpable) {
                    buildJsonObject {
                        dumpMarkedFields(this, value::class, value)
                    }
                } else {
                    error("I don't know how to transform ${value::class} into JSON!")
                }
            }
        }
    }

    // Needs to be "Any" because the compiler doesn't know what type it is for erased types
    private fun <T : Any> dumpMarkedFields(builder: JsonObjectBuilder, clazz: KClass<T>, instance: Any) {
        clazz.declaredMemberProperties.forEach {
            val dumpRunnerField = it.findAnnotation<DumpRunnerField>()

            if (dumpRunnerField != null) {
                val result = it.call(instance)
                builder.put(it.name, transformIntoJsonElement(result, true))
            }
        }

        // Special cases
        if (instance is VariableContainer) {
            builder.putJsonObject("variables") {
                for ((key, value) in instance.variables) {
                    val variableName = instance.vm.getVariableNameFromVariableSlot(key)
                    put(variableName, transformIntoJsonElement(value, false))
                }
            }
        }
    }

    fun dumpRunnerStateAsJson(runner: StarfaitRunner): JsonObject {
        return buildJsonObject {
            put("seed", runner.seed)
            putJsonObject("global") {
                dumpMarkedFields(this, GlobalObject::class, runner.vm.global)
            }

            putJsonObject("room") {
                dumpMarkedFields(this, RunnerRoom::class, runner.currentRoom)
            }
        }
    }

    fun dump(gameWAD: GameWAD, starfaitObject: Any, depth: Int, isListElement: Boolean?) {
        // We want to preserve the constructor order
        val constructorParametersName = starfaitObject::class
            .primaryConstructor!!
            .parameters
            .map { it.name }

        var isFirstElement = true
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
                        dump(gameWAD, element!!, depth + 2, true)
                    }
                } else {
                    if (value is StringPointer) {
                        value = gameWAD.strg.strings[value]
                    } else if (value is RoomFlags) {
                        value = RoomFlags.Flag.entries.filter { value.contains(it) }.joinToString(", ")
                    } else if (value is OPTNFlags) {
                        value = OPTNFlags.Flag.entries.filter { value.contains(it) }.joinToString(", ")
                    } else if (value is GMLColor) {
                        value = value.value.toHexString()
                    }

                    val addDash = isListElement == true && isFirstElement
                    println(("  ".repeat(depth - if (addDash) 1 else 0) + if (addDash) "- " else "") + it.name + ": " + value)
                    isFirstElement = false
                }
            }
    }
}