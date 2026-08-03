"""Build deterministic Android icon assets from the approved transparent mark."""

from pathlib import Path
import argparse

from PIL import Image


def fit_mark(source: Image.Image, canvas_size: int, subject_height_ratio: float) -> Image.Image:
    alpha = source.getchannel("A")
    bounds = alpha.getbbox()
    if bounds is None:
        raise ValueError("The source mark has no visible pixels")

    cropped = source.crop(bounds)
    target_height = round(canvas_size * subject_height_ratio)
    scale = target_height / cropped.height
    target_width = round(cropped.width * scale)
    resized = cropped.resize((target_width, target_height), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    left = (canvas_size - target_width) // 2
    top = (canvas_size - target_height) // 2
    canvas.alpha_composite(resized, (left, top))
    return canvas


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--resource-dir", required=True, type=Path)
    args = parser.parse_args()

    source = Image.open(args.input).convert("RGBA")
    args.resource_dir.mkdir(parents=True, exist_ok=True)

    launcher = fit_mark(source, canvas_size=864, subject_height_ratio=0.62)
    launcher.save(args.resource_dir / "canvault_icon_mark.png", optimize=True)

    in_app = fit_mark(source, canvas_size=512, subject_height_ratio=0.84)
    in_app.save(args.resource_dir / "canvault_logo.png", optimize=True)


if __name__ == "__main__":
    main()
