from __future__ import annotations

from PIL import Image

from generate_wart_emperor_male_state_sheets import (
    DRAWABLE,
    FRAME_COUNT,
    PREVIEWS,
    add_food,
    extract_food,
    frame,
    write_sheet,
)


def build() -> None:
    idle_sheet = Image.open(PREVIEWS / "loem_wart_emperor_female_idle_sheet.png").convert("RGBA")
    sleep_sheet = Image.open(PREVIEWS / "loem_wart_emperor_female_sleep_sheet.png").convert("RGBA")
    feeding_sheet = Image.open(PREVIEWS / "loem_wart_emperor_female_feeding_sheet.png").convert("RGBA")

    idle_frames = [frame(idle_sheet, index) for index in range(FRAME_COUNT)]
    sleep_frames = [frame(sleep_sheet, index) for index in range(FRAME_COUNT)]
    hungry_frames = [frame(feeding_sheet, index) for index in range(FRAME_COUNT)]

    majestic_melon = Image.open(DRAWABLE / "loem_wing_evolution_melon_sheet.webp").convert("RGBA")
    majestic_ham = Image.open(DRAWABLE / "loem_wing_evolution_ham_sheet.webp").convert("RGBA")
    melon_props = [extract_food(frame(majestic_melon, index)) for index in range(5)]
    ham_props = [extract_food(frame(majestic_ham, index)) for index in range(5)]
    food_centers = ((414, 382), (408, 374), (399, 367), (391, 363), (398, 371))

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
        "loem_wart_emperor_female_idle_sheet.webp": idle_frames,
        "loem_wart_emperor_female_hungry_sheet.webp": hungry_frames,
        "loem_wart_emperor_female_sleep_sheet.webp": sleep_frames,
        "loem_wart_emperor_female_melon_sheet.webp": melon_frames,
        "loem_wart_emperor_female_ham_sheet.webp": ham_frames,
    }
    for filename, frames in outputs.items():
        write_sheet(frames, DRAWABLE / filename)

    # Compose supplies combat motion; all static combat slots reuse the idle WebP.


if __name__ == "__main__":
    build()
