package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.statedumper.DumpRunnerField
import com.mrpowergamerbr.starfait.wad.Room
import com.mrpowergamerbr.starfait.wad.RoomFlags
import kotlin.math.floor

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
    var flags = RoomFlags(0u)

    /**
     * Creates and spawns an instance [instance] from the room data
     *
     * Does **NOT** fire the [EventType.CREATE] event!
     */
    fun createAndSpawnInstance(instance: Room.Instance) = createAndSpawnInstance(instance.objectIndex, instance.id) {
        this.x = instance.x.toFloat()
        this.y = instance.y.toFloat()
        this.imageXScale = instance.scaleX
        this.imageYScale = instance.scaleY
        this.imageAngle = instance.rotation
    }

    /**
     * Creates and spawns an instance with the [gameObjectIndex], using the settings configured in the [builder]
     *
     * Does **NOT** fire the [EventType.CREATE] event!
     */
    fun createAndSpawnInstance(gameObjectIndex: Int, instanceId: Int, builder: Instance.() -> (Unit)): Instance {
        val gameInstance = Instance(runner, instanceId, gameObjectIndex)
        val gameObject = gameInstance.gameObject

        println("Runner: [${runner.vm.currentOrNullCallFrameCodeName}] Spawning instance ${gameInstance.id} with object ${gameInstance.objectIndex} (${gameInstance.objectName}) at (${gameInstance.x}, ${gameInstance.y})")

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

    // Views are a bit hard to come by, because most of the GameMaker-HTML5 code only handles GameMaker: Studio 2+ cameras (because g_isZeus is set)
    // So we port the camera logic instead
    // See GameMaker-HTML5's CameraManager.js CCamera.prototype.Update
    fun updateViews() {
        if (RoomFlags.Flag.ENABLE_VIEWS !in this.flags)
            return

        for (view in views) {
            if (!view.visible)
                continue

            if (0 > view.objectIndex)
                continue

            val inst = if (StarfaitRunner.BASE_INSTANCE_ID > view.objectIndex) {
                instances.firstOrNull { !it.destroyed && it.objectIndex == view.objectIndex }
            } else {
                instances.firstOrNull { !it.destroyed && it.id == view.objectIndex }
            } ?: continue

            val halfwidth = view.viewWidth * 0.5f
            val halfheight = view.viewHeight * 0.5f
            var l = view.viewX.toFloat()
            var t = view.viewY.toFloat()

            val ix = floor(inst.x)
            val iy = floor(inst.y)

            if (2 * view.borderX >= view.viewWidth) {
                l = ix - halfwidth
            } else if (ix - view.borderX < view.viewX) {
                l = ix - view.borderX
            } else if (ix + view.borderX > (view.viewX + view.viewWidth)) {
                l = ix + view.borderX - view.viewWidth
            }

            if (2 * view.borderY >= view.viewHeight) {
                t = iy - halfheight
            } else if (iy - view.borderY < view.viewY) {
                t = iy - view.borderY
            } else if (iy + view.borderY > (view.viewY + view.viewHeight)) {
                t = iy + view.borderY - view.viewHeight
            }

            // Make sure it does not extend beyond the room
            if (0 > l) l = 0f
            if (l + view.viewWidth > this.width) l = (this.width - view.viewWidth).toFloat()
            if (0 > t) t = 0f
            if (t + view.viewHeight > this.height) t = (this.height - view.viewHeight).toFloat()

            // Restrict motion speed
            if (view.speedX >= 0) {
                if ((l < view.viewX) && (view.viewX - l > view.speedX)) l = (view.viewX - view.speedX).toFloat()
                if ((l > view.viewX) && (l - view.viewX > view.speedX)) l = (view.viewX + view.speedX).toFloat()
            }
            if (view.speedY >= 0) {
                if ((t < view.viewY) && (view.viewY - t > view.speedY)) t = (view.viewY - view.speedY).toFloat()
                if ((t > view.viewY) && (t - view.viewY > view.speedY)) t = (view.viewY + view.speedY).toFloat()
            }

            view.viewX = l.toInt()
            view.viewY = t.toInt()
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
        @DumpRunnerField var objectIndex: Int,
        @DumpRunnerField var borderX: Int,
        @DumpRunnerField var borderY: Int,
        @DumpRunnerField var speedX: Int,
        @DumpRunnerField var speedY: Int,
    )

    class RunnerBackground(
        @DumpRunnerField var enabled: Boolean,
        @DumpRunnerField var x: Int,
        @DumpRunnerField var y: Int,
        @DumpRunnerField var backgroundIndex: Int,
        @DumpRunnerField var stretch: Boolean,
        @DumpRunnerField var alpha: Float,
        @DumpRunnerField var tileX: Boolean,
        @DumpRunnerField var tileY: Boolean,
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
        @DumpRunnerField var visible: Boolean
    )
}