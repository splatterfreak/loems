#!/usr/bin/env python3
"""Apply the approved fixed-scale left-to-right melon bites to every feeding sheet."""

from __future__ import annotations

import shutil
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
PREVIEWS = ROOT / "art" / "previews"
BACKUPS = ROOT / "art" / "intermediate" / "melon_sheets_before_progressive_standard"
CELL = 512


@dataclass(frozen=True)
class MelonConfig:
    filename: str
    anchor_right: int
    anchor_bottom: int
    target_width: int
    contact_right: int | None = None
    contact_bottom: int | None = None

    @property
    def prop_right(self) -> int:
        return self.contact_right if self.contact_right is not None else self.anchor_right

    @property
    def prop_bottom(self) -> int:
        return self.contact_bottom if self.contact_bottom is not None else self.anchor_bottom


CONFIGS = (
    MelonConfig("loem_melon_sheet.webp", 420, 360, 100, 450, 324),
    MelonConfig("loem_armageddon_serpent_female_melon_sheet.webp", 460, 313, 95),
    MelonConfig("loem_armageddon_serpent_male_melon_sheet.webp", 458, 304, 95),
    MelonConfig("loem_bad_melon_sheet.webp", 478, 408, 120, 484, 310),
    MelonConfig("loem_gloom_wizard_poop_female_melon_sheet.webp", 442, 300, 92, 422, 385),
    MelonConfig("loem_gloom_wizard_poop_male_melon_sheet.webp", 453, 325, 92, 422, 385),
    MelonConfig("loem_mud_toad_melon_sheet.webp", 461, 372, 96, 466, 352),
    MelonConfig("loem_poop_melon_sheet.webp", 464, 427, 110, 480, 370),
    MelonConfig("loem_serpent_melon_sheet.webp", 464, 382, 100, 475, 319),
    MelonConfig("loem_space_rift_archmage_poop_melon_sheet.webp", 420, 240, 78),
    MelonConfig("loem_space_rift_urtoad_melon_sheet.webp", 452, 380, 82),
    MelonConfig("loem_space_rift_world_serpent_melon_sheet.webp", 405, 334, 90),
    MelonConfig("loem_stormkaiser_female_melon_sheet.webp", 433, 350, 110, 484, 370),
    MelonConfig("loem_stormkaiser_melon_sheet.webp", 428, 393, 110, 484, 370),
    MelonConfig("loem_ultra_cosmic_melon_sheet.webp", 407, 455, 72, 447, 308),
    MelonConfig("loem_wart_emperor_female_melon_sheet.webp", 455, 374, 98),
    MelonConfig("loem_wart_emperor_male_melon_sheet.webp", 463, 374, 98),
    MelonConfig("loem_wing_evolution_melon_sheet.webp", 441, 424, 128, 484, 342),
)


