package com.mrpowergamerbr.starfait

import com.mrpowergamerbr.starfait.renderer.noop.NoopRenderer
import com.mrpowergamerbr.starfait.runner.BufferedGameOutput
import com.mrpowergamerbr.starfait.runner.CollisionPair
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.vm.RValue
import com.mrpowergamerbr.starfait.vm.StarfaitVM
import com.mrpowergamerbr.starfait.vm.VariableContainer
import com.mrpowergamerbr.starfait.wad.GameWAD
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

object GameMakerFeatureTests {
    @Test
    fun `test solid collisions`() = runTestGame(
        "solid-collisions",
        0,
        100
    ) {
        withRunnerAtFrame(50) {
            assertEquals(32.0f, it.firstInstanceByName("obj_mover1").x)
            assert(it.firstInstanceByName("obj_mover2").x != 32.0f)
            assertEquals(32.0f, it.firstInstanceByName("obj_mover3").x)
            assertEquals(32.0f, it.firstInstanceByName("obj_mover4").x)
        }
    }

    @Test
    fun `test collision order`() = runTestGame(
        "collision-order",
        0,
        1
    ) {
        withRunnerAtFrame(0) {
            // How it is configured:
            // obj_collision1 has collision events to obj_collision2 and obj_collision3
            // obj_collision2 has collision events to obj_collision1 and obj_collision3
            // obj_collision3 has collision events to obj_collision2 and obj_collision3
            //
            // So if we iterate it, we need to have a list of
            // obj_collision1 -> [obj_collision2, obj_collision3]
            // obj_collision2 -> [obj_collision3]
            // (the obj_collision3 doesn't have one by itself because the only possible collision for it already exists)
            val objCollision1 = it.gameWAD.objectIndexByObjectName("obj_collision1")
            val objCollision2 = it.gameWAD.objectIndexByObjectName("obj_collision2")
            val objCollision3 = it.gameWAD.objectIndexByObjectName("obj_collision3")

            val requiredCollisionPairs = listOf(
                CollisionPair.of(objCollision1, objCollision2),
                CollisionPair.of(objCollision1, objCollision3),
                CollisionPair.of(objCollision2, objCollision3)
            )

            assertEquals(requiredCollisionPairs.sortedWith(compareBy(CollisionPair::objectIndex1).thenBy(CollisionPair::objectIndex2)), it.collisionPairs)
        }

        checkGameLogEndsWith(
            0,
            """
                    FRAME START (0)
                    I'm obj_collision1 (0) and I'm colliding with obj_collision3 (1)!
                    I'm obj_collision3 (1) and I'm colliding with obj_collision1 (0)!
                    I'm obj_collision1 (0) and I'm colliding with obj_collision2 (2)!
                    I'm obj_collision2 (2) and I'm colliding with obj_collision1 (0)!
                    I'm obj_collision3 (1) and I'm colliding with obj_collision2 (2)!
                    I'm obj_collision2 (2) and I'm colliding with obj_collision3 (1)!
                    FRAME END
            """.trimIndent()
        )
    }

    @Test
    fun `test precise collisions`() = runTestGame(
        "precise-collisions",
        0,
        1
    ) {
        withRunnerAtFrame(0) {
            it.instanceByIdAndName(100001, "obj_box").assertVariable(it.vm, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100002, "obj_box").assertVariable(it.vm, "colliding", RValue.Boolean(true))
            it.instanceByIdAndName(100003, "obj_box").assertVariable(it.vm, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100004, "obj_box").assertVariable(it.vm, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100005, "obj_box").assertVariable(it.vm, "colliding", RValue.Boolean(true))
            it.instanceByIdAndName(100000, "obj_loritta").assertVariable(it.vm, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100006, "obj_loritta").assertVariable(it.vm, "colliding", RValue.Boolean(true))
            it.instanceByIdAndName(100007, "obj_loritta").assertVariable(it.vm, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100008, "obj_loritta2").assertVariable(it.vm, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100009, "obj_loritta2").assertVariable(it.vm, "colliding", RValue.Boolean(true))
        }
    }

    fun runTestGame(gameId: String, seed: Long, exitAtFrame: Int, block: TestConfig.() -> Unit = {}) {
        val config = TestConfig().apply(block)
        val gamesDir = File(System.getProperty("games.dir"))
        val byteReader = StarfaitByteBuffer.fromByteArray(File(File(gamesDir, "$gameId/data.win").absolutePath).readBytes())
        val gameWAD = GameWAD.read(byteReader)
        val vm = StarfaitVM(
            gameWAD,
            gmlScriptsOpcodesToBeTraced = emptySet(),
            functionCallsToBeTraced = emptySet(),
            alarmsToBeTraced = emptySet(),
            eventsToBeTraced = emptySet(),
            traceVariableReads = emptySet(),
            traceVariableWrites = emptySet(),
            traceCodeExecution = emptySet()
        )
        vm.associateBuiltins(StarfaitUtils.remapReferences(gameWAD))

        val renderer = NoopRenderer
        val runner = StarfaitRunner(vm.gameWAD, vm, renderer, BufferedGameOutput(), seed, emptyMap())
        vm.runner = runner
        runner.start()

        repeat(exitAtFrame) {
            runner.step()

            config.runnerCallbacks[it]?.invoke(runner)
            config.gameLogExpectedEndsWith[it]?.let { expectedEndsWithString ->
                val outputBuffer = (runner.output as BufferedGameOutput).output
                val outputOnlyExpectedLines = outputBuffer.lines().dropLast(1).takeLast(expectedEndsWithString.lines().size).joinToString("\n")

                // We drop the last line because it is an empty \n
                assert(outputOnlyExpectedLines.endsWith(expectedEndsWithString)) { "Expected game log to end with \n---\n$expectedEndsWithString\n---\n...but it ended with \n---\n$outputOnlyExpectedLines\n---" }
            }
        }
    }

    class TestConfig {
        var runnerCallbacks = mutableMapOf<Int, (StarfaitRunner) -> (Unit)>()
        var gameLogExpectedEndsWith = mutableMapOf<Int, String>()

        fun withRunnerAtFrame(dumpStateAtFrame: Int, callback: (StarfaitRunner) -> (Unit)) {
            runnerCallbacks[dumpStateAtFrame] = callback
        }

        fun checkGameLogEndsWith(checkLogAtFrame: Int, expectedEndsWithString: String) {
            gameLogExpectedEndsWith[checkLogAtFrame] = expectedEndsWithString
        }
    }

    fun StarfaitRunner.firstInstanceByName(name: String) = this.currentRoom.instances.first { it.objectName == name }
    fun StarfaitRunner.instanceById(instanceId: Int) = this.currentRoom.instances.first { it.id == instanceId }
    fun StarfaitRunner.instanceByIdAndName(instanceId: Int, name: String): Instance {
        val matchedInstance = instanceById(instanceId)
        assertEquals(matchedInstance.objectName, name)
        return matchedInstance
    }

    fun GameWAD.objectIndexByObjectName(objectName: String): Int {
        val index = this.objt.objects.indexOfFirst { this.strg.getString(it.name) == objectName }
        if (index == -1)
            error("Couldn't find object index for object name \"$objectName\"!")
        return index
    }

    fun VariableContainer.assertVariable(vm: StarfaitVM, name: String, expected: RValue) {
        val varId = vm.getVariableSlotFromVariableName(name)
        val actual = this.getVariable(varId, null)
        assertEquals(expected.convertToBoolean(), actual.convertToBoolean())
    }
}