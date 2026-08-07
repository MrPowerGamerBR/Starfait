package com.mrpowergamerbr.starfait.runner

import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

object EventType {
    @Event const val CREATE = 0
    @Event const val DESTROY = 1
    @Event const val ALARM = 2
    @Event const val STEP = 3
    @Event const val OTHER = 7
    @Event const val DRAW = 8

    @SubTypesOf(STEP)
    object StepSubType {
        @Event const val NORMAL = 0
        @Event const val BEGIN = 1
        @Event const val END = 2
    }

    @SubTypesOf(OTHER)
    object OtherSubType {
        @Event const val GAME_START = 2
        @Event const val ROOM_START = 4
    }

    @SubTypesOf(DRAW)
    object DrawSubType {
        @Event const val DRAW = 0
    }

    private val eventTypesToName = mutableMapOf<Int, String>()
    private val eventSubtypesToName = mutableMapOf<EventNameKey, String>()

    init {
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