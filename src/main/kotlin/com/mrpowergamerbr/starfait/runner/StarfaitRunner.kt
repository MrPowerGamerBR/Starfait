package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.vm.StarfaitVM
import com.mrpowergamerbr.starfait.wad.GameWAD

class StarfaitRunner(val gameWAD: GameWAD, val vm: StarfaitVM) {
    lateinit var currentRoom: RunnerRoom

    fun start() {
        val firstRoomId = gameWAD.gen8.roomOrder.first()
        val room = gameWAD.room.rooms[firstRoomId]

        val currentRoom = RunnerRoom()

        for (instance in room.instances) {
            currentRoom.instances.add(Instance(instance.objectIndex))
        }

        this.currentRoom = currentRoom
    }

    fun step() {
        for (instance in currentRoom.instances) {
            val gameObject = gameWAD.objt.objects[instance.objectIndex]
            // TODO: This is borked because we technically need to filter by the subtype
            val code = gameWAD.code.codes[gameObject.events[EventType.STEP][EventType.StepSubType.NORMAL].actions[0].codeId]

            vm.currentInstance = instance
            vm.executeLoop(code)
        }
    }
}