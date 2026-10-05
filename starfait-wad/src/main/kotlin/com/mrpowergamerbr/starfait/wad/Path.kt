package com.mrpowergamerbr.starfait.wad

class Path(
    val name: StringPointer,
    val kind: PathKind,
    val closed: Boolean,
    val precision: Int,
    val points: List<Point>
) {
    class Point(
        val x: Float,
        val y: Float,
        val speed: Float
    )

    enum class PathKind(val value: Int) {
        LINEAR(0),
        CURVED(1)
    }
}