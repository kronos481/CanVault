package com.canvault.app.ui.assets

import androidx.annotation.DrawableRes
import com.canvault.app.R

@DrawableRes
fun brandLogoRes(brandId: String): Int? = when (brandId) {
    "mtn-montana-colors" -> R.drawable.brand_mtn
    "montana-cans" -> R.drawable.brand_montana_cans
    "molotow-belton" -> R.drawable.brand_molotow
    "loop-colors" -> R.drawable.brand_loop
    "flame" -> R.drawable.brand_flame
    "kobra" -> R.drawable.brand_kobra
    "ironlak" -> R.drawable.brand_ironlak
    "nbq" -> R.drawable.brand_nbq
    "dope" -> R.drawable.brand_dope
    "dang" -> R.drawable.brand_dang
    "clash" -> R.drawable.brand_clash
    "beat" -> R.drawable.brand_beat
    "scribo" -> R.drawable.brand_scribo
    "double-a" -> R.drawable.brand_double_a
    "krink" -> R.drawable.brand_krink
    else -> null
}

@DrawableRes
fun canArtworkRes(lineId: String): Int? = when (lineId) {
    "mtn-montana-colors:mtn-94" -> R.drawable.can_mtn_94
    "mtn-montana-colors:mtn-hardcore" -> R.drawable.can_mtn_hardcore
    "mtn-montana-colors:mtn-vice" -> R.drawable.can_mtn_vice
    "mtn-montana-colors:mtn-water-based-400" -> R.drawable.can_mtn_water_based
    "mtn-montana-colors:mtn-alien" -> R.drawable.can_mtn_alien

    "montana-cans:montana-black" -> R.drawable.can_montana_black
    "montana-cans:montana-gold" -> R.drawable.can_montana_gold
    "montana-cans:montana-white" -> R.drawable.can_montana_white
    "montana-cans:montana-tarblack" -> R.drawable.can_montana_tarblack
    "montana-cans:montana-blackout-tarblack" -> R.drawable.can_montana_blackout_tarblack
    "montana-cans:montana-ultra-wide" -> R.drawable.can_montana_ultrawide

    "molotow-belton:molotow-premium" -> R.drawable.can_molotow_premium
    "molotow-belton:molotow-burner",
    "molotow-belton:burner-chrome-600-ml",
    "molotow-belton:burner-gold-600-ml",
    "molotow-belton:burner-copper-600-ml",
    "molotow-belton:burner-black-600-ml",
    -> R.drawable.can_molotow_burner
    "molotow-belton:molotow-coversall" -> R.drawable.can_molotow_coversall

    "loop-colors:loop-400-ml" -> R.drawable.can_loop_400
    "loop-colors:loop-asphalt" -> R.drawable.can_loop_asphalt
    "flame:flame-blue" -> R.drawable.can_flame_blue
    "flame:flame-orange" -> R.drawable.can_flame_orange
    "kobra:kobra-hp" -> R.drawable.can_kobra_hp
    "kobra:kobra-lp" -> R.drawable.can_kobra_lp
    "ironlak:ironlak-400-ml" -> R.drawable.can_ironlak_400
    "ironlak:sugar-artists-acrylic" -> R.drawable.can_ironlak_sugar
    "nbq:nbq-fast" -> R.drawable.can_nbq_fast
    "nbq:nbq-slow" -> R.drawable.can_nbq_slow
    "dope:dope-action" -> R.drawable.can_dope_action
    "dope:dope-classic" -> R.drawable.can_dope_classic
    "dang:dang-prime" -> R.drawable.can_dang_prime
    "dang:dang-hi-flow" -> R.drawable.can_dang_hi_flow
    "clash:clash" -> R.drawable.can_clash_400
    "beat:beat" -> R.drawable.can_beat_400
    "scribo:scribo" -> R.drawable.can_scribo_400
    "double-a:double-a" -> R.drawable.can_double_a_400
    "krink:krink-k-750" -> R.drawable.can_krink_750
    else -> null
}
