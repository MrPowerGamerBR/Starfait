@file:Suppress("FunctionName")
package com.mrpowergamerbr.starfait.vm.builtins

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.GMLFunction
import com.mrpowergamerbr.starfait.MathUtils
import com.mrpowergamerbr.starfait.ini.INI
import com.mrpowergamerbr.starfait.runner.BoundingBox
import com.mrpowergamerbr.starfait.runner.CollisionMaskQuery
import com.mrpowergamerbr.starfait.runner.DebugBoundingBox
import com.mrpowergamerbr.starfait.runner.EventType
import com.mrpowergamerbr.starfait.runner.OpenINI
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.vm.DSMap
import com.mrpowergamerbr.starfait.vm.InstanceReferenceType
import com.mrpowergamerbr.starfait.vm.RValue
import com.mrpowergamerbr.starfait.vm.StarfaitVM
import com.mrpowergamerbr.starfait.vm.VariableInstanceType
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

object BuiltinFunctions {
    @GMLFunction
    fun show_debug_message(runner: StarfaitRunner, string: String) {
        runner.output.showDebugMessage(string)
    }

    @GMLFunction fun string(value: String) = value
    @GMLFunction fun real(value: Double) = value
    @GMLFunction fun is_undefined(value: RValue) = value is RValue.Undefined

    @GMLFunction(stub = true) fun steam_initialised() = true
    @GMLFunction(stub = true) fun steam_file_exists(fileName: String) = false
    @GMLFunction(stub = true) fun file_exists(fileName: String) = false
    @GMLFunction(stub = true) fun os_get_language(vm: StarfaitVM) = "pt"
    @GMLFunction(stub = true) fun os_get_region(vm: StarfaitVM) = "BR"
    @GMLFunction(stub = true) fun string_height(string: String) = 0.0
    @GMLFunction(stub = true) fun surface_get_width(surfaceId: Int) = 640.0
    @GMLFunction(stub = true) fun surface_get_height(surfaceId: Int) = 480.0
    @GMLFunction(stub = true) fun window_get_fullscreen() = false
    @GMLFunction(stub = true) fun joystick_exists(joyId: Int) = false
    @GMLFunction(stub = true) fun audio_sound_get_gain(index: Int) = 0.0

    @GMLFunction fun chr(chr: Int) = chr.toChar().toString()
    @GMLFunction fun ord(string: String) = string[0].code

    @GMLFunction fun string_upper(string: String) = string.uppercase()
    @GMLFunction fun string_lower(string: String) = string.lowercase()
    @GMLFunction fun string_repeat(string: String, count: Int) = string.repeat(count)
    @GMLFunction fun string_replace_all(string: String, oldValue: String, newValue: String) = string.replace(oldValue, newValue)

    @GMLFunction
    fun string_width(runner: StarfaitRunner, text: String): Int {
        val font = requireNotNull(runner.drawState.font)

        // This is essentially how the text is rendered... but WITHOUT the rendering
        var width = 0
        for (char in text) {
            val glyph = font.characterToGlyph[char]

            if (glyph != null) {
                width += glyph.shift
            }
        }

        return width
    }

    @GMLFunction
    fun string_length(value: RValue): Int {
        val string = (value as? RValue.String)?.value ?: return 0
        return string.length
    }

    @GMLFunction
    fun string_copy(value: RValue, position: Int, length: Int): String {
        val string = (value as? RValue.String)?.value ?: return ""

        val index = (position - 1).coerceAtLeast(0) // "The position of the first character in the string to copy from (numbered from 1)"
        val count = length.coerceAtLeast(0)

        // When testing in GameMaker: Studio 1.4.9999, it seems that the string_copy copies TRASH if you are copying beyond the string length
        // We don't do that, because it doesn't make sense to do that nor can it be reliably reproduced
        if (index >= string.length || count == 0)
            return ""

        // For anything else, we'll just cap it at index + count
        val coercedEndIndex = (index + count).coerceAtMost(string.length)

        if (index == coercedEndIndex)
            return ""

        return string.substring(index, coercedEndIndex)
    }

