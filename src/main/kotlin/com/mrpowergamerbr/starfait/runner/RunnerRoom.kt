package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.wad.GameObject
import com.mrpowergamerbr.starfait.wad.Room

class RunnerRoom(val runner: StarfaitRunner, val roomId: Int) {
    var width = 0
    var height = 0
    val instances = mutableListOf<Instance>()
    val views = mutableListOf<RunnerView>()

    fun createAndSpawnInstance(instance: Room.Instance) = createAndSpawnInstance(instance.objectIndex) {
        this.x = instance.x.toFloat()
        this.y = instance.y.toFloat()
    }

    fun createAndSpawnInstance(gameObjectIndex: Int, builder: Instance.() -> (Unit)): Instance {
        val gameInstance = Instance(runner, runner.currentInstanceId++, gameObjectIndex)
        val gameObject = gameInstance.gameObject

        // Initialize with the object's default values
        gameInstance.visible = gameObject.visible
        gameInstance.spriteIndex = gameObject.spriteIndex.toFloat()
        gameInstance.depth = gameObject.depth.toFloat()

        builder(gameInstance)
        this.instances.add(gameInstance)
        gameInstance.dispatchEvent(EventType.CREATE, 0)
        return gameInstance
    }

    class RunnerView(
        var viewX: Int,
        var viewY: Int,
        var viewWidth: Int,
        var viewHeight: Int,
        var portX: Int,
        var portY: Int,
        var portWidth: Int,
        var portHeight: Int,
        var visible: Boolean
    )
}