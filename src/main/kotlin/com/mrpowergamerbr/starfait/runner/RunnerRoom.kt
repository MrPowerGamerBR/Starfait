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
        gameInstance.dispatchEvent(EventType.CREATE, 0)
        return gameInstance
    }

    fun createAndSpawnInstance(gameObjectIndex: Int): Instance {
        val gameInstance = Instance(runner, runner.currentInstanceId++, gameObjectIndex)
        this.instances.add(gameInstance)
        gameInstance.dispatchEvent(EventType.CREATE, 0)
        return gameInstance
    }
}