package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.MathUtils
import com.mrpowergamerbr.starfait.vm.InstanceReferenceType
import com.mrpowergamerbr.starfait.vm.VariableScope
import com.mrpowergamerbr.starfait.wad.GameObject
import com.sun.tools.javac.jvm.ByteCodes.fmod
import kotlin.collections.filter
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class Instance(val runner: StarfaitRunner, var id: Int, var objectIndex: Int) : ObjectBase(runner.vm) {
    var x = 0.0f
    var y = 0.0f
    var xPrevious = 0.0f
    var yPrevious = 0.0f
    var xStart = 0.0f
    var yStart = 0.0f
    var imageSpeed = 1.0f
    var imageXScale = 1.0f
    var imageYScale = 1.0f
    var imageAlpha = 1.0f
    var imageIndex = 0.0f
    var spriteIndex = 0.0f
    var maskIndex = -1.0f
    var depth = 0.0f
    var imageBlend = -1.0f
    var imageAngle = 0.0f
    var pathSpeed = 0.0f
    var pathPosition = 0.0f

    var hspeed: Float
        get() = _hspeed
        set(value) { if (_hspeed == value) return; _hspeed = value; computeSpeed1() }

    var vspeed: Float
        get() = _vspeed
        set(value) { if (_vspeed == value) return; _vspeed = value; computeSpeed1() }

    var speed: Float
        get() = _speed
        set(value) { if (_speed == value) return; _speed = value; computeSpeed2() }

    var direction: Float
        get() = _direction
        set(value) {
            var v = value
            while (0.0f > v) v += 360.0f
            while (v > 360.0f) v -= 360.0f
            _direction = v % 360.0f   // note: set unconditionally, no early-out
            computeSpeed2()
        }

    var friction = 0.0f
    var gravity = 0.0f
    var gravityDirection = 270.0f
    var persistent = false
    var solid = false
    var alarms = Array(StarfaitRunner.MAX_ALARMS) { -1 }
    var visible = true
    var destroyed = false
        private set

    private var _hspeed = 0.0f
    private var _vspeed = 0.0f
    // "Note that directions in GameMaker are usually calculated as 0° being right, 90° being up, 180° being left and 270° being down"
    private var _direction = 0.0f
    private var _speed = 0.0f

    val gameObject: GameObject
        get() = runner.gameWAD.objt.objects[this.objectIndex]
    val objectName: String
        get() = runner.gameWAD.strg.getString(gameObject.name)

    fun adaptSpeed() {
        if (this.friction != 0.0f) {
            val currentSpeed = this.speed
            if (currentSpeed > 0.0f)
                this.speed = max(0.0f, currentSpeed - this.friction)
            else if (0.0f > currentSpeed)
                this.speed = min(0.0f, currentSpeed + this.friction)
        }

        if (this.gravity != 0.0f) {
            val radians = Math.toRadians(this.gravityDirection.toDouble()).toFloat()
            this.hspeed += this.gravity * MathUtils.clampFloat(cos(radians).toDouble()).toFloat()
            this.vspeed -= this.gravity * MathUtils.clampFloat(sin(radians).toDouble()).toFloat()
        }
    }

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

    // From GameMaker-HTML5
    private fun computeSpeed1() {
        // direction
        if (this.hspeed == 0.0f)
        {
            if (this.vspeed > 0)
            {
                this._direction = 270.0f;
            }
            else if (this.vspeed < 0)
            {
                this._direction = 90.0f;
            }
            //else { this.__direction = 0; }
        }
        else
        {
            var dd = MathUtils.clampFloat(180 * (atan2(this.vspeed, this.hspeed)) / Math.PI).toFloat()
            if (dd <= 0) { this._direction = -dd; } else { this._direction = 360.0f - dd; }
        }

        if (Math.abs(this._direction - MathUtils.delphiRound(this._direction)) < 0.0001)
        {
            this._direction = MathUtils.delphiRound(this._direction).toFloat()
        }
        this._direction = this._direction % 360.0f

        // speed
        this._speed = sqrt(MathUtils.squared(this.hspeed) + MathUtils.squared(this.vspeed))
        if (Math.abs(this.speed - MathUtils.delphiRound(this.speed)) < 0.0001) this._speed = MathUtils.delphiRound(this._speed).toFloat()
    }

    private fun computeSpeed2() {
        this._hspeed = this.speed * cos(Math.toRadians(this.direction.toDouble()).toFloat())
        this._vspeed = -this.speed * sin(Math.toRadians(this.direction.toDouble()).toFloat())

        // Round a bit
        if (abs(this._hspeed - MathUtils.delphiRound(this._hspeed)) < 0.0001) { this._hspeed = MathUtils.delphiRound(this._hspeed).toFloat(); }
        if (abs(this._vspeed - MathUtils.delphiRound(this._vspeed)) < 0.0001) { this._vspeed = MathUtils.delphiRound(this._vspeed).toFloat(); }
    }
}