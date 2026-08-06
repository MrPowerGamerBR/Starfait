package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.desktop.OpenGLRenderer
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

    fun start() {
        val firstRoomId = gameWAD.gen8.roomOrder.first()
        val room = gameWAD.room.rooms[firstRoomId]

        initializeRoom(firstRoomId, room)

        dispatchEventToAllInstances(EventType.OTHER, EventType.OtherSubType.GAME_START)
    }

    fun initializeRoom(roomId: Int, room: Room) {
        val currentRoom = RunnerRoom(this, roomId)
        currentRoom.width = room.width
        currentRoom.height = room.height

        if (RoomFlags.Flag.ENABLE_VIEWS in room.flags) {
            for (view in room.views) {
                currentRoom.views.add(
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

        // We need to set the currentRoom BEFORE we dispatch the create events
        this.currentRoom = currentRoom

        for (instance in room.instances) {
            currentRoom.createAndSpawnInstance(instance)
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

        forEachInstance { instance ->
            val instanceHasDrawEvent = instance.dispatchEvent(EventType.DRAW, EventType.DrawSubType.DRAW)

            if (!instanceHasDrawEvent) {
                // Draw the instance manually
                println("Instance ${instance.objectName} did not handle the draw event, drawing manually!")
                // renderer.drawInstance(instance)
            }
        }

        val pendingRoom = this.pendingRoom
        if (pendingRoom != null) {
            initializeRoom(pendingRoom.roomId, pendingRoom.room)
            this.pendingRoom = null
        }
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

    fun getGameObjectFromInstance(instance: Instance): GameObject {
        return gameWAD.objt.objects[instance.objectIndex]
    }
}