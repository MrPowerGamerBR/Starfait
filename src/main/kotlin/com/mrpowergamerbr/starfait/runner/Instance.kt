package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.vm.InstanceReferenceType
import com.mrpowergamerbr.starfait.vm.VariableScope
import com.mrpowergamerbr.starfait.wad.GameObject
import kotlin.collections.filter

class Instance(val runner: StarfaitRunner, var id: Int, var objectIndex: Int) : ObjectBase(runner.vm) {
    var x = 0.0f
    var y = 0.0f
    var xPrevious = 0.0f
    var yPrevious = 0.0f
    var imageSpeed = 1.0f
    var imageXScale = 1.0f
    var imageYScale = 1.0f
    var imageAlpha = 1.0f
    var imageIndex = 0.0f
    var spriteIndex = 0.0f
    var maskIndex = -1.0f
    var speed = 0.0f
    var depth = 0.0f
    var direction = 0.0f
    var imageBlend = -1.0f
    var imageAngle = 0.0f
    var pathSpeed = 0.0f
    var pathPosition = 0.0f
    var hspeed = 0.0f
    var vspeed = 0.0f
    var friction = 0.0f
    var persistent = false
    var solid = false
    var alarms = Array(StarfaitRunner.MAX_ALARMS) { -1 }
    var visible = true
    var destroyed = false
        private set

    val gameObject: GameObject
        get() = runner.gameWAD.objt.objects[this.objectIndex]
    val objectName: String
        get() = runner.gameWAD.strg.getString(gameObject.name)

    fun setAlarm(alarm: Int, time: Int) {
        require(alarm in 0 until StarfaitRunner.MAX_ALARMS) { "Invalid alarm index!" }

        println("Instance: [${runner.vm.currentCallFrameCodeName}] [$objectName] Alarm $alarm set to $time")

        this.alarms[alarm] = time
    }

    fun isAlarmActive(alarm: Int): Boolean {
        require(alarm in 0 until StarfaitRunner.MAX_ALARMS) { "Invalid alarm index!" }
        return this.alarms[alarm] >= 0
    }

    /**
     * Dispatches an event to this instance
     *
     * @return true if the instance had a event liostener for it
     */
    fun dispatchEvent(eventType: Int, eventSubtype: Int, other: Instance?): Boolean {
        return dispatchEvent(this.gameObject, eventType, eventSubtype, other)
    }

    /**
     * Dispatches an event to this instance
     *
     * @return true if the instance had a event liostener for it
     */
    fun dispatchEvent(gameObject: GameObject, eventType: Int, eventSubtype: Int, other: Instance?): Boolean {
        var gameObject = gameObject
        val events = gameObject.events[eventType]

        val subtypeEvents = if (eventType == EventType.COLLISION) {
            // TODO: This SUCKS even more
            val allParentsOfCollidedObject = mutableListOf<Int>()

            var targetGameObjectId = eventSubtype
            while (true) {
                if (0 > targetGameObjectId)
                    break

                allParentsOfCollidedObject.add(targetGameObjectId)
                val targetGameObject = vm.gameWAD.objt.objects[targetGameObjectId]
                targetGameObjectId = targetGameObject.parentId
            }

            val subtypeEvents = mutableListOf<GameObject.Action>()

            // Get the first matched event
            for (id in allParentsOfCollidedObject) {
                if (subtypeEvents.isNotEmpty())
                    break

                subtypeEvents.addAll(
                    events
                        .filter { it.subtype == id }
                        .flatMap { it.actions }
                )
            }

            subtypeEvents
        } else {
            var subtypeEvents = events
                .filter { it.subtype == eventSubtype }
                .flatMap { it.actions }

            // TODO: This SUCKS, we really should precalculate all events ahead of time on boot
            var attempts = 0
            while (subtypeEvents.isEmpty()) {
                if (0 > gameObject.parentId)
                    break

                gameObject = runner.gameWAD.objt.objects[gameObject.parentId]
                subtypeEvents = gameObject.events[eventType]
                    .filter { it.subtype == eventSubtype }
                    .flatMap { it.actions }
                attempts++
            }

            subtypeEvents
        }

        val previousRelative = this.runner.vm.dragAndDropRelativeValues
        this.runner.vm.dragAndDropRelativeValues = false
        for (action in subtypeEvents) {
            val code = this.runner.gameWAD.code.codes[action.codeId]

            if (runner.vm.eventsToBeTraced.contains("*") || runner.vm.eventsToBeTraced.contains(objectName)) {
                val eventTypeName = EventType.getEventTypeName(eventType)
                val eventSubtypeName = EventType.getEventSubtypeName(eventType, eventSubtype)

                println("Runner: [${this.objectName} / ${this.id}] Dispatching Event $eventTypeName $eventSubtypeName ($eventType, $eventSubtype)")
            }

            this.runner.vm.executeCode(code, this, other, gameObject, eventType, eventSubtype, emptyArray())
        }
        this.runner.vm.dragAndDropRelativeValues = previousRelative

        return subtypeEvents.isNotEmpty()
    }

