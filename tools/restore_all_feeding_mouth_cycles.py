#!/usr/bin/env python3
"""Rebuild every feeding sheet from isolated full-body open/closed key poses.

Each output cell starts from one locked, food-free full-body base. Only the
elliptical articulated mouth region differs between open and closed key poses;
the approved progressive food prop is then added once. This deliberately
discards legacy rectangular face patches, duplicate anatomy, and stale pixels.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageStat

import standardize_all_ham_feeding as ham
import standardize_all_melon_feeding as melon


ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
PREVIEWS = ROOT / "art" / "previews"
HAM_BACKUPS = ROOT / "art" / "intermediate" / "ham_sheets_before_fixed_bone_standard"
MELON_BACKUPS = ROOT / "art" / "intermediate" / "melon_sheets_before_progressive_standard"
CELL = 512


@dataclass(frozen=True)
class MouthConfig:
    prefix: str
    box: tuple[int, int, int, int]
    hungry_name: str | None = None
    closed_name: str | None = None
    open_from_backup: bool = False
    closed_from_backup: bool = False
    open_name: str | None = None
    body_clip_right: int = CELL
    mask_polygon: tuple[tuple[int, int], ...] | None = None
    open_offset: tuple[int, int] = (0, 0)
    open_frame_index: int | None = None
    closed_frame_index: int | None = None
    closed_patch_name: str | None = None
    closed_patch_origin: tuple[int, int] = (0, 0)
    closed_patch_polygon: tuple[tuple[int, int], ...] | None = None
    closed_patch_dark_box: tuple[int, int, int, int] | None = None
    frame_offset: tuple[int, int] = (0, 0)
    # Per frame: x/y movement plus local face width/height scale.
    head_motion: tuple[tuple[float, float, float, float], ...] | None = None
    head_motion_region: tuple[int, int, int, int] | None = None
    pose_sequence: tuple[str, ...] | None = None


# Boxes include lips, muzzle outline and the real lower jaw, but deliberately
# stop before the held food wherever possible. They are sprite-local 512px
# coordinates and are shared by ham and melon for the same anatomy.
CONFIGS = (
    MouthConfig(
        "loem",
        (275, 260, 491, 406),
        "loem_hungry_sheet.webp",
        mask_polygon=((280, 265), (478, 260), (490, 312), (465, 385), (355, 405), (295, 365), (275, 310)),
        closed_frame_index=0,
        closed_patch_name="loem_closed_mouth_patch_v2.png",
        closed_patch_origin=(0, 0),
        closed_patch_polygon=((279, 265), (479, 260), (491, 312), (466, 386), (355, 406), (294, 365), (274, 310)),
        pose_sequence=("open", "closed", "open", "closed", "open", "closed"),
        frame_offset=(-32, 0),
    ),
    MouthConfig(
        "loem_bad",
        (330, 250, 425, 355),
        closed_from_backup=True,
        open_name="loem_bad_open_mouth_canonical.webp",
    ),
    # Good already uses its approved hand-drawn open/closed canonical poses.
    MouthConfig("loem_poop", (285, 320, 390, 405), closed_name="loem_poop_evolution_idle_sheet.webp", open_from_backup=True),
    MouthConfig("loem_serpent", (330, 235, 405, 350), closed_name="loem_serpent_evolution_idle_sheet.webp", open_from_backup=True),
    # The legacy hungry sheet contains rectangular damage and is forbidden as
    # a source. A tight lower-mouth mask keeps both eyes and the rear wing from
    # the single clean idle body while still articulating teeth and lower jaw.
    MouthConfig(
        "loem_wing_evolution",
        (270, 292, 365, 352),
        hungry_name="loem_wing_evolution_idle_sheet.webp",
        body_clip_right=405,
        mask_polygon=((270, 290), (365, 290), (375, 315), (360, 355), (285, 360), (265, 325)),
        open_frame_index=0,
        closed_frame_index=0,
        closed_patch_name="loem_wing_evolution_closed_mouth_generated.png",
        closed_patch_origin=(199, 166),
        closed_patch_polygon=((270, 290), (365, 290), (375, 315), (360, 355), (285, 360), (265, 325)),
        pose_sequence=("open", "closed", "open", "closed", "open", "closed"),
    ),
    MouthConfig("loem_mud_toad", (295, 310, 395, 380)),
    MouthConfig("loem_armageddon_serpent_male", (360, 230, 455, 325)),
    MouthConfig("loem_armageddon_serpent_female", (360, 230, 455, 325)),
    MouthConfig(
        "loem_stormkaiser",
        (315, 275, 425, 350),
        mask_polygon=((320, 275), (410, 270), (425, 315), (390, 345), (340, 350), (315, 310)),
        closed_frame_index=0,
        closed_patch_name="loem_stormkaiser_closed_mouth_patch.png",
        closed_patch_origin=(0, 0),
        closed_patch_polygon=((314, 250), (430, 250), (438, 307), (326, 324), (297, 277)),
        pose_sequence=("open", "closed", "open", "closed", "open", "closed"),
    ),
    MouthConfig(
        "loem_stormkaiser_female",
        (320, 275, 425, 345),
        hungry_name="loem_stormkaiser_hungry_sheet.webp",
        mask_polygon=((325, 280), (415, 275), (425, 320), (390, 345), (340, 345), (320, 310)),
        open_offset=(-10, 0),
        closed_frame_index=0,
        closed_patch_name="loem_stormkaiser_closed_mouth_patch.png",
        closed_patch_origin=(0, 0),
        closed_patch_polygon=((327, 254), (415, 254), (424, 307), (323, 323), (304, 279)),
        pose_sequence=("open", "closed", "open", "closed", "open", "closed"),
    ),
    MouthConfig("loem_wart_emperor_male", (290, 280, 455, 455)),
    MouthConfig("loem_wart_emperor_female", (290, 280, 455, 455)),
    MouthConfig("loem_gloom_wizard_poop_male", (205, 300, 345, 395)),
    MouthConfig("loem_gloom_wizard_poop_female", (205, 300, 345, 395)),
    MouthConfig("loem_space_rift_urtoad", (145, 235, 375, 397)),
    MouthConfig("loem_space_rift_world_serpent", (225, 185, 458, 378)),
    MouthConfig("loem_space_rift_archmage_poop", (165, 155, 472, 355)),
    MouthConfig("loem_ultra_cosmic", (204, 194, 444, 348)),
)


def geometry(image: Image.Image) -> tuple[int, int, int]:
    if image.size == (1536, 1024):
        return 3, 2, 6
    if image.size == (2048, 1536):
        return 4, 3, 12
    raise ValueError(f"Unsupported feeding sheet geometry: {image.size}")


def get_frame(sheet: Image.Image, index: int, columns: int) -> Image.Image:
    x = (index % columns) * CELL
    y = (index // columns) * CELL
    return sheet.crop((x, y, x + CELL, y + CELL))


def clean_transparency(image: Image.Image) -> Image.Image:
    """Make fully transparent pixels carry no hidden previous-frame RGB."""
    rgba = image.convert("RGBA")
    red, green, blue, alpha = rgba.split()
    empty = Image.new("L", rgba.size)
    # Use alpha only as a binary selector. Multiplying RGB by a partial alpha
    # would premultiply it a second time and create dark/vanishing edge pixels.
    visible = alpha.point(lambda value: 255 if value else 0)
    red = Image.composite(red, empty, visible)
    green = Image.composite(green, empty, visible)
    blue = Image.composite(blue, empty, visible)
    return Image.merge("RGBA", (red, green, blue, alpha))


def clip_body_source(image: Image.Image, config: MouthConfig) -> Image.Image:
    """Remove known neighboring-cell remnants before adding the food prop."""
    if config.body_clip_right >= CELL:
        return image
    clipped = image.copy()
    clipped.paste((0, 0, 0, 0), (config.body_clip_right, 0, CELL, CELL))
    return clipped


def align_open_source(image: Image.Image, config: MouthConfig) -> Image.Image:
    """Translate the alternate mouth pose onto the locked base head anchor."""
    offset_x, offset_y = config.open_offset
    if offset_x == 0 and offset_y == 0:
        return image
    aligned = Image.new("RGBA", (CELL, CELL))
    aligned.alpha_composite(image, (offset_x, offset_y))
    return aligned


def move_head_and_upper_body(
    image: Image.Image,
    offset_x: float,
    offset_y: float,
    scale_x: float,
    scale_y: float,
    region: tuple[int, int, int, int],
) -> Image.Image:
    """Articulate the flattened face/head as one continuous snack pose.

    The smooth elliptical falloff keeps the motion inside the face, jaw and
    nearby cheek/neck pixels. Because the complete flattened frame is warped
    once, no second mouth, tongue, eye or head can remain underneath it.
    """
    if offset_x == 0 and offset_y == 0 and scale_x == 1 and scale_y == 1:
        return image

    center_x, center_y, radius_x, radius_y = region

    def weight(x: float, y: float) -> float:
        normalized = ((x - center_x) / radius_x) ** 2 + ((y - center_y) / radius_y) ** 2
        if normalized >= 1.0:
            return 0.0
        # Smoothstep falloff avoids a visible seam where the moving face joins
        # the locked horns, wings, torso and feet.
        linear = 1.0 - normalized
        return linear * linear * (3.0 - 2.0 * linear)

    mesh = []
    grid = 16
    for top in range(0, CELL, grid):
        bottom = min(CELL, top + grid)
        for left in range(0, CELL, grid):
            right = min(CELL, left + grid)

            def source(x: int, y: int) -> tuple[float, float]:
                amount = weight(x, y)
                scaled_x = center_x + (x - center_x) / scale_x
                scaled_y = center_y + (y - center_y) / scale_y
                return (
                    x + (scaled_x - x - offset_x) * amount,
                    y + (scaled_y - y - offset_y) * amount,
                )

            top_left = source(left, top)
            bottom_left = source(left, bottom)
            bottom_right = source(right, bottom)
            top_right = source(right, top)
            mesh.append(
                (
                    (left, top, right, bottom),
                    (*top_left, *bottom_left, *bottom_right, *top_right),
                ),
            )
    return image.transform(
        image.size,
        Image.Transform.MESH,
        mesh,
        resample=Image.Resampling.BICUBIC,
    )


def mouth_mask(config: MouthConfig) -> Image.Image:
    mask = Image.new("L", (CELL, CELL))
    draw = ImageDraw.Draw(mask)
    if config.mask_polygon:
        draw.polygon(config.mask_polygon, fill=255)
    else:
        draw.ellipse(config.box, fill=255)
    return mask.filter(ImageFilter.GaussianBlur(1.25))


def mouth_open_score(candidate: Image.Image, box: tuple[int, int, int, int]) -> float:
    """Prefer a large dark/red oral cavity, not mere whole-head movement."""
    crop = candidate.crop(box).convert("RGBA")
    score = 0.0
    for red, green, blue, alpha in crop.get_flattened_data():
        if alpha < 24:
            continue
        brightness = (red + green + blue) / 3
        dark_cavity = brightness < 82
        tongue = red > 105 and red > green * 1.35 and red > blue * 1.18
        if dark_cavity:
            score += 1.0
        if tongue:
            score += 1.7
    return score


def best_open_pose(config: MouthConfig) -> Image.Image:
    if config.open_name:
        return Image.open(PREVIEWS / config.open_name).convert("RGBA")
    if config.open_frame_index is not None:
        source_name = config.hungry_name or f"{config.prefix}_hungry_sheet.webp"
        source = Image.open(DRAWABLE / source_name).convert("RGBA")
        columns, _, count = geometry(source)
        if not 0 <= config.open_frame_index < count:
            raise ValueError(f"Invalid open frame for {config.prefix}: {config.open_frame_index}")
        return get_frame(source, config.open_frame_index, columns)
    if config.open_from_backup:
        name = "loem_feeding_sheet.webp" if config.prefix == "loem" else f"{config.prefix}_ham_sheet.webp"
        source = Image.open(HAM_BACKUPS / name).convert("RGBA")
        columns, _, count = geometry(source)
        candidates = [get_frame(source, index, columns) for index in range(count - 1)]
        opened = max(candidates, key=lambda item: mouth_open_score(item, config.box))
        clean = best_closed_pose(config)
        erase = Image.new("L", (CELL, CELL))
        left = max(0, config.box[2] - 24)
        ImageDraw.Draw(erase).rounded_rectangle(
            (left, max(0, config.box[1] - 20), CELL, min(CELL, config.box[3] + 80)),
            radius=18,
            fill=255,
        )
        erase = erase.filter(ImageFilter.GaussianBlur(1.0))
        return Image.composite(clean, opened, erase)
    hungry_name = config.hungry_name or f"{config.prefix}_hungry_sheet.webp"
    source = Image.open(DRAWABLE / hungry_name).convert("RGBA")
    columns, _, count = geometry(source)
    candidates = [get_frame(source, index, columns) for index in range(count)]
    return max(candidates, key=lambda item: mouth_open_score(item, config.box))


def best_closed_pose(config: MouthConfig) -> Image.Image:
    if config.closed_frame_index is not None:
        name = config.closed_name or ("loem_idle_sheet.webp" if config.prefix == "loem" else f"{config.prefix}_idle_sheet.webp")
        source = Image.open(DRAWABLE / name).convert("RGBA")
        columns, _, count = geometry(source)
        if not 0 <= config.closed_frame_index < count:
            raise ValueError(f"Invalid closed frame for {config.prefix}: {config.closed_frame_index}")
        pose = get_frame(source, config.closed_frame_index, columns)
        if config.closed_patch_name:
            patch = Image.open(ROOT / "art" / "intermediate" / config.closed_patch_name).convert("RGBA")
            layer = pose.copy()
            layer.alpha_composite(patch, config.closed_patch_origin)
            mask = Image.new("L", (CELL, CELL))
            if config.closed_patch_dark_box:
                left, top, right, bottom = config.closed_patch_dark_box
                source_pixels = pose.load()
                mask_pixels = mask.load()
                for y in range(top, bottom):
                    for x in range(left, right):
                        red, green, blue, alpha = source_pixels[x, y]
                        brightness = (red + green + blue) / 3
                        dark_mouth = brightness < 115
                        light_tooth = min(red, green, blue) > 190
                        if alpha > 16 and (dark_mouth or light_tooth):
                            mask_pixels[x, y] = 255
                mask = mask.filter(ImageFilter.MaxFilter(19)).filter(ImageFilter.GaussianBlur(1.5))
                if config.closed_patch_polygon:
                    boundary = Image.new("L", (CELL, CELL))
                    ImageDraw.Draw(boundary).polygon(config.closed_patch_polygon, fill=255)
                    boundary = boundary.filter(ImageFilter.GaussianBlur(1.0))
                    mask = ImageChops.multiply(mask, boundary)
            elif config.closed_patch_polygon:
                ImageDraw.Draw(mask).polygon(config.closed_patch_polygon, fill=255)
                mask = mask.filter(ImageFilter.GaussianBlur(1.0))
            else:
                raise ValueError(f"Missing closed patch mask for {config.prefix}")
            pose = Image.composite(layer, pose, mask)
        return pose
    if config.closed_from_backup:
        name = "loem_feeding_sheet.webp" if config.prefix == "loem" else f"{config.prefix}_ham_sheet.webp"
        source = Image.open(HAM_BACKUPS / name).convert("RGBA")
        columns, _, count = geometry(source)
        return get_frame(source, count - 1, columns)
    name = config.closed_name or ("loem_idle_sheet.webp" if config.prefix == "loem" else f"{config.prefix}_idle_sheet.webp")
    source = Image.open(DRAWABLE / name).convert("RGBA")
    columns, _, count = geometry(source)
    candidates = [get_frame(source, index, columns) for index in range(count)]
    return min(candidates, key=lambda item: mouth_open_score(item, config.box))


def pose_pattern(frame_count: int) -> tuple[str, ...]:
    if frame_count == 6:
        return ("open", "closed", "open", "closed", "open", "closed")
    if frame_count == 12:
        return (
            "open", "closed", "open", "closed", "open", "closed",
            "open", "closed", "open", "closed", "closed", "closed",
        )
    raise ValueError(frame_count)


def restore_sheet(
    path: Path,
    config: MouthConfig,
    food_configs: dict[str, object],
    prop_loader,
    stage_loader,
) -> None:
    target = Image.open(path).convert("RGBA")
    columns, rows, count = geometry(target)
    closed = clip_body_source(clean_transparency(best_closed_pose(config)), config)
    mask = mouth_mask(config)
    # Lock all anatomy outside the approved articulated region to the closed
    # full-body pose. This creates a complete open key pose without importing
    # any alternate body, eye, wing, or rectangular legacy crop.
    open_source = clip_body_source(clean_transparency(best_open_pose(config)), config)
    open_source = align_open_source(open_source, config)
    opened = clean_transparency(Image.composite(open_source, closed, mask))
    food_config = food_configs[path.name]
    props = prop_loader(food_config.target_width)
    stages = stage_loader(count)
    output = Image.new("RGBA", target.size)

    changed = []
    poses = config.pose_sequence or pose_pattern(count)
    if len(poses) != count:
        raise ValueError(f"Pose sequence length mismatch for {config.prefix}")
    for index, pose_name in enumerate(poses):
        original = get_frame(target, index, columns)
        repaired = (opened if pose_name == "open" else closed).copy()
        if config.head_motion:
            if len(config.head_motion) != count:
                raise ValueError(f"Head motion length mismatch for {config.prefix}")
            if config.head_motion_region is None:
                raise ValueError(f"{config.prefix}: head motion requires a face region")
            repaired = move_head_and_upper_body(
                repaired,
                *config.head_motion[index],
                config.head_motion_region,
            )

        stage = stages[index]
        if stage is not None:
            prop = props[stage]
            x = food_config.prop_right - prop.width
            y = food_config.prop_bottom - prop.height
            # Add exactly one approved prop to the isolated full-body frame.
            repaired.alpha_composite(prop, (x, y))

        offset_x, offset_y = config.frame_offset
        if offset_x or offset_y:
            shifted = Image.new("RGBA", repaired.size)
            shifted.alpha_composite(repaired, (offset_x, offset_y))
            repaired = shifted

        repaired = clean_transparency(repaired)
        diff = ImageChops.difference(original.crop(config.box), repaired.crop(config.box))
        changed.append(sum(ImageStat.Stat(diff).sum))
        output.alpha_composite(repaired, ((index % columns) * CELL, (index // columns) * CELL))

    output = clean_transparency(output)
    output.save(path, "WEBP", lossless=True, quality=100, method=6, exact=True)
    print(f"mouth cycle {path.name}: {columns}x{rows}, changed={changed}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--start-at", choices=[item.prefix for item in CONFIGS])
    parser.add_argument("--only", choices=[item.prefix for item in CONFIGS])
    parser.add_argument("--food", choices=("both", "ham", "melon"), default="both")
    parser.add_argument("--key-preview-dir", type=Path)
    args = parser.parse_args()
    ham_configs = {item.filename: item for item in ham.CONFIGS}
    melon_configs = {item.filename: item for item in melon.CONFIGS}
    started = args.start_at is None
    for config in CONFIGS:
        if args.only and config.prefix != args.only:
            continue
        if not started:
            started = config.prefix == args.start_at
        if not started:
            continue
        if args.key_preview_dir:
            closed = clip_body_source(clean_transparency(best_closed_pose(config)), config)
            source = clip_body_source(clean_transparency(best_open_pose(config)), config)
            source = align_open_source(source, config)
            opened = clean_transparency(Image.composite(source, closed, mouth_mask(config)))
            preview = Image.new("RGBA", (CELL * 3, CELL), (22, 20, 31, 255))
            preview.alpha_composite(closed, (0, 0))
            preview.alpha_composite(source, (CELL, 0))
            preview.alpha_composite(opened, (CELL * 2, 0))
            args.key_preview_dir.mkdir(parents=True, exist_ok=True)
            preview.convert("RGB").save(args.key_preview_dir / f"{config.prefix}_keys.jpg", quality=95)
        ham_name = "loem_feeding_sheet.webp" if config.prefix == "loem" else f"{config.prefix}_ham_sheet.webp"
        melon_name = "loem_melon_sheet.webp" if config.prefix == "loem" else f"{config.prefix}_melon_sheet.webp"
        if args.food in ("both", "ham"):
            restore_sheet(DRAWABLE / ham_name, config, ham_configs, ham.resized_props, ham.stages)
        if args.food in ("both", "melon"):
            restore_sheet(DRAWABLE / melon_name, config, melon_configs, melon.resized_props, melon.stages)


if __name__ == "__main__":
    main()
