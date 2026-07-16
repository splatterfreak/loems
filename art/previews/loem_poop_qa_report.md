# Haufen-Löm Sprite QA

All production sheets are RGBA PNGs at 1536 x 1024 px with six 512 x 512 px cells in a 3 x 2 layout. Alpha bounding boxes use `(left, top, right, bottom)`, margins use `(left, top, right, bottom)`, centers are alpha-weighted `(x, y)`, and ground is the lowest visible pixel.

## Per-frame measurements

### Idle

- F1 bbox `(56,56,453,448)`, margins `(56,56,59,64)`, center `(263.4,283.0)`, ground `447`
- F2 bbox `(58,56,453,448)`, margins `(58,56,59,64)`, center `(263.9,283.0)`, ground `447`
- F3 bbox `(59,56,453,448)`, margins `(59,56,59,64)`, center `(264.5,283.0)`, ground `447`
- F4 bbox `(59,56,455,448)`, margins `(59,56,57,64)`, center `(265.0,283.0)`, ground `447`
- F5 bbox `(59,56,453,448)`, margins `(59,56,59,64)`, center `(264.5,283.0)`, ground `447`
- F6 bbox `(58,56,453,448)`, margins `(58,56,59,64)`, center `(263.9,283.0)`, ground `447`

### Hungry

- F1 bbox `(59,56,452,448)`, margins `(59,56,60,64)`, center `(263.7,283.2)`, ground `447`
- F2 bbox `(60,53,452,448)`, margins `(60,53,60,64)`, center `(264.1,281.9)`, ground `447`
- F3 bbox `(60,50,451,448)`, margins `(60,50,61,64)`, center `(263.6,280.6)`, ground `447`
- F4 bbox `(60,53,452,448)`, margins `(60,53,60,64)`, center `(264.1,281.9)`, ground `447`
- F5 bbox `(59,56,452,448)`, margins `(59,56,60,64)`, center `(263.7,283.2)`, ground `447`
- F6 bbox `(59,57,453,448)`, margins `(59,57,59,64)`, center `(264.2,283.5)`, ground `447`

### Sleep

- F1 bbox `(56,127,456,448)`, margins `(56,127,56,64)`, center `(264.7,316.9)`, ground `447`
- F2 bbox `(56,124,455,448)`, margins `(56,124,57,64)`, center `(264.2,315.6)`, ground `447`
- F3 bbox `(57,122,455,448)`, margins `(57,122,57,64)`, center `(264.7,314.8)`, ground `447`
- F4 bbox `(56,124,455,448)`, margins `(56,124,57,64)`, center `(264.2,315.6)`, ground `447`
- F5 bbox `(56,127,456,448)`, margins `(56,127,56,64)`, center `(264.7,316.9)`, ground `447`
- F6 bbox `(55,128,456,448)`, margins `(55,128,56,64)`, center `(264.2,317.3)`, ground `447`

### Melon

- F1 bbox `(90,111,464,449)`, margins `(90,111,48,63)`, center `(272.1,309.0)`, ground `448`
- F2 bbox `(88,112,427,449)`, margins `(88,112,85,63)`, center `(264.9,306.7)`, ground `448`
- F3 bbox `(92,112,432,449)`, margins `(92,112,80,63)`, center `(269.0,306.4)`, ground `448`
- F4 bbox `(93,111,432,449)`, margins `(93,111,80,63)`, center `(268.5,305.4)`, ground `448`
- F5 bbox `(86,111,425,449)`, margins `(86,111,87,63)`, center `(262.1,305.6)`, ground `448`
- F6 bbox `(87,111,426,449)`, margins `(87,111,86,63)`, center `(263.0,305.7)`, ground `448`

### Ham

- F1 bbox `(86,112,464,448)`, margins `(86,112,48,64)`, center `(270.7,308.4)`, ground `447`
- F2 bbox `(88,112,461,448)`, margins `(88,112,51,64)`, center `(271.4,307.7)`, ground `447`
- F3 bbox `(96,112,435,447)`, margins `(96,112,77,65)`, center `(272.0,305.3)`, ground `446`
- F4 bbox `(88,112,464,447)`, margins `(88,112,48,65)`, center `(269.2,307.2)`, ground `446`
- F5 bbox `(86,113,439,448)`, margins `(86,113,73,64)`, center `(263.1,306.5)`, ground `447`
- F6 bbox `(86,113,436,448)`, margins `(86,113,76,64)`, center `(263.4,306.4)`, ground `447`

### Attack

