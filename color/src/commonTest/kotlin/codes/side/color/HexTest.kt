package codes.side.color

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class HexTest {

    @Test
    fun alphaGoesWhereItIsNamed() {
        val red = Srgb(1.0, 0.0, 0.0, alpha = 0.5)
        assertEquals("#FF0000", red.toHexString(HexAlpha.None))
        assertEquals("#80FF0000", red.toHexString(HexAlpha.First))
        assertEquals("#FF000080", red.toHexString(HexAlpha.Last))
        assertEquals("#FF000000", Srgb(1.0, 0.0, 0.0, alpha = null).toHexString(HexAlpha.Last))
    }

    @Test
    fun channelsRoundHalfUp() {
        assertEquals("#808080", Srgb(0.5, 0.5, 0.5).toHexString(HexAlpha.None))
        assertEquals("#7F7F7F", Srgb(127.49 / 255.0, 127.49 / 255.0, 127.49 / 255.0).toHexString(HexAlpha.None))
    }

    @Test
    fun colorsOutsideSrgbAreMappedFirst() {
        val vivid = OkLch(0.7, 0.4, 30.0)
        val css = vivid.toHexString(HexAlpha.None)
        assertEquals(vivid.toGamut(Srgb.gamut).toHexString(HexAlpha.None), css)
        assertNotEquals(css, vivid.toHexString(HexAlpha.None, GamutMapping.Clip))
        assertEquals("#FFFFFF", OkLch(1.2, 0.1, 30.0).toHexString(HexAlpha.None))
    }

    @Test
    fun parsingKeepsOnePointXsRules() {
        assertEquals(ColorValue.parseHex("#AABBCC", HexAlpha.None), ColorValue.parseHex("abc", HexAlpha.None))
        assertEquals(Srgb(1.0, 0.0, 0.0, 0xCC / 255.0), ColorValue.parseHex("#F00C", HexAlpha.Last))
        assertEquals(Srgb(0.0, 0.0, 0xCC / 255.0, 1.0), ColorValue.parseHex("#F00C", HexAlpha.First))
        assertEquals(Srgb(1.0, 0.0, 0.0, 0x80 / 255.0), ColorValue.parseHex("#80ff0000", HexAlpha.First))
        assertEquals(Srgb(1.0, 0.0, 0.0), ColorValue.parseHex("#FF0000", HexAlpha.Last))
        for (text in listOf("#F00C", "#FF0000CC")) assertNull(ColorValue.parseHexOrNull(text, HexAlpha.None))
        for (text in listOf("", "#", "#12", "#12345", "#1234567", "#123456789", "#GGG", "##123", " #123", "#١٢٣", "#ＡＢＣ")) {
            assertNull(ColorValue.parseHexOrNull(text, HexAlpha.Last), text)
        }
        assertFailsWith<IllegalArgumentException> { ColorValue.parseHex("#12", HexAlpha.Last) }
    }

    @Test
    fun hexRoundTripsEveryByte() {
        val random = Random(20260924)
        repeat(2_000) {
            val argb = random.nextInt()
            for (alpha in HexAlpha.entries) {
                val digits = when (alpha) {
                    HexAlpha.None -> (argb and 0xFFFFFF).toString(16).padStart(6, '0')
                    HexAlpha.First -> argb.toUInt().toString(16).padStart(8, '0')
                    HexAlpha.Last -> ((argb shl 8).toUInt() or (argb ushr 24).toUInt()).toString(16).padStart(8, '0')
                }
                val text = "#${digits.uppercase()}"
                assertEquals(text, ColorValue.parseHex(text, alpha).toHexString(alpha))
            }
        }
    }
}
