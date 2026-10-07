package com.mrpowergamerbr.starfait.runner.textfiles

import com.mrpowergamerbr.starfait.filesystem.LayeredFileSystem
import com.mrpowergamerbr.starfait.filesystem.virtual.VirtualFileSystem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

object TextFileManagerTest {
    @Test
    fun `test text file manager allocation IDs`() {
        val fs = LayeredFileSystem(VirtualFileSystem(), VirtualFileSystem())
        val manager = TextFileManager(fs)

        val textFile = manager.openTextFile("howdy.txt", true)
        assertEquals(1, textFile.textFileId)
        manager.closeTextFile(textFile.textFileId)

        val textFile2 = manager.openTextFile("howdy2.txt", true)
        assertEquals(1, textFile2.textFileId)
        manager.closeTextFile(textFile2.textFileId)
    }

    @Test
    fun `test text file manager allocation IDs nested`() {
        val fs = LayeredFileSystem(VirtualFileSystem(), VirtualFileSystem())
        val manager = TextFileManager(fs)

        val textFile = manager.openTextFile("howdy.txt", true)
        manager.closeTextFile(textFile.textFileId)
    }

    @Test
    fun `test text file manager write file`() {
        val fs = LayeredFileSystem(VirtualFileSystem(), VirtualFileSystem())
        val manager = TextFileManager(fs)
        val textFile = manager.openTextFile("howdy.txt", true)
        textFile.writeString("Loritta is so cute!")
        manager.closeTextFile(textFile.textFileId)
        assertContentEquals("Loritta is so cute!".toByteArray(), fs.readFile("howdy.txt"))
    }

    @Test
    fun `test text file manager write file with new lines`() {
        val fs = LayeredFileSystem(VirtualFileSystem(), VirtualFileSystem())
        val manager = TextFileManager(fs)
        val textFile = manager.openTextFile("howdy.txt", true)
        textFile.writeString("Loritta is so cute!")
        textFile.writeNewLine()
        textFile.writeString("yayy!")
        manager.closeTextFile(textFile.textFileId)
        assertContentEquals("Loritta is so cute!\nyayy!".toByteArray(), fs.readFile("howdy.txt"))
    }

    @Test
    fun `test text file manager read file real`() {
        val fs = LayeredFileSystem(VirtualFileSystem(), VirtualFileSystem())
        fs.writeFile("howdy.txt", "45.62 4002.8922 ".toByteArray(Charsets.UTF_8))
        val manager = TextFileManager(fs)
        val textFile = manager.openTextFile("howdy.txt", false)
        assertEquals(45.62, textFile.readReal())
        assertEquals(4002.8922, textFile.readReal())
        manager.closeTextFile(textFile.textFileId)
    }

    @Test
    fun `test text file manager write file real`() {
        val fs = LayeredFileSystem(VirtualFileSystem(), VirtualFileSystem())
        val manager = TextFileManager(fs)
        val textFile = manager.openTextFile("howdy.txt", true)
        textFile.writeReal(45.62)
        textFile.writeReal(4002.8922)
        manager.closeTextFile(textFile.textFileId)
        fs.writeFile("howdy.txt", "45.62 4002.89 ".toByteArray(Charsets.UTF_8))
    }

    @Test
    fun `test text file manager readString vs readLine`() {
        val fs = LayeredFileSystem(VirtualFileSystem(), VirtualFileSystem())
        fs.writeFile("howdy.txt", "Loritta is so cute!\nayaya".toByteArray(Charsets.UTF_8))
        val manager = TextFileManager(fs)

        val textFile = manager.openTextFile("howdy.txt", false)
        assertEquals("Loritta is so cute!", textFile.readString())
        assertEquals("", textFile.readString())
        // Now if we read the line itself, it should actually work
        assertEquals("", textFile.readLine())
        assertEquals("ayaya", textFile.readLine())
        manager.closeTextFile(textFile.textFileId)
    }

    @Test
    fun `test text file manager eof`() {
        val fs = LayeredFileSystem(VirtualFileSystem(), VirtualFileSystem())
        fs.writeFile("howdy.txt", "Loritta is so cute!\nayaya".toByteArray(Charsets.UTF_8))
        val manager = TextFileManager(fs)

        val textFile = manager.openTextFile("howdy.txt", false)
        assertEquals("Loritta is so cute!", textFile.readLine())
        assertEquals(true, textFile.hasRemaining())
        assertEquals("ayaya", textFile.readLine())
        assertEquals(false, textFile.hasRemaining())
        manager.closeTextFile(textFile.textFileId)
    }

    @Test
    fun `test text file manager trying to write to a read only file`() {
        val fs = LayeredFileSystem(VirtualFileSystem(), VirtualFileSystem())
        val manager = TextFileManager(fs)
        val textFile = manager.openTextFile("howdy.txt", false)
        assertFailsWith(IllegalStateException::class) {
            textFile.writeString("Loritta is so cute!")
        }
    }

    @Test
    fun `test text file manager trying to read from a write only file`() {
        val fs = LayeredFileSystem(VirtualFileSystem(), VirtualFileSystem())
        val manager = TextFileManager(fs)
        val textFile = manager.openTextFile("howdy.txt", true)
        assertFailsWith(IllegalStateException::class) {
            textFile.readString()
        }
    }

    @Test
    fun `test files should overwrite instead of appending`() {
        val fs = LayeredFileSystem(VirtualFileSystem(), VirtualFileSystem())
        val manager = TextFileManager(fs)
        val textFile = manager.openTextFile("howdy.txt", true)
        textFile.writeString("Loritta is so cute!")
        manager.closeTextFile(textFile.textFileId)
        val textFile2 = manager.openTextFile("howdy.txt", true)
        textFile2.writeString("Loritta is so cute!")
        manager.closeTextFile(textFile2.textFileId)
        assertContentEquals("Loritta is so cute!".toByteArray(), fs.readFile("howdy.txt"))
    }
}