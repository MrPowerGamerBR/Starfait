package com.mrpowergamerbr.starfait.wad

@JvmInline
value class RoomFlags(val flags: UInt) {
    operator fun contains(flag: Flag) = flags and (1u shl flag.bit) != 0u

    enum class Flag(val bit: Int) {
        /**
         * "Enable the use of Views" in GameMaker: Studio 1.4.9999
         */
        ENABLE_VIEWS(0),

        /**
         * "Clear Background with Window Colour" in GameMaker: Studio 1.4.9999
         */
        CLEAR_BACKGROUND_WITH_WINDOW_COLOUR(1),

        /**
         * "Clear Display Buffer with Window Colour" in GameMaker: Studio 1.4.9999 (bit set when disabled on the IDE)
         */
        DO_NOT_CLEAR_DISPLAY_SURFACE(2)
    }
}