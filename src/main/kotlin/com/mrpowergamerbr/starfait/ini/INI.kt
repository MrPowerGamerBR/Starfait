package com.mrpowergamerbr.starfait.ini

data class INI(val sections: MutableMap<String, MutableMap<String, String>>) {
    companion object {
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