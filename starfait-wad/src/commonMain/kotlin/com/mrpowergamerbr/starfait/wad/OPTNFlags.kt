package com.mrpowergamerbr.starfait.wad

import kotlin.jvm.JvmInline

@JvmInline
value class OPTNFlags(val flags: UInt) {
    operator fun contains(flag: Flag) = flags and (1u shl flag.bit) != 0u

    enum class Flag(val bit: Int) {
        FULLSCREEN(0),
        INTERPOLATE(1),
        USE_NEW_AUDIO(2),
        NO_BORDER(3),
        SHOW_CURSOR(4),
        SIZEABLE(5),
        STAY_ON_TOP(6),
        CHANGE_RESOLUTION(7),
        NO_BUTTONS(8),
        SCREEN_KEY(9),
        HELP_KEY(10),
        QUIT_KEY(11),
        SAVE_KEY(12),
        SCREENSHOT_KEY(13),
        CLOSE_ESC(14),
        FREEZE(15),
        SHOW_PROGRESS(16),
        LOAD_TRANSPARENT(17),
        SCALE_PROGRESS(18),
        // Read by the runner but unused
        WRITE_ERRORS(20),
        ABORT_ERRORS(21),
        VARIABLE_ERRORS(22),
        CREATION_EVENT_ORDER(23),
        USE_FRONT_TOUCH(24),
        USE_REAR_TOUCH(25),
        USE_FAST_COLLISION(26),
        FAST_COLLISION_COMPATIBILITY(27),
    }
}