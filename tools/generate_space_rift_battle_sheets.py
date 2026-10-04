from __future__ import annotations

from pathlib import Path
import argparse

from PIL import Image, ImageChops


CELL = 512
COLUMNS = 4
ROWS = 3
DRAWABLE = Path("app/src/main/res/drawable-nodpi")
INTERMEDIATE = Path("art/intermediate")
FORMS = ("urtoad", "world_serpent")
ACTION_STATES = ("attack", "hit", "double_attack", "double_hit")


def normalize_urtoad_recolorable_skin(image: Image.Image, state: str) -> Image.Image:
    """Restores the neutral-gray body mask without changing portal effects or facial details."""
    if state not in ("attack", "double_attack", "defeat"):
        return image

    result = image.convert("RGBA")
    pixels = result.load()
    for y in range(result.height):
        for x in range(result.width):
            red, green, blue, alpha = pixels[x, y]
            if alpha == 0:
                continue

            maximum = max(red, green, blue)
            minimum = min(red, green, blue)
            saturation = (maximum - minimum) / maximum if maximum else 0
            protected_tooth = red > 175 and green > 110 and blue < 150 and red - green > 25
            warm_body = (
                maximum >= 100 and red > green * 1.01 and red > blue * 1.01 and
                not protected_tooth
            )
            # Orange wing skin belongs to the recolorable body, not the portal effect palette.
            wing_surface = (
                (x < result.width * 0.45 or x > result.width * 0.55) and
                y < result.height * 0.88 and maximum >= 80 and
                # Keep saturated blue/purple rift energy, but neutralize every wing surface.
                not (blue > red * 1.10 and blue >= 90) and not protected_tooth
            )
            if warm_body or wing_surface:
                shade = round(red * 0.299 + green * 0.587 + blue * 0.114)
                pixels[x, y] = (shade, shade, shade, alpha)
    return clear_transparent_rgb(result)


def clear_transparent_rgb(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    rgba.putdata(
        [
            (0, 0, 0, 0) if alpha == 0 else (red, green, blue, alpha)
            for red, green, blue, alpha in rgba.get_flattened_data()
        ]
    )
    return rgba


def prepare_canonical(path: Path, maximum_size: tuple[int, int]) -> Image.Image:
    source = Image.open(path).convert("RGBA")
    alpha = source.getchannel("A").point(lambda value: 0 if value <= 16 else value)
    source.putalpha(alpha)
    bbox = alpha.getbbox()
    if bbox is None:
        raise ValueError(f"Canonical pose contains no visible pixels: {path}")
    pose = source.crop(bbox)
    scale = min(maximum_size[0] / pose.width, maximum_size[1] / pose.height)
    size = (round(pose.width * scale), round(pose.height * scale))
    return clear_transparent_rgb(pose.resize(size, Image.Resampling.LANCZOS))


def positioned_frames(
    poses: tuple[Image.Image, ...],
    offsets: tuple[tuple[int, int], ...],
    ground_y: int = 447,
) -> list[Image.Image]:
    if len(poses) != COLUMNS * ROWS or len(offsets) != COLUMNS * ROWS:
        raise ValueError("Space-rift battle sheets require exactly twelve frames")
    frames: list[Image.Image] = []
    for pose, (offset_x, offset_y) in zip(poses, offsets):
        frame = Image.new("RGBA", (CELL, CELL))
        x = (CELL - pose.width) // 2 + offset_x
        y = ground_y - pose.height + 1 + offset_y
        frame.alpha_composite(pose, (x, y))
        frames.append(clear_transparent_rgb(frame))
    return frames


def normalize_urtoad_wing_cells(frames: list[Image.Image]) -> list[Image.Image]:
    """Apply the recolor-mask repair after positioning, where wing tip bounds are fixed."""
    repaired: list[Image.Image] = []
    for frame in frames:
        pixels = frame.load()
        for y in range(100, 450):
            for x in tuple(range(24, 206)) + tuple(range(306, 488)):
                red, green, blue, alpha = pixels[x, y]
                if alpha == 0 or blue > red * 1.10:
                    continue
                shade = round(red * 0.299 + green * 0.587 + blue * 0.114)
                pixels[x, y] = (shade, shade, shade, alpha)
        repaired.append(clear_transparent_rgb(frame))
    return repaired


def write_sheet(frames: list[Image.Image], output: Path) -> None:
    sheet = Image.new("RGBA", (CELL * COLUMNS, CELL * ROWS))
    for index, frame in enumerate(frames):
        row, column = divmod(index, COLUMNS)
        sheet.alpha_composite(frame, (column * CELL, row * CELL))
    sheet = clear_transparent_rgb(sheet)
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output, "WEBP", lossless=True, quality=100, method=6, exact=True)
    decoded = Image.open(output).convert("RGBA")
    if ImageChops.difference(sheet, decoded).getbbox(alpha_only=False) is not None:
        raise ValueError(f"Lossless WebP round-trip changed pixels: {output}")


