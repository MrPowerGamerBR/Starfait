package com.mrpowergamerbr.starfait.vm

class VMStack {
    val stack = Array(1024) { RValue() }
    var top = 0
        private set

    fun push(): RValue {
        require(stack.size != top) { "VM stack overflow! Values on the stack: ${stack.joinToString { it.toGMLString() }}" }
        return stack[top++]
    }

    fun pop(): RValue {
        require(top != 0) { "VM stack underflow!" }
        return stack[--top]
    }

    fun peek(): RValue {
        require(stack.size != top) { "You can't peek an empty stack!" }
        return stack[top - 1]
    }

    fun peekAt(index: Int): RValue {
        require(top > index) { "Trying to peek out of bounds! You tried peeking $index but the top of the stack is at $top!" }
        return stack[index]
    }
}