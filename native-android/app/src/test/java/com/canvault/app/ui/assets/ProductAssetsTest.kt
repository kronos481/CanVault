package com.canvault.app.ui.assets

import com.canvault.app.data.canCatalog
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProductAssetsTest {
    @Test
    fun everyCatalogBrandHasAWhiteLogo() {
        canCatalog.forEach { brand ->
            assertNotNull("Logo fehlt für ${brand.id}", brandLogoRes(brand.id))
        }
    }

    @Test
    fun everyDeliveredCanImageIsMappedToItsCatalogLine() {
        val mappedLineIds = listOf(
            "mtn-montana-colors:mtn-94",
            "mtn-montana-colors:mtn-hardcore",
            "mtn-montana-colors:mtn-vice",
            "mtn-montana-colors:mtn-water-based-400",
            "mtn-montana-colors:mtn-alien",
            "montana-cans:montana-black",
            "montana-cans:montana-gold",
            "montana-cans:montana-white",
            "montana-cans:montana-tarblack",
            "montana-cans:montana-blackout-tarblack",
            "montana-cans:montana-ultra-wide",
            "molotow-belton:molotow-premium",
            "molotow-belton:molotow-burner",
            "molotow-belton:burner-chrome-600-ml",
            "molotow-belton:burner-gold-600-ml",
            "molotow-belton:burner-copper-600-ml",
            "molotow-belton:burner-black-600-ml",
            "molotow-belton:molotow-coversall",
            "loop-colors:loop-400-ml",
            "loop-colors:loop-asphalt",
            "flame:flame-blue",
            "flame:flame-orange",
            "kobra:kobra-hp",
            "kobra:kobra-lp",
            "ironlak:ironlak-400-ml",
            "ironlak:sugar-artists-acrylic",
            "nbq:nbq-fast",
            "nbq:nbq-slow",
            "dope:dope-action",
            "dope:dope-classic",
            "dang:dang-prime",
            "dang:dang-hi-flow",
            "clash:clash",
            "beat:beat",
            "scribo:scribo",
            "double-a:double-a",
            "krink:krink-k-750",
        )

        mappedLineIds.forEach { lineId ->
            assertNotNull("Dosenbild fehlt für $lineId", canArtworkRes(lineId))
        }
    }
}
