from __future__ import annotations

from collections import deque
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


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
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output)


def draw_tongue(current: Image.Image, index: int, feeding: bool = False) -> tuple[int, int]:
    draw = ImageDraw.Draw(current, "RGBA")
    idle_lengths = (0, 14, 25, 34, 22, 8)
    feeding_lengths = (20, 32, 45, 54, 38, 16)
    length = (feeding_lengths if feeding else idle_lengths)[index]
    start = (390, 337)
    if length == 0:
        return start
    wave = (0, 5, 10, 14, 8, 3)[index]
    middle = (start[0] + wave, start[1] + length // 2)
    end = (start[0] + wave // 2, start[1] + length)
    points = (start, middle, end)
    draw.line(points, fill=(35, 24, 29, 255), width=17, joint="curve")
    draw.line(points, fill=(230, 102, 137, 255), width=11, joint="curve")
    draw.line(points, fill=(255, 170, 190, 180), width=3, joint="curve")
    draw.ellipse((end[0] - 7, end[1] - 7, end[0] + 8, end[1] + 8), fill=(35, 24, 29, 255))
    draw.ellipse((end[0] - 5, end[1] - 5, end[0] + 6, end[1] + 6), fill=(235, 110, 145, 255))
    return end


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


def build() -> None:
    approved_idle = Image.open(PREVIEWS / "loem_mudwing_idle_sheet.png").convert("RGBA")
    approved_sleep = Image.open(PREVIEWS / "loem_mud_toad_sleep_sheet.png").convert("RGBA")
    base_frames = [frame(approved_idle, index) for index in range(FRAME_COUNT)]
    sleep_frames = [frame(approved_sleep, index) for index in range(FRAME_COUNT)]

    idle_frames: list[Image.Image] = []
    hungry_frames: list[Image.Image] = []
    for index, source in enumerate(base_frames):
        idle = source.copy()
        if index in (1, 2, 3, 4):
            draw_tongue(idle, index)
        idle_frames.append(idle)

        hungry = source.copy()
        draw_tongue(hungry, index, feeding=True)
        hungry_frames.append(hungry)

    majestic_melon = Image.open(DRAWABLE / "loem_wing_evolution_melon_sheet.png").convert("RGBA")
    majestic_ham = Image.open(DRAWABLE / "loem_wing_evolution_ham_sheet.png").convert("RGBA")
    melon_props = [extract_food(frame(majestic_melon, index)) for index in range(5)]
    ham_props = [extract_food(frame(majestic_ham, index)) for index in range(5)]

    melon_frames: list[Image.Image] = []
    ham_frames: list[Image.Image] = []
    for index, source in enumerate(base_frames):
        melon = source.copy()
        ham = source.copy()
        tongue_end = draw_tongue(melon, index, feeding=True)
        draw_tongue(ham, index, feeding=True)
        if index < 5:
            center = (414, 377)
            add_food(melon, melon_props[index], center)
            add_food(ham, ham_props[index], center)
        melon_frames.append(melon)
        ham_frames.append(ham)

    outputs = {
        "loem_mud_toad_idle_sheet.png": idle_frames,
        "loem_mud_toad_hungry_sheet.png": hungry_frames,
        "loem_mud_toad_sleep_sheet.png": sleep_frames,
        "loem_mud_toad_melon_sheet.png": melon_frames,
        "loem_mud_toad_ham_sheet.png": ham_frames,
    }
    for filename, frames in outputs.items():
        write_sheet(frames, DRAWABLE / filename)

    for state in (
        "battle_attack",
        "battle_hit",
        "battle_double_attack",
        "battle_double_hit",
        "battle_victory",
    ):
        write_sheet([current.copy() for current in idle_frames], DRAWABLE / f"loem_mud_toad_{state}_sheet.png")


if __name__ == "__main__":
    build()
