from __future__ import annotations

import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "native-android" / "app" / "src" / "main" / "res" / "drawable-nodpi"
ASSETS = ROOT / "native-ios" / "CANVAULT" / "Assets.xcassets"
ICON_SOURCE = ROOT / "assets" / "brand" / "canvault-logo-source.png"


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def imageset(name: str, image: Image.Image) -> None:
    folder = ASSETS / f"{name}.imageset"
    folder.mkdir(parents=True, exist_ok=True)
    filename = f"{name}.png"
    image.save(folder / filename, "PNG", optimize=True)
    write_json(folder / "Contents.json", {
        "images": [{"filename": filename, "idiom": "universal", "scale": "1x"}],
        "info": {"author": "xcode", "version": 1},
    })


ASSETS.mkdir(parents=True, exist_ok=True)
write_json(ASSETS / "Contents.json", {"info": {"author": "xcode", "version": 1}})

for source in sorted(SOURCE.iterdir()):
    if source.suffix.lower() not in {".png", ".webp"}:
        continue
    if source.stem in {"canvault_logo", "canvault_icon_mark"}:
        continue
    with Image.open(source) as image:
        imageset(source.stem, image.convert("RGBA"))

app_icon_source = ICON_SOURCE if ICON_SOURCE.exists() else SOURCE / "canvault_logo.png"
with Image.open(app_icon_source) as image:
    icon = image.convert("RGB").resize((1024, 1024), Image.Resampling.LANCZOS)
    icon_folder = ASSETS / "AppIcon.appiconset"
    icon_folder.mkdir(parents=True, exist_ok=True)
    icon.save(icon_folder / "AppIcon-1024.png", "PNG", optimize=True)
    write_json(icon_folder / "Contents.json", {
        "images": [{"filename": "AppIcon-1024.png", "idiom": "universal", "platform": "ios", "size": "1024x1024"}],
        "info": {"author": "xcode", "version": 1},
    })
    imageset("AppLogo", icon.resize((512, 512), Image.Resampling.LANCZOS).convert("RGBA"))

write_json(ASSETS / "AccentColor.colorset" / "Contents.json", {
    "colors": [{
        "color": {"color-space": "srgb", "components": {"alpha": "1.000", "blue": "0.761", "green": "0.894", "red": "0.345"}},
        "idiom": "universal",
    }],
    "info": {"author": "xcode", "version": 1},
})
write_json(ASSETS / "LaunchBackground.colorset" / "Contents.json", {
    "colors": [{
        "color": {"color-space": "srgb", "components": {"alpha": "1.000", "blue": "0.055", "green": "0.043", "red": "0.035"}},
        "idiom": "universal",
    }],
    "info": {"author": "xcode", "version": 1},
})

print(f"Created iOS asset catalog at {ASSETS}")