    @GMLFunction
    fun string_delete(value: RValue, rawIndex: Int, count: Int): String {
        val string = (value as? RValue.String)?.value ?: return ""

        // "The position of the first character to remove (from 1). Negative indices are supported and count
        // from the end of the string, e.g. -1 indicates the last character."
        val index = if (0 > rawIndex) string.length + rawIndex else rawIndex - 1

        // "A negative number means characters are deleted backwards. For example, -4 indicates the character
        // at index and the 3 characters in front of it will be deleted."
        val begin: Int
        val end: Int
        if (count >= 0) {
            begin = index
            end = index + count
        } else {
            end = index + 1
            begin = index + 1 + count
        }

        val coercedBegin = begin.coerceIn(0, string.length)
        val coercedEnd = end.coerceIn(0, string.length)

        if (coercedBegin >= coercedEnd)
            return string

        return string.substring(0, coercedBegin) + string.substring(coercedEnd)
    }

    @GMLFunction
    fun string_char_at(string: String, position: Int): String {
        // "with the index starting at 1 for the first character"
        // "however if the given index is equal to or smaller than 0, then the first character of the string is returned."
        val index = position.coerceAtLeast(1) - 1

        // If no character is found or the string is shorter than the given index value, an empty string "" is returned,
        val char = string.getOrNull(index) ?: return ""

        return char.toString()
    }

    @GMLFunction
    fun string_pos(stringValue: RValue, substrValue: RValue): Int {
        val string = (stringValue as RValue.String).value
        val substr = (substrValue as RValue.String).value

        val index = string.indexOf(substr)

        // "The function will return 0 if it's not found at all, or the position of the first character of the sub-string if it is found.
        // Keep in mind that for legacy support strings are indexed from 1, so 1 is the first position in the string, not 0 as you may expect."
        return index + 1
    }

    @GMLFunction fun round(input: Double) = MathUtils.delphiRound(input).toDouble()
    @GMLFunction fun sin(input: Double) = kotlin.math.sin(input)
    @GMLFunction fun cos(input: Double) = kotlin.math.cos(input)
    @GMLFunction fun floor(input: Double) = kotlin.math.floor(input)
    @GMLFunction fun ceil(input: Double) = kotlin.math.ceil(input)
    @GMLFunction fun abs(input: Double) = input.absoluteValue
    @GMLFunction fun degtorad(input: Double) = Math.toRadians(input)
    @GMLFunction fun choose(runner: StarfaitRunner, args: List<RValue>) = args.random(runner.random)
    @GMLFunction fun random(runner: StarfaitRunner, n: Double) = runner.random.nextDouble() * n

    @GMLFunction
    fun irandom(runner: StarfaitRunner, bound: Int): Int {
        // If it is 0 it won't ever have any other random number :3
        if (bound == 0) {
            return 0
        } else if (bound > 0) {
            return runner.random.nextInt(bound + 1)
        } else {
            return -runner.random.nextInt(-bound + 1)
        }
    }

    @GMLFunction fun min(v1: Double, v2: Double) = kotlin.math.min(v1, v2)
    @GMLFunction fun max(v1: Double, v2: Double) = kotlin.math.max(v1, v2)
    @GMLFunction fun lengthdir_x(len: Double, dir: Double) = kotlin.math.cos(Math.toRadians(dir)) * len
    @GMLFunction fun lengthdir_y(len: Double, dir: Double) = -kotlin.math.sin(Math.toRadians(dir)) * len
    @GMLFunction fun power(x: Double, n: Double) = x.pow(n)

    @GMLFunction("make_colour_rgb") fun make_color_rgb(red: Int, green: Int, blue: Int) = GMLColor.fromRGB(red, green, blue).value
    @GMLFunction("make_colour_hsv") fun make_color_hsv(hue: Float, saturation: Float, value: Float) = GMLColor.fromHSV(hue, saturation, value).value

    @GMLFunction fun draw_self(runner: StarfaitRunner, vm: StarfaitVM) = runner.renderer.drawInstance(vm.currentSelfInstance)

    @GMLFunction
    fun draw_text(runner: StarfaitRunner, x: Double, y: Double, string: String) {
        val font = runner.drawState.font
        if (font == null) {
            println("Runner: Trying to draw without a valid font bound!")
            return
        }

        runner.renderer.drawText(font, x.toFloat(), y.toFloat(), 1f, 1f, 0f, string)
    }

    @GMLFunction
    fun draw_text_transformed(runner: StarfaitRunner, x: Double, y: Double, string: String, xscale: Double, yscale: Double, angle: Double) {
        val font = runner.drawState.font
        if (font == null) {
            println("Runner: Trying to draw without a valid font bound!")
            return
        }

        runner.renderer.drawText(font, x.toFloat(), y.toFloat(), xscale.toFloat(), yscale.toFloat(), angle.toFloat(), string)
    }


    @GMLFunction("draw_set_colour")
    fun draw_set_color(runner: StarfaitRunner, vm: StarfaitVM, color: Int) {
        runner.drawState.color = GMLColor(color)
    }

