from __future__ import annotations

from collections import deque
from pathlib import Path

from PIL import Image, ImageFilter


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
    prop.thumbnail((72, 72), Image.Resampling.LANCZOS)
    return prop


def add_food(current: Image.Image, prop: Image.Image, center: tuple[int, int]) -> None:
    current.alpha_composite(prop, (center[0] - prop.width // 2, center[1] - prop.height // 2))


def build() -> None:
    idle_sheet = Image.open(PREVIEWS / "loem_wart_emperor_male_idle_sheet.png").convert("RGBA")
    sleep_sheet = Image.open(PREVIEWS / "loem_wart_emperor_male_sleep_sheet.png").convert("RGBA")
    feeding_sheet = Image.open(PREVIEWS / "loem_wart_emperor_male_feeding_sheet.png").convert("RGBA")

    idle_frames = [frame(idle_sheet, index) for index in range(FRAME_COUNT)]
    sleep_frames = [frame(sleep_sheet, index) for index in range(FRAME_COUNT)]
    hungry_frames = [frame(feeding_sheet, index) for index in range(FRAME_COUNT)]

    majestic_melon = Image.open(DRAWABLE / "loem_wing_evolution_melon_sheet.png").convert("RGBA")
    majestic_ham = Image.open(DRAWABLE / "loem_wing_evolution_ham_sheet.png").convert("RGBA")
    melon_props = [extract_food(frame(majestic_melon, index)) for index in range(5)]
    ham_props = [extract_food(frame(majestic_ham, index)) for index in range(5)]

    # Only the food moves. The body, four feet, two wings, two horns and tail remain
    # pixel-identical across the feeding loop so generated anatomy cannot drift.
    food_centers = ((420, 382), (414, 374), (405, 367), (397, 363), (404, 371))
    melon_frames: list[Image.Image] = []
    ham_frames: list[Image.Image] = []
    for index, source in enumerate(hungry_frames):
        melon = source.copy()
        ham = source.copy()
        if index < 5:
            add_food(melon, melon_props[index], food_centers[index])
            add_food(ham, ham_props[index], food_centers[index])
        melon_frames.append(melon)
        ham_frames.append(ham)

    outputs = {
        "loem_wart_emperor_male_idle_sheet.png": idle_frames,
        "loem_wart_emperor_male_hungry_sheet.png": hungry_frames,
        "loem_wart_emperor_male_sleep_sheet.png": sleep_frames,
        "loem_wart_emperor_male_melon_sheet.png": melon_frames,
        "loem_wart_emperor_male_ham_sheet.png": ham_frames,
    }
    for filename, frames in outputs.items():
        write_sheet(frames, DRAWABLE / filename)

    # Battle movement is handled by Compose. Reusing the approved idle frames keeps
    # the creature's anatomy and silhouette consistent in every combat state.
    for state in (
        "battle_attack",
        "battle_hit",
        "battle_double_attack",
        "battle_double_hit",
        "battle_victory",
    ):
        write_sheet([current.copy() for current in idle_frames], DRAWABLE / f"loem_wart_emperor_male_{state}_sheet.png")


if __name__ == "__main__":
    build()
