package com.mrpowergamerbr.starfait.runner

import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

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

    @SubTypesOf(STEP)
    object StepSubType {
        @Event const val NORMAL = 0
        @Event const val BEGIN = 1
        @Event const val END = 2
    }

    @SubTypesOf(OTHER)
    object OtherSubType {
        @Event const val OUTSIDE_ROOM = 0
        @Event const val GAME_START = 2
        @Event const val ROOM_START = 4
        @Event const val ROOM_END = 5
        @Event const val USER_0 = 10
        @Event const val USER_1 = 11
        @Event const val USER_2 = 12
        @Event const val USER_3 = 13
        @Event const val USER_4 = 14
        @Event const val USER_5 = 15
        @Event const val USER_6 = 16
        @Event const val USER_7 = 17
        @Event const val USER_8 = 18
        @Event const val USER_9 = 19
        @Event const val USER_10 = 20
        @Event const val USER_11 = 21
        @Event const val USER_12 = 22
        @Event const val USER_13 = 23
        @Event const val USER_14 = 24
        @Event const val USER_15 = 25
    }

    @SubTypesOf(DRAW)
    object DrawSubType {
        @Event const val DRAW = 0
    }

    val MAX_EVENT_ID: Int
    private val eventTypesToName = mutableMapOf<Int, String>()
    private val eventSubtypesToName = mutableMapOf<EventNameKey, String>()

    init {
        MAX_EVENT_ID = EventType::class.members
            .filter { it.hasAnnotation<EventType.Event>() }
            .maxOf { it.call() as Int }

        EventType::class.members
            .filter { it.hasAnnotation<EventType.Event>() }
            .forEach {
                val name = it.name
                val eventTypeValue = it.call() as Int

                val subtypeClass = EventType::class.nestedClasses
                    .firstOrNull {
                        it.findAnnotation<EventType.SubTypesOf>()?.eventType == eventTypeValue
                    }

                val members = subtypeClass?.members

                eventTypesToName[eventTypeValue] = name

                if (members != null) {
                    for (subtype in subtypeClass.members.filter { it.hasAnnotation<EventType.Event>() }) {
                        val subeventName = subtype.name
                        val eventSubtypeValue = subtype.call() as Int

                        eventSubtypesToName[EventNameKey(eventTypeValue, eventSubtypeValue)] = subeventName
                    }
                }
            }
    }

    fun getEventTypeName(eventType: Int) = eventTypesToName[eventType] ?: "UNKNOWN"
    fun getEventSubtypeName(eventType: Int, eventSubtype: Int) = eventSubtypesToName[EventNameKey(eventType, eventSubtype)] ?: "UNKNOWN"

    private annotation class Event
    private annotation class SubTypesOf(val eventType: Int)

    data class EventNameKey(val eventType: Int, val eventSubtype: Int)
}