    @GMLFunction
    fun draw_set_alpha(runner: StarfaitRunner, vm: StarfaitVM, alpha: Float) {
        runner.drawState.alpha = alpha
    }

    @GMLFunction
    fun draw_rectangle(runner: StarfaitRunner, vm: StarfaitVM, x1: Float, y1: Float, x2: Float, y2: Float, outline: Boolean) {
        runner.renderer.fillRectangle(x1, y1, (x2 - x1) + 1f, (y2 - y1) + 1f, runner.drawState.color, runner.drawState.alpha, outline)
    }

    @GMLFunction
    fun action_move_to(runner: StarfaitRunner, vm: StarfaitVM, x: Double, y: Double) {
        var x = x
        var y = y

        if (vm.dragAndDropRelativeValues) {
            x += vm.currentSelfInstance.x
            y += vm.currentSelfInstance.y
        }

        vm.currentSelfInstance.x = x.toFloat()
        vm.currentSelfInstance.y = y.toFloat()
    }

    @GMLFunction
    fun action_set_hspeed(runner: StarfaitRunner, vm: StarfaitVM, value: Float) {
        val self = vm.currentSelfInstance
        self.hspeed = if (vm.dragAndDropRelativeValues) self.hspeed + value else value
    }

    @GMLFunction
    fun action_set_vspeed(runner: StarfaitRunner, vm: StarfaitVM, value: Float) {
        val self = vm.currentSelfInstance
        self.vspeed = if (vm.dragAndDropRelativeValues) self.vspeed + value else value
    }

    @GMLFunction
    fun action_set_friction(runner: StarfaitRunner, vm: StarfaitVM, value: Float) {
        val self = vm.currentSelfInstance
        self.friction = if (vm.dragAndDropRelativeValues) self.friction + value else value
    }

    @GMLFunction
    fun action_kill_object(runner: StarfaitRunner, vm: StarfaitVM) {
        vm.currentSelfInstance.markAsDestroyed(true)
    }

    @GMLFunction
    fun action_set_relative(runner: StarfaitRunner, vm: StarfaitVM, enabled: Boolean) {
        vm.dragAndDropRelativeValues = enabled
    }

    @GMLFunction
    fun action_create_object(runner: StarfaitRunner, vm: StarfaitVM, objectId: Int, x: Float, y: Float) {
        var x = x
        var y = y

        if (vm.dragAndDropRelativeValues) {
            x += vm.currentSelfInstance.x
            y += vm.currentSelfInstance.y
        }

        val instance = runner.currentRoom.createAndSpawnInstance(objectId, runner.allocateNewRuntimeInstanceId()) {
            this.x = x
            this.y = y
        }
        instance.dispatchEvent(EventType.CREATE, 0, null)
    }

    @GMLFunction
    fun action_move(runner: StarfaitRunner, vm: StarfaitVM, possibleDirections: String, speed: Float) {
        val enabledIndices = mutableListOf<Int>()

        repeat(9) {
            if (possibleDirections[it] == '1')
                enabledIndices.add(it)
        }

        val whereAreWeGoing = enabledIndices.random(runner.random)

        val direction = when (whereAreWeGoing) {
            0 -> 225
            1 -> 270
            2 -> 315
            3 -> 180
            4 -> null
            5 -> 0
            6 -> 135
            7 -> 90
            8 -> 45
            else -> error("I don't know how to handle $whereAreWeGoing!")
        }

        if (direction != null) {
            vm.currentSelfInstance.direction = direction.toFloat()
            vm.currentSelfInstance.speed = speed
        } else {
            vm.currentSelfInstance.speed = 0.0f
        }
    }

    @GMLFunction
    fun action_set_alarm(runner: StarfaitRunner, vm: StarfaitVM, steps: Int, alarm: Int) {
        var steps = steps

        if (vm.dragAndDropRelativeValues && vm.currentSelfInstance.isAlarmActive(alarm))
            steps += vm.currentSelfInstance.alarms[alarm]

        vm.currentSelfInstance.setAlarm(alarm, steps)
    }

    @GMLFunction
    fun move_snap(runner: StarfaitRunner, vm: StarfaitVM, gridWidth: Double, gridHeight: Double) {
        val currentInstance = vm.currentSelfInstance
        val gridCellX = MathUtils.delphiRound(currentInstance.x / gridWidth)
        val gridCellY = MathUtils.delphiRound(currentInstance.y / gridHeight)

        currentInstance.x = (gridCellX * gridWidth).toFloat()
        currentInstance.y = (gridCellY * gridHeight).toFloat()
    }

