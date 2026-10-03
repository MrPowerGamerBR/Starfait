package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.runner.EventKey
import com.mrpowergamerbr.starfait.MathUtils
import com.mrpowergamerbr.starfait.statedumper.DumpRunnerField
import com.mrpowergamerbr.starfait.vm.InstanceReferenceType
import com.mrpowergamerbr.starfait.wad.GameObject
import com.mrpowergamerbr.starfait.wad.Sprite
import kotlin.math.*

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
    @DumpRunnerField var pathIndex = -1
    @DumpRunnerField var pathPosition = 0.0f
    @DumpRunnerField var pathPositionPrevious = 0.0f
    @DumpRunnerField var pathSpeed = 0.0f
    @DumpRunnerField var pathScale = 1.0f
    @DumpRunnerField var pathOrientation = 0.0f
    @DumpRunnerField var pathEndAction = 0
    @DumpRunnerField var pathXStart = 0.0f
    @DumpRunnerField var pathYStart = 0.0f

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

    fun revertToPreviousPosition() {
        this.x = this.xPrevious
        this.y = this.yPrevious
        this.pathPosition = this.pathPositionPrevious
    }

    // See GameMaker-HTML5's yyInstance.js Assign_Path
    fun assignPath(ind: Int, speed: Float, scale: Float, orient: Float, absolute: Boolean, endact: Int) {
        // First validate the path exists, and assign the index.
        this.pathIndex = -1
        val pPath = runner.paths.getOrNull(ind) ?: return
        if (0.0f >= pPath.length) return
        if (0.0f > scale) return
        this.pathIndex = ind

        this.pathSpeed = speed
        this.pathPosition = if (this.pathSpeed >= 0.0f) 0.0f else 1.0f

        this.pathScale = scale

        this.pathOrientation = orient
        this.pathEndAction = endact
        if (absolute) {
            val pNode = pPath.getPosition(if (this.pathSpeed >= 0.0f) 0.0f else 1.0f)
            this.x = pNode.x
            this.y = pNode.y

            val pNode0 = pPath.getPosition(0.0f)
            this.pathXStart = pNode0.x
            this.pathYStart = pNode0.y
        } else {
            this.pathXStart = this.x
            this.pathYStart = this.y
        }
    }

    // See GameMaker-HTML5's yyInstance.js Adapt_Path, with the end actions of the bytecode 16 runner (libyoyo.so.c CInstance::Adapt_Path)
    fun adaptPath() {
        // check whether the path exists
        val pPath = runner.paths.getOrNull(this.pathIndex) ?: return
        if (0.0f >= pPath.length) return

        // get the new path position
        val orient = Math.toRadians(this.pathOrientation.toDouble()).toFloat()

        val sp = pPath.getPosition(this.pathPosition).speed / (100 * this.pathScale)
        this.pathPosition += this.pathSpeed * sp / pPath.length

        // handle end actions if required
        val pNode0 = pPath.getPosition(0.0f)
        if ((this.pathPosition >= 1.0f) || (0.0f >= this.pathPosition)) {
            when (this.pathEndAction) {
                // stop moving
                PathEndAction.STOP -> {
                    // Unlike GameMaker-HTML5, the native runner rewinds to the start when going backwards, which matters for Undertale's blt_handbullet
                    if (0.0f > this.pathSpeed) {
                        this.pathPosition = 0.0f
                        this.pathIndex = -1
                    } else if (this.pathSpeed != 0.0f) {
                        this.pathPosition = 1.0f
                        this.pathIndex = -1
                    }
                }

                // continue from start position
                PathEndAction.RESTART -> {
                    if (0.0f > this.pathPosition) {
                        this.pathPosition++
                    } else {
                        this.pathPosition--
                    }
                }

                // continue from current position
                PathEndAction.CONTINUE -> {
                    val pNode1 = pPath.getPosition(1.0f)
                    val xx = pNode1.x - pNode0.x
                    val yy = pNode1.y - pNode0.y
                    val xdif = this.pathScale * (xx * cos(orient) + yy * sin(orient))
                    val ydif = this.pathScale * (yy * cos(orient) - xx * sin(orient))

                    if (0.0f > this.pathPosition) {
                        this.pathXStart -= xdif
                        this.pathYStart -= ydif
                        this.pathPosition++
                    } else {
                        this.pathXStart += xdif
                        this.pathYStart += ydif
                        this.pathPosition--
                    }
                }

                // reverse
                PathEndAction.REVERSE -> TODO("path_action_reverse is not implemented")

                // stop moving
                else -> {
                    this.pathPosition = 1.0f
                    this.pathIndex = -1
                }
            }
        }

        // find the new position in the room
        val pNode = pPath.getPosition(this.pathPosition)
        val xx = pNode.x - pNode0.x // relative
        val yy = pNode.y - pNode0.y

        val newx = this.pathXStart + this.pathScale * (xx * cos(orient) + yy * sin(orient))
        val newy = this.pathYStart + this.pathScale * (yy * cos(orient) - xx * sin(orient))

        // trick to set the direction
        this.hspeed = newx - this.x
        this.vspeed = newy - this.y

        // normal speed should not be used
        this.speed = 0.0f

        // Set the new position
        this.x = newx
        this.y = newy
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

            var subtypeEvents = emptyList<GameObject.Action>()

            // Get the first matched event of the collided object, including all parents
            for (id in allParentsOfCollidedObject) {
                subtypeEvents = runner.flattenedEvents[EventKey(gameObjectIndex, eventType, id)] ?: emptyList()
                if (subtypeEvents.isNotEmpty())
                    break
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

        if (imageAngle == 0f) {
            val width = (sprite.bboxRight + 1) - sprite.bboxLeft
            val height = (sprite.bboxBottom + 1) - sprite.bboxTop

            var bboxLeft: Float
            var bboxRight: Float

            if (runner.collisionCompatibilityMode) {
                bboxLeft = MathUtils.delphiRound(this.x + this.imageXScale * (sprite.bboxLeft - sprite.originX)).toFloat()
                bboxRight = MathUtils.delphiRound(bboxLeft + (this.imageXScale * width)).toFloat()
            } else {
                bboxLeft = (this.x + this.imageXScale * (sprite.bboxLeft - sprite.originX))
                bboxRight = (bboxLeft + (this.imageXScale * width))
            }

            val _bboxLeft = bboxLeft
            val _bboxRight = bboxRight

            bboxLeft = min(_bboxLeft, _bboxRight)
            bboxRight = max(_bboxLeft, _bboxRight)

            var bboxTop: Float
            var bboxBottom: Float

            if (runner.collisionCompatibilityMode) {
                bboxTop = MathUtils.delphiRound(this.y + this.imageYScale * (sprite.bboxTop - sprite.originY)).toFloat()
                bboxBottom = MathUtils.delphiRound(bboxTop + (this.imageYScale * height)).toFloat()
            } else {
                bboxTop = (this.y + this.imageYScale * (sprite.bboxTop - sprite.originY))
                bboxBottom = (bboxTop + (this.imageYScale * height))
            }

            val _bboxTop = bboxTop
            val _bboxBottom = bboxBottom

            bboxTop = min(_bboxTop, _bboxBottom)
            bboxBottom = max(_bboxTop, _bboxBottom)

            if (runner.collisionCompatibilityMode) {
                bboxRight -= 1
                bboxBottom -= 1
            }

            return BoundingBox.fromWorldCoordinates(
                bboxLeft,
                bboxTop,
                bboxRight,
                bboxBottom
            )
        } else {
            // TODO: This looks ugly!!! But that's YoYo's fault because this code is straight lifted from GameMaker-HTML5
            //  Maybe make it more idiomatic later?
            // base on sprite bbox, and these can't change, so will always be in order, left<right and top<bottom
            val xmin = this.imageXScale * (sprite.bboxLeft - sprite.originX)
            val xmax = this.imageXScale * (sprite.bboxRight - sprite.originX + 1)

            val ymin = this.imageYScale * (sprite.bboxTop - sprite.originY)
            val ymax = this.imageYScale * (sprite.bboxBottom - sprite.originY + 1)

            val cc = cos(this.imageAngle * PI / 180.0)
            val ss = sin(this.imageAngle * PI / 180.0)

            // factor out "common" calculations...
            var cc_xmax = cc * xmax
            var cc_xmin = cc * xmin
            var ss_ymax = ss * ymax
            var ss_ymin = ss * ymin
            var t: Double
            if (cc_xmax < cc_xmin) {
                t = cc_xmin
                cc_xmin = cc_xmax
                cc_xmax = t
            }
            if (ss_ymax < ss_ymin) {
                t = ss_ymin
                ss_ymin = ss_ymax
                ss_ymax = t
            }

            val bboxLeft: Double
            val bboxRight: Double

            if (runner.collisionCompatibilityMode) {
                bboxLeft = floor((this.x + cc_xmin + ss_ymin) + 0.5)
                bboxRight = floor((this.x + cc_xmax + ss_ymax) - 0.5)
            } else {
                bboxLeft = (this.x + cc_xmin + ss_ymin)
                bboxRight = (this.x + cc_xmax + ss_ymax)
            }

            // factor out "common" calculations...
            var cc_ymax = cc * ymax
            var cc_ymin = cc * ymin
            var ss_xmax = ss * xmax
            var ss_xmin = ss * xmin
            if (cc_ymax < cc_ymin) {
                t = cc_ymin
                cc_ymin = cc_ymax
                cc_ymax = t
            }
            if (ss_xmax < ss_xmin) {
                t = ss_xmin
                ss_xmin = ss_xmax
                ss_xmax = t
            }

            var bboxTop: Double
            var bboxBottom: Double
            if (runner.collisionCompatibilityMode) {
                bboxTop = floor((this.y + cc_ymin - ss_xmax) + 0.5)
                bboxBottom = floor((this.y + cc_ymax - ss_xmin) - 0.5)
            } else {
                bboxTop = ((this.y + cc_ymin - ss_xmax))
                bboxBottom = ((this.y + cc_ymax - ss_xmin))
            }

            return BoundingBox.fromWorldCoordinates(
                bboxLeft.toFloat(),
                bboxTop.toFloat(),
                bboxRight.toFloat(),
                bboxBottom.toFloat()
            )
        }
    }

    // See GameMaker-HTML5's "Collision_Instance"
    fun isCollidingWith(other: Instance, precise: Boolean): Boolean {
        if (this == other || this.destroyed || other.destroyed)
            return false

        val bbox1 = other.getBoundingBox()
        val bbox2 = this.getBoundingBox()

        if (!bbox2.isCollidingWith(bbox1, runner.collisionCompatibilityMode))
            return false

        val sprite1 = this.collisionSprite() ?: return false
        val sprite2 = other.collisionSprite() ?: return false
        if (sprite1.frames.isEmpty() || sprite2.frames.isEmpty())
            return false

        if (!precise || (!sprite1.precise && !sprite2.precise)) {
            if (!runner.collisionCompatibilityMode) {
                val l = max(bbox1.left, bbox2.left)
                val t = max(bbox1.top, bbox2.top)
                val r = min(bbox1.right, bbox2.right)
                val b = min(bbox1.bottom, bbox2.bottom)

                if (floor(l + 0.5f) == floor(r + 0.5f))
                    return false

                if (floor(t + 0.5f) == floor(b + 0.5f))
                    return false
            }
            return true
        }

        if (!runner.collisionCompatibilityMode)
            TODO("Non-compatibility mode precise collisions")

        return origPreciseCollision(this, bbox2, sprite1, other, bbox1, sprite2)
    }

    private fun collisionSprite(): Sprite? {
        val index = (if (this.maskIndex >= 0.0f) this.maskIndex else this.spriteIndex).toInt()
        return runner.gameWAD.sprt.sprites.getOrNull(index)
    }

    // See GameMaker-HTML5's "OrigPreciseCollision"
    private fun origPreciseCollision(
        inst1: Instance,
        bb1: BoundingBox,
        sprite1: Sprite,
        inst2: Instance,
        bb2: BoundingBox,
        sprite2: Sprite
    ): Boolean {
        val img1 = Math.floorMod(inst1.imageIndex.toInt(), sprite1.collisionMasks.size.coerceAtLeast(1))
        val img2 = Math.floorMod(inst2.imageIndex.toInt(), sprite2.collisionMasks.size.coerceAtLeast(1))

        val x1 = MathUtils.delphiRound(inst1.x).toDouble()
        val y1 = MathUtils.delphiRound(inst1.y).toDouble()
        val x2 = MathUtils.delphiRound(inst2.x).toDouble()
        val y2 = MathUtils.delphiRound(inst2.y).toDouble()

        val angle1 = inst1.imageAngle.toDouble()
        val angle2 = inst2.imageAngle.toDouble()

        val scale1x = 1.0 / inst1.imageXScale
        val scale1y = 1.0 / inst1.imageYScale
        val scale2x = 1.0 / inst2.imageXScale
        val scale2y = 1.0 / inst2.imageYScale

        // Compute overlapping bounding box
        val l = max(bb1.left, bb2.left).toDouble()
        val r = min(bb1.right, bb2.right).toDouble()
        val t = max(bb1.top, bb2.top).toDouble()
        val b = min(bb1.bottom, bb2.bottom).toDouble()

        if (scale1x == 1.0 && scale2x == 1.0 && scale1y == 1.0 && scale2y == 1.0 && angle1 == 0.0 && angle2 == 0.0) {
            // No rotation or scaling
            var i = l
            while (r >= i) {
                var j = t
                while (b >= j) {
                    val xx1 = i - x1 + sprite1.originX
                    val yy1 = j - y1 + sprite1.originY
                    val xx2 = i - x2 + sprite2.originX
                    val yy2 = j - y2 + sprite2.originY

                    if (sprite1.containsMaskPixel(xx1, yy1, img1, false) && sprite2.containsMaskPixel(xx2, yy2, img2, false))
                        return true

                    j += 1.0
                }
                i += 1.0
            }
        } else if (angle1 == 0.0 && angle2 == 0.0) {
            // Scaling but no rotation
            var i = l
            while (r >= i) {
                var j = t
                while (b >= j) {
                    val xx1 = (i - x1) * scale1x + sprite1.originX
                    val yy1 = (j - y1) * scale1y + sprite1.originY
                    val xx2 = (i - x2) * scale2x + sprite2.originX
                    val yy2 = (j - y2) * scale2y + sprite2.originY

                    if (sprite1.containsMaskPixel(xx1, yy1, img1, true) && sprite2.containsMaskPixel(xx2, yy2, img2, true))
                        return true

                    j += 1.0
                }
                i += 1.0
            }
        } else {
            // "Do Everything" case - rotation AND scaling!
            val ss1 = sin(-angle1 * PI / 180.0)
            val cc1 = cos(-angle1 * PI / 180.0)
            val ss2 = sin(-angle2 * PI / 180.0)
            val cc2 = cos(-angle2 * PI / 180.0)

            var i = l
            while (r >= i) {
                var j = t
                while (b >= j) {
                    val xx1 = (cc1 * (i - x1) + ss1 * (j - y1)) * scale1x + sprite1.originX
                    val yy1 = (cc1 * (j - y1) - ss1 * (i - x1)) * scale1y + sprite1.originY
                    val xx2 = (cc2 * (i - x2) + ss2 * (j - y2)) * scale2x + sprite2.originX
                    val yy2 = (cc2 * (j - y2) - ss2 * (i - x2)) * scale2y + sprite2.originY

                    if (sprite1.containsMaskPixel(xx1, yy1, img1, true) && sprite2.containsMaskPixel(xx2, yy2, img2, true))
                        return true

                    j += 1.0
                }
                i += 1.0
            }
        }

        return false
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