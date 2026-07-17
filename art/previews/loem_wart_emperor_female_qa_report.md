# Warzenkaiserin-Löm integration QA

- Status: PASS; gameplay and all sprite states integrated.
- Sheets: idle, hungry, sleep, melon, ham, attack, hit, double attack, double hit, victory.
- Image size: 1536 x 1024 px, RGBA; 3 columns x 2 rows with 512 x 512 px cells.
- App crop: the same six `416 x 320` rectangles used by the male variant.
- Evolution: female Matschkröten-Löm at the same deterministic adult window from 336 to 384 hours.
- Values: identical to the male Warzenkaiser-Löm: 38,000 g healthy weight, 23 base strength, 22 base defense.
- Anatomy inventory: 1 head; 2 eyes with the far eye naturally occluded; 2 swept-back horns; 2 attached muddy wings; 4 legs/feet with perspective overlap; 1 tail; 1 attached tongue.
- Anatomy continuity: PASS across every state and all six frames.
- Recolor safety: PASS; body and warts remain neutral gray while protected facial and wing details remain outside the recolor mask.
- Clipping and neighbor bleed: PASS; 60 px left/right margin and 64 px bottom margin.
- Idle, hungry, sleep and combat jitter: center drift 0.0 px, ground drift 0 px, height change 0.0%, loop IoU 1.000.
- Feeding movement is limited to the established melon or ham prop while the full creature anatomy remains pixel-identical.
- Visual distinction: PASS; slightly smaller silhouette, more poised stance, slimmer muzzle, elegant eyelash, swept-back horns, cleaner wing contours and tidier wart clusters while remaining a powerful wart-covered toad dragon.

## Canonical frame measurements

- Idle/feeding alpha bounding box: `(60, 228, 452, 448)`.
- Sleep alpha bounding box: `(60, 229, 452, 448)`.
- Ground line: `447` in every frame.
