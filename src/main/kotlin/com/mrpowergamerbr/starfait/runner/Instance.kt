package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.wad.GameObject
import kotlin.collections.filter

class Instance(val runner: StarfaitRunner, var id: Int, var objectIndex: Int) : ObjectBase() {
    var x = 0.0f
    var y = 0.0f
    var imageSpeed = 1.0f
    var imageXScale = 1.0f
    var imageYScale = 1.0f
    var imageAlpha = 1.0f
    var imageIndex = 0.0f
    var spriteIndex = 0.0f
    var depth = 0.0f
    var alarms = Array(StarfaitRunner.MAX_ALARMS) { -1 }
    var visible = true
    var destroyed = false

    val gameObject: GameObject
        get() = runner.gameWAD.objt.objects[this.objectIndex]
    val objectName: String
        get() = runner.gameWAD.strg.getString(gameObject.name)

    fun setAlarm(alarm: Int, time: Int) {
        require(alarm in 0 until StarfaitRunner.MAX_ALARMS) { "Invalid alarm index!" }

        println("Instance: [${runner.vm.currentCallFrameCodeName}] [$objectName] Alarm $alarm set to $time")

        this.alarms[alarm] = time
    }

    fun dispatchEvent(eventType: Int, eventSubtype: Int) {
        var gameObject = this.runner.gameWAD.objt.objects[this.objectIndex]
        val events = gameObject.events[eventType]

        var subtypeEvents = events
            .filter { it.subtype == eventSubtype }
            .flatMap { it.actions }

        // TODO: This SUCKS, we really should precalculate all events ahead of time on boot
        var attempts = 0
        while (subtypeEvents.isEmpty()) {
            if (0 > gameObject.parentId)
                break

            gameObject = runner.gameWAD.objt.objects[gameObject.parentId]
            subtypeEvents = gameObject.events[eventType]
                .filter { it.subtype == eventSubtype }
                .flatMap { it.actions }
            attempts++
        }

        for (action in subtypeEvents) {
            val code = this.runner.gameWAD.code.codes[action.codeId]
            this.runner.vm.executeCode(code, this, emptyArray())
        }
    }
}