    fun roomGotoRelative(runner: StarfaitRunner, vm: StarfaitVM, offset: Int) {
        val newRoomIndexInRoomOrder = vm.gameWAD.gen8.roomOrder.indexOf(runner.currentRoom.roomId) + offset
        val newRoomIndex = vm.gameWAD.gen8.roomOrder[newRoomIndexInRoomOrder]
        runner.setPendingRoom(newRoomIndex)
    }

    @GMLFunction fun room_goto_next(runner: StarfaitRunner, vm: StarfaitVM) = roomGotoRelative(runner, vm, 1)
    @GMLFunction fun room_goto_previous(runner: StarfaitRunner, vm: StarfaitVM) = roomGotoRelative(runner, vm, -1)
    @GMLFunction fun room_goto(runner: StarfaitRunner, vm: StarfaitVM, roomIndex: Int) = runner.setPendingRoom(roomIndex)

    fun roomGetRelative(runner: StarfaitRunner, vm: StarfaitVM, roomIndex: Int, offset: Int): Int {
        val positionInRoomOrder = vm.gameWAD.gen8.roomOrder.indexOf(roomIndex)
        val newRoomIndex = vm.gameWAD.gen8.roomOrder[positionInRoomOrder + offset]
        return newRoomIndex
    }

    @GMLFunction fun room_next(runner: StarfaitRunner, vm: StarfaitVM, roomIndex: Int) = roomGetRelative(runner, vm, roomIndex, 1)
    @GMLFunction fun room_previous(runner: StarfaitRunner, vm: StarfaitVM, roomIndex: Int) = roomGetRelative(runner, vm, roomIndex, -1)

    @GMLFunction
    fun room_get_name(runner: StarfaitRunner, vm: StarfaitVM, roomIndex: Int): String {
        return runner.gameWAD.strg.getString(runner.gameWAD.room.rooms[roomIndex].name)
    }

    @GMLFunction
    fun tile_layer_shift(runner: StarfaitRunner, vm: StarfaitVM, depth: Int, x: Int, y: Int) {
        for (tile in runner.currentRoom.tiles) {
            if (tile.depth == depth) {
                tile.x += x
                tile.y += y
            }
        }
    }

    fun toggleTileLayerVisibility(runner: StarfaitRunner, depth: Int, visible: Boolean) {
        for (tile in runner.currentRoom.tiles) {
            if (tile.depth == depth) {
                tile.visible = visible
            }
        }
    }

    @GMLFunction fun tile_layer_show(runner: StarfaitRunner, depth: Int) = toggleTileLayerVisibility(runner, depth, true)
    @GMLFunction fun tile_layer_hide(runner: StarfaitRunner, depth: Int) = toggleTileLayerVisibility(runner, depth, false)
    
    @GMLFunction
    fun game_restart(runner: StarfaitRunner, vm: StarfaitVM) {
        runner.pendingRestart = true
    }

    @GMLFunction
    fun ini_open(runner: StarfaitRunner, fileName: String) {
        val content = runner.fileSystem.readFile(fileName)?.toString(Charsets.UTF_8) ?: ""
        runner.openINI = OpenINI(fileName, INI.parse(content))
    }

    @GMLFunction
    fun ini_close(runner: StarfaitRunner) {
        val openINI = runner.openINI ?: return

        runner.fileSystem.writeFile(openINI.path, openINI.ini.writeToString().toByteArray(Charsets.UTF_8))
        runner.openINI = null
    }

    @GMLFunction fun ini_section_exists(runner: StarfaitRunner, section: String) = runner.openINI?.ini?.sections?.containsKey(section) == true

    @GMLFunction fun ini_read_string(runner: StarfaitRunner, section: String, key: String, defaultValue: String) = runner.openINI?.ini?.readString(section, key) ?: defaultValue
    @GMLFunction fun ini_read_real(runner: StarfaitRunner, section: String, key: String, defaultValue: Double) = runner.openINI?.ini?.readReal(section, key) ?: defaultValue

    @GMLFunction fun ini_write_string(runner: StarfaitRunner, section: String, key: String, value: String) = runner.openINI?.ini?.writeString(section, key, value)
    @GMLFunction fun ini_write_real(runner: StarfaitRunner, section: String, key: String, value: Double) = runner.openINI?.ini?.writeReal(section, key, value)

