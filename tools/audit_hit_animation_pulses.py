"""Audit normal battle-hit sheets for multiple horizontal recoil pulses."""
from __future__ import annotations

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "app/src/main/res/drawable-nodpi"
REPORT = ROOT / "art/qa/hit-animation-pulse-audit.md"


def frame_centers(path: Path) -> list[float]:
    image = Image.open(path).convert("RGBA")
    columns = 4 if image.width == 2048 else 3
    rows = 3 if image.height == 1536 else 2
    cell_width = image.width // columns
    cell_height = image.height // rows
    centers: list[float] = []
    for index in range(columns * rows):
        x = index % columns * cell_width
        y = index // columns * cell_height
        bounds = image.crop((x, y, x + cell_width, y + cell_height)).getchannel("A").getbbox()
        if bounds is None:
            raise ValueError(f"Empty frame {index + 1} in {path.name}")
        centers.append((bounds[0] + bounds[2]) / 2)
    return centers


def has_second_recoil(centers: list[float]) -> bool:
    # A normal hit has one leftward extremum then recovers. Ignore sub-pixel bounds noise.
    movement = [center - centers[0] for center in centers]
    trough = movement.index(min(movement))
    return any(
        movement[index] < movement[index - 1] - 2
        for index in range(trough + 2, len(movement))
    )


def main() -> int:
    hits = sorted(DRAWABLE.glob("loem_*_battle_hit_sheet.webp"))
    lines = [
        "# Treffer-Puls-Audit",
        "",
        "Prüft nur normale `battle_hit`-Sheets. Ein Treffer darf genau einen Rückstoß "
        "und eine Erholung enthalten; `battle_double_hit` ist absichtlich ausgeschlossen.",
        "",
        "| Sheet | X-Mittelpunkte je Frame | Zweiter Rückstoß im Sheet |",
        "|---|---|---|",
    ]
    flagged: list[Path] = []
    for path in hits:
        centers = frame_centers(path)
        second_recoil = has_second_recoil(centers)
        if second_recoil:
            flagged.append(path)
        values = ", ".join(f"{value:.1f}" for value in centers)
        lines.append(f"| `{path.name}` | {values} | {'ja' if second_recoil else 'nein'} |")

    lines.extend(
        [
            "",
            "## Ergebnis",
            "",
            "- Sheets mit zweitem Rückstoß: " +
            (", ".join(f"`{path.name}`" for path in flagged) if flagged else "keine"),
            "- Die Laufzeitbewegung wird zusätzlich im `BattleSequence` geprüft: ein normaler "
            "Treffer nutzt eine einzige Sinus-Halbwelle; damit kann sie keinen zweiten Puls erzeugen.",
        ],
    )
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(REPORT)
    return 1 if flagged else 0


if __name__ == "__main__":
    raise SystemExit(main())
