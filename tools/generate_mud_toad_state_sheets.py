from __future__ import annotations

from collections import deque
from math import hypot
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter


CELL = 512
FRAME_COUNT = 6
DRAWABLE = Path("app/src/main/res/drawable-nodpi")
PREVIEWS = Path("art/previews")


def frame(sheet: Image.Image, index: int) -> Image.Image:
    row, column = divmod(index, 3)
    return sheet.crop((column * CELL, row * CELL, (column + 1) * CELL, (row + 1) * CELL))


def write_sheet(frames: list[Image.Image], output: Path) -> None:
    sheet = Image.new("RGBA", (CELL * 3, CELL * 2))
    for index, current in enumerate(frames):
        row, column = divmod(index, 3)
        sheet.alpha_composite(current, (column * CELL, row * CELL))
    sheet.putdata(
        [
            (0, 0, 0, 0) if alpha == 0 else (red, green, blue, alpha)
            for red, green, blue, alpha in sheet.get_flattened_data()
        ]
    )
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output, "WEBP", lossless=True, quality=100, method=6, exact=True)
    decoded = Image.open(output).convert("RGBA")
    if ImageChops.difference(sheet, decoded).getbbox() is not None:
        raise ValueError(f"Lossless WebP round-trip changed pixels: {output}")


def cubic_point(
    start: tuple[float, float],
    control_a: tuple[float, float],
    control_b: tuple[float, float],
    end: tuple[float, float],
    amount: float,
) -> tuple[float, float]:
    inverse = 1.0 - amount
    return (
        inverse**3 * start[0]
        + 3 * inverse**2 * amount * control_a[0]
        + 3 * inverse * amount**2 * control_b[0]
        + amount**3 * end[0],
        inverse**3 * start[1]
        + 3 * inverse**2 * amount * control_a[1]
        + 3 * inverse * amount**2 * control_b[1]
        + amount**3 * end[1],
    )


def tongue_polygon(
    points: list[tuple[float, float]],
    root_width: float,
    tip_width: float,
) -> list[tuple[float, float]]:
    left: list[tuple[float, float]] = []
    right: list[tuple[float, float]] = []
    last = len(points) - 1
    for index, point in enumerate(points):
        before = points[max(0, index - 1)]
        after = points[min(last, index + 1)]
        tangent_x = after[0] - before[0]
        tangent_y = after[1] - before[1]
        tangent_length = max(0.001, hypot(tangent_x, tangent_y))
        normal_x = -tangent_y / tangent_length
        normal_y = tangent_x / tangent_length
        progress = index / last
        # A broad root and flattened taper make this read as muscle, not tubing.
        half_width = (root_width + (tip_width - root_width) * progress) / 2
        left.append((point[0] + normal_x * half_width, point[1] + normal_y * half_width))
        right.append((point[0] - normal_x * half_width, point[1] - normal_y * half_width))
    return left + list(reversed(right))


def draw_tongue(
    current: Image.Image,
    end: tuple[int, int] | None,
    bend: int = 0,
) -> tuple[int, int]:
    start = (389, 340)
    if end is None:
        return start

    scale = 4
    overlay = Image.new("RGBA", (CELL * scale, CELL * scale))
    draw = ImageDraw.Draw(overlay, "RGBA")
    control_a = (start[0] + 8 + bend, start[1] + (end[1] - start[1]) * 0.22)
    control_b = (end[0] + bend, start[1] + (end[1] - start[1]) * 0.72)
    centerline = [
        cubic_point(start, control_a, control_b, end, step / 16)
        for step in range(17)
    ]

    def scaled(points: list[tuple[float, float]]) -> list[tuple[int, int]]:
        return [(round(x * scale), round(y * scale)) for x, y in points]

    outer = tongue_polygon(centerline, root_width=18, tip_width=11)
    inner = tongue_polygon(centerline, root_width=12, tip_width=7)
    draw.polygon(scaled(outer), fill=(35, 24, 29, 255))
    outer_tip_radius = 11 * scale / 2
    draw.ellipse(
        (
            end[0] * scale - outer_tip_radius,
            end[1] * scale - outer_tip_radius,
            end[0] * scale + outer_tip_radius,
            end[1] * scale + outer_tip_radius,
        ),
        fill=(35, 24, 29, 255),
    )
    draw.polygon(scaled(inner), fill=(232, 100, 137, 255))
    inner_tip_radius = 7 * scale / 2
    draw.ellipse(
        (
            end[0] * scale - inner_tip_radius,
            end[1] * scale - inner_tip_radius,
            end[0] * scale + inner_tip_radius,
            end[1] * scale + inner_tip_radius,
        ),
        fill=(232, 100, 137, 255),
    )

    # A short asymmetric highlight reinforces the soft, wet surface without
    # turning the tongue into a uniform striped tube.
    if hypot(end[0] - start[0], end[1] - start[1]) > 17:
        highlight = [
            cubic_point(start, control_a, control_b, end, amount)
            for amount in (0.24, 0.42, 0.60)
        ]
        draw.line(
            scaled([(x - 2, y - 1) for x, y in highlight]),
            fill=(255, 177, 193, 170),
            width=2 * scale,
        )

    overlay = overlay.resize((CELL, CELL), Image.Resampling.LANCZOS)
    current.alpha_composite(overlay)
    return end


IDLE_TONGUES = (
    None,
    (398, 346),
    (408, 350),
    (418, 356),
    (407, 351),
    (397, 345),
)
HUNGRY_TONGUES = (
    (404, 352),
    (418, 361),
    (425, 376),
    (414, 392),
    (423, 376),
    (414, 358),
)


