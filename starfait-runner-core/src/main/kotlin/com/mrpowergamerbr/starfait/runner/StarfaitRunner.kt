package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.WADUtils
import com.mrpowergamerbr.starfait.audio.AudioManager
import com.mrpowergamerbr.starfait.filesystem.LayeredFileSystem
import com.mrpowergamerbr.starfait.renderer.Renderer
import com.mrpowergamerbr.starfait.runner.resources.RunnerResources
import com.mrpowergamerbr.starfait.runner.textfiles.TextFileManager
import com.mrpowergamerbr.starfait.vm.StarfaitVM
import com.mrpowergamerbr.starfait.wad.GameObject
import com.mrpowergamerbr.starfait.wad.Room
import com.mrpowergamerbr.starfait.wad.RoomFlags
import kotlin.comparisons.compareBy
import kotlin.random.Random

class StarfaitRunner(
    val resources: RunnerResources,
    val vm: StarfaitVM,
    val renderer: Renderer,
    val audioManager: AudioManager,
    val layeredFileSystem: LayeredFileSystem,
    val output: GameOutput,
    val seed: Long,
    val replaceRooms: Map<Int, Int>
) {
    companion object {
        // You can check that by creating a room with a single instance where the instance's code is just "show_debug_message(id)"
        // Keep in mind that this is DIFFERENT from the *first* ID used when a new instance is created via code!
        const val BASE_INSTANCE_ID = 100_000
        const val MAX_ALARMS = 12
    }

    lateinit var currentRoom: RunnerRoom

    val operatingSystem = YYOperatingSystem.WINDOWS
    var currentInstanceId = BASE_INSTANCE_ID
    private var pendingRoom: PendingRoom? = null
    var pendingRestart = false
    var gameEnded = false
    val startedAt = System.currentTimeMillis()
    val keyboard = RunnerKeyboard()
    val random = Random(seed)
    val persistentRooms = mutableMapOf<Int, RunnerRoom>()
    var drawState = DrawState(resources)
    var openINI: OpenINI? = null
    val textFiles = TextFileManager(layeredFileSystem)

    var enableCollisionRendering = false
    val debugRectangles = mutableListOf<DebugBoundingBox>()
    var drawInvisibleInstances = false
    // TODO: This should be actually verified against the WAD, only for newer GM games!
    val collisionCompatibilityMode = true
    var currentViewId: Int? = null

    /**
     * Flattened map of events, indexed by [EventKey].
     *
     * The flattened map is used to avoid looking up parent objects to figure out which event the object *actually* has, that is:
     * If you have an obj_main1 with a Create event, and obj_main2 inherits obj_main1, the flattened map will contain the Create event for obj_main2!
     *
     * Subevents (like collision events) are NOT flattened! (See [collisionPairs])
     */
    val flattenedEvents = mutableMapOf<EventKey, List<GameObject.Action>>()

    /**
     * Collision pairs, sorted in ascending order.
     *
     * This has all the possible collisions that the game has registered collision events, including if the object has inherited collision events, that is:
     * If you have an obj_main2 that inherits obj_main1, and obj_main1 has a collision event, and obj_player has an event against obj_main1,
     * the collision pair will contain the collision event for obj_main1 and obj_main2.
     */
    val collisionPairs = mutableListOf<CollisionPair>()

    fun start() {
        generateFlattenedEvents()
        generateCollisionPairs()
        resetCurrentInstanceId()
        initializeFirstRoom()
    }

    fun initializeFirstRoom() {
        val firstRoomId = resources.roomOrder.first()
        val room = resources.room.rooms[firstRoomId]

        initializeRoom(firstRoomId, room, false)

        dispatchEventToAllInstances(EventType.OTHER, EventType.OtherSubType.GAME_START)
    }

    fun initializeRoom(roomId: Int, room: Room, keepPersistentInstances: Boolean) {
        val persistentRoom = persistentRooms.remove(roomId)
        println("Runner: Initializing room $roomId (${resources.strg.getString(room.name)}) - Has persistent room? ${persistentRoom != null}")

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
            newRoom.flags = room.flags

            for (view in room.views) {
                println("Visible? ${view.visible} - View BorderX is ${view.borderX}, View BorderY is ${view.borderY}, objectIndex is ${view.objectIndex}")
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
                        view.objectIndex,
                        view.borderX,
                        view.borderY,
                        view.speedX,
                        view.speedY,
                        0f
                    )
                )
            }

            newRoom.backgroundColor = room.backgroundColor

            for (background in room.backgrounds) {
                newRoom.backgrounds.add(
                    RunnerRoom.RunnerBackground(
                        background.enabled,
                        background.x,
                        background.y,
                        background.backgroundIndex,
                        background.stretch,
                        1.0f,
                        background.tileX,
                        background.tileY
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
                        tile.scaleX,
                        true
                    )
                )
            }

            // TODO: This sucks, maybe make it nullable?
            if (this::currentRoom.isInitialized) {
                val previousRoom = this.currentRoom
                if (keepPersistentInstances)
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

            val newlyCreatedInstances = mutableListOf<Pair<Room.Instance, Instance>>()
            for (instance in room.instances) {
                newlyCreatedInstances.add(Pair(instance, newRoom.createAndSpawnInstance(instance)))
            }

            for ((roomInstance, instance) in newlyCreatedInstances) {
                instance.dispatchEvent(EventType.CREATE, 0, null)
                if (roomInstance.creationCodeId != WADUtils.NO_CREATION_CODE_ID) {
                    vm.executeCode(resources.code.codes[roomInstance.creationCodeId], instance, null, instance.gameObject, 0, 0, arrayOf())
                }
            }
        }

        if (room.creationCodeId != WADUtils.NO_CREATION_CODE_ID) {
            // When running a room creation code, it seems that GameMaker creates a "fake"/"dummy" instance for it
            // Because you *can* set "self" variables, but at the same time it isn't bound to the global instance, and changing rooms does NOT persist the variable
            val dummy = Instance(this, 0, 0)
            // You may be wondering: "Wow what a gigantic hack"
            // But THAT'S what the original runner does!
            // If you call "event_perform(0, 0)" on a room creation code, it WILL execute the event of the first object in the list!
            // So that's why it is this horrible horrid hack
            // PS: I did test, and the original YoYo Runner doesn't crash if there isn't any object while using "event_perform(0, 0)"
            // Oh well, let's just pretend that any competent game will at least have ONE object on the list, right?
            val dummyGameObject = resources.objt.objects[0]
            vm.executeCode(resources.code.codes[room.creationCodeId], dummy, null, dummyGameObject, 0, 0, arrayOf())
        }

        forEachInstance {
            it.dispatchEvent(EventType.OTHER, EventType.OtherSubType.ROOM_START, null)
        }
    }

    fun tick() {
        forEachInstance {
            it.xPrevious = it.x
            it.yPrevious = it.y
            it.pathPositionPrevious = it.pathPosition
            it.animate()
        }

        forEachInstance { it.updateImage() }

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

        val drawables = mutableListOf<Drawable>()

        for (tile in currentRoom.tiles) {
            drawables.add(Drawable.Tile(tile, tile.depth))
        }

        forEachInstanceByDepth { instance ->
            drawables.add(Drawable.Instance(instance, instance.depth.toInt()))
        }

        if (RoomFlags.Flag.ENABLE_VIEWS in currentRoom.flags) {
            for ((index, view) in currentRoom.views.withIndex()) {
                if (!view.visible)
                    continue

                this.currentViewId = index

                renderer.withView(view) {
                    drawBackgrounds()
                    drawDrawables(drawables)
                    drawDrawablesEnd(drawables)
                }
            }
        } else {
            renderer.withView(null) {
                drawBackgrounds()
                drawDrawables(drawables)
                drawDrawablesEnd(drawables)
            }
        }

        this.currentViewId = null

        if (this.pendingRestart) {
            this.pendingRestart = false

            reset()
            initializeFirstRoom()

            this.renderer.doSwap = false
        } else {
            val pendingRoom = this.pendingRoom
            val hasPendingRoom = pendingRoom != null

            if (hasPendingRoom) {
                initializeRoom(pendingRoom.roomId, pendingRoom.room, true)
                this.pendingRoom = null
            }

            this.currentRoom.instances.removeAll { it.destroyed }
            this.renderer.doSwap = !hasPendingRoom
        }

        this.audioManager.tick()
        this.keyboard.resetPressedAndReleasedKeys()
    }

    fun allocateNewRuntimeInstanceId() = this.currentInstanceId++

    fun handlePersistentRoomInstanceHandover(previousRoom: RunnerRoom, newRoom: RunnerRoom) {
        forEachInstance {
            it.dispatchEvent(EventType.OTHER, EventType.OtherSubType.ROOM_END, null)
        }

        val persistentInstances = previousRoom.instances.filter { it.persistent }
        newRoom.instances.addAll(persistentInstances)
    }

    private fun drawBackgrounds() {
        for (runnerBackground in currentRoom.backgrounds) {
            if (!runnerBackground.enabled)
                continue

            // Sometimes a runnerBackground index can be negative even though the runnerBackground is enabled
            val backgroundIndex = runnerBackground.backgroundIndex
            if (0 > backgroundIndex)
                continue

            val background = resources.backgrounds[runnerBackground.backgroundIndex]
            val tpag = background.texturePageEntry ?: continue

            renderer.drawBackground(
                tpag,
                runnerBackground.x.toFloat(),
                runnerBackground.y.toFloat(),
                if (runnerBackground.stretch) currentRoom.width.toFloat() else tpag.sourceWidth.toFloat(),
                if (runnerBackground.stretch) currentRoom.height.toFloat() else tpag.sourceHeight.toFloat(),
                runnerBackground.tileX,
                runnerBackground.tileY,
                currentRoom.width,
                currentRoom.height,
                runnerBackground.alpha,
            )
        }
    }

    private fun drawDrawables(drawables: List<Drawable>) {
        for (drawable in drawables.sortedByDescending { it.depth }) {
            when (drawable) {
                is Drawable.Instance -> {
                    val instance = drawable.instance

                    // "This works by telling GameMaker to skip Draw Events for this instance, so care must be taken when using this as it means that
                    // no code placed in any of the Draw events will be run while the instance is not visible."
                    if (instance.visible || this.drawInvisibleInstances) {
                        val instanceHasDrawEvent = instance.dispatchEvent(EventType.DRAW, EventType.DrawSubType.DRAW, null)

                        if (!instanceHasDrawEvent) {
                            // Draw the instance manually
                            // println("Instance ${instance.objectName} did not handle the draw event, drawing manually!")
                            renderer.drawInstance(instance)
                        }
                    }
                }
                is Drawable.Tile -> {
                    val tile = drawable.tile

                    if (tile.visible) {
                        val background = resources.backgrounds[tile.backgroundIndex]
                        val tpag = background.texturePageEntry

                        if (tpag == null)
                            continue

                        renderer.drawTile(tpag, tile)
                    }
                }
            }
        }
    }

    private fun drawDrawablesEnd(drawables: List<Drawable>) {
        for (drawable in drawables.sortedByDescending { it.depth }) {
            when (drawable) {
                is Drawable.Instance -> {
                    val instance = drawable.instance

                    // "This works by telling GameMaker to skip Draw Events for this instance, so care must be taken when using this as it means that
                    // no code placed in any of the Draw events will be run while the instance is not visible."
                    if (instance.visible || this.drawInvisibleInstances) {
                        instance.dispatchEvent(EventType.DRAW, EventType.DrawSubType.DRAW_END, null)
                    }
                }
                is Drawable.Tile -> {}
            }
        }
    }

    private fun dispatchKeyboard(eventType: Int, targetArray: Array<Boolean>) {
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

    private fun dispatchOutsideRoom() {
        // GameMaker-HTML5's "./scripts/Events.js"
        forEachInstance { instance ->
            val bbox = instance.getBoundingBox()
            val outside = 0.0f > bbox.right || bbox.left > currentRoom.width || 0.0f > bbox.bottom || bbox.top > currentRoom.height

            if (outside && !instance.outsideRoom)
                instance.dispatchEvent(EventType.OTHER, EventType.OtherSubType.OUTSIDE_ROOM, instance)

            instance.outsideRoom = outside
        }
    }

    private fun dispatchCollisions() {
        // Contrary to popular belief: It seems that GameMaker-HTML5 does NOT model collision dispatch like the C++ YoYo Runner
        // So that's why this is a bit different
        // The tl;dr is: You MUST iterate by the objectIndex (ascending order)
        for (pair in this.collisionPairs) {
            // Duplicate list to avoid ConcurrentModificationExceptions
            val stableList = this.currentRoom.instances.toList()

            for (selfInstance in stableList) {
                if (selfInstance.destroyed)
                    continue

                if (selfInstance.objectIndex == pair.objectIndex1) {
                    for (otherInstance in stableList) {
                        if (otherInstance.destroyed)
                            continue

                        if (otherInstance.objectIndex == pair.objectIndex2) {
                            // We can't collide with self
                            if (selfInstance == otherInstance)
                                continue

                            if (!selfInstance.isCollidingWith(otherInstance, true))
                                continue

                            // Solid is ONLY handled if any of the instances has a collision event against the other instance!
                            // However, if we are here, it does mean that one of the two objects DO have a collision event (because of the collision pair)
                            val eitherSolid = selfInstance.solid || otherInstance.solid

                            if (eitherSolid) {
                                selfInstance.revertToPreviousPosition()
                                otherInstance.revertToPreviousPosition()
                            }

                            selfInstance.dispatchEvent(EventType.COLLISION, otherInstance.objectIndex, otherInstance)
                            otherInstance.dispatchEvent(EventType.COLLISION, selfInstance.objectIndex, selfInstance)

                            if (collisionCompatibilityMode && eitherSolid) {
                                // We do not call the end-of-path event again
                                selfInstance.adaptPath()
                                otherInstance.adaptPath()
                                selfInstance.x += selfInstance.hspeed
                                selfInstance.y += selfInstance.vspeed
                                otherInstance.x += otherInstance.hspeed
                                otherInstance.y += otherInstance.vspeed

                                // If collision is not resolved then set to previous position
                                if (selfInstance.isCollidingWith(otherInstance, true)) {
                                    selfInstance.revertToPreviousPosition()
                                    otherInstance.revertToPreviousPosition()
                                }
                            }
                        }
                    }
                }
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
        for (instance in this.currentRoom.instances.toList()) {
            if (instance.destroyed)
                continue

            action(instance)
        }
    }

    // This is baaaaaad and needs to be optimized later
    // Some events like step, alarms, keyboards, mouse, joystick, outside room/intersect, collisions... are iterated based on the objectIndex, NOT the instance list!
    inline fun forEachInstanceObjectIndex(action: (Instance) -> Unit) {
        for ((index, obj) in resources.objt.objects.withIndex()) {
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
        return resources.objt.objects[instance.objectIndex]
    }

    fun setPendingRoom(roomId: Int) {
        val roomId = replaceRooms[roomId] ?: roomId
        pendingRoom = PendingRoom(roomId, resources.room.rooms[roomId])
    }

    /**
     * Flattens events
     */
    private fun generateFlattenedEvents() {
        // The Flattening:tm:
        for ((objectIndex, obj) in resources.objt.objects.withIndex()) {
            val objectTree = resources.objt.getGameObjectTree(objectIndex, obj).map { it.gameObject }

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

    /**
     * Generates a list of collision pairs, ordered by the [GameObject]'s index in ascending order.
     *
     * If you have these objects:
     *
     * * obj_collision1 has collision events to obj_collision2 and obj_collision3
     * * obj_collision2 has collision events to obj_collision1 and obj_collision3
     * * obj_collision3 has collision events to obj_collision2 and obj_collision3
     *
     * The collision pairs of them will be:
     *
     * * `obj_collision1` -> `[obj_collision2, obj_collision3]`
     * * `obj_collision2` -> `[obj_collision3]`
     *
     * (the `obj_collision3` doesn't have one by itself because the only possible collision for it already exists)
     */
    private fun generateCollisionPairs() {
        // We first use a set to avoid duplicates, then we convert to a ArrayList for fast iteration (yippee)
        val collisionPairs = mutableSetOf<CollisionPair>()

        for ((selfGameObjectIndex, selfGameObject) in resources.objt.objects.withIndex()) {
            // Which events do we have?
            for ((otherGameObjectIndex, otherGameObject) in resources.objt.objects.withIndex()) {
                val objectTree = resources.objt.getGameObjectTree(otherGameObjectIndex, otherGameObject)

                // We now iterate all the ancestors, if ANY of them have a collision event, we register the pair
                for ((ancestorGameObjectIndex, _) in objectTree) {
                    // Check if we actually have a collision event
                    val hasCollisionEvent = this.flattenedEvents[EventKey(selfGameObjectIndex, EventType.COLLISION, ancestorGameObjectIndex)]?.isNotEmpty() == true

                    if (hasCollisionEvent) {
                        collisionPairs.add(CollisionPair.of(selfGameObjectIndex, otherGameObjectIndex))
                        break
                    }
                }
            }
        }

        this.collisionPairs.addAll(collisionPairs.toList().sortedWith(compareBy(CollisionPair::objectIndex1).thenBy(CollisionPair::objectIndex2)))
    }

    private fun resetCurrentInstanceId() {
        // This looks and sounds stupid, but if you create a room with an instance, and that instance creates a new instance, the ID of the first instance will be 100_000 and the ID of the new instance will be 100_002
        this.currentInstanceId = this.resources.maxRoomInstanceId + 1
    }

    private fun reset() {
        this.vm.reset()
        this.persistentRooms.clear()
        this.audioManager.stopAllSounds()
        resetCurrentInstanceId()
        this.drawState = DrawState(this.resources)
        this.openINI = null
    }
}