package codes.side.colorpicker.foundation

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Wraps [pixels] — packed `0xAARRGGBB`, row-major from the top — as an [ImageBitmap].
 *
 * The only thing in the library a platform has to answer for itself. Filling a bitmap by
 * drawing one rectangle per pixel costs a call into the toolkit per pixel, which on a plane
 * is over sixteen thousand of them and, on the web, that many crossings out of Wasm. Every
 * toolkit can take the whole array at once instead; none of them agree on how to ask.
 */
internal expect fun imageBitmapFromPixels(pixels: IntArray, width: Int, height: Int): ImageBitmap
