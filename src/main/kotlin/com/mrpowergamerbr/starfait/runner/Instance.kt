package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.EventKey
import com.mrpowergamerbr.starfait.MathUtils
import com.mrpowergamerbr.starfait.StarfaitUtils
import com.mrpowergamerbr.starfait.statedumper.DumpRunnerField
import com.mrpowergamerbr.starfait.vm.InstanceReferenceType
import com.mrpowergamerbr.starfait.wad.GameObject
import kotlin.collections.filter
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class Instance(val runner: StarfaitRunner, @DumpRunnerField var id: Int, @DumpRunnerField var objectIndex: Int) : ObjectBase(runner.vm) {
    @DumpRunnerField var x = 0.0f
    @DumpRunnerField var y = 0.0f
    @DumpRunnerField var xPrevious = 0.0f
    @DumpRunnerField var yPrevious = 0.0f
    @DumpRunnerField var xStart = 0.0f
    @DumpRunnerField var yStart = 0.0f
    @DumpRunnerField var imageSpeed = 1.0f
    @DumpRunnerField var imageXScale = 1.0f
    @DumpRunnerField var imageYScale = 1.0f
    @DumpRunnerField var imageAlpha = 1.0f
    @DumpRunnerField var imageIndex = 0.0f
    @DumpRunnerField var spriteIndex = 0.0f
    @DumpRunnerField var maskIndex = -1.0f
    @DumpRunnerField var depth = 0.0f
    @DumpRunnerField var imageBlend = -1.0f
    @DumpRunnerField var imageAngle = 0.0f
    @DumpRunnerField var pathSpeed = 0.0f
    @DumpRunnerField var pathPosition = 0.0f

    @DumpRunnerField var hspeed: Float
        get() = _hspeed
        set(value) { if (_hspeed == value) return; _hspeed = value; computeSpeed1() }

    @DumpRunnerField var vspeed: Float
        get() = _vspeed
        set(value) { if (_vspeed == value) return; _vspeed = value; computeSpeed1() }

    @DumpRunnerField var speed: Float
        get() = _speed
        set(value) { if (_speed == value) return; _speed = value; computeSpeed2() }

    @DumpRunnerField var direction: Float
        get() = _direction
        set(value) {
            var v = value
            while (0.0f > v) v += 360.0f
            while (v > 360.0f) v -= 360.0f
            _direction = v % 360.0f   // note: set unconditionally, no early-out
            computeSpeed2()
        }

    @DumpRunnerField var friction = 0.0f
    @DumpRunnerField var gravity = 0.0f
    @DumpRunnerField var gravityDirection = 270.0f
    @DumpRunnerField var persistent = false
    @DumpRunnerField var solid = false
    @DumpRunnerField var alarms = Array(StarfaitRunner.MAX_ALARMS) { -1 }
    @DumpRunnerField var visible = true
    @DumpRunnerField var destroyed = false
        private set

    /**
     * Whether the instance was outside the room on the last check, the outside room event only fires when the instance *leaves* the room
     */
    @DumpRunnerField var outsideRoom = false

    private var _hspeed = 0.0f
    private var _vspeed = 0.0f
    // "Note that directions in GameMaker are usually calculated as 0° being right, 90° being up, 180° being left and 270° being down"
    private var _direction = 0.0f
    private var _speed = 0.0f

    val gameObject: GameObject
        get() = runner.gameWAD.objt.objects[this.objectIndex]
    @DumpRunnerField val objectName: String
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
        return dispatchEvent(this.objectIndex, this.gameObject, eventType, eventSubtype, other)
    }

    /**
     * Dispatches an event to this instance
     *
     * @return true if the instance had an event listener for it
     */
    fun dispatchEvent(gameObjectIndex: Int, gameObject: GameObject, eventType: Int, eventSubtype: Int, other: Instance?): Boolean {
        val subtypeEvents = if (eventType == EventType.COLLISION) {
            val allParentsOfCollidedObject = mutableListOf<Int>()

            var targetGameObjectId = eventSubtype
            while (true) {
                if (0 > targetGameObjectId)
                    break

                allParentsOfCollidedObject.add(targetGameObjectId)
                val targetGameObject = vm.gameWAD.objt.objects[targetGameObjectId]
                targetGameObjectId = targetGameObject.parentId
            }

            var subtypeEvents = listOf<GameObject.Action>()

            // Get the first matched event of the collided object, including all parents
            for (id in allParentsOfCollidedObject) {
                subtypeEvents = runner.flattenedEvents[EventKey(gameObjectIndex, eventType, id)] ?: emptyList()
            }

            subtypeEvents
        } else {
            runner.flattenedEvents[EventKey(gameObjectIndex, eventType, eventSubtype)]
        }

        if (subtypeEvents.isNullOrEmpty())
            return false

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
        return true
    }

    fun markAsDestroyed(fireDestroyEvent: Boolean) {
        // If it is destroyed, we must ensure that we are NOT going to dispatch any of the destroy events
        if (this.destroyed)
            return

        this.destroyed = true

        if (fireDestroyEvent)
            dispatchEvent(EventType.DESTROY, 0, null)
    }

    // See GameMaker-HTML5's Compute_BoundingBox
    fun getBoundingBox(): BoundingBox {
        // bbox_*: "Please note that when the instance has no sprite assigned the value returned will be the same as the instance Y position."
        val maskIndex = (if (this.maskIndex >= 0.0f) this.maskIndex else this.spriteIndex).toInt()

        if (0 > maskIndex) {
            return BoundingBox.fromWorldCoordinates(
                this.x,
                this.y,
                this.x,
                this.y
            )
        }

        val sprite = runner.gameWAD.sprt.sprites[maskIndex]

        val scaledBBoxLeftAndOffset = (sprite.bboxLeft - sprite.originX) * this.imageXScale
        val scaledBBoxRightAndOffset = (sprite.bboxRight - sprite.originX + if (runner.collisionCompatibilityMode) 1 else 0) * this.imageXScale
        val scaledBBoxTopAndOffset = (sprite.bboxTop - sprite.originY) * this.imageYScale
        val scaledBBoxBottomAndOffset = (sprite.bboxBottom - sprite.originY + if (runner.collisionCompatibilityMode) 1 else 0) * this.imageYScale

        val bboxLeftAbsolute = this.x + scaledBBoxLeftAndOffset
        val bboxRightAbsolute = this.x + scaledBBoxRightAndOffset
        val bboxTopAbsolute = this.y + scaledBBoxTopAndOffset
        val bboxBottomAbsolute = this.y + scaledBBoxBottomAndOffset

        return BoundingBox.fromWorldCoordinates(
            bboxLeftAbsolute,
            bboxTopAbsolute,
            bboxRightAbsolute,
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
                else -> {
                    // THANKS TOBY FOX FOR SETTING SOME ABSURD INT64 VALUE!!
                    println("Runner: Tried querying for unsupported InstanceReferenceType ID $queryId! This may be a bug, or it just may be a script doing something weird")
                    false
                }
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