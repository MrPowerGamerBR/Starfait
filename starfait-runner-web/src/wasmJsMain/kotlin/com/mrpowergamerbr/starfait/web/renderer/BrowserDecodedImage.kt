package com.mrpowergamerbr.starfait.web.renderer

import com.mrpowergamerbr.starfait.renderer.opengl.DecodedImage
import web.images.ImageBitmap

class BrowserDecodedImage(width: Int, height: Int, val image: ImageBitmap) : DecodedImage(width, height)