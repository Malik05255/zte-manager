package com.malik.ztesmartmanager.core.protocol

import com.malik.ztesmartmanager.core.profile.RadioIdEncoding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZteRadioIdParserTest {
    @Test
    fun hexFirst_decodesDigitOnlyLegacyTokenAsHex() {
        assertEquals(100, ZteRadioIdParser.parseInt("64", 503, RadioIdEncoding.HEX_FIRST))
    }

    @Test
    fun safeAuto_rejectsDigitOnlyTokenWhenBothRadicesAreValid() {
        assertNull(ZteRadioIdParser.parseInt("64", 503, RadioIdEncoding.SAFE_AUTO))
    }

    @Test
    fun safeAuto_acceptsExplicitHexPrefix() {
        assertEquals(100, ZteRadioIdParser.parseInt("0x64", 503, RadioIdEncoding.SAFE_AUTO))
    }

    @Test
    fun safeAuto_acceptsHexLettersBecauseDecimalIsImpossible() {
        assertEquals(486, ZteRadioIdParser.parseInt("1E6", 503, RadioIdEncoding.SAFE_AUTO))
    }

    @Test
    fun safeAuto_acceptsOnlyInRangeInterpretation() {
        assertEquals(503, ZteRadioIdParser.parseInt("503", 503, RadioIdEncoding.DECIMAL_FIRST))
        assertEquals(503, ZteRadioIdParser.parseInt("503", 503, RadioIdEncoding.SAFE_AUTO))
    }

    @Test
    fun invalidOrOutOfRangeTokensReturnUnknown() {
        assertNull(ZteRadioIdParser.parseInt("-1", 503, RadioIdEncoding.HEX_FIRST))
        assertNull(ZteRadioIdParser.parseInt("xyz", 503, RadioIdEncoding.HEX_FIRST))
        assertNull(ZteRadioIdParser.parseInt("FFFF", 503, RadioIdEncoding.HEX_FIRST))
    }

    @Test
    fun cellIdUsesSameFailClosedPolicy() {
        assertEquals(0x1234L, ZteRadioIdParser.parseLong("1234", encoding = RadioIdEncoding.HEX_FIRST))
        assertNull(ZteRadioIdParser.parseLong("1234", encoding = RadioIdEncoding.SAFE_AUTO))
        assertEquals(0x1234L, ZteRadioIdParser.parseLong("0x1234", encoding = RadioIdEncoding.SAFE_AUTO))
    }
}
