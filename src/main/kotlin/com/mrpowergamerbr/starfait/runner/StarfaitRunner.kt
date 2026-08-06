package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.vm.StarfaitVM
import com.mrpowergamerbr.starfait.wad.GameObject
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.Room
import com.mrpowergamerbr.starfait.wad.RoomFlags

class StarfaitRunner(
    val gameWAD: GameWAD,
    val vm: StarfaitVM,
    val renderer: Renderer
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

    fun start() {
        val firstRoomId = gameWAD.gen8.roomOrder.first()
        val room = gameWAD.room.rooms[firstRoomId]

        initializeRoom(firstRoomId, room)

        dispatchEventToAllInstances(EventType.OTHER, EventType.OtherSubType.GAME_START)
    }

    fun initializeRoom(roomId: Int, room: Room) {
        val newRoom = RunnerRoom(this, roomId)
        newRoom.width = room.width
        newRoom.height = room.height

        if (RoomFlags.Flag.ENABLE_VIEWS in room.flags) {
            for (view in room.views) {
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
                        view.visible
                    )
                )
            }
        }

        // TODO: This sucks, maybe make it nullable?
        if (this::currentRoom.isInitialized) {
            val previousRoom = this.currentRoom
            val persistentInstances = previousRoom.instances.filter { it.persistent }
            newRoom.instances.addAll(persistentInstances)
        }

        // We need to set the currentRoom BEFORE we dispatch the create events
        this.currentRoom = newRoom

        for (instance in room.instances) {
            newRoom.createAndSpawnInstance(instance)
        }
    }

    fun step() {
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
                        instance.dispatchEvent(EventType.ALARM, alarmId)
                    }
                }
            }
        }

        dispatchEventToAllInstances(EventType.STEP, EventType.StepSubType.NORMAL)
        dispatchEventToAllInstances(EventType.STEP, EventType.StepSubType.END)

        forEachInstanceByDepth { instance ->
            val instanceHasDrawEvent = instance.dispatchEvent(EventType.DRAW, EventType.DrawSubType.DRAW)

            if (!instanceHasDrawEvent) {
                // Draw the instance manually
                println("Instance ${instance.objectName} did not handle the draw event, drawing manually!")
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

    fun dispatchEventToAllInstances(eventType: Int, eventSubtype: Int) {
        forEachInstance { instance ->
            instance.dispatchEvent(eventType, eventSubtype)
        }
    }

    inline fun forEachInstance(crossinline action: (Instance) -> Unit) {
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