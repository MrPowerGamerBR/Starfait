package com.mrpowergamerbr.starfait.runner

class RunnerRoom(val roomId: Int) {
    var width = 0
    var height = 0
    val instances = mutableListOf<Instance>()
}