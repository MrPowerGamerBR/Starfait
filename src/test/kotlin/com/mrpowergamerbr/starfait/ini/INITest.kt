package com.mrpowergamerbr.starfait.ini

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals

object INITest {
    @ParameterizedTest
    @ValueSource(strings = [ "undertale.ini", "config.ini", "true_config.ini", "dr.ini", "keyconfig_0.ini" ])
    fun `test ini roundtrip`(fileName: String) {
        INITest::class.java.getResourceAsStream("/${fileName}").use {
            val originalText = it.readAllBytes().toString(Charsets.UTF_8)
            val ini = INI.parse(originalText)
            // Sometimes GameMaker has \n, sometimes it doesn't
            assertEquals(
                originalText.replace("\r\n", "\n").removeSuffix("\n"),
                ini.writeToString().removeSuffix("\n")
            )
        }
    }

    @Test
    fun `test replicate roundtrip`() {
        val ini = INI.empty()
        ini.writeReal("Flowey", "Met1", 1.0)
        ini.writeReal("Toriel", "TS", 1.0)
        ini.writeReal("Toriel", "Bscotch", 1.0)
        ini.writeReal("General", "fun", 71.0)
        ini.writeString("General", "Name", "CHARA")
        ini.writeReal("General", "Love", 1.0)
        ini.writeReal("General", "Time", 70854.0)
        ini.writeReal("General", "Kills", 0.0)
        ini.writeReal("General", "Room", 46.0)
        ini.writeReal("Sans", "M1", 1.0)
        ini.writeReal("Papyrus", "M1", 2.0)

        INITest::class.java.getResourceAsStream("/undertale.ini").use {
            val originalText = it.readAllBytes().toString(Charsets.UTF_8)
            // Sometimes GameMaker has \n, sometimes it doesn't
            assertEquals(
                originalText.replace("\r\n", "\n").removeSuffix("\n"),
                ini.writeToString().removeSuffix("\n")
            )
        }
    }
}