def canonical_path(form: str, state: str) -> Path:
    if state in ("attack", "double_attack"):
        # Projectiles belong exclusively to BattleProjectileEffect in the app.
        return INTERMEDIATE / f"loem_space_rift_{form}_{state}_no_projectile.png"
    return INTERMEDIATE / f"loem_space_rift_{form}_{state}_canonical.png"


def output_path(form: str, state: str) -> Path:
    return DRAWABLE / f"loem_space_rift_{form}_battle_{state}_sheet.webp"


def build_form(form: str, states: tuple[str, ...]) -> None:
    sizes = {
        "urtoad": {
            "idle": (384, 270), "attack": (390, 270), "hit": (380, 270),
            "double_attack": (390, 270), "double_hit": (370, 285),
            "victory": (380, 270), "defeat": (390, 220),
        },
        "world_serpent": {
            "idle": (384, 300), "attack": (380, 285), "hit": (370, 280),
            "double_attack": (380, 278), "double_hit": (360, 285),
            "victory": (370, 280), "defeat": (390, 220),
        },
    }[form]
    poses = {
        state: normalize_urtoad_recolorable_skin(
            prepare_canonical(canonical_path(form, state), size),
            state,
        ) if form == "urtoad" else prepare_canonical(canonical_path(form, state), size)
        for state, size in sizes.items()
    }
    idle = poses["idle"]

    action_offsets = {
        "attack": ((0, 0), (0, 0), (-6, 0), (2, 0), (9, 0), (13, 0),
                   (7, 0), (12, 0), (4, 0), (-3, 0), (0, 0), (0, 0)),
        "hit": ((0, 0), (0, 0), (-4, -3), (-9, -5), (-12, -3), (-7, 0),
                (-3, 0), (-8, -2), (-4, 0), (-1, 0), (0, 0), (0, 0)),
        "double_attack": ((0, 0), (0, 0), (5, 0), (12, 0), (-4, 0), (9, 0),
                          (13, 0), (-3, 0), (11, 0), (4, 0), (0, 0), (0, 0)),
        "double_hit": ((0, 0), (0, 0), (-6, -4), (-11, -6), (-8, -3), (-3, 0),
                       (-10, -4), (-5, -2), (-2, 0), (0, 0), (0, 0), (0, 0)),
    }
    for state in ACTION_STATES:
        if state not in states:
            continue
        action = poses[state]
        offsets = action_offsets[state]
        if form in ("urtoad", "world_serpent") and state == "hit":
            offsets = (
                (0, 0), (0, 0), (-4, 0), (-9, 0), (-12, 0), (-7, 0),
                (-3, 0), (-1, 0), (0, 0), (0, 0), (0, 0), (0, 0),
            )
        frames = positioned_frames((idle, idle) + (action,) * 8 + (idle, idle), offsets)
        if form == "urtoad" and state in ("attack", "double_attack", "hit"):
            frames = normalize_urtoad_wing_cells(frames)
        write_sheet(frames, output_path(form, state))

    victory = poses["victory"]
    if "victory" in states:
        write_sheet(
        positioned_frames(
            (victory,) * 12,
            ((0, 0),) * 12,
        ),
        output_path(form, "victory"),
    )
    defeat = poses["defeat"]
    if "defeat" in states:
        frames = positioned_frames(
            (defeat,) * 12,
            ((0, -14), (-2, -10), (2, -6), (-2, -3), (1, 0), (0, -2),
             (-1, 0), (1, 0), (0, 0), (0, 0), (0, 0), (0, 0)),
        )
        if form == "urtoad":
            frames = normalize_urtoad_wing_cells(frames)
        write_sheet(
        frames,
        output_path(form, "defeat"),
    )


def build() -> None:
    parser = argparse.ArgumentParser(description="Build projectile-free space-rift battle sheets")
    parser.add_argument("--states", nargs="+", choices=(*ACTION_STATES, "victory", "defeat"),
                        default=[*ACTION_STATES, "victory", "defeat"])
    args = parser.parse_args()
    for form in FORMS:
        build_form(form, tuple(args.states))


if __name__ == "__main__":
    build()