def largest_component(mask: Image.Image) -> Image.Image:
    pixels = mask.load()
    visited: set[tuple[int, int]] = set()
    largest: list[tuple[int, int]] = []
    for y in range(mask.height):
        for x in range(mask.width):
            if not pixels[x, y] or (x, y) in visited:
                continue
            component: list[tuple[int, int]] = []
            queue = deque([(x, y)])
            visited.add((x, y))
            while queue:
                current_x, current_y = queue.popleft()
                component.append((current_x, current_y))
                for next_x, next_y in (
                    (current_x - 1, current_y),
                    (current_x + 1, current_y),
                    (current_x, current_y - 1),
                    (current_x, current_y + 1),
                ):
                    if (
                        0 <= next_x < mask.width
                        and 0 <= next_y < mask.height
                        and pixels[next_x, next_y]
                        and (next_x, next_y) not in visited
                    ):
                        visited.add((next_x, next_y))
                        queue.append((next_x, next_y))
            if len(component) > len(largest):
                largest = component
    result = Image.new("L", mask.size)
    result_pixels = result.load()
    for x, y in largest:
        result_pixels[x, y] = 255
    return result


def extract_food(source_frame: Image.Image) -> Image.Image:
    rgba = source_frame.convert("RGBA")
    mask = Image.new("L", rgba.size)
    source_pixels = rgba.load()
    mask_pixels = mask.load()
    for y in range(rgba.height):
        for x in range(rgba.width):
            red, green, blue, alpha = source_pixels[x, y]
            if alpha > 32 and max(red, green, blue) - min(red, green, blue) > 46:
                mask_pixels[x, y] = 255
    component = largest_component(mask)
    outline = component.filter(ImageFilter.MaxFilter(9))
    prop = rgba.copy()
    prop.putalpha(Image.composite(rgba.getchannel("A"), Image.new("L", rgba.size), outline))
    bbox = prop.getbbox()
    if bbox is None:
        raise ValueError("Could not find food prop")
    prop = prop.crop(bbox)
    prop.thumbnail((82, 82), Image.Resampling.LANCZOS)
    return prop


def add_food(current: Image.Image, prop: Image.Image, center: tuple[int, int]) -> None:
    current.alpha_composite(prop, (center[0] - prop.width // 2, center[1] - prop.height // 2))


def add_sleep_breath(current: Image.Image, index: int) -> None:
    """Animate a tiny wet tongue glint; silhouette and body pixels stay locked."""
    strength = (0, 30, 70, 95, 55, 18)[index]
    if strength == 0:
        return
    overlay = Image.new("RGBA", current.size)
    draw = ImageDraw.Draw(overlay, "RGBA")
    draw.line((336, 346, 340, 349), fill=(255, 205, 216, strength), width=2)
    current.alpha_composite(overlay)


def build() -> None:
    approved_idle = Image.open(PREVIEWS / "loem_mudwing_idle_sheet.png").convert("RGBA")
    approved_sleep = Image.open(PREVIEWS / "loem_mud_toad_sleep_sheet.png").convert("RGBA")
    canonical_idle = frame(approved_idle, 0)
    canonical_sleep = frame(approved_sleep, 0)
    base_frames = [canonical_idle.copy() for _ in range(FRAME_COUNT)]
    sleep_frames = [canonical_sleep.copy() for _ in range(FRAME_COUNT)]

    idle_frames: list[Image.Image] = []
    hungry_frames: list[Image.Image] = []
    for index, source in enumerate(base_frames):
        idle = source.copy()
        draw_tongue(idle, IDLE_TONGUES[index], bend=(0, 1, 3, 5, 3, 1)[index])
        idle_frames.append(idle)

        hungry = source.copy()
        draw_tongue(hungry, HUNGRY_TONGUES[index], bend=(2, 5, 9, 12, 9, 4)[index])
        hungry_frames.append(hungry)

        add_sleep_breath(sleep_frames[index], index)

    majestic_melon = Image.open(DRAWABLE / "loem_wing_evolution_melon_sheet.webp").convert("RGBA")
    majestic_ham = Image.open(DRAWABLE / "loem_wing_evolution_ham_sheet.webp").convert("RGBA")
    melon_props = [extract_food(frame(majestic_melon, index)) for index in range(5)]
    ham_props = [extract_food(frame(majestic_ham, index)) for index in range(5)]

    melon_frames: list[Image.Image] = []
    ham_frames: list[Image.Image] = []
    for index, source in enumerate(base_frames):
        melon = source.copy()
        ham = source.copy()
        draw_tongue(melon, HUNGRY_TONGUES[index], bend=(2, 5, 9, 12, 9, 4)[index])
        draw_tongue(ham, HUNGRY_TONGUES[index], bend=(2, 5, 9, 12, 9, 4)[index])
        if index < 5:
            center = (414, 377)
            add_food(melon, melon_props[index], center)
            add_food(ham, ham_props[index], center)
        melon_frames.append(melon)
        ham_frames.append(ham)

    outputs = {
        "loem_mud_toad_idle_sheet.webp": idle_frames,
        "loem_mud_toad_hungry_sheet.webp": hungry_frames,
        "loem_mud_toad_sleep_sheet.webp": sleep_frames,
        "loem_mud_toad_melon_sheet.webp": melon_frames,
        "loem_mud_toad_ham_sheet.webp": ham_frames,
    }
    for filename, frames in outputs.items():
        write_sheet(frames, DRAWABLE / filename)

    # Compose animates combat; all static combat slots reuse the idle WebP.


if __name__ == "__main__":
    build()
