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


def build_gender(gender: str, food_centers: tuple[tuple[int, int], ...]) -> None:
    prefix = f"loem_gloom_wizard_poop_{gender}"
    idle_sheet = Image.open(PREVIEWS / f"{prefix}_idle_sheet.png").convert("RGBA")
    sleep_sheet = Image.open(PREVIEWS / f"{prefix}_sleep_sheet.png").convert("RGBA")
    feeding_sheet = Image.open(PREVIEWS / f"{prefix}_feeding_sheet.png").convert("RGBA")

    idle_frames = [frame(idle_sheet, index) for index in range(FRAME_COUNT)]
    sleep_frames = [frame(sleep_sheet, index) for index in range(FRAME_COUNT)]
    hungry_frames = [frame(feeding_sheet, index) for index in range(FRAME_COUNT)]

    melon_source = Image.open(DRAWABLE / "loem_wing_evolution_melon_sheet.webp").convert("RGBA")
    ham_source = Image.open(DRAWABLE / "loem_wing_evolution_ham_sheet.webp").convert("RGBA")
    melon_props = [extract_food(frame(melon_source, index)) for index in range(5)]
    ham_props = [extract_food(frame(ham_source, index)) for index in range(5)]

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
        f"{prefix}_idle_sheet.webp": idle_frames,
        f"{prefix}_hungry_sheet.webp": hungry_frames,
        f"{prefix}_sleep_sheet.webp": sleep_frames,
        f"{prefix}_melon_sheet.webp": melon_frames,
        f"{prefix}_ham_sheet.webp": ham_frames,
    }
    for filename, frames in outputs.items():
        write_sheet(frames, DRAWABLE / filename)

    # Compose supplies battle movement; all static combat slots reuse the idle WebP.


def build() -> None:
    build_gender(
        "male",
        ((318, 340), (311, 337), (302, 334), (294, 332), (300, 336)),
    )
    build_gender(
        "female",
        ((315, 340), (308, 337), (299, 334), (291, 332), (297, 336)),
    )


if __name__ == "__main__":
    build()
