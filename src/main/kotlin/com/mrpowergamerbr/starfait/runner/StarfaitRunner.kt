package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.EventKey
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
    val seed: Long,
    val replaceRooms: Map<Int, Int>
) {
    companion object {
        // You can check that by creating a room with a single instance where the instance's code is just "show_debug_message(id)"
        const val BASE_INSTANCE_ID = 100_000
        const val MAX_ALARMS = 12
        const val HAS_NO_PARENT_ID = -100
    }

    lateinit var currentRoom: RunnerRoom
    val paths = gameWAD.path.paths.map { RunnerPath(it) }
    val operatingSystem = YYOperatingSystem.WINDOWS
    var currentInstanceId = BASE_INSTANCE_ID
    private var pendingRoom: PendingRoom? = null
    val startedAt = System.currentTimeMillis()
    val keyboard = RunnerKeyboard()
    var drawSetColor = GMLColor(0xFFFFFF)
    var activeDrawFont = gameWAD.font.fonts.firstOrNull()
    val random = Random(seed)
    val persistentRooms = mutableMapOf<Int, RunnerRoom>()

    var enableCollisionRendering = true
    val debugRectangles = mutableListOf<BoundingBox>()
    // TODO: This should be actually verified against the WAD, only for newer GM games!
    val collisionCompatibilityMode = true
    val flattenedEvents = mutableMapOf<EventKey, List<GameObject.Action>>()

    fun start() {
        generateFlattenedEvents()

        val firstRoomId = gameWAD.gen8.roomOrder.first()
        val room = gameWAD.room.rooms[firstRoomId]

        initializeRoom(firstRoomId, room)

        dispatchEventToAllInstances(EventType.OTHER, EventType.OtherSubType.GAME_START)

        // TODO: Kinda hacky isn't it?
        renderer.setDrawColor(drawSetColor)
    }

    fun initializeRoom(roomId: Int, room: Room) {
        println("Runner: Initializing room $roomId (${gameWAD.strg.getString(room.name)})")

        val persistentRoom = persistentRooms[roomId]
        if (persistentRoom != null) {
            val previousRoom = this.currentRoom
            handlePersistentRoomInstanceHandover(previousRoom, persistentRoom)

            this.currentRoom = persistentRoom
        } else {
            val newRoom = RunnerRoom(this, roomId)
            newRoom.width = room.width
            newRoom.height = room.height
            newRoom.speed = room.speed
            newRoom.persistent = room.persistent

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
                handlePersistentRoomInstanceHandover(previousRoom, newRoom)

                if (previousRoom.persistent) {
                    // If the current room is persistent, we'll store it in our map instead of destroying the instance
                    persistentRooms[previousRoom.roomId] = previousRoom

                    // However we should NOT persist persistent instances in the persistent room
                    previousRoom.instances.removeAll { it.persistent }
                }
            }

            // We need to set the currentRoom BEFORE we dispatch the create events
            this.currentRoom = newRoom

            val newlyCreatedInstances = mutableListOf<Instance>()
            for (instance in room.instances) {
                newlyCreatedInstances.add(newRoom.createAndSpawnInstance(instance))
            }

            for (instance in newlyCreatedInstances) {
                instance.dispatchEvent(EventType.CREATE, 0, null)
            }
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

        dispatchEventToAllInstancesObjectIndexOrder(EventType.STEP, EventType.StepSubType.BEGIN)

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

        dispatchKeyboard(EventType.KEYBOARD, keyboard.keyHeld)
        dispatchKeyboard(EventType.KEY_PRESS, keyboard.keyPressed)
        dispatchKeyboard(EventType.KEY_RELEASE, keyboard.keyReleased)

        dispatchEventToAllInstancesObjectIndexOrder(EventType.STEP, EventType.StepSubType.NORMAL)

        forEachInstance { instance ->
            instance.adaptSpeed()
            instance.adaptPath()

            instance.x += instance.hspeed
            instance.y += instance.vspeed
        }

        dispatchOutsideRoom()
        dispatchCollisions()
        dispatchEventToAllInstancesObjectIndexOrder(EventType.STEP, EventType.StepSubType.END)

        currentRoom.updateViews()

        for (background in currentRoom.backgrounds) {
            if (!background.enabled)
                continue

            // Sometimes a background index can be negative even though the background is enabled
            val backgroundIndex = background.backgroundIndex
            if (0 > backgroundIndex)
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

    fun handlePersistentRoomInstanceHandover(previousRoom: RunnerRoom, newRoom: RunnerRoom) {
        forEachInstance {
            it.dispatchEvent(EventType.OTHER, EventType.OtherSubType.ROOM_END, null)
        }

        val persistentInstances = previousRoom.instances.filter { it.persistent }
        newRoom.instances.addAll(persistentInstances)
    }

    fun dispatchKeyboard(eventType: Int, targetArray: Array<Boolean>) {
        var anyKey = false
        var noKey = true
        for ((index, trigger) in targetArray.withIndex()) {
            if (trigger) {
                dispatchEventToAllInstances(eventType, index)
                anyKey = true
                noKey = false
            }
        }

        if (anyKey)
            dispatchEventToAllInstances(eventType, RunnerKeyboard.VK_ANYKEY)
        if (noKey)
            dispatchEventToAllInstances(eventType, RunnerKeyboard.VK_NOKEY)
    }

    fun dispatchOutsideRoom() {
        // GameMaker-HTML5's "./scripts/Events.js"
        forEachInstance { instance ->
            val bbox = instance.getBoundingBox()
            val outside = 0.0f > bbox.right || bbox.left > currentRoom.width || 0.0f > bbox.bottom || bbox.top > currentRoom.height

            if (outside && !instance.outsideRoom)
                instance.dispatchEvent(EventType.OTHER, EventType.OtherSubType.OUTSIDE_ROOM, instance)

            instance.outsideRoom = outside
        }
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

    fun dispatchEventToAllInstancesObjectIndexOrder(eventType: Int, eventSubtype: Int) {
        forEachInstanceObjectIndex { instance ->
            instance.dispatchEvent(eventType, eventSubtype, null)
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

    // This is baaaaaad and needs to be optimized later
    // Some events like step, alarms, keyboards, mouse, joystick, outside room/intersect, collisions... are iterated based on the objectIndex, NOT the instance list!
    inline fun forEachInstanceObjectIndex(action: (Instance) -> Unit) {
        for ((index, obj) in gameWAD.objt.objects.withIndex()) {
            // Duplicate list to avoid ConcurrentModificationExceptions
            // This also needs to be iterated oldest first, when two instances have the same objectIndex
            for (instance in this.currentRoom.instances.toList()) {
                if (instance.destroyed)
                    continue

                if (instance.objectIndex == index)
                    action(instance)
            }
        }
    }

    fun firstInstance(filter: (Instance) -> Boolean): Instance? {
        for (instance in this.currentRoom.instances.reversed()) {
            val result = filter(instance)

            if (result)
                return instance
        }

        return null
    }

    fun filterInstances(filter: (Instance) -> Boolean): List<Instance> {
        val allInstances = mutableListOf<Instance>()

        for (instance in this.currentRoom.instances.reversed()) {
            val result = filter(instance)

            if (result)
                allInstances.add(instance)
        }

        return allInstances
    }

    inline fun forEachInstanceByDepth(action: (Instance) -> Unit) {
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

    fun setPendingRoom(roomId: Int) {
        val roomId = replaceRooms[roomId] ?: roomId
        pendingRoom = PendingRoom(roomId, gameWAD.room.rooms[roomId])
    }

    /**
     * Flattens events
     */
    fun generateFlattenedEvents() {
        // The Flattening:tm:
        for ((objectIndex, obj) in gameWAD.objt.objects.withIndex()) {
            val objectTree = mutableListOf<GameObject>()
            var currentObject = obj

            while (true) {
                objectTree.add(currentObject)

                val parentId = currentObject.parentId
                if (parentId == HAS_NO_PARENT_ID)
                    break

                currentObject = gameWAD.objt.objects[parentId]
            }

            // It seems that SOMETIMES GameMaker events are out of bounds
            // Even though that we are only targeting 1.4.1804 (for Undertale), their events have IDs 11 (TRIGGER) and 12 (CLEANUP) which are only used in GM:S 2
            for (eventType in 0..EventType.MAX_EVENT_ID) {
                // Top (child events) ALWAYS trumps parent events!
                for (entry in objectTree) {
                    val subevents = entry.events[eventType]

                    if (subevents.isEmpty())
                        continue

                    for (eventSubtype in subevents) {
                        val key = EventKey(objectIndex, eventType, eventSubtype.subtype)

                        if (!flattenedEvents.containsKey(key)) {
                            // The current event tree does not have it! Let's put it
                            flattenedEvents[key] = eventSubtype.actions
                        }
                    }
                }
            }
        }
    }
}