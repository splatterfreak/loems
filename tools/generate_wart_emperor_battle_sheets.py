from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageChops


CELL = 512
DRAWABLE = Path("app/src/main/res/drawable-nodpi")
INTERMEDIATE = Path("art/intermediate")
GENDERS = ("male", "female")
ACTION_STATES = ("attack", "hit", "double_attack", "double_hit")


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
    if len(poses) != 6 or len(offsets) != 6:
        raise ValueError("Warzenkaiser battle sheets require exactly six frames")
    frames: list[Image.Image] = []
    for pose, (offset_x, offset_y) in zip(poses, offsets):
        frame = Image.new("RGBA", (CELL, CELL))
        x = (CELL - pose.width) // 2 + offset_x
        y = ground_y - pose.height + 1 + offset_y
        frame.alpha_composite(pose, (x, y))
        frames.append(clear_transparent_rgb(frame))
    return frames


def write_sheet(frames: list[Image.Image], output: Path) -> None:
    sheet = Image.new("RGBA", (CELL * 3, CELL * 2))
    for index, frame in enumerate(frames):
        row, column = divmod(index, 3)
        sheet.alpha_composite(frame, (column * CELL, row * CELL))
    sheet = clear_transparent_rgb(sheet)
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output, "WEBP", lossless=True, quality=100, method=6, exact=True)
    decoded = Image.open(output).convert("RGBA")
    if ImageChops.difference(sheet, decoded).getbbox() is not None:
        raise ValueError(f"Lossless WebP round-trip changed pixels: {output}")


def canonical_path(gender: str, state: str) -> Path:
    return INTERMEDIATE / f"loem_wart_emperor_{gender}_{state}_canonical.png"


def output_path(gender: str, state: str) -> Path:
    return DRAWABLE / f"loem_wart_emperor_{gender}_battle_{state}_sheet.webp"


def build_gender(gender: str) -> None:
    idle = prepare_canonical(canonical_path(gender, "idle"), (380, 270))
    poses = {
        "attack": prepare_canonical(canonical_path(gender, "attack"), (380, 270)),
        "hit": prepare_canonical(canonical_path(gender, "hit"), (370, 285)),
        "double_attack": prepare_canonical(
            canonical_path(gender, "double_attack"), (380, 270)
        ),
        "double_hit": prepare_canonical(
            canonical_path(gender, "double_hit"), (370, 285)
        ),
        "victory": prepare_canonical(canonical_path(gender, "victory"), (360, 285)),
        "defeat": prepare_canonical(canonical_path(gender, "defeat"), (390, 220)),
    }

    action_offsets = {
        "attack": ((0, 0), (-8, 0), (5, 0), (14, 0), (5, 0), (0, 0)),
        "hit": ((0, 0), (-5, -5), (-11, -3), (-5, 0), (-2, 0), (0, 0)),
        "double_attack": ((0, 0), (7, 0), (14, 0), (-5, 0), (10, 0), (0, 0)),
        "double_hit": ((0, 0), (-7, -6), (-12, -4), (-5, 0), (-2, 0), (0, 0)),
    }
    for state in ACTION_STATES:
        action = poses[state]
        frames = positioned_frames(
            (idle, action, action, action, action, idle), action_offsets[state]
        )
        write_sheet(frames, output_path(gender, state))

    victory = poses["victory"]
    write_sheet(
        positioned_frames(
            (victory,) * 6,
            ((0, 0), (0, -5), (0, -11), (0, -16), (0, -7), (0, 0)),
        ),
        output_path(gender, "victory"),
    )
    defeat = poses["defeat"]
    write_sheet(
        positioned_frames(
            (defeat,) * 6,
            ((0, -12), (-2, -5), (2, 0), (-1, -2), (0, 0), (0, 0)),
        ),
        output_path(gender, "defeat"),
    )


def build() -> None:
    for gender in GENDERS:
        build_gender(gender)


if __name__ == "__main__":
    build()
