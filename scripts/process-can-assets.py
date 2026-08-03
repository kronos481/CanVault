from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageChops, ImageOps


CAN_FILES = {
    "Beat/Beat_400.png": "can_beat_400.webp",
    "Clash/Clash_400.png": "can_clash_400.webp",
    "Dang/Dang_HiFlow.png": "can_dang_hi_flow.webp",
    "Dang/Dang_Prime.png": "can_dang_prime.webp",
    "Dope/Dope_Action.png": "can_dope_action.webp",
    "Dope/Dope_Classic.png": "can_dope_classic.webp",
    "DoubleA/DoubleA_400.png": "can_double_a_400.webp",
    "Flame/Flame_Blue.png": "can_flame_blue.webp",
    "Flame/Flame_Orange.png": "can_flame_orange.webp",
    "Ironlak/Ironlak_400.png": "can_ironlak_400.webp",
    "Ironlak/Ironlak_Sugar.png": "can_ironlak_sugar.webp",
    "Kobra/Kobra_Hp.png": "can_kobra_hp.webp",
    "Kobra/Kobra_Lp.png": "can_kobra_lp.webp",
    "Krink/Krink_750.png": "can_krink_750.webp",
    "Loop/Loop_400.png": "can_loop_400.webp",
    "Loop/Loop_Asphalt.png": "can_loop_asphalt.webp",
    "Molotow/Molotow_Burner.png": "can_molotow_burner.webp",
    "Molotow/Molotow_Coversall.png": "can_molotow_coversall.webp",
    "Molotow/Molotow_Premium.png": "can_molotow_premium.webp",
    "Montana_Cans/Montana_Black.png": "can_montana_black.webp",
    "Montana_Cans/Montana_Blackout_Tarblack.png": "can_montana_blackout_tarblack.webp",
    "Montana_Cans/Montana_Gold.png": "can_montana_gold.webp",
    "Montana_Cans/Montana_Tarblack.png": "can_montana_tarblack.webp",
    "Montana_Cans/Montana_Ultrawide.png": "can_montana_ultrawide.webp",
    "Montana_Cans/Montana_White.png": "can_montana_white.webp",
    "MTN/MTN_94.png": "can_mtn_94.webp",
    "MTN/MTN_Alien.png": "can_mtn_alien.webp",
    "MTN/MTN_Hardcore.png": "can_mtn_hardcore.webp",
    "MTN/MTN_Vice.png": "can_mtn_vice.webp",
    "MTN/MTN_WaterBased.png": "can_mtn_water_based.webp",
    "NBQ/NBQ_Fast.png": "can_nbq_fast.webp",
    "NBQ/NBQ_Slow.png": "can_nbq_slow.webp",
    "Scribo/Scribo_400.png": "can_scribo_400.webp",
}

LOGO_FILES = {
    "beat.png": "brand_beat.png",
    "clash.png": "brand_clash.png",
    "dang.png": "brand_dang.png",
    "dope.png": "brand_dope.png",
    "double-a.png": "brand_double_a.png",
    "flame.png": "brand_flame.png",
    "ironlak.png": "brand_ironlak.png",
    "kobra.png": "brand_kobra.png",
    "krink.png": "brand_krink.png",
    "loop.png": "brand_loop.png",
    "molotow.png": "brand_molotow.png",
    "montana-cans.png": "brand_montana_cans.png",
    "mtn.png": "brand_mtn.png",
    "nbq.png": "brand_nbq.png",
    "scribo.png": "brand_scribo.png",
}


def alpha_bbox(image: Image.Image) -> tuple[int, int, int, int]:
    bbox = image.getchannel("A").getbbox()
    if bbox is None:
        raise ValueError("Asset enthält keine sichtbaren Pixel.")
    return bbox


def process_can(source: Path, destination: Path) -> None:
    image = Image.open(source).convert("RGBA")
    image = image.crop(alpha_bbox(image))
    canvas_size = (512, 768)
    padding = 24
    scale = min(
        (canvas_size[0] - padding * 2) / image.width,
        (canvas_size[1] - padding * 2) / image.height,
    )
    resized = image.resize(
        (max(1, round(image.width * scale)), max(1, round(image.height * scale))),
        Image.Resampling.LANCZOS,
    )
    canvas = Image.new("RGBA", canvas_size, (0, 0, 0, 0))
    position = (
        (canvas_size[0] - resized.width) // 2,
        (canvas_size[1] - resized.height) // 2,
    )
    canvas.alpha_composite(resized, position)
    canvas.save(destination, "WEBP", quality=90, method=6)


def process_logo(source: Path, destination: Path) -> None:
    image = Image.open(source).convert("RGBA")
    image = image.crop(alpha_bbox(image))
    if source.name.lower() == "dang.png":
        # The original uses a solid pink badge behind cream lettering. Turning every
        # visible pixel white would create an unreadable white disc, so keep only the
        # light wordmark and its antialiased edges for the monochrome app variant.
        luminance = ImageOps.grayscale(image)
        light_wordmark = luminance.point(
            lambda value: max(0, min(255, round((value - 135) * 255 / 75))),
        )
        image.putalpha(ImageChops.multiply(image.getchannel("A"), light_wordmark))
        image = image.crop(alpha_bbox(image))
    scale = min(1.0, 640 / image.width, 240 / image.height)
    if scale < 1.0:
        image = image.resize(
            (max(1, round(image.width * scale)), max(1, round(image.height * scale))),
            Image.Resampling.LANCZOS,
        )
    alpha = image.getchannel("A")
    white = Image.new("RGBA", image.size, (255, 255, 255, 0))
    white.putalpha(alpha)
    white.save(destination, "PNG", optimize=True)


def main() -> None:
    parser = argparse.ArgumentParser(description="Prepare CANVAULT Android product assets.")
    parser.add_argument("input_root", type=Path)
    parser.add_argument("output_root", type=Path)
    args = parser.parse_args()
    args.output_root.mkdir(parents=True, exist_ok=True)

    for relative_path, output_name in CAN_FILES.items():
        process_can(args.input_root / relative_path, args.output_root / output_name)
    logo_root = args.input_root / "_BRANDLOGOS"
    for input_name, output_name in LOGO_FILES.items():
        process_logo(logo_root / input_name, args.output_root / output_name)

    print(f"Processed {len(CAN_FILES)} cans and {len(LOGO_FILES)} logos into {args.output_root}")


if __name__ == "__main__":
    main()
