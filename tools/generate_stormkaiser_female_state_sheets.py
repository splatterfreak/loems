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
    if len(frames) != FRAME_COUNT:
        raise ValueError(f"Expected {FRAME_COUNT} frames, got {len(frames)}")
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
            chroma = max(red, green, blue) - min(red, green, blue)
            if alpha > 32 and chroma > 46:
                mask_pixels[x, y] = 255
    colored_component = largest_component(mask)
    outline_mask = colored_component.filter(ImageFilter.MaxFilter(9))
    prop = rgba.copy()
    prop.putalpha(Image.composite(rgba.getchannel("A"), Image.new("L", rgba.size), outline_mask))
    bbox = prop.getbbox()
    if bbox is None:
        raise ValueError("Could not find the food prop")
    prop = prop.crop(bbox)
    prop.thumbnail((92, 92), Image.Resampling.LANCZOS)
    return prop


def add_hungry_expression(current: Image.Image, index: int) -> None:
    draw = ImageDraw.Draw(current, "RGBA")
    length = (8, 12, 16, 12, 8, 6)[index]
    start_x, start_y = 367, 257
    tongue = (
        (start_x - 5, start_y),
        (start_x + 4, start_y),
        (start_x + 5, start_y + length - 2),
        (start_x, start_y + length + 4),
        (start_x - 4, start_y + length - 1),
    )
    draw.polygon(tongue, fill=(224, 111, 137, 245))
    draw.line(tongue + (tongue[0],), fill=(48, 30, 37, 245), width=2, joint="curve")
    draw.line(
        (start_x, start_y + 3, start_x, start_y + length + 1),
        fill=(170, 65, 94, 210),
        width=1,
    )


def composite_centered(current: Image.Image, prop: Image.Image, center: tuple[int, int]) -> None:
    left = center[0] - prop.width // 2
    top = center[1] - prop.height // 2
    current.alpha_composite(prop, (left, top))


def build() -> None:
    approved_idle = Image.open(PREVIEWS / "loem_stormkaiser_female_idle_sheet.png").convert("RGBA")
    approved_frames = [frame(approved_idle, index) for index in range(FRAME_COUNT)]

    idle_output = DRAWABLE / "loem_stormkaiser_female_idle_sheet.png"
    write_sheet([current.copy() for current in approved_frames], idle_output)

    hungry_frames = []
    for index, source in enumerate(approved_frames):
        current = source.copy()
        add_hungry_expression(current, index)
        hungry_frames.append(current)
    write_sheet(hungry_frames, DRAWABLE / "loem_stormkaiser_female_hungry_sheet.png")

    male_melon = Image.open(DRAWABLE / "loem_stormkaiser_melon_sheet.png").convert("RGBA")
    male_ham = Image.open(DRAWABLE / "loem_stormkaiser_ham_sheet.png").convert("RGBA")
    melon_props = [extract_food(frame(male_melon, index)) for index in range(5)]
    ham_props = [extract_food(frame(male_ham, index)) for index in range(5)]
    melon_centers = ((390, 294), (385, 278), (381, 274), (380, 279), (377, 278))
    ham_centers = ((392, 356), (392, 294), (390, 280), (386, 281), (384, 282))

    melon_frames = []
    ham_frames = []
    for index, source in enumerate(approved_frames):
        melon_frame = source.copy()
        ham_frame = source.copy()
        if index < 5:
            composite_centered(melon_frame, melon_props[index], melon_centers[index])
            composite_centered(ham_frame, ham_props[index], ham_centers[index])
        melon_frames.append(melon_frame)
        ham_frames.append(ham_frame)
    write_sheet(melon_frames, DRAWABLE / "loem_stormkaiser_female_melon_sheet.png")
    write_sheet(ham_frames, DRAWABLE / "loem_stormkaiser_female_ham_sheet.png")

    # Battle motion is driven by BattleLoemSprite translations in Compose. Reusing the locked
    # canonical frames keeps the approved anatomy exact in every battle frame.
    for state in (
        "battle_attack",
        "battle_hit",
        "battle_double_attack",
        "battle_double_hit",
        "battle_victory",
    ):
        write_sheet(
            [current.copy() for current in approved_frames],
            DRAWABLE / f"loem_stormkaiser_female_{state}_sheet.png",
        )


if __name__ == "__main__":
    build()
