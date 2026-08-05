package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.vm.StarfaitVM
import com.mrpowergamerbr.starfait.wad.GameObject
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.Room

class StarfaitRunner(val gameWAD: GameWAD, val vm: StarfaitVM) {
    companion object {
        // You can check that by creating a room with a single instance where the instance's code is just "show_debug_message(id)"
        const val BASE_INSTANCE_ID = 100_000
    }

    lateinit var currentRoom: RunnerRoom
    val operatingSystem = YYOperatingSystem.WINDOWS
    var currentInstanceId = BASE_INSTANCE_ID
    var pendingRoom: PendingRoom? = null

    fun start() {
        val firstRoomId = gameWAD.gen8.roomOrder.first()
        val room = gameWAD.room.rooms[firstRoomId]

        val currentRoom = initializeRoom(firstRoomId, room)
        this.currentRoom = currentRoom

        for (instance in currentRoom.instances) {
            val gameObject = getGameObjectFromInstance(instance)
            val gameStartEvents = gameObject.events[EventType.OTHER]
            for (action in gameStartEvents.filter { it.subtype == EventType.OtherSubType.GAME_START } .flatMap { it.actions }) {
                val code = gameWAD.code.codes[action.codeId]

                vm.currentInstance = instance
                vm.executeCode(code, emptyArray())
            }
        }
    }

    fun initializeRoom(roomId: Int, room: Room): RunnerRoom {
        val currentRoom = RunnerRoom(roomId)

        for (instance in room.instances) {
            val gameInstance = Instance(currentInstanceId++, instance.objectIndex)
            currentRoom.instances.add(gameInstance)
            val gameObject = getGameObjectFromInstance(gameInstance)

            val createEvents = gameObject.events[EventType.CREATE]
            for (action in createEvents.flatMap { it.actions }) {
                val code = gameWAD.code.codes[action.codeId]

                vm.currentInstance = gameInstance
                vm.executeCode(code, emptyArray())
            }
        }

        return currentRoom
    }

    fun step() {
        for (instance in currentRoom.instances) {
            val gameObject = gameWAD.objt.objects[instance.objectIndex]

            val stepEvents = gameObject.events[EventType.STEP]

            val beginStepEvents = stepEvents.filter { it.subtype == EventType.StepSubType.BEGIN }.flatMap { it.actions }
            for (action in beginStepEvents) {
                val code = gameWAD.code.codes[action.codeId]

                vm.currentInstance = instance
                vm.executeCode(code, emptyArray())
            }

            val normalStepEvents = stepEvents.filter { it.subtype == EventType.StepSubType.NORMAL }.flatMap { it.actions }
            for (action in normalStepEvents) {
                val code = gameWAD.code.codes[action.codeId]

                vm.currentInstance = instance
                vm.executeCode(code, emptyArray())
            }

            val endStepEvents = stepEvents.filter { it.subtype == EventType.StepSubType.END }.flatMap { it.actions }
            for (action in endStepEvents) {
                val code = gameWAD.code.codes[action.codeId]

                vm.currentInstance = instance
                vm.executeCode(code, emptyArray())
            }
        }

        val pendingRoom = this.pendingRoom
        if (pendingRoom != null) {
            val runnerRoom = initializeRoom(pendingRoom.roomId, pendingRoom.room)
            this.currentRoom = runnerRoom
            this.pendingRoom = null
        }
    }

    fun getGameObjectFromInstance(instance: Instance): GameObject {
        return gameWAD.objt.objects[instance.objectIndex]
    }
}