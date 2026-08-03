package com.mrpowergamerbr.starfait.wad

class Path(
    val name: StringPointer,
    val kind: Int,
    val closed: Boolean,
    val precision: Int,
    val points: List<Point>
) {
    class Point(
        val x: Float,
        val y: Float,
        val speed: Float
    )
}