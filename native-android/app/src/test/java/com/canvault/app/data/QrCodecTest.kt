package com.canvault.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodecTest {
    private val can = CanItem(
        brandId = "montana-cans",
        canLineId = "montana-cans:montana-black",
        colorName = "Mint Test",
        colorCode = "MX-01",
        customHex = "#58E4C2",
    )

    @Test
    fun ownQrPayloadRoundTrips() {
        val result = QrCodec.interpret(QrCodec.encode(can), "QR")
        assertTrue(result is ScanInterpretation.CatalogMatch)
        val payload = (result as ScanInterpretation.CatalogMatch).payload
        assertEquals(can.brandId, payload.brandId)
        assertEquals(can.canLineId, payload.canLineId)
        assertEquals(can.colorName, payload.colorName)
    }

    @Test
    fun externalBarcodeRequiresManualReview() {
        val result = QrCodec.interpret("4006381333931", "EAN-13")
        assertTrue(result is ScanInterpretation.ExternalCode)
    }

    @Test
    fun malformedCanVaultPayloadIsRejected() {
        val result = QrCodec.interpret("{\"app\":\"canvault\",\"version\":1}", "QR")
        assertTrue(result is ScanInterpretation.Invalid)
    }
}
