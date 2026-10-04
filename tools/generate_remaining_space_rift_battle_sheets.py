"""Assemble the approved dedicated cosmic/archmage key poses; never draw shots."""
from pathlib import Path
import argparse
from PIL import Image
from generate_space_rift_battle_sheets import (
    DRAWABLE, INTERMEDIATE, clear_transparent_rgb, prepare_canonical,
    positioned_frames, write_sheet,
)

STATES = ('attack', 'hit', 'double_attack', 'double_hit', 'victory', 'defeat')
FORMS = ('ultra_cosmic', 'space_rift_archmage_poop')
OFFSETS = {
    'attack': ((0, 0), (0, 0), (-5, 0), (2, 0), (8, 0), (12, 0),
               (7, 0), (4, 0), (0, 0), (-3, 0), (0, 0), (0, 0)),
    'hit': ((0, 0), (0, 0), (-4, -2), (-9, -4), (-12, -2), (-6, 0),
             (-3, 0), (-1, 0), (0, 0), (0, 0), (0, 0), (0, 0)),
    'double_attack': ((0, 0), (0, 0), (5, 0), (12, 0), (-4, 0), (0, 0),
                      (5, 0), (12, 0), (-3, 0), (0, 0), (0, 0), (0, 0)),
    'double_hit': ((0, 0), (0, 0), (-6, -3), (-11, -5), (-4, 0), (0, 0),
                   (-10, -4), (-6, -2), (-2, 0), (0, 0), (0, 0), (0, 0)),
    'victory': ((0, 0), (0, -3), (0, -6), (0, -10), (0, -12), (0, -8),
                (0, -4), (0, -2), (0, -4), (0, 0), (0, 0), (0, 0)),
    'defeat': ((0, -12), (-2, -8), (2, -4), (-2, -2), (1, 0), (0, -1),
               (-1, 0), (1, 0), (0, 0), (0, 0), (0, 0), (0, 0)),
}


def build(form: str, states: list[str]) -> None:
    maximum = (376, 330) if form == 'ultra_cosmic' else (376, 376)
    idle = clear_transparent_rgb(Image.open(DRAWABLE / f'loem_{form}_idle_sheet.webp')
                                 .convert('RGBA').crop((0, 0, 512, 512)))
    # Preserve the existing idle exactly, including its torso anchor and ground.
    idle_bbox = idle.getchannel('A').getbbox()
    ground = idle_bbox[3] - 1
    for state in states:
        path = INTERMEDIATE / f'loem_{form}_{state}_canonical.png'
        source = Image.open(path)
        if source.mode != 'RGBA' or source.getchannel('A').getextrema() != (0, 255):
            raise ValueError(f'Not a transparent canonical cutout: {path}')
        pose = prepare_canonical(path, maximum)
        frames = positioned_frames((pose,) * 12, OFFSETS[state], ground_y=ground)
        if state not in ('victory', 'defeat'):
            for index in (0, 1, 10, 11):
                frames[index] = idle.copy()
            if state.startswith('double_'):
                frames[5] = idle.copy()
        write_sheet(frames, DRAWABLE / f'loem_{form}_battle_{state}_sheet.webp')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--forms', nargs='+', choices=FORMS, default=list(FORMS))
    parser.add_argument('--states', nargs='+', choices=STATES, default=list(STATES))
    args = parser.parse_args()
    for form in args.forms:
        build(form, args.states)
