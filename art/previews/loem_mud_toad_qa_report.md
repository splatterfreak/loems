# Matschkröten-Löm sprite QA

- Status: PASS and integrated into gameplay plus the debug picker.
- Sheets: idle, hungry, sleep, melon, ham, attack, hit, double attack, double hit, and victory.
- Image size: 1536 x 1024 px per sheet, RGBA.
- Layout: 3 columns x 2 rows, 6 frames, 512 x 512 px per cell.
- App crop: fixed 416 x 320 px rectangles at `(48,144)`, `(560,144)`, `(1072,144)`, `(48,656)`, `(560,656)`, and `(1072,656)`.
- Anatomy inventory: 1 head; 2 eyes with the far eye occluded; 2 horns; 2 limp wings; 4 legs/feet; 1 tail; fixed missing-tooth pattern; 1 attached tongue where the state uses it.
- Anatomy continuity: PASS across every frame and state.
- Recolor safety: PASS by visual inspection; body pixels remain neutral gray while eyes, mouth, tongue, cheek, horns, outlines, food, and muddy wings remain protected colors.
- Clipping and neighbor bleed: PASS with at least 48 px padding.
- Body jitter: PASS; torso and feet are locked, ground drift is 0 px, body height change is 0.0%.
- Idle/hungry/battle full-alpha center drift: at most 0.5 px.
- Feeding full-alpha center drift: 4.9 px for melon and 6.1 px for ham due solely to the food prop; the underlying body center remains fixed.
- Visual inspection: PASS; flattened heavy silhouette, collapsed muddy wings, tooth gaps, sleeping tongue tip, and downward curling frog tongue remain readable without changing the canonical anatomy.