- F1 bbox `(64,88,440,449)`, margins `(64,88,72,63)`, center `(259.4,296.8)`, ground `448`
- F2 bbox `(62,88,440,449)`, margins `(62,88,72,63)`, center `(258.6,296.5)`, ground `448`
- F3 bbox `(51,122,437,449)`, margins `(51,122,75,63)`, center `(258.7,311.2)`, ground `448`
- F4 bbox `(58,94,464,449)`, margins `(58,94,48,63)`, center `(259.2,297.0)`, ground `448`
- F5 bbox `(56,127,435,449)`, margins `(56,127,77,63)`, center `(257.7,312.6)`, ground `448`
- F6 bbox `(67,92,435,448)`, margins `(67,92,77,64)`, center `(258.7,297.4)`, ground `447`

### Hit

- F1 bbox `(61,76,441,448)`, margins `(61,76,71,64)`, center `(258.9,290.6)`, ground `447`
- F2 bbox `(61,78,441,447)`, margins `(61,78,71,65)`, center `(259.2,291.2)`, ground `446`
- F3 bbox `(49,135,451,448)`, margins `(49,135,61,64)`, center `(259.6,319.7)`, ground `447`
- F4 bbox `(48,190,440,448)`, margins `(48,190,72,64)`, center `(256.8,344.0)`, ground `447`
- F5 bbox `(60,112,438,448)`, margins `(60,112,74,64)`, center `(256.7,304.3)`, ground `447`
- F6 bbox `(75,88,439,448)`, margins `(75,88,73,64)`, center `(259.5,295.3)`, ground `447`

### Double attack

- F1 bbox `(82,110,421,448)`, margins `(82,110,91,64)`, center `(258.7,305.9)`, ground `447`
- F2 bbox `(78,111,439,448)`, margins `(78,111,73,64)`, center `(259.0,303.7)`, ground `447`
- F3 bbox `(77,111,463,448)`, margins `(77,111,49,64)`, center `(258.5,306.2)`, ground `447`
- F4 bbox `(77,111,462,448)`, margins `(77,111,50,64)`, center `(257.5,305.7)`, ground `447`
- F5 bbox `(77,135,413,448)`, margins `(77,135,99,64)`, center `(257.0,318.4)`, ground `447`
- F6 bbox `(82,113,420,448)`, margins `(82,113,92,64)`, center `(258.5,307.0)`, ground `447`

### Double hit

- F1 bbox `(85,120,423,448)`, margins `(85,120,89,64)`, center `(259.5,308.7)`, ground `447`
- F2 bbox `(50,166,434,447)`, margins `(50,166,78,65)`, center `(259.5,330.7)`, ground `446`
- F3 bbox `(81,121,422,447)`, margins `(81,121,90,65)`, center `(258.6,309.0)`, ground `446`
- F4 bbox `(48,211,453,448)`, margins `(48,211,59,64)`, center `(257.9,352.3)`, ground `447`
- F5 bbox `(56,252,448,447)`, margins `(56,252,64,65)`, center `(258.0,368.5)`, ground `446`
- F6 bbox `(72,230,439,448)`, margins `(72,230,73,64)`, center `(260.0,363.7)`, ground `447`

### Victory

- F1 bbox `(69,86,433,448)`, margins `(69,86,79,64)`, center `(258.7,295.2)`, ground `447`
- F2 bbox `(69,84,437,448)`, margins `(69,84,75,64)`, center `(260.9,294.3)`, ground `447`
- F3 bbox `(62,86,462,448)`, margins `(62,86,50,64)`, center `(259.8,294.7)`, ground `447`
- F4 bbox `(73,90,429,448)`, margins `(73,90,83,64)`, center `(258.8,296.6)`, ground `447`
- F5 bbox `(71,101,431,448)`, margins `(71,101,81,64)`, center `(258.6,302.1)`, ground `447`
- F6 bbox `(74,102,428,448)`, margins `(74,102,84,64)`, center `(258.4,302.6)`, ground `447`

## App crops and result

Every Haufen-Löm state uses the same stable 416 x 416 app crop per cell: `(48,48)`, `(560,48)`, `(1072,48)`, `(48,560)`, `(560,560)`, `(1072,560)`. Compared with the Wurst-Löm's variable 299-417 px wide and 189-272 px high crops, the Haufen-Löm crop is intentionally larger and fixed; its idle alpha fill is about 397 x 392 px, so it reads as a giant mound without render pulsing.

- Clipping and neighbor bleed: PASS; minimum margin is 48 px.
- Ground stability: PASS; idle is fixed, other states vary by at most one antialiased pixel.
- Idle jitter: PASS; center drift is 1.6 px with deliberate lower-mound sway.
- Recolor safety: PASS; body/limbs are neutral gray, while outlines, eyes, tooth, cheeks, mouth and food remain outside the mask.
- Visual readability: PASS; the form is clearly a larger, weaker follow-up to Wurst-Löm. Hit animations deliberately squash the mound while maintaining its anchor and ground.
- Debug registration: PASS; all ten state/battle sheets are individually selectable in the debug sprite picker.
- Build verification: `testDebugUnitTest` and `assembleDebug` PASS.
