package com.mrpowergamerbr.starfait.vm

class VMStack {
    val stack = Array(1024) { RValue() }
    var top = 0
        private set

    fun push() = stack[top++]
    fun pop() = stack[--top]
    fun peek() = stack[top - 1]
    fun peekAt(index: Int) = stack[index]
}