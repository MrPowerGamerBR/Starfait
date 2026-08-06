package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.wad.GameObject
import com.mrpowergamerbr.starfait.wad.Room
import com.mrpowergamerbr.starfait.wad.TexturePageEntryPointer

class RunnerRoom(val runner: StarfaitRunner, val roomId: Int) {
    var width = 0
    var height = 0
    val instances = mutableListOf<Instance>()
    val views = mutableListOf<RunnerView>()
    val backgrounds = mutableListOf<RunnerBackground>()

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
        gameInstance.persistent = gameObject.persistent

        builder(gameInstance)
        this.instances.add(gameInstance)
        gameInstance.dispatchEvent(EventType.CREATE, 0)
        return gameInstance
    }

    fun updateViews() {
        for (view in views) {
            if (view.visible) {
                // TODO: Pretty sure that this can also be a instanceId
                val followingObjectId = view.objectId
                val followingObject = instances.firstOrNull { it.objectIndex == followingObjectId }

                // TODO: Actually do something useful with it
            }
        }
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
        var visible: Boolean,
        var objectId: Int,
    )

    class RunnerBackground(
        var enabled: Boolean,
        var x: Int,
        var y: Int,
        var backgroundIndex: Int
    )
}