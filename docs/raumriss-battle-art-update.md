# Raumriss battle art update — 2026-09-12

**Historical report from 2026-09-12.** The blockers below were repaired in the 2026-09-13 follow-up. See [current completion and QA report](battle-animation-completion.md) for the final state and APK; the old build hash below is historical.

Final recolor diagnostic (`art/qa/projectiles/recolor-review.jpg`) found incomplete recoloring in several skin areas: particularly Urkröte attack, Ultra double attack and Erzmagier attack; the other wizard poses also need neutral-skin cleanup. Inspecting the full-resolution Ultra double-attack source found the right wing too close to/cut by the source boundary; it must be regenerated, not merely rescaled. Numeric alpha/crop checks cannot detect damage already present in a source. These findings supersede the preliminary visual notes below. Canonical cutouts and mapped sheets are retained as work in progress so the task can resume without losing work.

## Delivered scope

Four existing sheets repaired: attack and double attack for Raumriss-Urkröte and Raumriss-Weltschlange. Twelve sheets added: attack, hit, double attack, double hit, victory, defeat for Ultra-Raumriss-Löm and Ultra-Armageddon-Raumriss-Erzmagierhaufen-Löm.

The 66 pre-update battle sheets were visually audited with `tools/audit_battle_art.py`; only the four replaced attack sheets contained outgoing shots. The game's `BattleProjectileEffect` remains the sole owner of traveling projectiles. Permanent chest/tail portals, the wizard's held orb and staff, and stationary hit flashes are not outgoing projectiles.

## Generation brief and final prompt set

Built-in image generation/editing was used, not the API/CLI fallback. Source cutouts are saved as `art/intermediate/loem_{form}_{state}_canonical.png`; the four repaired poses use `_no_projectile.png`. Rejected checkerboard/RGB outputs were not integrated.

Shared brief: preserve the approved character's anatomy, equipment, camera angle and ornament colors. Full silhouette with all tips visible. Body light neutral gray for runtime recoloring. Genuine transparent alpha background. No fired projectiles, beams, traveling energy balls, outgoing trails, floating icons or detached props. Express the action through posture, limbs, mouth and recoil only. Body-attached portals and held equipment stay contained.

| State | Ultra-Raumriss pose | Erzmagierhaufen pose |
|---|---|---|
| Attack | Braced forward roar, open mouth | Determined forward stance, staff held left, orb supported right |
| Hit | Grimace, closed eyes and recoil | Wincing, closed eyes, bracing with equipment held |
| Double attack | Strong low lunge, wings flexed | Strong shouting stance; staff top lowered to hat height to protect apparent body size |
| Double hit | Seated recoil, forepaws raised | Strong seated recoil, equipment retained, stationary impact flashes |
| Victory | Proud smile and raised front paw | Joyful smile and raised staff |
| Defeat | Belly and head lowered to ground, wings folded | Slumped seated posture, exhausted closed eyes, staff and cyan orb still held |

Repair prompt for Urkröte/Weltschlange attack and double attack: remove the complete outgoing beam/shot/trail; preserve the character pose and all anatomy; replace emitted energy with a contained flat body portal; transparent background and complete silhouette.

Successful final wizard refinement: “Edit only the staff-holding arm pose: lower the hand to chest height and angle the same staff left, so the staff's top crystal sits at the SAME HEIGHT as the hat top, not above the hat. Keep determined shouting face, braced legs and forward double-attack posture, robe, cyan orb in right palm. Preserve transparent background. Full staff must remain visible, no shots, no beams or external effects.” Follow-up: “Isolate the wizard on a transparent background.”

## Assembly and QA

- 2048×1536 lossless WebP, 4×3 cells of 512×512, 12 frames at 100 ms each. True RGBA, hidden RGB under alpha zero cleared, pixel-identical WebP round-trip checked across all channels.
- Dedicated key poses with controlled offsets and repeated frames, not twelve independently painted poses. Attacks/hits return to the exact idle pose; the new double actions use two impulses. Outcomes settle and hold their last three frames; real battle playback is non-looping.
- Deliberate motion allowances: horizontal lunge/recoil, up to 12 px outcome bob/settling (the older correction builder allows 13 px lunge). Full-silhouette center includes wings/tails and may move more than the torso. These battle clips are not evaluated with idle-only 2 px limits.
- All 16 sheets pass `tools/qa_sprite_sheet.py`, using alpha threshold zero, minimum 48 px padding and explicit battle-motion tolerances. Additional checks prove every nontransparent pixel lies inside the actual app crop, not merely inside its cell. The Weltschlange double attack was reduced from a 295 px maximum height to 278 px to keep its raised wing intact inside the existing crop.
- Per-frame alpha bounds, margins, alpha center and ground line: [frame report](../art/qa/projectiles/frame-report.md). Machine-readable results: [results](../art/qa/projectiles/results.json).
- App crops (repeated at 512 px cell offsets): Ultra `(48,96,416,368)`; Erzmagier `(48,48,416,416)`; Urkröte `(48,176,416,288)`; Weltschlange `(48,160,416,304)`. Dimensions and crop anchors remain unchanged from passive states. New actions reuse the production idle at their start/end; defeated silhouettes are intentionally lower.
- Preliminary contact-sheet review: no checkerboard, detached outgoing shots or neighboring cells. Attacks and outcomes read distinctly at game size. Final full-resolution/recolor review found the blocking issues listed above; visual QA is **not passed**. Matching GIFs use exact app crops and are draft previews only.
- Anatomy: Ultra and Weltschlange each retain one head, two eyes, two horns, two wings, four limbs and one tail. Urkröte retains one head, two eyes, two horns, two wings, four limbs and one tail. Erzmagier retains one stacked head/body, two eyes, two arms/hands, two feet, one hat, one staff on image-left and one orb supported on image-right. Perspective may occlude limbs; no swapped attachment sides are used.
- All twelve new states are registered in both gameplay and debug selection. Existing outcome flow selects victory/defeat in `RESULT_HOLD` and passes `playOnceAndHold = true`; this change does not alter gameplay values or rules.
- `testDebugUnitTest assembleDebug`: successful, 111 tests and zero failures. Device/emulator gameplay was not exercised in this pass. Build generated 2026-09-12 20:18, SHA-256 `803B46420F65942E4C8311C8879E304C7DA10646994A73E7EBA7E0D7A2A8328B`; draft only, final visual approval outstanding.

## Permanent rules and remaining gaps

The no-projectile rule is recorded in both `AGENTS.md` and `.codex/skills/sprite-generator/SKILL.md`. The canonical [animation inventory](animation-inventory.md) was updated in the same change. It counts dedicated artwork only, not fallback mappings.

All four Raumriss forms have all eleven states. The eighteen non-Raumriss gaps described in this historical report were filled on 2026-09-13: Wurst-Löm and both Trübsal-Zauberhaufen variants now have their six dedicated battle states. See the canonical inventory for current coverage. Junges Löm is not battle-capable.
