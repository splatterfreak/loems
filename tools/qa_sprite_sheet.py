import argparse
import sys
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageChops


@dataclass
class FrameStats:
    bbox: tuple[int, int, int, int]
    center_x: float
    center_y: float
    area: int
    mask: Image.Image


def analyze(frame: Image.Image, alpha_threshold: int) -> FrameStats:
    mask = frame.getchannel("A").point(lambda value: 255 if value > alpha_threshold else 0)
    bbox = mask.getbbox()
    if bbox is None:
        raise ValueError("Frame has no visible pixels")
    pixels = mask.load()
    points = [
        (x, y)
        for y in range(frame.height)
        for x in range(frame.width)
        if pixels[x, y]
    ]
    area = len(points)
    return FrameStats(
        bbox=bbox,
        center_x=sum(point[0] for point in points) / area,
        center_y=sum(point[1] for point in points) / area,
        area=area,
        mask=mask,
    )


def silhouette_iou(first: Image.Image, second: Image.Image) -> float:
    intersection = ImageChops.multiply(first, second).histogram()[255]
    union = sum(
        1
        for first_value, second_value in zip(
            first.get_flattened_data(),
            second.get_flattened_data(),
        )
        if first_value or second_value
    )
    return intersection / union if union else 1.0


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate a regular Loems sprite sheet.")
    parser.add_argument("sheet", type=Path)
    parser.add_argument("--columns", type=int, default=3)
    parser.add_argument("--rows", type=int, default=2)
    parser.add_argument("--minimum-padding", type=int, default=48)
    parser.add_argument("--maximum-center-drift", type=float, default=2.0)
    parser.add_argument("--maximum-ground-drift", type=int, default=0)
    parser.add_argument("--maximum-height-change", type=float, default=0.02)
    parser.add_argument("--minimum-loop-iou", type=float, default=0.85)
    parser.add_argument("--alpha-threshold", type=int, default=32)
    args = parser.parse_args()

    sheet = Image.open(args.sheet).convert("RGBA")
    if sheet.width % args.columns or sheet.height % args.rows:
        print("FAIL: sheet dimensions do not divide evenly into the requested grid")
        return 1

    cell_width = sheet.width // args.columns
    cell_height = sheet.height // args.rows
    frames = []
    for row in range(args.rows):
        for column in range(args.columns):
            left = column * cell_width
            top = row * cell_height
            frame = sheet.crop((left, top, left + cell_width, top + cell_height))
            frames.append(analyze(frame, args.alpha_threshold))

    failures = []
    heights = []
    for index, frame in enumerate(frames, start=1):
        left, top, right, bottom = frame.bbox
        margins = (left, top, cell_width - right, cell_height - bottom)
        heights.append(bottom - top)
        print(
            f"F{index}: bbox={frame.bbox} margins={margins} "
            f"center=({frame.center_x:.1f},{frame.center_y:.1f}) ground={bottom - 1}"
        )
        if min(margins) < args.minimum_padding:
            failures.append(f"F{index} padding {min(margins)}px < {args.minimum_padding}px")

    center_drift = max(frame.center_x for frame in frames) - min(frame.center_x for frame in frames)
    grounds = [frame.bbox[3] - 1 for frame in frames]
    ground_drift = max(grounds) - min(grounds)
    height_change = (max(heights) - min(heights)) / max(heights)
    loop_ious = [
        silhouette_iou(frames[index].mask, frames[(index + 1) % len(frames)].mask)
        for index in range(len(frames))
    ]
    print(
        f"Summary: center_drift={center_drift:.1f}px ground_drift={ground_drift}px "
        f"height_change={height_change:.1%} minimum_loop_iou={min(loop_ious):.3f}"
    )

    if center_drift > args.maximum_center_drift:
        failures.append(f"center drift {center_drift:.1f}px > {args.maximum_center_drift}px")
    if ground_drift > args.maximum_ground_drift:
        failures.append(f"ground drift {ground_drift}px > {args.maximum_ground_drift}px")
    if height_change > args.maximum_height_change:
        failures.append(f"height change {height_change:.1%} > {args.maximum_height_change:.1%}")
    if min(loop_ious) < args.minimum_loop_iou:
        failures.append(f"loop silhouette IoU {min(loop_ious):.3f} < {args.minimum_loop_iou:.3f}")

    if failures:
        print("FAIL")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
