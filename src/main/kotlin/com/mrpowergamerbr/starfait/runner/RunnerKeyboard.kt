package com.mrpowergamerbr.starfait.runner

class RunnerKeyboard {
    companion object {
        const val GML_KEY_COUNT = 256

        const val VK_BACKSPACE = 8
        const val VK_TAB = 9
        const val VK_ENTER = 13
        const val VK_SHIFT = 16
        const val VK_CONTROL = 17
        const val VK_ALT = 18
        const val VK_PAUSE = 19
        const val VK_ESCAPE = 27
        const val VK_SPACE = 32
        const val VK_PAGEUP = 33
        const val VK_PAGEDOWN = 34
        const val VK_END = 35
        const val VK_HOME = 36
        const val VK_LEFT = 37
        const val VK_UP = 38
        const val VK_RIGHT = 39
        const val VK_DOWN = 40
        const val VK_PRINTSCREEN = 44
        const val VK_INSERT = 45
        const val VK_DELETE = 46
        const val VK_NUMPAD0 = 96
        const val VK_MULTIPLY = 106
        const val VK_ADD = 107
        const val VK_SUBTRACT = 109
        const val VK_DECIMAL = 110
        const val VK_DIVIDE = 111
        const val VK_F1 = 112
        const val VK_LSHIFT = 160
        const val VK_RSHIFT = 161
        const val VK_LCONTROL = 162
        const val VK_RCONTROL = 163
        const val VK_LALT = 164
        const val VK_RALT = 165
    }

    val keyPressed = Array(GML_KEY_COUNT) { false }
    val keyHeld = Array(GML_KEY_COUNT) { false }
    val keyReleased = Array(GML_KEY_COUNT) { false }

    fun tick() {
        keyPressed.fill(false)
        keyReleased.fill(false)
    }

    fun onKeyDown(key: Int) {
        keyPressed[key] = true
        keyHeld[key] = true
    }

    fun onKeyUp(key: Int) {
        keyReleased[key] = true
        keyHeld[key] = false
    }
}