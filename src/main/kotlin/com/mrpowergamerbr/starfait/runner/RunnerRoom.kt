package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.statedumper.DumpRunnerField
import com.mrpowergamerbr.starfait.wad.GameObject
import com.mrpowergamerbr.starfait.wad.Room
import com.mrpowergamerbr.starfait.wad.TexturePageEntryPointer

class RunnerRoom(val runner: StarfaitRunner, @DumpRunnerField val roomId: Int) {
    @DumpRunnerField var width = 0
    @DumpRunnerField var height = 0
    @DumpRunnerField var speed = 0
    @DumpRunnerField var backgroundColor = GMLColor(0)
    @DumpRunnerField var persistent = false
    @DumpRunnerField val instances = mutableListOf<Instance>()
    @DumpRunnerField val views = mutableListOf<RunnerView>()
    @DumpRunnerField val backgrounds = mutableListOf<RunnerBackground>()
    @DumpRunnerField val tiles = mutableListOf<RunnerTile>()

    fun createAndSpawnInstance(instance: Room.Instance) = createAndSpawnInstance(instance.objectIndex) {
        this.x = instance.x.toFloat()
        this.y = instance.y.toFloat()
        this.imageXScale = instance.scaleX
        this.imageYScale = instance.scaleY
        this.imageAngle = instance.rotation
    }

    fun createAndSpawnInstance(gameObjectIndex: Int, builder: Instance.() -> (Unit)): Instance {
        val gameInstance = Instance(runner, runner.currentInstanceId++, gameObjectIndex)
        val gameObject = gameInstance.gameObject

        println("Runner: Spawning instance ${gameInstance.id} with object ${gameInstance.objectIndex} (${gameInstance.objectName}) at (${gameInstance.x}, ${gameInstance.y})")

        // Initialize with the object's default values
        gameInstance.visible = gameObject.visible
        gameInstance.spriteIndex = gameObject.spriteIndex.toFloat()
        gameInstance.depth = gameObject.depth.toFloat()
        gameInstance.persistent = gameObject.persistent
        gameInstance.solid = gameObject.solid
        gameInstance.maskIndex = gameObject.maskId.toFloat()

        builder(gameInstance)

        gameInstance.xPrevious = gameInstance.x
        gameInstance.yPrevious = gameInstance.y
        gameInstance.xStart = gameInstance.x
        gameInstance.yStart = gameInstance.y

        this.instances.add(gameInstance)
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
        @DumpRunnerField var viewX: Int,
        @DumpRunnerField var viewY: Int,
        @DumpRunnerField var viewWidth: Int,
        @DumpRunnerField var viewHeight: Int,
        @DumpRunnerField var portX: Int,
        @DumpRunnerField var portY: Int,
        @DumpRunnerField var portWidth: Int,
        @DumpRunnerField var portHeight: Int,
        @DumpRunnerField var visible: Boolean,
        @DumpRunnerField var objectId: Int,
    )

    class RunnerBackground(
        @DumpRunnerField var enabled: Boolean,
        @DumpRunnerField var x: Int,
        @DumpRunnerField var y: Int,
        @DumpRunnerField var backgroundIndex: Int
    )

    class RunnerTile(
        @DumpRunnerField var x: Int,
        @DumpRunnerField var y: Int,
        @DumpRunnerField var backgroundIndex: Int,
        @DumpRunnerField var sourceX: Int,
        @DumpRunnerField var sourceY: Int,
        @DumpRunnerField var width: Int,
        @DumpRunnerField var height: Int,
        @DumpRunnerField var depth: Int,
        @DumpRunnerField var id: Int,
        @DumpRunnerField var scaleX: Float,
    )
}