    fun markAsDestroyed(fireDestroyEvent: Boolean) {
        check(!this.destroyed) { "Instance is already destroyed!" }

        this.destroyed = true

        if (fireDestroyEvent)
            dispatchEvent(EventType.DESTROY, 0, null)
    }

    fun getBoundingBox(): BoundingBox {
        // bbox_*: "Please note that when the instance has no sprite assigned the value returned will be the same as the instance Y position."
        val maskIndex = (if (this.maskIndex >= 0.0f) this.maskIndex else this.spriteIndex).toInt()

        if (0 > maskIndex) {
            return BoundingBox(
                this.x,
                this.x,
                this.y,
                this.y
            )
        }

        val sprite = runner.gameWAD.sprt.sprites[maskIndex]

        val scaledBBoxLeftAndOffset = (sprite.bboxLeft - sprite.originX) * this.imageXScale
        val scaledBBoxRightAndOffset = (sprite.bboxRight - sprite.originX) * this.imageXScale
        val scaledBBoxTopAndOffset = (sprite.bboxTop - sprite.originY) * this.imageYScale
        val scaledBBoxBottomAndOffset = (sprite.bboxBottom - sprite.originY) * this.imageYScale

        val bboxLeftAbsolute = this.x + scaledBBoxLeftAndOffset
        val bboxRightAbsolute = this.x + scaledBBoxRightAndOffset
        val bboxTopAbsolute = this.y + scaledBBoxTopAndOffset
        val bboxBottomAbsolute = this.y + scaledBBoxBottomAndOffset

        return BoundingBox(
            bboxLeftAbsolute,
            bboxRightAbsolute,
            bboxTopAbsolute,
            bboxBottomAbsolute
        )
    }

    fun matchesObjectOrInstanceId(
        queryId: Int,
        excludeInstance: Instance?,
        skipDestroyed: Boolean,
        checkHierarchy: Boolean,
        checkSpecialSelf: Boolean,
        checkSpecialOther: Boolean,
        checkSpecialAll: Boolean,
        checkSpecialNoone: Boolean,
    ): Boolean {
        if (this == excludeInstance)
            return false

        if (skipDestroyed && this.destroyed)
            return false

        if (0 > queryId) {
            return when {
                checkSpecialSelf && queryId == InstanceReferenceType.SELF -> vm.currentSelfInstance == this
                checkSpecialOther && queryId == InstanceReferenceType.OTHER -> vm.currentCallFrame.other == this
                checkSpecialAll && queryId == InstanceReferenceType.ALL -> true
                checkSpecialNoone && queryId == InstanceReferenceType.NOONE -> false
                else -> error("Unsupported InstanceReferenceType ID $queryId!")
            }
        }

        if (queryId >= StarfaitRunner.BASE_INSTANCE_ID) {
            return this.id == queryId
        } else {
            if (this.objectIndex == queryId)
                return true

            if (checkHierarchy) {
                // TODO: We really should optimize this by storing a bit set or a boolean set
                var parentGameObjectId = this.gameObject.parentId

                while (true) {
                    if (0 > parentGameObjectId)
                        break

                    if (parentGameObjectId == queryId)
                        return true

                    parentGameObjectId = runner.gameWAD.objt.objects[parentGameObjectId].parentId
                }
            }

            return false
        }
    }
}