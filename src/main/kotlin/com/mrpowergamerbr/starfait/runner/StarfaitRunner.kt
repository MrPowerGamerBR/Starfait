package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.vm.StarfaitVM
import com.mrpowergamerbr.starfait.wad.GameObject
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.Room
import com.mrpowergamerbr.starfait.wad.RoomFlags
import kotlin.random.Random

class StarfaitRunner(
    val gameWAD: GameWAD,
    val vm: StarfaitVM,
    val renderer: Renderer,
    val seed: Long
) {
    companion object {
        // You can check that by creating a room with a single instance where the instance's code is just "show_debug_message(id)"
        const val BASE_INSTANCE_ID = 100_000
        const val MAX_ALARMS = 12
    }

    lateinit var currentRoom: RunnerRoom
    val operatingSystem = YYOperatingSystem.WINDOWS
    var currentInstanceId = BASE_INSTANCE_ID
    var pendingRoom: PendingRoom? = null
    val startedAt = System.currentTimeMillis()
    val keyboard = RunnerKeyboard()
    var drawSetColor = GMLColor(0xFFFFFF)
    var activeDrawFont = gameWAD.font.fonts.firstOrNull()
    val random = Random(seed)

    fun start() {
        val firstRoomId = gameWAD.gen8.roomOrder.first()
        val room = gameWAD.room.rooms[firstRoomId]

        initializeRoom(firstRoomId, room)

        dispatchEventToAllInstances(EventType.OTHER, EventType.OtherSubType.GAME_START)

        // TODO: Kinda hacky isn't it?
        renderer.setDrawColor(drawSetColor)
    }

    fun initializeRoom(roomId: Int, room: Room) {
        val newRoom = RunnerRoom(this, roomId)
        println("Runner: Initializing room $roomId (${gameWAD.strg.getString(room.name)})")
        newRoom.width = room.width
        newRoom.height = room.height

        if (RoomFlags.Flag.ENABLE_VIEWS in room.flags) {
            for (view in room.views) {
                println("Visible? ${view.visible} - View BorderX is ${view.borderX}, View BorderY is ${view.borderY}, objectId is ${view.objectId}")
                newRoom.views.add(
                    RunnerRoom.RunnerView(
                        view.viewX,
                        view.viewY,
                        view.viewWidth,
                        view.viewHeight,
                        view.portX,
                        view.portY,
                        view.portWidth,
                        view.portHeight,
                        view.visible,
                        view.objectId
                    )
                )
            }
        }

        newRoom.backgroundColor = room.backgroundColor

        for (background in room.backgrounds) {
            newRoom.backgrounds.add(
                RunnerRoom.RunnerBackground(
                    background.enabled,
                    background.x,
                    background.y,
                    background.backgroundIndex
                )
            )
        }

        for (tile in room.tiles) {
            newRoom.tiles.add(
                RunnerRoom.RunnerTile(
                    tile.x,
                    tile.y,
                    tile.backgroundIndex,
                    tile.sourceX,
                    tile.sourceY,
                    tile.width,
                    tile.height,
                    tile.depth,
                    tile.id,
                    tile.scaleX
                )
            )
        }

        // TODO: This sucks, maybe make it nullable?
        if (this::currentRoom.isInitialized) {
            val previousRoom = this.currentRoom

            forEachInstance {
                it.dispatchEvent(EventType.OTHER, EventType.OtherSubType.ROOM_END, null)
            }

            val persistentInstances = previousRoom.instances.filter { it.persistent }
            newRoom.instances.addAll(persistentInstances)
        }

        // We need to set the currentRoom BEFORE we dispatch the create events
        this.currentRoom = newRoom

        for (instance in room.instances) {
            newRoom.createAndSpawnInstance(instance)
        }

        forEachInstance {
            it.dispatchEvent(EventType.OTHER, EventType.OtherSubType.ROOM_START, null)
        }
    }

    fun step() {
        forEachInstance {
            it.xPrevious = it.x
            it.yPrevious = it.y
        }

        dispatchEventToAllInstances(EventType.STEP, EventType.StepSubType.BEGIN)

        // Alarms are checked PER ALARM ID, not per instance!
        repeat(MAX_ALARMS) { alarmId ->
            forEachInstance {  instance ->
                val alarmTick = instance.alarms[alarmId]

                // >= because we also need to tick down from 0 to -1
                if (alarmTick >= 0) {
                    val newValue = alarmTick - 1
                    instance.alarms[alarmId] = newValue

                    if (newValue == 0) {
                        println("Runner: [${instance.objectName}] Alarm $alarmId triggered!")
                        instance.dispatchEvent(EventType.ALARM, alarmId, null)
                    }
                }
            }
        }

        dispatchEventToAllInstances(EventType.STEP, EventType.StepSubType.NORMAL)
        dispatchCollisions()
        dispatchEventToAllInstances(EventType.STEP, EventType.StepSubType.END)

        currentRoom.updateViews()

        for (background in currentRoom.backgrounds) {
            if (!background.enabled)
                continue

            val background = gameWAD.bgnd.backgrounds[background.backgroundIndex]
            val tpag = gameWAD.tpag.entries[background.texturePageEntryPointer]!!

            renderer.drawBackground(
                tpag,
                currentRoom.width.toFloat(),
                currentRoom.height.toFloat()
            )
        }

        // TODO: I'm pretty sure we need to interleave instances and tiles based on the depth
        //  But for now, let's just render all tiles behind instances and hope for the best
        for (tile in currentRoom.tiles) {
            val background = gameWAD.bgnd.backgrounds[tile.backgroundIndex]
            val tpag = gameWAD.tpag.entries[background.texturePageEntryPointer]!!

            renderer.drawTile(tpag, tile)
        }

        forEachInstanceByDepth { instance ->
            val instanceHasDrawEvent = instance.dispatchEvent(EventType.DRAW, EventType.DrawSubType.DRAW, null)

            if (!instanceHasDrawEvent) {
                // Draw the instance manually
                // println("Instance ${instance.objectName} did not handle the draw event, drawing manually!")
                renderer.drawInstance(instance)
            }
        }

        val pendingRoom = this.pendingRoom
        if (pendingRoom != null) {
            initializeRoom(pendingRoom.roomId, pendingRoom.room)
            this.pendingRoom = null
        }

        this.keyboard.tick()
    }

    fun dispatchCollisions() {
        forEachInstance { selfInstance ->
            val selfBBox = selfInstance.getBoundingBox()

            forEachInstance { otherInstance ->
                // We can't collide with self
                if (selfInstance == otherInstance)
                    return@forEachInstance

                val otherBBox = otherInstance.getBoundingBox()

                if (!selfBBox.isCollidingWith(otherBBox))
                    return@forEachInstance

                selfInstance.dispatchEvent(EventType.COLLISION, otherInstance.objectIndex, otherInstance)
                otherInstance.dispatchEvent(EventType.COLLISION, selfInstance.objectIndex, selfInstance)
            }
        }
    }

    fun dispatchEventToAllInstances(eventType: Int, eventSubtype: Int) {
        forEachInstance { instance ->
            instance.dispatchEvent(eventType, eventSubtype, null)
        }
    }

    inline fun forEachInstance(action: (Instance) -> Unit) {
        // reversed creates a copy, so we don't need to worry about ConcurrentModificationException here
        for (instance in this.currentRoom.instances.reversed()) {
            if (instance.destroyed)
                continue

            action(instance)
        }
    }

    inline fun forEachInstanceByDepth(crossinline action: (Instance) -> Unit) {
        // sortedBy creates a copy, so we don't need to worry about ConcurrentModificationException here
        //
        // "In GameMaker the lower the depth value for an instance, the "closer to the camera" that instance will be drawn,
        // while a higher depth value means that the instance will be drawn "further away from the camera",
        // i.e.: -1000 is drawn on top of -100, which is drawn on top of 0, and so on."
        //
        // TODO: Technically a instance CAN BE INSERTED and it WILL BE PROCESSED HERE as long as it has a higher depth (that is, negative depth)
        // TODO: What happens with instances of the same depth? When testing it in GameMaker, it seems that it is based on the instance creation order (older instances on top?)
        for (instance in this.currentRoom.instances.sortedByDescending { it.depth }) {
            if (instance.destroyed)
                continue

            action(instance)
        }
    }

    fun getGameObjectFromInstance(instance: Instance): GameObject {
        return gameWAD.objt.objects[instance.objectIndex]
    }
}