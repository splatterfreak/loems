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

    majestic_melon = Image.open(DRAWABLE / "loem_wing_evolution_melon_sheet.png").convert("RGBA")
    majestic_ham = Image.open(DRAWABLE / "loem_wing_evolution_ham_sheet.png").convert("RGBA")
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
        "loem_wart_emperor_female_idle_sheet.png": idle_frames,
        "loem_wart_emperor_female_hungry_sheet.png": hungry_frames,
        "loem_wart_emperor_female_sleep_sheet.png": sleep_frames,
        "loem_wart_emperor_female_melon_sheet.png": melon_frames,
        "loem_wart_emperor_female_ham_sheet.png": ham_frames,
    }
    for filename, frames in outputs.items():
        write_sheet(frames, DRAWABLE / filename)

    # Compose supplies combat motion. Locked idle frames guarantee that the female
    # form keeps two wings, two horns, four feet, one tail and one tongue throughout.
    for state in (
        "battle_attack",
        "battle_hit",
        "battle_double_attack",
        "battle_double_hit",
        "battle_victory",
    ):
        write_sheet(
            [current.copy() for current in idle_frames],
            DRAWABLE / f"loem_wart_emperor_female_{state}_sheet.png",
        )


if __name__ == "__main__":
    build()
