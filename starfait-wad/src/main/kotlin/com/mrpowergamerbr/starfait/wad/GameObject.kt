package com.mrpowergamerbr.starfait.wad

class GameObject(
    val name: StringPointer,
    val spriteIndex: Int,
    val visible: Boolean,
    val solid: Boolean,
    val depth: Int,
    val persistent: Boolean,
    val parentId: Int,
    val maskId: Int,
    val physicsObject: Boolean,
    val physicsSensor: Boolean,
    val physicsShape: Int,
    val physicsDensity: Float,
    val physicsRestitution: Float,
    val physicsGroup: Int,
    val physicsLinearDamping: Float,
    val physicsAngularDamping: Float,
    val physicsFriction: Float,
    val physicsAwake: Boolean,
    val physicsKinematic: Boolean,
    val physicsVertices: List<Vertex>,
    val events: List<List<Event>>
) {
    class Vertex(
        val x: Float,
        val y: Float
    )

    class Event(
        val subtype: Int,
        val actions: List<Action>
    )

    class Action(
        val libraryId: Int,
        val actionId: Int,
        val kind: Int,
        val allowRelative: Boolean,
        val isQuestion: Boolean,
        val applyToSomething: Boolean,
        val execType: Int,
        val functionName: StringPointer,
        val codeId: Int,
        val argumentsUsed: Int,
        val appliesTo: Int,
        val relative: Boolean,
        val isNot: Boolean,
        val arguments: List<DnDArgument>
    )
}
