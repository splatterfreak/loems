#!/usr/bin/env python3
"""Apply the progressive mouth-cycle standard to every affected legacy form."""

from __future__ import annotations

from pathlib import Path

from PIL import Image

from repair_six_frame_feeding_mouths import frame, mouth_mask, repair_sheet


ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
PREVIEWS = ROOT / "art" / "previews"


# prefix, closed-source filename, source frame, mouth box, food box, protect food
CONFIGS = (
    ("loem_mud_toad", "loem_mud_toad_sleep_sheet.webp", 0, (295, 310, 395, 380), (350, 300, 470, 440), False),
    ("loem_gloom_wizard_poop_male", "loem_gloom_wizard_poop_male_sleep_sheet.webp", 0, (205, 310, 340, 385), (245, 275, 410, 410), False),
    ("loem_gloom_wizard_poop_female", "loem_gloom_wizard_poop_female_sleep_sheet.webp", 0, (205, 310, 340, 385), (245, 275, 410, 410), False),
    ("loem_armageddon_serpent_male", "loem_armageddon_serpent_male_idle_sheet.webp", 0, (360, 230, 455, 325), (370, 260, 480, 390), False),
    ("loem_armageddon_serpent_female", "loem_armageddon_serpent_female_idle_sheet.webp", 0, (360, 230, 455, 325), (370, 260, 480, 390), False),
    ("loem_stormkaiser", "loem_stormkaiser_melon_sheet.webp", 5, (335, 225, 450, 345), (330, 235, 485, 420), False),
    ("loem_stormkaiser_female", "loem_stormkaiser_female_melon_sheet.webp", 5, (335, 225, 455, 345), (330, 235, 485, 420), False),
    ("loem_wart_emperor_male", "loem_wart_emperor_male_closed_chew_canonical.webp", 0, (290, 280, 455, 455), (385, 285, 490, 405), True),
    ("loem_wart_emperor_female", "loem_wart_emperor_female_closed_chew_canonical.webp", 0, (290, 280, 455, 455), (385, 285, 490, 405), True),
)


def main() -> None:
    for prefix, source_name, source_frame, mouth_box_values, food_box, protect in CONFIGS:
        source_path = (
            PREVIEWS / source_name
            if source_name.endswith("_canonical.webp")
            else DRAWABLE / source_name
        )
        source = Image.open(source_path).convert("RGBA")
        closed_pose = frame(source, source_frame) if source.size == (1536, 1024) else source
        repair_sheet(
            DRAWABLE / f"{prefix}_melon_sheet.webp",
            closed_pose,
            mouth_mask(mouth_box_values, 1.0),
            food_box,
            protect,
        )
        repair_sheet(
            DRAWABLE / f"{prefix}_ham_sheet.webp",
            closed_pose,
            mouth_mask(mouth_box_values, 1.0),
            food_box,
            protect,
        )


if __name__ == "__main__":
    main()
