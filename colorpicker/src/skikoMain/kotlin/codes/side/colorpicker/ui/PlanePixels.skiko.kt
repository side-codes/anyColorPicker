package codes.side.colorpicker.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

internal actual fun imageBitmapFromPixels(
    pixels: IntArray,
    width: Int,
    height: Int,
): ImageBitmap {
    // Skia reads the buffer in byte order, so an 0xAARRGGBB int lands as BGRA little-endian.
    val bytes = ByteArray(pixels.size * 4)
    for (i in pixels.indices) {
        val pixel = pixels[i]
        val offset = i * 4
        bytes[offset] = (pixel and 0xFF).toByte()
        bytes[offset + 1] = ((pixel shr 8) and 0xFF).toByte()
        bytes[offset + 2] = ((pixel shr 16) and 0xFF).toByte()
        bytes[offset + 3] = ((pixel shr 24) and 0xFF).toByte()
    }

    val bitmap = Bitmap()
    bitmap.allocPixels(
        ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL),
    )
    bitmap.installPixels(bytes)
    bitmap.setImmutable()
    return bitmap.asComposeImageBitmap()
}
