# Warzenkaiser-Löm male integration QA

- Status: PASS; gameplay and all sprite states integrated.
- Sheets: idle, hungry, sleep, melon, ham, attack, hit, double attack, double hit, victory.
- Image size: 1536 x 1024 px, RGBA.
- Layout: 3 columns x 2 rows, 6 frames, 512 x 512 px per cell.
- App crop: six `416 x 320` rectangles, inset 48 px horizontally and 144 px vertically per cell.
- Evolution: male Matschkröten-Löm at the deterministic adult window between 336 and 384 hours; the female uses the parallel Warzenkaiserin-Löm variant with identical values.
- Weight profile: 38,000 g healthy; 19,000 g minimum; 114,000 g maximum.
- Battle base values: 23 strength, 22 defense.
- Anatomy inventory: 1 head; 2 eyes with the far eye naturally occluded; 2 horns; 2 attached wings; 4 legs/feet with the far rear leg naturally overlapped; 1 tail; 1 attached tongue; fixed wart and tooth patterns.
- Anatomy continuity: PASS across every state and all six frames.
- Recolor safety: PASS; the body and integrated warts are neutral gray while eyes, teeth, tongue, cheek, horns, outlines and muddy wing membranes remain protected colors.
- Clipping and neighbor bleed: PASS; minimum left/right margin 52 px and bottom margin 64 px.
- Idle, hungry, sleep and combat jitter: center drift 0.0 px, ground drift 0 px, height change 0.0%, loop IoU 1.000.
- Melon: center drift 0.7 px, ground drift 0 px, loop IoU 0.993; movement comes only from the food prop.
- Ham: center drift 2.4 px, ground drift 0 px, loop IoU 0.987; movement comes only from the food prop.
- Visual readability: PASS; larger horns, broad toad body, wart clusters, ragged wings, crooked/missing teeth, heavy brow and thick tongue clearly communicate the male third evolution.

## Canonical frame measurements

- Idle/sleep alpha bounding box: `(52, 221, 460, 448)`.
- Hungry/feeding alpha bounding box: `(52, 217, 460, 448)`.
- Margins: left 52 px, right 52 px, bottom 64 px.
- Ground line: `447` in every frame.
