package com.mrpowergamerbr.starfait.vm

class GMLArray {
    // Sparsely packed because GameMaker uses 32000 to differentiate multi dimensional arrays
    val entries = mutableMapOf<Int, RValue>()
}