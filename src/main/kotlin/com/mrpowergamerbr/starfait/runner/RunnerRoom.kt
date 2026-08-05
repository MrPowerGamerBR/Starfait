package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.wad.GameObject
import com.mrpowergamerbr.starfait.wad.Room

class RunnerRoom(val runner: StarfaitRunner, val roomId: Int) {
    var width = 0
    var height = 0
    val instances = mutableListOf<Instance>()

    fun createAndSpawnInstance(instance: Room.Instance): Instance {
        val gameInstance = Instance(runner, runner.currentInstanceId++, instance.objectIndex)
        this.instances.add(gameInstance)
        val gameObject = runner.getGameObjectFromInstance(gameInstance)

        val createEvents = gameObject.events[EventType.CREATE]
        for (action in createEvents.flatMap { it.actions }) {
            val code = runner.gameWAD.code.codes[action.codeId]

            val previousInstance = runner.vm.currentInstance
            runner.vm.currentInstance = gameInstance
            runner.vm.executeCode(code, emptyArray())
            runner.vm.currentInstance = previousInstance
        }
        return gameInstance
    }

    fun createAndSpawnInstance(gameObjectIndex: Int): Instance {
        val gameInstance = Instance(runner, runner.currentInstanceId++, gameObjectIndex)
        this.instances.add(gameInstance)
        val gameObject = runner.getGameObjectFromInstance(gameInstance)

        val createEvents = gameObject.events[EventType.CREATE]
        for (action in createEvents.flatMap { it.actions }) {
            val code = runner.gameWAD.code.codes[action.codeId]

            val previousInstance = runner.vm.currentInstance
            runner.vm.currentInstance = gameInstance
            runner.vm.executeCode(code, emptyArray())
            runner.vm.currentInstance = previousInstance
        }
        return gameInstance
    }
}