package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.runner.StarfaitRunner

abstract class BuiltinFunction(val names: List<String>, val parameterRange: IntRange) {
    abstract fun execute(runner: StarfaitRunner, vm: StarfaitVM, args: List<RValue>): RValue
}