    @GMLFunction
    fun instance_create(runner: StarfaitRunner, vm: StarfaitVM, x: Double, y: Double, objectIndex: Int): Int {
        val instance = runner.currentRoom.createAndSpawnInstance(objectIndex, runner.allocateNewRuntimeInstanceId()) {
            this.x = x.toFloat()
            this.y = y.toFloat()
        }
        instance.dispatchEvent(EventType.CREATE, 0, null)

        return instance.id
    }

    @GMLFunction
    fun instance_destroy(vm: StarfaitVM) {
        // TODO: instance ID
        // TODO: execute_event_flag
        vm.currentSelfInstance.markAsDestroyed(true)
    }

    @GMLFunction fun keyboard_check(runner: StarfaitRunner, keyId: Int) = runner.keyboard.keyHeld[keyId]
    @GMLFunction fun keyboard_check_pressed(runner: StarfaitRunner, keyId: Int) = runner.keyboard.keyPressed[keyId]
    @GMLFunction fun keyboard_check_released(runner: StarfaitRunner, keyId: Int) = runner.keyboard.keyReleased[keyId]
    @GMLFunction fun keyboard_check_direct(runner: StarfaitRunner, keyId: Int) = runner.keyboard.keyHeld[keyId]

    @GMLFunction
    fun script_execute(vm: StarfaitVM, args: List<RValue>): RValue {
        val callFrame = vm.currentCallFrame
        val scriptId = args[0].convertToInt32()
        val script = vm.gameWAD.scpt.scripts[scriptId]
        val code = vm.gameWAD.code.codes[script.codeIndex]
        val arguments = args.drop(1)
        val scriptResult = vm.executeCode(code, vm.currentSelfInstance, callFrame.other, callFrame.eventObject, callFrame.eventType, callFrame.eventSubtype, arguments.toTypedArray())
        if (scriptResult != null) {
            return scriptResult
        } else {
            return RValue.Undefined
        }
    }

    @GMLFunction
    fun event_inherited(vm: StarfaitVM) {
        // event_inherited is like a script_execute, but for parent events
        val callFrame = vm.currentCallFrame
        val currentInstance = callFrame.self

        val parentId = callFrame.eventObject.parentId
        var gameObject = vm.gameWAD.objt.objects[parentId]

        currentInstance.dispatchEvent(parentId, gameObject, callFrame.eventType, callFrame.eventSubtype, callFrame.other)
    }

    @GMLFunction
    fun event_user(vm: StarfaitVM, number: Int) {
        require(number in 0..15) { "User Event is out of bounds!" }
        // event_user is like event_inherited, but for user events
        val currentInstance = vm.currentSelfInstance
        currentInstance.dispatchEvent(EventType.OTHER, EventType.OtherSubType.USER_0 + number, null)
    }

    @GMLFunction
    fun event_perform(vm: StarfaitVM, eventType: Int, eventSubtype: Int) {
        // event_perform is like event_user, but for all events
        val currentInstance = vm.currentSelfInstance
        currentInstance.dispatchEvent(eventType, eventSubtype, null)
    }

    @GMLFunction
    fun audio_play_sound(runner: StarfaitRunner, index: Int, priority: Int, loop: Boolean): Int {
        val sound = runner.gameWAD.sond.sounds[index]
        return runner.audioManager.playSound(index, sound, loop)
    }

    @GMLFunction fun audio_stop_all(runner: StarfaitRunner) = runner.audioManager.stopAllSounds()
    @GMLFunction fun audio_stop_sound(runner: StarfaitRunner, soundIndexOrInstanceId: Int) = runner.audioManager.stopSound(soundIndexOrInstanceId)
    @GMLFunction fun audio_is_playing(runner: StarfaitRunner, soundIndexOrInstanceId: Int) = runner.audioManager.isAudioPlaying(soundIndexOrInstanceId)
    @GMLFunction fun audio_pause_sound(runner: StarfaitRunner, soundIndexOrInstanceId: Int) = runner.audioManager.pauseSound(soundIndexOrInstanceId)
    @GMLFunction fun audio_resume_sound(runner: StarfaitRunner, soundIndexOrInstanceId: Int) = runner.audioManager.resumeSound(soundIndexOrInstanceId)
    @GMLFunction fun audio_sound_pitch(runner: StarfaitRunner, soundIndexOrInstanceId: Int, pitch: Float) = runner.audioManager.setAudioPitch(soundIndexOrInstanceId, pitch)
    @GMLFunction fun audio_sound_gain(runner: StarfaitRunner, soundIndexOrInstanceId: Int, pitch: Float, timeMs: Int?) = runner.audioManager.setAudioGain(soundIndexOrInstanceId, pitch, timeMs ?: 0)

