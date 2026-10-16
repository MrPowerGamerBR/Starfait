package com.mrpowergamerbr.starfait.runner

sealed class Drawable(val depth: Int) {
    class Tile(val tile: RunnerRoom.RunnerTile, depth: Int) : Drawable(depth)
    class Instance(val instance: com.mrpowergamerbr.starfait.runner.Instance, depth: Int) : Drawable(depth)
}