def frame(sheet: Image.Image, index: int, columns: int) -> Image.Image:
    return sheet.crop(
        (
            (index % columns) * CELL,
            (index // columns) * CELL,
            (index % columns + 1) * CELL,
            (index // columns + 1) * CELL,
        )
    )


def resized_props(target_width: int) -> list[Image.Image]:
    source = [
        Image.open(PREVIEWS / f"loem_good_melon_bite_{index}.webp").convert("RGBA")
        for index in range(5)
    ]
    scale = target_width / source[0].width
    return [
        prop.resize(
            (max(1, round(prop.width * scale)), max(1, round(prop.height * scale))),
            Image.Resampling.LANCZOS,
        )
        for prop in source
    ]


def cleanup_mask(current: Image.Image, config: MelonConfig) -> Image.Image:
    left = max(0, config.anchor_right - config.target_width - 30)
    top = max(0, config.anchor_bottom - round(config.target_width * 1.18) - 20)
    right = min(CELL, max(config.anchor_right + 18, 490))
    bottom = min(CELL, config.anchor_bottom + 18)
    source = current.load()
    mask = Image.new("L", current.size)
    target = mask.load()
    for y in range(top, bottom):
        for x in range(left, right):
            red, green, blue, alpha = source[x, y]
            flesh = red >= 100 and red >= green * 1.18 and red >= blue * 1.35
            rind = green >= 58 and green >= red * 0.45 and green >= blue * 1.10
            if alpha > 20 and (flesh or rind):
                target[x, y] = 255
    mask = mask.filter(ImageFilter.MaxFilter(9)).filter(ImageFilter.GaussianBlur(0.55))
    bounds = Image.new("L", current.size)
    ImageDraw.Draw(bounds).rectangle((left, top, right - 1, bottom - 1), fill=255)
    return Image.composite(mask, Image.new("L", current.size), bounds)


def complete_prop_region(config: MelonConfig) -> Image.Image:
    left = max(0, config.anchor_right - config.target_width - 75)
    top = max(0, config.anchor_bottom - round(config.target_width * 2.10) - 30)
    right = min(CELL, config.anchor_right + 35)
    bottom = min(CELL, config.anchor_bottom + 35)
    mask = Image.new("L", (CELL, CELL))
    ImageDraw.Draw(mask).rounded_rectangle((left, top, right, bottom), radius=24, fill=255)
    return mask.filter(ImageFilter.GaussianBlur(1.2))


def stages(frame_count: int) -> tuple[int | None, ...]:
    if frame_count == 6:
        return (0, 1, 2, 3, 4, None)
    if frame_count == 12:
        return (0, 0, 1, 1, 2, 2, 3, 3, 4, 4, None, None)
    raise ValueError(f"Unsupported feeding frame count: {frame_count}")


def standardize(config: MelonConfig) -> None:
    path = DRAWABLE / config.filename
    BACKUPS.mkdir(parents=True, exist_ok=True)
    backup = BACKUPS / config.filename
    if not backup.exists():
        shutil.copy2(path, backup)
    # Always rebuild from the untouched pre-standardization sheet. Besides
    # preventing stacked props, this preserves the original jaw keyframes for
    # the dedicated anatomical mouth-cycle pass.
    source = Image.open(backup).convert("RGBA")
    if source.size == (CELL * 3, CELL * 2):
        columns, rows = 3, 2
    elif source.size == (CELL * 4, CELL * 3):
        columns, rows = 4, 3
    else:
        raise ValueError(f"Unsupported sheet geometry: {path} {source.size}")

    frame_count = columns * rows
    hungry_name = (
        "loem_hungry_sheet.webp"
        if config.filename == "loem_melon_sheet.webp"
        else config.filename.replace("_melon_sheet.webp", "_hungry_sheet.webp")
    )
    hungry = Image.open(DRAWABLE / hungry_name).convert("RGBA")
    hungry_columns = 4 if hungry.size == (CELL * 4, CELL * 3) else 3
    final_clean = (
        frame(source, frame_count - 1, columns)
        if config.filename == "loem_bad_melon_sheet.webp"
        else frame(hungry, 0, hungry_columns)
    )
    props = resized_props(config.target_width)
    result = Image.new("RGBA", source.size)
    for index, stage in enumerate(stages(frame_count)):
        current = frame(source, index, columns)
        cleanup = cleanup_mask(current, config)
        if config.filename != "loem_bad_melon_sheet.webp":
            cleanup = ImageChops.lighter(cleanup, complete_prop_region(config))
        current = Image.composite(final_clean, current, cleanup)
        if stage is not None:
            prop = props[stage]
            current.alpha_composite(
                prop,
                (config.prop_right - prop.width, config.prop_bottom - prop.height),
            )
        result.alpha_composite(current, ((index % columns) * CELL, (index // columns) * CELL))
    result.save(path, "WEBP", lossless=True, quality=100, method=6)
    print(f"standardized {config.filename}: {columns}x{rows}, width={config.target_width}")


def main() -> None:
    for config in CONFIGS:
        standardize(config)


if __name__ == "__main__":
    main()
