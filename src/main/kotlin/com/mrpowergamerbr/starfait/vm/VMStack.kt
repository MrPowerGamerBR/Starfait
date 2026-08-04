package com.mrpowergamerbr.starfait.vm

class VMStack {
    val stack = Array<String?>(1024) { null }
    var top = 0

    fun pushString(string: String) {
        stack[top++] = string
    }

    fun popString(): String {
        return stack[--top]!!
    }
}