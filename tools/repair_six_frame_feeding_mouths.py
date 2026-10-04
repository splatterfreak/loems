#!/usr/bin/env python3
"""Give legacy six-frame feeding sheets a real open/closed jaw cycle.

The existing food stages and full-body drawings remain untouched.  Only a small,
feathered mouth/jaw patch is copied from an approved closed-mouth pose into
frames 2, 4 and 6, producing open -> closed -> reopen cycles without body jitter.
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


CELL = 512
FRAME_COUNT = 6
CLOSED_FRAME_INDICES = (1, 3, 5)


def frame(sheet: Image.Image, index: int) -> Image.Image:
    column = index % 3
    row = index // 3
    return sheet.crop((column * CELL, row * CELL, (column + 1) * CELL, (row + 1) * CELL))


def mouth_mask(box: tuple[int, int, int, int], blur: float) -> Image.Image:
    mask = Image.new("L", (CELL, CELL))
    ImageDraw.Draw(mask).ellipse(box, fill=255)
    return mask.filter(ImageFilter.GaussianBlur(blur))


def food_mask(image: Image.Image, box: tuple[int, int, int, int]) -> Image.Image:
    """Keep the largest saturated prop component and its outline above the jaw patch."""
    rgba = image.convert("RGBA")
    source = rgba.load()
    left, top, right, bottom = box
    candidates: set[tuple[int, int]] = set()
    for y in range(top, bottom):
        for x in range(left, right):
            red, green, blue, alpha = source[x, y]
            maximum = max(red, green, blue)
            minimum = min(red, green, blue)
            saturated = maximum - minimum >= 38
            warm_food = red >= 105 and red >= green * 1.08 and red >= blue * 1.18
            green_rind = green >= 78 and green >= red * 0.52 and green >= blue * 1.18
            if alpha > 16 and saturated and (warm_food or green_rind):
                candidates.add((x, y))

    components: list[set[tuple[int, int]]] = []
    while candidates:
        pending = [candidates.pop()]
        component = {pending[0]}
        while pending:
            x, y = pending.pop()
            for neighbor in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                if neighbor in candidates:
                    candidates.remove(neighbor)
                    component.add(neighbor)
                    pending.append(neighbor)
        components.append(component)

    mask = Image.new("L", rgba.size)
    if components:
        eligible = [component for component in components if len(component) >= 18]
        if eligible:
            food_component = max(
                eligible,
                key=lambda component: sum(x for x, _ in component) / len(component),
            )
            target = mask.load()
            for x, y in food_component:
                target[x, y] = 255
    return mask.filter(ImageFilter.MaxFilter(7)).filter(ImageFilter.GaussianBlur(0.7))


def repair_sheet(
    target_path: Path,
    closed_pose: Image.Image,
    mask: Image.Image,
    food_box: tuple[int, int, int, int],
    protect_food_box: bool,
) -> None:
    target = Image.open(target_path).convert("RGBA")
    if target.size != (CELL * 3, CELL * 2):
        raise ValueError(f"Expected a 3x2 512px sheet: {target_path} ({target.size})")

    repaired: list[Image.Image] = []
    for index in range(FRAME_COUNT):
        current = frame(target, index)
        if index in CLOSED_FRAME_INDICES:
            original = current
            active_mask = mask
            if protect_food_box and index != FRAME_COUNT - 1:
                active_mask = mask.copy()
                ImageDraw.Draw(active_mask).rectangle(food_box, fill=0)
            current = Image.composite(closed_pose, current, active_mask)
            if not protect_food_box and index != FRAME_COUNT - 1:
                current = Image.composite(original, current, food_mask(original, food_box))
        repaired.append(current)

    output = Image.new("RGBA", target.size)
    for index, current in enumerate(repaired):
        output.alpha_composite(current, ((index % 3) * CELL, (index // 3) * CELL))
    output.save(target_path, "WEBP", lossless=True, quality=100, method=6)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("melon_sheet", type=Path)
    parser.add_argument("ham_sheet", type=Path)
    parser.add_argument("closed_source_sheet", type=Path)
    parser.add_argument("--closed-frame", type=int, default=0)
    parser.add_argument("--mouth-box", type=int, nargs=4, required=True)
    parser.add_argument("--food-box", type=int, nargs=4, required=True)
    parser.add_argument(
        "--protect-food-box",
        action="store_true",
        help="Keep the original food rectangle untouched instead of segmenting the prop.",
    )
    parser.add_argument("--blur", type=float, default=2.0)
    args = parser.parse_args()

    source = Image.open(args.closed_source_sheet).convert("RGBA")
    if source.size == (CELL * 3, CELL * 2):
        closed_pose = frame(source, args.closed_frame)
    elif source.size == (CELL, CELL):
        closed_pose = source
    else:
        raise ValueError(f"Unsupported closed-pose source: {source.size}")
    mask = mouth_mask(tuple(args.mouth_box), args.blur)

    food_box = tuple(args.food_box)
    repair_sheet(args.melon_sheet, closed_pose, mask, food_box, args.protect_food_box)
    repair_sheet(args.ham_sheet, closed_pose, mask, food_box, args.protect_food_box)


if __name__ == "__main__":
    main()
