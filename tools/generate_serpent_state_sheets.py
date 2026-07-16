from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


CELL = 512
MELON_PROPS: list[Image.Image | None] = []
HAM_PROPS: list[Image.Image | None] = []


def frame(sheet: Image.Image, index: int) -> Image.Image:
    row, column = divmod(index, 3)
    return sheet.crop((column * CELL, row * CELL, (column + 1) * CELL, (row + 1) * CELL))


def draw_hungry(image: Image.Image, index: int) -> None:
    draw = ImageDraw.Draw(image, "RGBA")
    length = (12, 18, 25, 18, 12, 8)[index]
    draw.line((435, 272, 435, 272 + length), fill=(75, 174, 231, 235), width=5)
    draw.ellipse((431, 268 + length, 439, 277 + length), fill=(92, 190, 242, 225))


def draw_sleep(image: Image.Image, index: int) -> None:
    draw = ImageDraw.Draw(image, "RGBA")
    draw.ellipse((381, 195, 426, 248), fill=(190, 187, 193, 255))
    drop = (0, 1, 2, 1, 0, 0)[index]
    draw.arc((388, 216 + drop, 420, 238 + drop), 10, 170, fill=(24, 24, 25, 255), width=5)


def draw_melon(image: Image.Image, index: int) -> None:
    prop = MELON_PROPS[index]
    if prop is not None:
        positions = ((382, 280), (382, 275), (382, 283), (382, 286), (382, 288))
        image.alpha_composite(prop, positions[index])


def draw_ham(image: Image.Image, index: int) -> None:
    prop = HAM_PROPS[index]
    if prop is not None:
        positions = ((374, 280), (381, 275), (385, 282), (390, 286), (401, 289))
        image.alpha_composite(prop, positions[index])


def extract_food(frame_image: Image.Image, crop_box: tuple[int, int, int, int], max_size: tuple[int, int]) -> Image.Image:
    """Keep the established saturated food art plus its nearby outline pixels."""
    crop = frame_image.crop(crop_box).convert("RGBA")
    pixels = crop.load()
    color_mask = Image.new("L", crop.size)
    mask_pixels = color_mask.load()
    for y in range(crop.height):
        for x in range(crop.width):
            red, green, blue, alpha = pixels[x, y]
            if alpha > 32 and max(red, green, blue) - min(red, green, blue) > 34:
                mask_pixels[x, y] = 255
    colored_alpha = Image.composite(crop.getchannel("A"), Image.new("L", crop.size), color_mask)
    outline_alpha = colored_alpha.filter(ImageFilter.MaxFilter(7))
    cleaned = Image.new("RGBA", crop.size, (28, 22, 26, 0))
    cleaned.putalpha(outline_alpha)
    colored = crop.copy()
    colored.putalpha(colored_alpha)
    cleaned.alpha_composite(colored)
    bbox = cleaned.getbbox()
    if bbox is None:
        raise ValueError("No food pixels found")
    crop = cleaned.crop(bbox)
    crop.thumbnail(max_size, Image.Resampling.LANCZOS)
    return crop


def build(source: Image.Image, output: Path, painter, vertical_offsets: tuple[int, ...] = (0, 0, 0, 0, 0, 0)) -> None:
    result = Image.new("RGBA", source.size)
    for index in range(6):
        current = frame(source, index)
        offset_y = vertical_offsets[index]
        if offset_y:
            aligned = Image.new("RGBA", current.size)
            aligned.alpha_composite(current, (0, offset_y))
            current = aligned
        painter(current, index)
        row, column = divmod(index, 3)
        result.alpha_composite(current, (column * CELL, row * CELL))
    output.parent.mkdir(parents=True, exist_ok=True)
    result.save(output)


def main() -> None:
    global MELON_PROPS, HAM_PROPS
    drawable = Path("app/src/main/res/drawable-nodpi")
    idle_source = Image.open(drawable / "loem_serpent_evolution_idle_sheet.png").convert("RGBA")
    eating_source = Image.open(drawable / "loem_serpent_eating_mouth_sheet.png").convert("RGBA")
    wing_melon = Image.open(drawable / "loem_wing_evolution_melon_sheet.png").convert("RGBA")
    wing_ham = Image.open(drawable / "loem_wing_evolution_ham_sheet.png").convert("RGBA")
    melon_crops = ((305, 265, 440, 420), (290, 270, 440, 410), (330, 300, 460, 405),
                   (315, 305, 430, 415), (325, 320, 420, 400))
    ham_crops = ((315, 285, 490, 475), (320, 260, 495, 420), (345, 275, 510, 415),
                 (335, 290, 495, 430), (350, 300, 480, 410))
    MELON_PROPS = [extract_food(frame(wing_melon, i), melon_crops[i], (82, 94)) for i in range(5)] + [None]
    HAM_PROPS = [extract_food(frame(wing_ham, i), ham_crops[i], (88, 92)) for i in range(5)] + [None]
    build(idle_source, drawable / "loem_serpent_hungry_sheet.png", draw_hungry)
    build(idle_source, drawable / "loem_serpent_sleep_sheet.png", draw_sleep)
    feeding_alignment = (-2, -2, 0, 0, 0, 0)
    build(eating_source, drawable / "loem_serpent_melon_sheet.png", draw_melon, feeding_alignment)
    build(eating_source, drawable / "loem_serpent_ham_sheet.png", draw_ham, feeding_alignment)


if __name__ == "__main__":
    main()
