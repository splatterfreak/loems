from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageChops


CELL = 512
DRAWABLE = Path("app/src/main/res/drawable-nodpi")
INTERMEDIATE = Path("art/intermediate")


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
    frames: list[Image.Image] = []
    if len(poses) != len(offsets):
        raise ValueError("Each frame needs one pose and one offset")
    for pose, (offset_x, offset_y) in zip(poses, offsets):
        frame = Image.new("RGBA", (CELL, CELL))
        x = (CELL - pose.width) // 2 + offset_x
        y = ground_y - pose.height + 1 + offset_y
        frame.alpha_composite(pose, (x, y))
        frames.append(clear_transparent_rgb(frame))
    return frames


def write_sheet(frames: list[Image.Image], output: Path) -> None:
    if len(frames) != 6:
        raise ValueError("Battle action sheets require exactly six frames")
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


def build() -> None:
    idle = prepare_canonical(
        INTERMEDIATE / "loem_mud_toad_idle_canonical.png",
        maximum_size=(360, 250),
    )
    attack = prepare_canonical(
        INTERMEDIATE / "loem_mud_toad_attack_canonical.png",
        maximum_size=(360, 250),
    )
    hit = prepare_canonical(
        INTERMEDIATE / "loem_mud_toad_hit_canonical.png",
        maximum_size=(370, 280),
    )
    double_attack = prepare_canonical(
        INTERMEDIATE / "loem_mud_toad_double_attack_canonical.png",
        maximum_size=(360, 225),
    )

    # A compact wind-up, forward lunge, and return to the initial anchor.
    attack_frames = positioned_frames(
        (idle, attack, attack, attack, attack, idle),
        offsets=((-6, 0), (-10, 0), (6, 0), (16, 0), (5, 0), (-6, 0)),
    )
    # The recoil rises slightly, moves back, and settles into the first pose.
    hit_frames = positioned_frames(
        (idle, hit, hit, hit, hit, idle),
        offsets=((0, 0), (-8, -6), (-14, -4), (-8, 0), (-3, 0), (0, 0)),
    )
    # Two readable thrust beats, returning to the same loop anchor.
    double_attack_frames = positioned_frames(
        (idle, double_attack, double_attack, double_attack, double_attack, idle),
        offsets=((-8, 0), (8, 0), (18, 0), (-5, 0), (12, 0), (-8, 0)),
    )

    write_sheet(attack_frames, DRAWABLE / "loem_mud_toad_battle_attack_sheet.webp")
    write_sheet(hit_frames, DRAWABLE / "loem_mud_toad_battle_hit_sheet.webp")
    write_sheet(
        double_attack_frames,
        DRAWABLE / "loem_mud_toad_battle_double_attack_sheet.webp",
    )


if __name__ == "__main__":
    build()