    @GMLFunction
    fun variable_global_exists(vm: StarfaitVM, variableName: String): Boolean {
        // TODO: This is kinda nasty, I think it would be better to have a proper variable name -> varId lookup table
        val varId = vm.getVariableSlotFromVariableName(VariableInstanceType.GLOBAL, variableName)
        println("varId: $varId")
        return vm.global.variables.containsKey(varId)
    }

    @GMLFunction
    fun ds_map_create(vm: StarfaitVM): Int {
        val newIndex = vm.dsMapPool.size
        vm.dsMapPool.add(DSMap())
        return newIndex
    }

    @GMLFunction
    fun ds_map_add(vm: StarfaitVM, id: Int, key: RValue, value: RValue): Boolean {
        val dsMap = vm.dsMapPool[id]
        if (dsMap.containsKey(key))
            return false

        dsMap.set(key, value)
        return true
    }

    @GMLFunction
    fun ds_map_set(vm: StarfaitVM, id: Int, key: RValue, value: RValue) {
        val dsMap = vm.dsMapPool[id]
        dsMap.set(key, value)
    }

    @GMLFunction
    fun ds_map_find_value(vm: StarfaitVM, id: Int, key: RValue): RValue {
        val dsMap = vm.dsMapPool[id]
        val value = dsMap.get(key)
        if (value == null)
            return RValue.Undefined

        return value
    }

    @GMLFunction
    fun instance_exists(runner: StarfaitRunner, queryId: Int): Boolean {
        // "This function does not take into account those instances that have been deactivated using the instance deactivate functions."
        runner.forEachInstance { instance ->
            val matches = instance.matchesObjectOrInstanceId(
                queryId,
                excludeInstance = null,
                skipDestroyed = true,
                checkHierarchy = true,
                checkSpecialSelf = true,
                checkSpecialOther = true,
                checkSpecialAll = true,
                checkSpecialNoone = true
            )

            if (matches)
                return true
        }

        return false
    }

    @GMLFunction
    fun instance_number(runner: StarfaitRunner, queryId: Int): Int {
        var count = 0
        // "This function does not take into account those instances that have been deactivated using the instance deactivate functions."
        runner.forEachInstance { instance ->
            val matches = instance.matchesObjectOrInstanceId(
                queryId,
                excludeInstance = null,
                skipDestroyed = true,
                checkHierarchy = true,
                checkSpecialSelf = true,
                checkSpecialOther = true,
                checkSpecialAll = true,
                checkSpecialNoone = true
            )

            if (matches)
                count++
        }

        return count
    }

    @GMLFunction
    fun instance_find(runner: StarfaitRunner, queryId: Int, targetIndex: Int): Int {
        var index = 0
        // TODO: Does this need to check for deactivated instances?
        runner.forEachInstance { instance ->
            val matches = instance.matchesObjectOrInstanceId(
                queryId,
                excludeInstance = null,
                skipDestroyed = true,
                checkHierarchy = true,
                checkSpecialSelf = true,
                checkSpecialOther = true,
                checkSpecialAll = true,
                checkSpecialNoone = true
            )

            if (matches) {
                if (index == targetIndex)
                    return instance.id

                index++
            }
        }

        return InstanceReferenceType.NOONE
    }

    @GMLFunction
    fun draw_sprite(runner: StarfaitRunner, vm: StarfaitVM, spriteIndex: Int, subimg: Int, x: Float, y: Float) {
        runner.renderer.drawSprite(vm.gameWAD.sprt.sprites[spriteIndex], subimg, x, y, 1f, 1f, 0f, GMLColor(0xFFFFFF), runner.drawState.alpha)
    }

    @GMLFunction
    fun draw_sprite_ext(runner: StarfaitRunner, vm: StarfaitVM, spriteIndex: Int, subimg: Int, x: Float, y: Float, xscale: Float, yscale: Float, rot: Float, color: Int, alpha: Float) {
        runner.renderer.drawSprite(
            vm.gameWAD.sprt.sprites[spriteIndex],
            subimg,
            x,
            y,
            xscale,
            yscale,
            rot,
            GMLColor(color),
            alpha,
        )
    }

