package com.mrpowergamerbr.starfait.statedumper

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.runner.GlobalObject
import com.mrpowergamerbr.starfait.runner.RunnerRoom
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.vm.RValue
import com.mrpowergamerbr.starfait.vm.VariableContainer
import kotlinx.serialization.json.*
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

// The Last Word in Dumping Things:tm:
// We keep it split up because if we decide to port Starfait to Kotlin/JS, we can't use Reflection to get the dumpable fields
object StateDumper {
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
                    val variableName = instance.vm.getVariableNameFromVariableIndex(key)
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
}