package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.Event
import com.mrpowergamerbr.starfait.Subevent
import com.mrpowergamerbr.starfait.SubtypesOf

object EventType {
    @Event const val CREATE = 0
    @Event const val DESTROY = 1
    @Event const val ALARM = 2
    @Event const val STEP = 3
    @Event const val COLLISION = 4
    @Event const val KEYBOARD = 5
    @Event const val OTHER = 7
    @Event const val DRAW = 8
    @Event const val KEY_PRESS = 9
    @Event const val KEY_RELEASE = 10

    @SubtypesOf(STEP)
    object StepSubType {
        @Subevent const val NORMAL = 0
        @Subevent const val BEGIN = 1
        @Subevent const val END = 2
    }

    @SubtypesOf(OTHER)
    object OtherSubType {
        @Subevent const val OUTSIDE_ROOM = 0
        @Subevent const val GAME_START = 2
        @Subevent const val ROOM_START = 4
        @Subevent const val ROOM_END = 5
        @Subevent const val ANIMATION_END = 7
        @Subevent const val USER_0 = 10
        @Subevent const val USER_1 = 11
        @Subevent const val USER_2 = 12
        @Subevent const val USER_3 = 13
        @Subevent const val USER_4 = 14
        @Subevent const val USER_5 = 15
        @Subevent const val USER_6 = 16
        @Subevent const val USER_7 = 17
        @Subevent const val USER_8 = 18
        @Subevent const val USER_9 = 19
        @Subevent const val USER_10 = 20
        @Subevent const val USER_11 = 21
        @Subevent const val USER_12 = 22
        @Subevent const val USER_13 = 23
        @Subevent const val USER_14 = 24
        @Subevent const val USER_15 = 25
    }

    @SubtypesOf(DRAW)
    object DrawSubType {
        @Subevent const val DRAW = 0
        @Subevent const val DRAW_END = 77
    }

    val MAX_EVENT_ID = MaxEventId

    fun getEventTypeName(eventType: Int) = EventTypesToName[eventType] ?: "UNKNOWN"
    fun getEventSubtypeName(eventType: Int, eventSubtype: Int) = EventSubtypesToName[EventNameKey(eventType, eventSubtype)] ?: "UNKNOWN"
}