    @GMLFunction
    fun collision_rectangle(runner: StarfaitRunner, vm: StarfaitVM, x1: Double, y1: Double, x2: Double, y2: Double, obj: Int, usePreciseCollisionCheck: Boolean, notme: Boolean): Int {
        val self = vm.currentSelfInstance

        val selfBBox = BoundingBox.fromWorldCoordinates(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())

        if (runner.enableCollisionRendering)
            runner.debugRectangles.add(DebugBoundingBox(selfBBox, GMLColor.fromRGB(255, 0, 0)))

        runner.forEachInstance { instance ->
            val matches = instance.matchesObjectOrInstanceId(
                obj,
                excludeInstance = if (notme) self else null,
                skipDestroyed = true,
                checkHierarchy = true,
                checkSpecialSelf = true,
                checkSpecialOther = true,
                checkSpecialAll = true,
                checkSpecialNoone = true
            )

            if (matches) {
                val bbox = instance.getBoundingBox()

                if (selfBBox.isCollidingWith(bbox, runner.collisionCompatibilityMode)) {
                    if (usePreciseCollisionCheck) {
                        val collisionSprite = instance.getCollisionSprite()

                        if (collisionSprite != null && CollisionMaskQuery.checkPreciseCollisionInstanceRectangle(instance, bbox, collisionSprite, selfBBox, runner.collisionCompatibilityMode)) {
                            return instance.id
                        }
                    } else {
                        return instance.id
                    }
                }
            }
        }

        return InstanceReferenceType.NOONE
    }

    @GMLFunction
    fun collision_point(runner: StarfaitRunner, vm: StarfaitVM, x: Float, y: Float, obj: Int, usePreciseCollisionCheck: Boolean, notme: Boolean): Int {
        val self = vm.currentSelfInstance

        runner.forEachInstance { instance ->
            val matches = instance.matchesObjectOrInstanceId(
                obj,
                excludeInstance = if (notme) self else null,
                skipDestroyed = true,
                checkHierarchy = true,
                checkSpecialSelf = true,
                checkSpecialOther = true,
                checkSpecialAll = true,
                checkSpecialNoone = true
            )

            if (matches) {
                val bbox = instance.getBoundingBox()

                if (bbox.contains(x, y)) {
                    if (usePreciseCollisionCheck) {
                        val collisionSprite = instance.getCollisionSprite()

                        if (collisionSprite != null && CollisionMaskQuery.checkPreciseCollisionInstancePoint(instance, collisionSprite, x, y, runner.collisionCompatibilityMode)) {
                            return instance.id
                        }
                    } else {
                        return instance.id
                    }
                }
            }
        }

        return InstanceReferenceType.NOONE
    }

    @GMLFunction
    fun instance_position(runner: StarfaitRunner, x: Float, y: Float, obj: Int): Int {
        runner.forEachInstance { instance ->
            val matches = instance.matchesObjectOrInstanceId(
                obj,
                excludeInstance = null,
                skipDestroyed = true,
                checkHierarchy = true,
                checkSpecialSelf = true,
                checkSpecialOther = true,
                checkSpecialAll = true,
                checkSpecialNoone = true
            )

            if (matches) {
                if (instance.getBoundingBox().contains(x, y)) {
                    val collisionSprite = instance.getCollisionSprite()

                    if (collisionSprite?.precise == true) {
                        if (CollisionMaskQuery.checkPreciseCollisionInstancePoint(instance, collisionSprite, x, y, runner.collisionCompatibilityMode)) {
                            return instance.id
                        }
                    } else {
                        return instance.id
                    }
                }
            }
        }

        return InstanceReferenceType.NOONE
    }

    @GMLFunction
    fun collision_line(runner: StarfaitRunner, vm: StarfaitVM, x1: Float, y1: Float, x2: Float, y2: Float, obj: Int, usePreciseCollisionCheck: Boolean, notme: Boolean): Int {
        val self = vm.currentSelfInstance

        runner.forEachInstance { instance ->
            val matches = instance.matchesObjectOrInstanceId(
                obj,
                excludeInstance = if (notme) self else null,
                skipDestroyed = true,
                checkHierarchy = true,
                checkSpecialSelf = true,
                checkSpecialOther = true,
                checkSpecialAll = true,
                checkSpecialNoone = true
            )

            if (matches) {
                if (instance.getBoundingBox().intersectsLine(x1, y1, x2, y2)) {
                    return instance.id
                }
            }
        }

        return InstanceReferenceType.NOONE
    }

    @GMLFunction
    fun draw_set_font(runner: StarfaitRunner, fontIndex: Int) {
        val font = runner.gameWAD.font.fonts.getOrNull(fontIndex)
        if (font == null)
            println("Runner: Tried setting invalid font with draw_set_font!")
        runner.drawState.font = font
    }

