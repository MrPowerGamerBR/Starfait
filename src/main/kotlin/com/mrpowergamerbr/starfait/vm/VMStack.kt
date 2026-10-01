package com.mrpowergamerbr.starfait.vm

class VMStack {
    val stack = Array<RValue>(1024) { RValue.Undefined }
    var top = 0
        private set

    fun push(value: RValue) {
        require(stack.size != top) { "VM stack overflow! Values on the stack: ${stack.joinToString { it.toStringTyped() }}" }
        stack[top++] = value
    }

    fun pop(): RValue {
        require(top != 0) { "VM stack underflow!" }
        return stack[--top]
    }

    fun peek(): RValue {
        require(top != 0) { "You can't peek an empty stack!" }
        return stack[top - 1]
    }

    fun peekAt(index: Int): RValue {
        require(top > index) { "Trying to peek out of bounds! You tried peeking $index but the top of the stack is at $top!" }
        return stack[index]
    }
}