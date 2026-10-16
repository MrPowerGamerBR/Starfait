package com.mrpowergamerbr.starfait

/**
 * Puts [value] at [key] if [this] doesn't already have a entry with the [key]
 *
 * If it already has a entry with the [key], returns the current value and the map isn't modified
 */
fun <K, V> MutableMap<K, V>.putIfAbsent(key: K, value: V): V? {
    if (this.containsKey(key)) {
        return this[key]
    } else {
        this[key] = value
        return null
    }
}

fun <K, V> LinkedHashMap<K, V>.putFirst(key: K, value: V) {
    // This sucks a lot, but there's no other way to do this (the JVM does have a putFirst, but alas...)
    val oldEntries = this.entries.toList()
    this.clear()
    this[key] = value
    for (entry in oldEntries) {
        this[entry.key] = entry.value
    }
}