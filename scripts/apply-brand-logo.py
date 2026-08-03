"""Apply the approved CANVAULT logo to Android, iOS, Expo and repository assets."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image


def fit(source: Image.Image, size: int, subject_ratio: float) -> Image.Image:
    alpha = source.getchannel("A")
    bounds = alpha.getbbox()
    if bounds is None:
        raise ValueError("Logo contains no visible pixels")
    cropped = source.crop(bounds)
    target_height = round(size * subject_ratio)
    scale = target_height / cropped.height
    resized = cropped.resize((round(cropped.width * scale), target_height), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.alpha_composite(resized, ((size - resized.width) // 2, (size - resized.height) // 2))
    return canvas


def write_json(path: Path, payload: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--project-root", required=True, type=Path)
    args = parser.parse_args()

    project = args.project_root.resolve()
    source = Image.open(args.input).convert("RGBA")

    android = project / "native-android" / "app" / "src" / "main" / "res" / "drawable-nodpi"
    android.mkdir(parents=True, exist_ok=True)
    fit(source, 864, 0.62).save(android / "canvault_icon_mark.png", "PNG", optimize=True)
    fit(source, 512, 0.84).save(android / "canvault_logo.png", "PNG", optimize=True)

    brand = project / "assets" / "brand"
    brand.mkdir(parents=True, exist_ok=True)
    project_icon = fit(source, 1024, 0.84)
    project_icon.save(brand / "canvault-icon.png", "PNG", optimize=True)
    source.save(brand / "canvault-logo-source.png", "PNG", optimize=True)

    shared_images = project / "assets" / "images"
    shared_images.mkdir(parents=True, exist_ok=True)
    project_icon.save(shared_images / "icon.png", "PNG", optimize=True)
    fit(source, 1024, 0.70).save(shared_images / "splash-icon.png", "PNG", optimize=True)
    fit(source, 432, 0.62).save(shared_images / "android-icon-foreground.png", "PNG", optimize=True)
    fit(source, 64, 0.84).save(shared_images / "favicon.png", "PNG", optimize=True)

    ios_assets = project / "native-ios" / "CANVAULT" / "Assets.xcassets"
    app_icon = ios_assets / "AppIcon.appiconset"
    app_icon.mkdir(parents=True, exist_ok=True)
    opaque = Image.new("RGB", source.size, "white")
    opaque.paste(source, mask=source.getchannel("A"))
    opaque = opaque.resize((1024, 1024), Image.Resampling.LANCZOS)
    opaque.save(app_icon / "AppIcon-1024.png", "PNG", optimize=True)
    write_json(app_icon / "Contents.json", {
        "images": [{"filename": "AppIcon-1024.png", "idiom": "universal", "platform": "ios", "size": "1024x1024"}],
        "info": {"author": "xcode", "version": 1},
    })
    app_logo = ios_assets / "AppLogo.imageset"
    app_logo.mkdir(parents=True, exist_ok=True)
    fit(source, 512, 0.84).save(app_logo / "AppLogo.png", "PNG", optimize=True)
    write_json(app_logo / "Contents.json", {
        "images": [{"filename": "AppLogo.png", "idiom": "universal", "scale": "1x"}],
        "info": {"author": "xcode", "version": 1},
    })

    print("Applied the new CANVAULT logo to Android, iOS and shared project assets.")


if __name__ == "__main__":
    main()
