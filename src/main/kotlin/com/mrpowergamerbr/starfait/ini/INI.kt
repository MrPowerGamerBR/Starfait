package com.mrpowergamerbr.starfait.ini

import com.mrpowergamerbr.starfait.MathUtils
import java.util.Locale

@ConsistentCopyVisibility
data class INI private constructor(val sections: MutableMap<String, MutableMap<String, String>>) {
    companion object {
        fun empty(): INI {
            return INI(mutableMapOf())
        }

        fun parse(text: String): INI {
            val sections = mutableMapOf<String, MutableMap<String, String>>()
            var currentSectionName: String? = null
            val currentSectionProperties = mutableMapOf<String, String>()

            fun appendCurrentSectionIfNeeded() {
                if (currentSectionName != null) {
                    sections[currentSectionName!!] = currentSectionProperties.toMutableMap()
                    currentSectionName = null
                    currentSectionProperties.clear()
                }
            }

            for (line in text.lines()) {
                if (line.startsWith("[")) {
                    appendCurrentSectionIfNeeded()
                    currentSectionName = line.removePrefix("[").removeSuffix("]")
                    continue
                }

                val name = line.substringBefore("=")
                if (name.isBlank())
                    continue

                val value = line.substringAfter("=")

                // TODO: GameMaker's docs show an example where reals are not saved with quotes, but GameMaker: Studio 1.4.9999 seems to always save them with quotes
                // GameMaker doesn't seem to escape quotes altogether, so we also won't do it :3
                currentSectionProperties[name] = value.removeSurrounding("\"")
            }

            appendCurrentSectionIfNeeded()

            return INI(sections)
        }
    }

    fun readString(section: String, key: String): String? {
        return this.sections[section]?.get(key)
    }

    fun readReal(section: String, key: String): Double? {
        return this.sections[section]?.get(key)?.let { MathUtils.strtod(it) }
    }

    fun writeReal(section: String, key: String, value: Double) {
        this.sections.getOrPut(section) { mutableMapOf() }[key] = "%.6f".format(Locale.ROOT, value)
    }

    fun writeString(section: String, key: String, value: String) {
        this.sections.getOrPut(section) { mutableMapOf() }[key] = value
    }

    fun writeToString(): String {
        val builder = StringBuilder()

        for ((sectionName, entries) in this.sections) {
            builder.appendLine("[$sectionName]")
            for ((key, value) in entries) {
                builder.appendLine("$key=\"$value\"")
            }
        }

        return builder.toString()
    }
}