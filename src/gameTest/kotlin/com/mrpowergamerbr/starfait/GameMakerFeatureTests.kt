package com.mrpowergamerbr.starfait

import com.mrpowergamerbr.starfait.renderer.noop.NoopRenderer
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.vm.StarfaitVM
import com.mrpowergamerbr.starfait.wad.GameWAD
import org.junit.jupiter.api.Test
import java.io.File

object GameMakerFeatureTests {
    @Test
    fun `test solid collisions`() = runTestGame(
        "solid-collisions",
        0,
        100
    ) {
        withRunnerStateAtFrame(50) {
            assert(it.firstInstanceByName("obj_mover1").x == 32.0f)
            assert(it.firstInstanceByName("obj_mover2").x != 32.0f)
            assert(it.firstInstanceByName("obj_mover3").x == 32.0f)
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
        val runner = StarfaitRunner(vm.gameWAD, vm, renderer, seed, emptyMap())
        vm.runner = runner
        runner.start()

        repeat(exitAtFrame) {
            runner.step()

            val callback = config.dumpedStates[it]
            if (callback != null) {
                callback(runner)
            }
        }
    }

    class TestConfig {
        var dumpedStates = mutableMapOf<Int, (StarfaitRunner) -> (Unit)>()

        fun withRunnerStateAtFrame(dumpStateAtFrame: Int, callback: (StarfaitRunner) -> (Unit)) {
            dumpedStates[dumpStateAtFrame] = callback
        }
    }

    fun StarfaitRunner.firstInstanceByName(name: String) = this.currentRoom.instances.first { it.objectName == name }
}