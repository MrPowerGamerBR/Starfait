package com.mrpowergamerbr.starfait.renderer.opengl

abstract class DecodedImage(val width: Int, val height: Int)

class RawPixelsDecodedImage(width: Int, height: Int, val pixels: ByteArray) : DecodedImage(width, height)