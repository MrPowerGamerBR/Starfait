package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteReader
import com.mrpowergamerbr.starfait.wad.DnDArgument
import com.mrpowergamerbr.starfait.wad.GameObject

class OBJT(val objects: List<GameObject>) {
    companion object {
        fun read(reader: StarfaitByteReader): OBJT {
            val addresses = reader.readAddresses()

            val objects = ArrayList<GameObject>(addresses.size)
            for (address in addresses) {
                reader.position = address

                val name = reader.readStringPointer()
                val spriteIndex = reader.readInt()
                val visible = reader.readIntBoolean()
                val solid = reader.readIntBoolean()
                val depth = reader.readInt()
                val persistent = reader.readIntBoolean()
                val parentId = reader.readInt()
                val maskId = reader.readInt()
                val physicsObject = reader.readIntBoolean()
                val physicsSensor = reader.readIntBoolean()
                val physicsShape = reader.readInt()
                val physicsDensity = reader.readFloat()
                val physicsRestitution = reader.readFloat()
                val physicsGroup = reader.readInt()
                val physicsLinearDamping = reader.readFloat()
                val physicsAngularDamping = reader.readFloat()
                val physicsVertexCount = reader.readInt()
                val physicsFriction = reader.readFloat()
                val physicsAwake = reader.readIntBoolean()
                val physicsKinematic = reader.readIntBoolean()

                val physicsVertices = ArrayList<GameObject.Vertex>(physicsVertexCount)
                repeat(physicsVertexCount) {
                    physicsVertices.add(GameObject.Vertex(reader.readFloat(), reader.readFloat()))
                }

                // One list of events per event type, each event holding its own actions
                val eventTypeAddresses = reader.readAddresses()

                val events = ArrayList<List<GameObject.Event>>(eventTypeAddresses.size)
                for (eventTypeAddress in eventTypeAddresses) {
                    reader.position = eventTypeAddress

                    val eventAddresses = reader.readAddresses()

                    val eventsOfThisType = ArrayList<GameObject.Event>(eventAddresses.size)
                    for (eventAddress in eventAddresses) {
                        reader.position = eventAddress

                        // Event
                        val subtype = reader.readInt()
                        val actionAddresses = reader.readAddresses()

                        val actions = ArrayList<GameObject.Action>(actionAddresses.size)
                        for (actionAddress in actionAddresses) {
                            // Action
                            reader.position = actionAddress

                            // Most of these fields are unused because they are used for GameMaker's Legacy DnD, newer GameMaker versions just convert the DnD blocks to GML when compiled
                            // The only one we care about is the codeId
                            // (The GM:S 1.0.198 runner has more data about it, the GM:S 1.4.1804 skips all of these fields directly to +0x20, which is the codeId field)
                            val libraryId = reader.readInt()
                            val actionId = reader.readInt()
                            val kind = reader.readInt()
                            val allowRelative = reader.readIntBoolean()
                            val isQuestion = reader.readIntBoolean()
                            val applyToSomething = reader.readIntBoolean()
                            val execType = reader.readInt()
                            val functionName = reader.readStringPointer()
                            val codeId = reader.readInt() // In pre-bytecode versions, this would have been a StringPointer, the GML code is stored in the STRG chunk
                            val argumentsUsed = reader.readInt()
                            val appliesTo = reader.readInt()
                            val relative = reader.readIntBoolean()
                            val isNot = reader.readIntBoolean()
                            val argumentCount = reader.readInt()
                            val arguments = ArrayList<DnDArgument>(argumentCount)
                            repeat(argumentCount) {
                                val kind = reader.readInt()
                                val string = reader.readStringPointer()
                                arguments.add(
                                    DnDArgument(
                                        kind = kind,
                                        string = string
                                    )
                                )
                            }
                            actions.add(
                                GameObject.Action(
                                    libraryId = libraryId,
                                    actionId = actionId,
                                    kind = kind,
                                    allowRelative = allowRelative,
                                    isQuestion = isQuestion,
                                    applyToSomething = applyToSomething,
                                    execType = execType,
                                    functionName = functionName,
                                    codeId = codeId,
                                    argumentsUsed = argumentsUsed,
                                    appliesTo = appliesTo,
                                    relative = relative,
                                    isNot = isNot,
                                    arguments = arguments
                                )
                            )
                        }

                        eventsOfThisType.add(GameObject.Event(subtype, actions))
                    }

                    events.add(eventsOfThisType)
                }

                objects.add(
                    GameObject(
                        name = name,
                        spriteIndex = spriteIndex,
                        visible = visible,
                        solid = solid,
                        depth = depth,
                        persistent = persistent,
                        parentId = parentId,
                        maskId = maskId,
                        physicsObject = physicsObject,
                        physicsSensor = physicsSensor,
                        physicsShape = physicsShape,
                        physicsDensity = physicsDensity,
                        physicsRestitution = physicsRestitution,
                        physicsGroup = physicsGroup,
                        physicsLinearDamping = physicsLinearDamping,
                        physicsAngularDamping = physicsAngularDamping,
                        physicsFriction = physicsFriction,
                        physicsAwake = physicsAwake,
                        physicsKinematic = physicsKinematic,
                        physicsVertices = physicsVertices,
                        events = events
                    )
                )
            }

            return OBJT(objects)
        }
    }
}
