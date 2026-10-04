# Löm animation inventory

This is the canonical coverage list for production sprite mappings. Update it whenever a sprite asset or runtime mapping changes.

Legend: `yes` means dedicated state artwork is wired into gameplay; `missing` means gameplay currently uses idle or a static fallback; `n/a` means the form cannot use the state.

All forms currently have dedicated mappings for idle, hungry, sleep, melon feeding, and ham feeding. Some sheets intentionally contain repeated poses and therefore fewer unique bitmap frames.

| Löm form | Attack | Hit | Double attack | Double hit | Victory | Defeat |
|---|---|---|---|---|---|---|
| Junges Löm | n/a | n/a | n/a | n/a | n/a | n/a |
| Flügel-Löm | yes | yes | yes | yes | yes | yes (knockout sheet) |
| Wurst-Löm | yes | yes | yes | yes | yes | yes |
| Majestätischer Flügel-Löm | yes | yes | yes | yes | yes | yes (knockout sheet) |
| Prunkschlangen-Löm | yes | yes | yes | yes | yes | yes (knockout sheet) |
| Haufen-Löm | yes | yes | yes | yes | yes | yes (knockout sheet) |
| Matschkröten-Löm | yes | yes | yes | yes (defeat sheet) | yes | yes |
| Sturmkaiser-Löm | yes | yes | yes | yes | yes | yes (knockout sheet) |
| Sturmkaiserin-Löm | yes | yes | yes | yes | yes | yes |
| Warzenkaiser-Löm | yes | yes | yes | yes | yes | yes |
| Warzenkaiserin-Löm | yes | yes | yes | yes | yes | yes |
| Trübsal-Zauberhaufen-Löm | yes | yes | yes | yes | yes | yes |
| Trübsal-Zauberhaufen-Lömin | yes | yes | yes | yes | yes | yes |
| Armageddon-Prunkschlangenkaiser-Löm | yes | yes | yes | yes | yes | yes |
| Armageddon-Prunkschlangenkaiserin-Löm | yes | yes | yes | yes | yes | yes |
| Ultra-Raumriss-Löm | yes | yes | yes | yes | yes | yes |
| Raumriss-Urkröte | yes | yes | yes | yes | yes | yes |
| Raumriss-Weltschlange | yes | yes | yes | yes | yes | yes |
| Ultra-Armageddon-Raumriss-Erzmagierhaufen-Löm | yes | yes | yes | yes | yes | yes |

`Junges Löm` cannot battle before its first evolution. A purpose-built final double-hit or knockout animation may serve as defeat when it visibly ends in a defeated pose.

## Projectile-free Raumriss update — 2026-09-12

Historical status (2026-09-12): numeric checks passed, but recolor defects and a clipped Ultra double-attack wing blocked visual approval. The follow-up below tracks their repair; the earlier APK was not the final delivery.

- All four Raumriss forms now have the full eleven-state set: idle, hungry, sleep, melon, ham, attack, hit, double attack, double hit, victory, defeat. All are available in normal gameplay and debug selection.
- Replaced attack and double-attack artwork for Raumriss-Urkröte and Raumriss-Weltschlange: no baked-in shots, beams, or outgoing trails. Permanent contained portals remain part of the character.
- Added six dedicated battle sheets each for Ultra-Raumriss-Löm and Ultra-Armageddon-Raumriss-Erzmagierhaufen-Löm. These are key-pose animations with controlled movement and repeated frames, not twelve independently redrawn poses. Outcome animations play once and hold their final frame in battle.
- The projectile audit inspected all 66 pre-update battle sheets; the four replaced attack sheets were the only sheets containing outgoing shots. New sheets must follow the same no-projectile rule in AGENTS.md and the sprite-generator skill.
- At that stage, Wurst-Löm and both Trübsal-Zauberhaufen variants still lacked six battle states each. These 18 gaps are now filled by the follow-up below.

## Sturmkaiserin update — 2026-09-13

- Added dedicated attack, hit, double attack, double hit, victory and defeat sheets for Sturmkaiserin-Löm.
- Follow-up correction: the dedicated female defeat resource is assigned to STORMKAISER_FEMALE; STORMKAISER_MALE retains its own knockout resource. The prior completion report overlooked this mapping error.
- All six are wired into normal battle result/state selection and debug selection. Victory and defeat use the existing non-looping result-hold flow.
- Character sheets contain no fired projectiles or outgoing trails; elemental shots remain owned by `BattleProjectileEffect`.
- This initially left 18 battle-state gaps, filled by the follow-up below.
- Per-frame measurements and app-crop checks: [QA report](../art/qa/projectiles/frame-report.md). Reviewed animations: [preview gallery](../art/qa/projectiles/index.html).

## Remaining battle states — 2026-09-13

- Added six dedicated battle states each for Wurst-Löm and both Trübsal-Zauberhaufen variants (18 sheets). All have normal gameplay and debug mappings, including dedicated victory and defeat. Both gender variants retain identical timings and gameplay values.
- These are six-frame key-pose sequences with controlled movement and repeated poses, not six independently redrawn poses. Double actions have two pulses. Outcomes play once and hold their purpose-built final pose.
- Wurst uses stable `(48,128,416,336)` crops in 512px cells; both wizards use `(48,48,416,416)`. All production exports are transparent lossless WebP with verified pixel-identical roundtrips and no hidden RGB.
- Repaired Ultra-Raumriss double-attack wing geometry and skin recoloring; corrected Urkröte attack skin and five Erzmagier battle poses. Removed the Erzmagier double-hit impact starbursts too. No character fires its own projectile.
- Current coverage: all 19 forms have the five passive/feeding states; all 18 battle-capable forms have all six battle states. Junges Löm cannot battle. No dedicated-state coverage gaps remain. Older purpose-built knockout/defeat sharing is labeled explicitly above.
- Numeric QA and GIFs: [Wurst](../art/qa/bad-battle/frame-report.md), [Zauberhaufen](../art/qa/gloom-battle/frame-report.md), [Raumriss](../art/qa/projectiles/frame-report.md). Final build and visual review status: [completion report](battle-animation-completion.md).

## Urkröte battle review — 2026-09-15

- Regenerated Raumriss-Urkröte attack, hit, victory and defeat from the retained canonical poses. Wing skin/tips and defeated body surfaces now pass the runtime body-color review; the violet-blue Raumriss effects, eyes, teeth and outlines remain protected details.
- Normal Urkröte hit now has one recoil and one recovery in both sheet offsets and `BattleSequence`; the old second recoil was removed. All revised states pass the fixed app-crop and lossless-WebP QA. Recolor review contact sheets and GIF previews are in `art/qa/projectiles/` and `art/previews/`.
- The complete normal-hit scan is recorded in [hit-animation-pulse-audit](../art/qa/hit-animation-pulse-audit.md). It found remaining sheet-level second recoils only for Ultra-Raumriss-Löm, Raumriss-Weltschlange and Ultra-Armageddon-Raumriss-Erzmagierhaufen-Löm. Runtime hit motion is now a single recoil for every form.
