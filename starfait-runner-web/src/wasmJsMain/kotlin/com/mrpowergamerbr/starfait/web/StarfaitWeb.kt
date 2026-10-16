package com.mrpowergamerbr.starfait.web

import com.mrpowergamerbr.starfait.StarfaitByteBuffer
import com.mrpowergamerbr.starfait.StarfaitUtils
import com.mrpowergamerbr.starfait.TimeUtils
import com.mrpowergamerbr.starfait.audio.noop.NoopAudioManager
import com.mrpowergamerbr.starfait.filesystem.LayeredFileSystem
import com.mrpowergamerbr.starfait.filesystem.virtual.VirtualFileSystem
import com.mrpowergamerbr.starfait.renderer.noop.NoopRenderer
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.runner.StdoutGameOutput
import com.mrpowergamerbr.starfait.runner.resources.RunnerResources
import com.mrpowergamerbr.starfait.vm.StarfaitVM
import com.mrpowergamerbr.starfait.wad.GameWAD
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import web.blob.byteArray
import web.dom.ElementId
import web.dom.document
import web.events.addEventHandler
import web.html.HTMLInputElement
import web.input.INPUT
import web.input.InputEvent

class StarfaitWeb {
    fun start() {
        println("Howdy from Kotlin/WASM!")

        document.getElementById(ElementId("file-input"))!!
            .addEventHandler(InputEvent.INPUT) {
                println("File changed!")

                val element = it.currentTarget as HTMLInputElement
                val wadFile = element.files!!.item(0)!!

                GlobalScope.launch {
                    val gameWAD = GameWAD.read(StarfaitByteBuffer.fromByteArray(wadFile.byteArray(), 0))
                    println(gameWAD.strg.getString(gameWAD.gen8.gameDisplayName))

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
                    val layeredFileSystem = LayeredFileSystem(VirtualFileSystem(), VirtualFileSystem())
                    val runner = StarfaitRunner(
                        RunnerResources.create(vm.gameWAD, renderer),
                        vm,
                        renderer,
                        NoopAudioManager(),
                        layeredFileSystem,
                        StdoutGameOutput(),
                        TimeUtils.currentTimeMillis(),
                        emptyMap()
                    )
                    // renderer.runner = runner
                    vm.runner = runner
                    // vm.runner.enableCollisionRendering = enableCollisionRendering
                    // vm.runner.drawInvisibleInstances = drawInvisibleInstances
                    runner.start()

                    repeat(200) {
                        runner.tick()
                    }

                }
            }
    }
}