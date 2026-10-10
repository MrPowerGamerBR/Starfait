package com.mrpowergamerbr.starfait

import com.mrpowergamerbr.starfait.audio.noop.NoopAudioManager
import com.mrpowergamerbr.starfait.filesystem.LayeredFileSystem
import com.mrpowergamerbr.starfait.filesystem.virtual.VirtualFileSystem
import com.mrpowergamerbr.starfait.renderer.noop.NoopRenderer
import com.mrpowergamerbr.starfait.runner.BufferedGameOutput
import com.mrpowergamerbr.starfait.runner.CollisionPair
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.runner.resources.RunnerResources
import com.mrpowergamerbr.starfait.vm.RValue
import com.mrpowergamerbr.starfait.vm.StarfaitVM
import com.mrpowergamerbr.starfait.vm.VariableContainer
import com.mrpowergamerbr.starfait.vm.VariableInstanceType
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
            val objCollision1 = it.resources.objectIndexByObjectName("obj_collision1")
            val objCollision2 = it.resources.objectIndexByObjectName("obj_collision2")
            val objCollision3 = it.resources.objectIndexByObjectName("obj_collision3")

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
            it.instanceByIdAndName(100001, "obj_box").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100002, "obj_box").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(true))
            it.instanceByIdAndName(100003, "obj_box").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100004, "obj_box").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100005, "obj_box").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(true))
            it.instanceByIdAndName(100000, "obj_loritta").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100006, "obj_loritta").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(true))
            it.instanceByIdAndName(100007, "obj_loritta").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100008, "obj_loritta2").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100009, "obj_loritta2").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(true))
            it.instanceByIdAndName(100011, "obj_precisebox").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100012, "obj_precisebox").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100013, "obj_precisebox").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100014, "obj_precisebox").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100015, "obj_precisebox").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(true))
            it.instanceByIdAndName(100016, "obj_precisebox").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(true))
            it.instanceByIdAndName(100017, "obj_precisebox").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(true))
            it.instanceByIdAndName(100018, "obj_precisebox").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(true))
            // Those two are *technically* colliding, but GameMaker doesn't consider them as colliding
            it.instanceByIdAndName(100019, "obj_precisebox").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100020, "obj_precisebox").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100021, "obj_precisebox").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(true))
            it.instanceByIdAndName(100022, "obj_precisebox").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(true))

            it.instanceByIdAndName(100024, "obj_face").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding1", RValue.Real(-4.0))
            it.instanceByIdAndName(100024, "obj_face").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding2", RValue.Real(100024.0))
            it.instanceByIdAndName(100024, "obj_face").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding3", RValue.Real(-4.0))
            it.instanceByIdAndName(100024, "obj_face").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding4", RValue.Real(100024.0))
            it.instanceByIdAndName(100024, "obj_face").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding5", RValue.Real(-4.0))
            it.instanceByIdAndName(100024, "obj_face").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding6", RValue.Real(100024.0))

            it.instanceByIdAndName(100025, "obj_facenotprecise").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding1", RValue.Real(100025.0))
            it.instanceByIdAndName(100025, "obj_facenotprecise").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding2", RValue.Real(100025.0))
            it.instanceByIdAndName(100025, "obj_facenotprecise").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding3", RValue.Real(100025.0))
            it.instanceByIdAndName(100025, "obj_facenotprecise").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding4", RValue.Real(100025.0))
            it.instanceByIdAndName(100025, "obj_facenotprecise").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding5", RValue.Real(100025.0))
            it.instanceByIdAndName(100025, "obj_facenotprecise").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding6", RValue.Real(100025.0))

            // These test stretched sprites
            it.instanceByIdAndName(100027, "obj_collideprecise2").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(true))
            it.instanceByIdAndName(100028, "obj_collideprecise2").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(true))
            it.instanceByIdAndName(100029, "obj_collideprecise2").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(false))
            it.instanceByIdAndName(100030, "obj_collideprecise2").assertBooleanVariable(it.vm, VariableInstanceType.SELF, "colliding", RValue.Boolean(true))
        }
    }

    @Test
    fun `test persistent rooms being removed from the persistent rooms list after the room has been marked as non-persistent`() = runTestGame(
        "persistent-rooms",
        0,
        51
    ) {
        withRunnerAtFrame(50) {
            assertEquals(it.persistentRooms.size, 0)
            it.vm.global.assertInt32Variable(it.vm, VariableInstanceType.GLOBAL, "createdCounter", RValue.Int32(6))
        }
    }

    @Test
    fun `test action move and speed`() = runTestGame(
        "action-move",
        0,
        2
    ) {
        withRunnerAtFrame(1) {
            assertEquals(218.34314f, it.firstInstanceByName("obj_moveUPLEFT").x)
            assertEquals(154.34314f, it.firstInstanceByName("obj_moveUPLEFT").y)

            assertEquals(256.0f, it.firstInstanceByName("obj_moveUP").x)
            assertEquals(152.0f, it.firstInstanceByName("obj_moveUP").y)

            assertEquals(293.65686f, it.firstInstanceByName("obj_moveUPRIGHT").x)
            assertEquals(154.34314f, it.firstInstanceByName("obj_moveUPRIGHT").y)

            assertEquals(216.0f, it.firstInstanceByName("obj_moveLEFT").x)
            assertEquals(192.0f, it.firstInstanceByName("obj_moveLEFT").y)

            assertEquals(218.34314f, it.firstInstanceByName("obj_moveDOWNLEFT").x)
            assertEquals(229.65686f, it.firstInstanceByName("obj_moveDOWNLEFT").y)

            assertEquals(256.0f, it.firstInstanceByName("obj_moveDOWN").x)
            assertEquals(232.0f, it.firstInstanceByName("obj_moveDOWN").y)

            assertEquals(293.65686f, it.firstInstanceByName("obj_moveDOWNRIGHT").x)
            assertEquals(229.65686f, it.firstInstanceByName("obj_moveDOWNRIGHT").y)

            assertEquals(296.0f, it.firstInstanceByName("obj_moveRIGHT").x)
            assertEquals(192.0f, it.firstInstanceByName("obj_moveRIGHT").y)

            assertEquals(256.0f, it.firstInstanceByName("obj_middle").x)
            assertEquals(192.0f, it.firstInstanceByName("obj_middle").y)
        }
    }

    @Test
    fun `test if there are elements on the stack after after using exit or return within a repeat block`() = runTestGame(
        "unbalanced-stack-repeat",
        0,
        2
    ) {
        withRunnerAtFrame(1) {
            assertEquals(0, it.vm.stack.top)
        }
    }

    @Test
    fun `test real parsing and printing`() = runTestGame(
        "real-parsing-and-printing",
        0,
        1
    ) {
        checkGameLogEndsWith(
            0,
            """
                    real(10.0): 10
                    real(10.5): 10.50
                    real(10,0): 10
                    real(10,5): 10
                    real(): 0
                    real(            ): 0
                    real(  4002.8922): 4002.89
                    real(4002.8922  ): 4002.89
                    real(Loritta is so cute!): 0
                    real(Loritta 4562.4562): 0
                    real(4562.4562 Loritta): 4562.46
                    real(.5): 0.50
                    real(..5): 0
                    real(a9): 0
                    real(9a): 9
                    real(9a.5): 9
                    real(45 62): 45
                    real(.   50): 0
            """.trimIndent()
        )
    }

    @Test
    fun `test move snap`() = runTestGame(
        "move-snap",
        0,
        2
    ) {
        withRunnerAtFrame(1) {
            assertEquals(128.0f, it.instanceByIdAndName(100000, "obj_tobesnapped").x)
            assertEquals(128.0f, it.instanceByIdAndName(100000, "obj_tobesnapped").y)
            assertEquals(128.0f, it.instanceByIdAndName(100001, "obj_tobesnapped").x)
            assertEquals(256.0f, it.instanceByIdAndName(100001, "obj_tobesnapped").y)
            assertEquals(256.0f, it.instanceByIdAndName(100002, "obj_tobesnapped").x)
            assertEquals(256.0f, it.instanceByIdAndName(100002, "obj_tobesnapped").y)
            assertEquals(256.0f, it.instanceByIdAndName(100003, "obj_tobesnapped").x)
            assertEquals(128.0f, it.instanceByIdAndName(100003, "obj_tobesnapped").y)
        }
    }

    @Test
    fun `test ini read, write, and virtual file system`() = runTestGame(
        "ini",
        0,
        2
    ) {
        onExit {
            val vfs = (it.layeredFileSystem.saveFileSystem as VirtualFileSystem)

            // Because GameMaker seems to shuffle the lines
            assertEquals(
                """
                    [Characters]
                    Gabriela="very cute!"
                    Pantufa="also cute"
                    Loritta="cute"
                    [Howdy]
                    ayaya="1.000000"
                    longboi="9999999999999.000000"
                    test="1234.000000"
                    answer="5.0 Loritta is so "cute"!"
                    hello="world"
                """.trimIndent(),
                vfs.files["test.ini"]!!.toString(Charsets.UTF_8).removeSuffix("\n")
            )
        }
    }

    @Test
    fun `test instance find and instance ids room vs runtime ordering`() = runTestGame(
        "instance-find-and-instance-ids",
        0,
        2
    ) {
        withRunnerAtFrame(1) {
            val first = 100006
            for (instanceId in first..100015) {
                it.instanceByIdAndName(instanceId, "obj_tobecreated")
                    .assertInt32Variable(it.vm, VariableInstanceType.SELF, "findresult", RValue.Int32(first))
            }
        }
    }

    @Test
    fun `test arithmetic quirks`() = runTestGame(
        "arithmetic-quirks",
        0,
        1
    ) {
        withRunnerAtFrame(0) {
            it.firstInstanceByName("obj_main").assertVariableType<RValue.Int64>(it.vm, VariableInstanceType.SELF, "value")
            it.firstInstanceByName("obj_main").assertVariableType<RValue.Real>(it.vm, VariableInstanceType.SELF, "value2")
            it.firstInstanceByName("obj_main").assertVariableType<RValue.Real>(it.vm, VariableInstanceType.SELF, "value3")
            it.firstInstanceByName("obj_main").assertVariableType<RValue.Int64>(it.vm, VariableInstanceType.SELF, "value4")
            it.firstInstanceByName("obj_main").assertVariableType<RValue.Real>(it.vm, VariableInstanceType.SELF, "value5")
        }

        checkGameLogEndsWith(0, """
            9007199254740994
            9007199254740998
            9007199254740996
            9007199254741000
        """.trimIndent())
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
        val runner = StarfaitRunner(RunnerResources.create(vm.gameWAD, renderer), vm, renderer, NoopAudioManager(), LayeredFileSystem(VirtualFileSystem(), VirtualFileSystem()), BufferedGameOutput(), seed, emptyMap())
        vm.runner = runner
        runner.start()

        repeat(exitAtFrame) {
            runner.tick()

            config.runnerCallbacks[it]?.invoke(runner)
            config.gameLogExpectedEndsWith[it]?.let { expectedEndsWithString ->
                val outputBuffer = (runner.output as BufferedGameOutput).output
                val outputOnlyExpectedLines = outputBuffer.lines().dropLast(1).takeLast(expectedEndsWithString.lines().size).joinToString("\n")

                // We drop the last line because it is an empty \n
                assert(outputOnlyExpectedLines.endsWith(expectedEndsWithString)) { "Expected game log to end with \n---\n$expectedEndsWithString\n---\n...but it ended with \n---\n$outputOnlyExpectedLines\n---" }
            }
        }

        config.onExit(runner)
    }

    class TestConfig {
        var runnerCallbacks = mutableMapOf<Int, (StarfaitRunner) -> (Unit)>()
        var gameLogExpectedEndsWith = mutableMapOf<Int, String>()
        var onExit: (StarfaitRunner) -> (Unit) = {}

        fun withRunnerAtFrame(dumpStateAtFrame: Int, callback: (StarfaitRunner) -> (Unit)) {
            runnerCallbacks[dumpStateAtFrame] = callback
        }

        fun checkGameLogEndsWith(checkLogAtFrame: Int, expectedEndsWithString: String) {
            gameLogExpectedEndsWith[checkLogAtFrame] = expectedEndsWithString
        }

        fun onExit(callback: (StarfaitRunner) -> (Unit)) {
            onExit = callback
        }
    }

    fun StarfaitRunner.firstInstanceByName(name: String) = this.currentRoom.instances.first { it.objectName == name }
    fun StarfaitRunner.instanceById(instanceId: Int) = this.currentRoom.instances.first { it.id == instanceId }
    fun StarfaitRunner.instanceByIdAndName(instanceId: Int, name: String): Instance {
        val matchedInstance = instanceById(instanceId)
        assertEquals(matchedInstance.objectName, name)
        return matchedInstance
    }

    fun RunnerResources.objectIndexByObjectName(objectName: String): Int {
        val index = this.objt.objects.indexOfFirst { this.strg.getString(it.name) == objectName }
        if (index == -1)
            error("Couldn't find object index for object name \"$objectName\"!")
        return index
    }

    fun VariableContainer.assertInt32Variable(vm: StarfaitVM, variableInstanceType: Int, name: String, expected: RValue) {
        val varId = vm.getVariableSlotFromVariableName(variableInstanceType, name)
        val actual = this.getVariable(varId, null)
        assertEquals(expected.convertToInt32(), actual.convertToInt32())
    }

    inline fun <reified T> VariableContainer.assertVariableType(vm: StarfaitVM, variableInstanceType: Int, name: String) {
        val varId = vm.getVariableSlotFromVariableName(variableInstanceType, name)
        val actual = this.getVariable(varId, null)
        assert(actual is T)
    }

    fun VariableContainer.assertBooleanVariable(vm: StarfaitVM, variableInstanceType: Int, name: String, expected: RValue) {
        val varId = vm.getVariableSlotFromVariableName(variableInstanceType, name)
        val actual = this.getVariable(varId, null)
        assertEquals(expected.convertToBoolean(), actual.convertToBoolean())
    }
}