    // See GameMaker-HTML5's Function_Path.js
    @GMLFunction
    fun path_start(vm: StarfaitVM, path: Int, speed: Float, endAction: Int, absolute: Boolean) {
        vm.currentSelfInstance.assignPath(path, speed, 1.0f, 0.0f, absolute, endAction)
    }

    @GMLFunction
    fun path_end(vm: StarfaitVM) {
        vm.currentSelfInstance.assignPath(-1, 0.0f, 1.0f, 0.0f, false, 0)
    }

    @GMLFunction
    fun move_towards_point(runner: StarfaitRunner, vm: StarfaitVM, x: Double, y: Double, sp: Double) {
        val dx = x - vm.currentSelfInstance.x
        val dy = y - vm.currentSelfInstance.y
        val angle = Math.toDegrees(atan2(-dy, dx))

        val direction = angle.toFloat() % 360
        vm.currentSelfInstance.direction = direction
        vm.currentSelfInstance.speed = sp.toFloat()

        runner.debugRectangles.add(
            DebugBoundingBox(
                BoundingBox.fromWorldCoordinates(
                    x.toFloat() - 4,
                    y.toFloat() - 4,
                    x.toFloat() + 4,
                    y.toFloat() + 4
                ),
                GMLColor.fromRGB(0, 0, 255)
            )
        )
    }

    @GMLFunction
    fun distance_to_point(runner: StarfaitRunner, vm: StarfaitVM, targetX: Float, targetY: Float): Double {
        val self = vm.currentCallFrame.self

        val bbox = self.getBoundingBox()

        runner.debugRectangles.add(
            DebugBoundingBox(
                BoundingBox.fromWorldCoordinates(
                    targetX - 4,
                    targetY - 4,
                    targetX + 4,
                    targetY + 4
                ),
                GMLColor.fromRGB(20, 200, 255)
            )
        )
        return bbox.distanceTowardsPoint(targetX, targetY).toDouble()
    }

    // See GameMaker-HTML5's Function_Movement.js and Instance_SearchLoop2 (Globals.js)
    @GMLFunction
    fun distance_to_object(runner: StarfaitRunner, vm: StarfaitVM, queryId: Int): Double {
        // Instance_SearchLoop2 returns this "error code" when nothing matches
        var distanceSquared = 10000000000.0

        val selfBBox = vm.currentSelfInstance.getBoundingBox()

        runner.forEachInstance { instance ->
            val matches = instance.matchesObjectOrInstanceId(
                queryId,
                excludeInstance = null,
                skipDestroyed = true,
                checkHierarchy = true,
                checkSpecialSelf = true,
                checkSpecialOther = true,
                checkSpecialAll = true,
                checkSpecialNoone = true
            )

            if (matches) {
                val otherBBox = instance.getBoundingBox()
                val d = selfBBox.distanceTowardsBoundingBoxSquared(otherBBox)

                if (distanceSquared > d)
                    distanceSquared = d
            }
        }

        return sqrt(distanceSquared)
    }

    @GMLFunction fun point_direction(x1: Double, y1: Double, x2: Double, y2: Double) = Math.toDegrees(atan2(-(y2 - y1), x2 - x1))

    @GMLFunction
    fun object_get_name(vm: StarfaitVM, objectIndex: Int): RValue {
        // When testing in GameMaker: Studio 1.4.9999 it seems that it DOES NOT throw on a invalid object, it just returns undefined
        val gameObject = vm.gameWAD.objt.objects.getOrNull(objectIndex) ?: return RValue.Undefined
        return RValue.String(vm.gameWAD.strg.getString(gameObject.name))
    }

    @GMLFunction
    fun draw_sprite_part(runner: StarfaitRunner, spriteIndex: Int, subimg: Int, left: Float, top: Float, width: Float, height: Float, x: Float, y: Float) {
        val sprite = runner.gameWAD.sprt.sprites[spriteIndex]

        runner.renderer.drawSpritePart(sprite, subimg, left, top, width, height, x, y, 1f, 1f, GMLColor(0xFFFFFF), runner.drawState.alpha)
    }

    @GMLFunction
    fun draw_sprite_part_ext(runner: StarfaitRunner, spriteIndex: Int, subimg: Int, left: Float, top: Float, width: Float, height: Float, x: Float, y: Float, xscale: Float, yscale: Float, color: Int, alpha: Float) {
        val sprite = runner.gameWAD.sprt.sprites[spriteIndex]

        runner.renderer.drawSpritePart(sprite, subimg, left, top, width, height, x, y, xscale, yscale, GMLColor(color), alpha)
    }
}