---
name: sprite-generator
description: Generate, refine, or review Loems game sprite assets and sprite sheets. Use when Codex creates prompts, image assets, sprite sheets, frame specs, recolorable character variants, animation frames, or QA notes for Loems sprites, especially when alignment, consistent style, transparent backgrounds, runtime recoloring, and frame-to-frame coherence matter.
---

# Sprite Generator

## Overview

Create production-ready Loems sprites that animate without jitter, fit the existing Android renderer, and can be recolored at runtime. Treat each sprite sheet as both art and data: consistent cell geometry, stable anchors, clean alpha, and controlled colors are requirements, not polish.

## Core Contract

Battle projectile ownership: `BattleProjectileEffect` renders every elemental shot separately. Character sheets must contain no fired projectiles, beams, traveling energy balls, or outgoing trails, including in attack and double-attack poses. Animate posture, limbs, mouth, and recoil; preserve permanent body-attached portals or contained glow only. Include this exclusion in battle prompts and visually inspect every finished frame for duplicate shots.

Every generated or edited Loems sprite must satisfy these constraints:

- Use lossless WebP with transparent background for every production Android asset. PNG is allowed only as a temporary generation or editing source and must be converted to pixel-identical lossless WebP before integration.
- Keep all frames in equal-sized cells unless editing an existing sheet that already has measured `SpriteFrame` rectangles.
- Leave at least 48 px of transparent safety padding between visible pixels and every cell edge for generated sheets. Treat 28 px as the absolute minimum for existing legacy sheets only.
- Keep every visible pixel inside its own cell. Wings, horns, tails, food, effects, outlines, antialiasing, and glow pixels must never be clipped by a cell edge or appear inside a neighboring frame's cell.
- Treat horns as first-class silhouette elements, like wings: they must be clearly visible, symmetrical or intentionally posed, stable across frames, and fully inside the padding area with their outline and antialiasing intact.
- Keep the body center on the same X coordinate in every frame.
- Keep feet or the lowest body point on the same ground line in every frame.
- Keep body scale constant across frames; only the intended pose, squash, stretch, expression, prop, or effect may change.
- Keep style, line weight, lighting direction, texture density, eye style, mouth style, and prop rendering consistent across the whole sheet.
- Lock an anatomy inventory before generation and preserve it in every frame. Every frame must contain the same realistic number of heads, eyes, horns, wings, arms/hands or forelegs/front paws, legs/feet or hind legs/hind paws, tails, fingers/toes, and other repeated body parts defined by the canonical design. Perspective may hide or overlap a part, but it must remain anatomically accounted for; parts must never appear, disappear, duplicate, merge into impossible anatomy, or swap attachment sides between frames.
- Remove solid backgrounds, grid lines, separator lines, labels, watermarks, shadows clipped by the cell, and stray alpha pixels.
- Make the body neutral light gray and recolorable; keep eyes, teeth, outlines, cheeks, food, props, effects, and accessories outside the body recolor mask.
- Treat wings, wing membranes, wing tips, horns, tails, scales, and other permanent anatomical surfaces as body unless they are explicitly a protected accessory or elemental effect. They must use the same neutral-gray recolor mask as the torso in every state, including attack, hit, victory, and defeat. Do not leave warm, orange, pink, or otherwise fixed-color body fragments on an otherwise recolorable Löm.
- Compare every battle-state frame after applying each supported runtime body color. Body-colored regions must match the selected color consistently; only protected outlines, eyes, teeth, cheeks, held props, and deliberate elemental effects may retain fixed hues. This is a blocking visual-review check, not a prompt-only requirement.
- A normal `hit` animation represents exactly one impact and one recovery. It may contain one recoil extremum, but must not introduce a second shake, recoil, or impact pulse through either sprite-frame offsets or runtime transforms. Only explicitly named `double hit` states may have two readable impact pulses.
- Every battle-capable Loem form must have dedicated victory and defeat animations. Idle, attack, hit, or another generic state may not stand in for either battle outcome. A purpose-built final double-hit or knockout sequence may also serve as defeat only when it visibly resolves into a defeated pose. The result sequence must play the outcome animation once and then hold its final frame.
- Keep `docs/animation-inventory.md` synchronized whenever a Loem sprite asset or runtime sprite mapping is added, removed, replaced, or repurposed. Mark only dedicated state artwork as present; idle and static fallbacks stay listed as missing.

Reject and regenerate any sheet where a frame was originally clipped, cropped, or bleeding into a neighboring cell. Do not "fix" a clipped generated sheet by merely recentering or scaling the damaged cells; that can align the sheet while preserving broken art.

## Recolor Rules

The app recolors body pixels by detecting light, nearly neutral gray pixels. Design sprites so this mask is intentional:

- Body base and shaded body pixels: neutral gray, roughly RGB channels within 30 values of each other, with brightness in the app's mask range.
- Preserve shading through gray value changes, not hue changes.
- Do not make teeth, eyes, outlines, cheek blush, food, accessories, or effects neutral gray enough to enter the body mask.
- Use deliberate non-gray hues for non-body details: dark outlines, white/off-white teeth, black eyes, pink cheeks, saturated food colors.
- Avoid global color grading, colored rim light, or texture noise on the body that would break neutral-gray recoloring.

## Frame Layout

Prefer these defaults unless the user or current sheet says otherwise:

- 6 frames per animation, arranged 3 columns x 2 rows.
- 512 x 512 px cells for full-cell sheets, or match the existing sheet's measured rectangles when replacing current assets.
- Transparent margin around the visible character, never cropped to the bounding box.
- For winged, horned, tailed, or effect-heavy forms, either keep the character small enough to preserve 48 px padding or use a larger cell size. Never let impressive silhouettes trade away safe margins.
- Reserve extra top padding for horns. Horn tips and their antialiasing must not approach the top cell edge; if horns make the character too tall, scale the whole character down or increase cell size.
- Consistent per-frame anchor:
  - `anchor_x`: center of torso/body mass.
  - `ground_y`: feet, belly bottom, or lowest intended contact point.
  - `scale_ref`: main body height/width, unchanged across frames.
- Use the same canonical pose footprint in all frames. A wing flap may rotate or flex, but the leftmost/rightmost extent should not jump more than the intended animation amount.
- Export at native resolution. Do not upscale with blurry interpolation.

When using trimmed rectangles, preserve or report exact `x`, `y`, `width`, and `height` values for each frame. Do not assume equal source rectangles after trimming; the renderer may use explicit `SpriteFrame` data.

For Loems app integration, do not pass full 512 x 512 safety cells to `LoemSprite` unless the visible sprite intentionally fills most of the cell. The renderer scales the selected `SpriteFrame` rectangle to the on-screen sprite size. If the source rectangle includes too much transparent padding, the character renders too small; if frame rectangles vary loosely, the character can appear to pulse or twitch.

Use stable render crop rectangles:

- Keep the source cells large enough for generation safety and clipping checks.
- Define `SpriteFrame` rectangles as equal-size crops around the shared visible union for the animation, not necessarily the entire cell.
- Keep every crop rectangle's bottom line, torso anchor, and dimensions consistent across idle frames.
- Include enough crop margin so wing tips, horn tips, outlines, and antialiasing are not clipped.
- Compare the resulting rendered size against existing Loems sprites before accepting the asset.
- Match comparable sprite size by measuring the rendered alpha fill, not by eye alone. Over-wide or over-tall render crops scale too much transparent padding and make the Loem look smaller even when the source art is large.
- For state variants of the same evolution, reuse the exact same stable crop geometry whenever possible. Hungry, sleep, melon, ham, and other variants should change expression or props inside the crop, not the body anchor, frame dimensions, rendered size, or ground line.
- Express character states through the sprite art itself, not UI-like symbols. Avoid floating `!`, `?`, `Z`, emoji-style badges, labels, or icon overlays in sprite sheets. Hunger should read through eyes, brows, mouth, tongue, posture, or drool; sleep should read through closed eyes, relaxed mouth, lowered posture, or calmer breathing.
- Food props must match the existing Loems food style. Melon and ham variants should use the same kind of large, shaded, outlined food pieces as the established feeding sheets, not simplified icon-sized props.
- Keep each feeding prop visually recognizable as the same item shown in the food selector. Preserve its characteristic silhouette, palette, orientation, and major details when translating a compact selector symbol into the larger sprite-art version; do not substitute a different cut, preparation, or food shape.
- When replacing or standardizing an existing feeding prop, rebuild from a clean pre-edit source and remove the complete previous prop layer first, including detached crumbs, highlights, bone/rind fragments, outlines, and differently positioned intermediate stages. Never composite a replacement over an uncleared old prop. Every frame may contain exactly one logical food item or its single physical remainder; a second overlapping or nearby food silhouette is a blocking QA failure. Make bulk standardization idempotent so rerunning it cannot stack another prop layer.
- For every feeding animation, edit the original character anatomy directly in each frame. The mouth opening must deform the actual jaw, cheek, muzzle outline, teeth, and tongue as one coherent head drawing. Never simulate eating by drawing or compositing a separate mouth shape over an unchanged closed-mouth sprite. Build and approve the mouth/jaw motion first, then place the established food art and bite stages into those anatomically edited frames.
- Before drawing or masking any feeding pose, perform a mouth-anatomy coverage audit on the canonical frame. Explicitly identify the rear mouth corner, complete upper lip through the snout tip, every upper tooth, oral cavity, tongue, every lower tooth, full gray lower jaw and chin, and the jaw-to-cheek/neck attachment. Record one polygon that contains this complete connected assembly while excluding the eye and unrelated head/body. Inspect an annotated or high-contrast crop before proceeding. A mask that covers only the cavity, tongue, left/rear half, or lower teeth while leaving the snout, front teeth, chin, or outer jaw static is a blocking failure.
- Compare open and closed key poses as complete head silhouettes before adding food. The upper lip/snout and full lower-jaw outline must both participate coherently in the bite; no old lip, tooth row, chin tip, or jaw contour may remain visible outside or beneath the articulated pose. Reject the key poses if toggling them reads as one moving half attached to a static second mouth.
- Every eating sequence must contain a clearly readable mouth cycle with at least three distinct phases: open/approach, closed bite or chew, and reopen/release. The real lower jaw, muzzle, cheeks, teeth, and tongue must visibly move between those phases. Moving only the food, head, body, glow, or camera while the mouth drawing stays unchanged is a blocking QA failure, even when the sheet otherwise animates smoothly.
- Treat feeding as a one-shot, irreversible consumption sequence. For every bite, show `mouth open with the current food amount -> mouth closed on the food -> mouth reopened with a visibly smaller bitten remainder`. Never return to an earlier, larger, or less-bitten food stage later in the sequence.
- Make the visible food amount monotonically decrease across the complete feeding sequence. It may stay unchanged only between the open and closed contact frames of the same bite. After each closed bite, remove a clearly readable piece before the next open frame. End with no food visible and a closed-mouth chew or swallow pose suitable for holding as the final non-looping app frame.
- For bone-in food, apply that monotonic check to the edible meat mask rather than the combined meat-plus-bone alpha area. Revealing a previously covered section of the fixed bone can keep the total prop silhouette level or make it slightly larger even while the edible amount correctly decreases. Verify separately that meat pixels strictly decrease after completed bites and reach zero, while the bone remains invariant.
- Never represent eating by uniformly scaling, resizing, or squeezing the complete food sprite. A smaller copy of the untouched food is not a bite stage and is a blocking QA failure.
- Keep the food's original scale, orientation, supporting-hand/holding anchor, rind or bone thickness, lighting, and unaffected outer edges fixed between bite stages. Show consumption by cumulatively removing material from the mouth-facing edge and redrawing that edge with clear concave, irregular semicircular tooth marks. Previously removed material must never return. Only the final physical remnant may have much smaller bounds because most of the original food has actually been bitten away.
- For bone-in foods such as ham, model one complete fixed bone running through the meat from the start; do not treat the initially protruding end as a short handle that later grows into a full bone. Begin with a completely whole, unbitten piece whose meat hides the mouth-facing bone head and most of the shaft. As bites advance from the mouth-facing edge, reveal that buried bone head first and then an increasingly long, continuous section of the same shaft. Remove only meat: the bone must never appear from nowhere, split, float, change length, scale, shape, orientation, color, or anchor. The penultimate visible food stage may retain only a small meat patch around the far shaft; the last visible food stage shows that same complete bone alone, with both ends and the full connecting shaft visible. The following final animation frame contains neither meat nor bone.
- Determine bite direction from the actual mouth-to-food contact in the composed character frame and keep that direction consistent. If the mouth reaches the food from the left, the first missing chunk must be on the left and every later bite must advance left-to-right; mirror this rule for contact from the right. Never generate visually attractive bite marks on the side facing away from the mouth.
- Lock the character outside the articulated mouth, cheeks, food, and supporting-hand region throughout feeding. Do not recrop, recenter, rescale, or redraw the full character between bites. Prefer paired open/closed mouth key poses plus progressively reduced food stages over independently generated full-body frames.
- Build feeding sequences from one canonical locked full-body bitmap, never from six independently generated character renders. Duplicate that bitmap first, then derive at most two approved head/jaw key poses inside one explicitly bounded articulation mask. Outside that mask and the food region, require pixel-identical body, eye, feet, tail, horns, wings, outline, scale, torso anchor, and ground line in every frame. Any body-only pixel difference outside the approved regions is a blocking jitter failure.
- Keep all feeding variants for the same Loem at the same apparent size. Melon, ham, and later foods must reuse one shared set of equal-size app render crops with identical width, height, bottom line, and torso position. Measure the rendered body fill after excluding the food mask; reject a variant if its body appears more than 3% larger or smaller than another food variant.
- Treat empty space between teeth, tongue, lips, jaw, and food as true transparency unless it is deliberately part of the dark mouth cavity. Reject opaque white or checker-filled polygons behind teeth, white patches bridging separate teeth, and background-colored fills inside an otherwise open mouth. Inspect every mouth frame over both a dark and a saturated contrasting background.
- Every production feeding frame must be one complete, flattened full-body RGBA composition. Never place or animate a rectangular mouth, head, or face crop over another character render. Build the approved open-mouth and closed-mouth key poses on one locked full-body base, then flatten and isolate every cell before sheet assembly. The runtime must display isolated frame bitmaps or otherwise prove that no neighboring cell, stale layer, or alternate body pose can bleed into the active frame.
- Clear RGB values wherever alpha is zero before lossless WebP export. Reject hidden old-character pixels, semi-transparent rectangular bands, doubled eyes, doubled outlines, or remnants of another body pose even when they become visible only on a contrasting background.
- A closed lower jaw must be compact, rounded, and anatomically continuous from cheek to neck. Reject a long vertical chin wall, plate-like slab, detached or doubled jaw outline, or a jaw contour that crosses or clips into the neck, torso, hand, or food.
- Inspect every revised feeding sheet at 2x or 4x scale on a contrasting background. Compare the complete external head and chin silhouette through every open/closed transition; a clean mouth interior does not pass when the outer jaw clips, jumps, or overlays another contour.
- Preview one-shot feeding animations from full food to the empty held final frame. A GIF viewer may repeat from empty back to full, but never encode that reset as an in-sequence frame or use a ping-pong order in production.
- Exempt non-looping feeding sheets from last-to-first silhouette-IoU requirements. Validate forward frame transitions, the empty final hold, and the app clip's non-looping behavior instead; the intentional empty-to-full GIF preview reset is not an animation transition used by the app.
- If wide wings, horns, tail, or effects change the overall silhouette so much that the character appears to slide even when the torso anchor is stable, reject those frames for idle use. Regenerate a calmer idle or build the idle loop from the stable neutral frames instead of using every generated frame.
- Idle animations must move deliberately but never twitch. For idle and state-idle loops such as hungry idle or sleep idle, keep the body anchor, feet, horns, wings, tail, and outer silhouette stable. Add life through tiny local changes such as breath shading, eyelids, mouth, tongue, drool, cheek motion, or a controlled 1-2 px breathing deformation that does not change the ground line or apparent screen position.
- For majestic winged idle variants, wings and tail should move enough to feel alive, similar to the other Loems idle loops. Keep the movement controlled rather than frozen: a readable small local flap/sway or overlay motion is fine when feet, ground line, body center, crop size, and frame-to-frame scale stay stable, and there are no cropped or flickering edge pixels.
- Head bob is allowed and often desirable for hungry or breathing idles. Keep it small and rhythmic, coordinated with mouth/tongue/drool motion, and never let it pull the feet, crop, or whole body position with it.
- Full-body idle bob is allowed when the character otherwise feels too stiff. Keep it rhythmic and vertical-first, usually within about 2-4 px, with a fixed app crop, stable scale, no lateral drift, no clipped pixels, and enough cell padding at both the highest and lowest frame.
- Use a "locked-anchor idle" rule when generated frames still jitter after crop alignment: choose the cleanest neutral frame as the body base for all idle frames, then layer small local expression/effect/breath changes. Do not accept a loop where the whole silhouette changes shape, slides, or scales frame to frame.

### Canonical-Base Idle Generation

Do not ask an image model to redraw all idle frames independently. That produces six related illustrations rather than one animation and causes changes in anatomy, scale, placement, and ground line even when the prompt requests consistency.

For every new idle animation:

1. Generate and approve one canonical neutral pose with safe padding.
2. Normalize that pose into the target cell and lock its body anchor, ground line, scale, anatomy, outline, feet, and main silhouette.
3. Duplicate the canonical pose into every idle frame.
4. Derive motion only through controlled local edits or overlays. Prefer eyelids, mouth details, breath shading, and a separately isolated tail tip. Never redraw or regenerate the whole body for another idle frame.
5. If controlled local edits are not available, deliver a stable static idle loop made from the repeated canonical frame. A still loop is acceptable for design approval; a twitching redraw is not.
6. Build the final 3 x 2 sheet from those locked cells. Do not rely on the image model to lay out or align the grid.

## Evolution Readability

When the user asks for a further development, evolution, majestic form, upgraded form, or stronger variant, make the visual delta obvious at thumbnail size while preserving the Loems identity:

- Change the silhouette, not just polish the same character. Examples: taller proud posture, larger but fully contained wings, more confident chest shape, clearer horns, longer rounded tail, more orderly back spikes, or a calmer regal expression.
- Keep the species markers: rounded cloud-like body, big glossy black eye, open friendly toothy mouth, pink cheek, dark outline, neutral-gray recolorable body.
- For majestic winged evolutions, include both stronger wings and more developed horns. Horns should read at thumbnail size as deliberate growth, not as accidental ears or cropped spikes.
- Avoid accidental downgrade signals: cramped pose, smaller wings, baby proportions, timid hunch, inconsistent face, or tiny detail changes that only read when zoomed in.
- State the intended evolution delta in the prompt using measurable language, such as "wings about 25% larger and horns about 30% taller than the reference, both fully inside each cell with 48 px padding" or "taller upright posture while feet stay on the same ground line."

## Generation Workflow

### New Loem Approval Gate

When creating a completely new Loem form or evolution tier, generate only its idle sprite sheet first. Treat that idle as the canonical design reference for silhouette, proportions, face, ornamentation, palette, body anchor, ground line, and render crop. Present the idle for user review and iterate on it until the user explicitly approves the design. Do not generate hungry, sleep, feeding, training, evolution-transition, or other state sheets before that approval. After approval, derive every remaining state from the approved idle and preserve its anatomy, scale, anchor, crop geometry, line weight, and recolor mask.

1. Identify the animation state: idle, hungry, feeding, sleep, training, evolution, good evolution, bad evolution, or a user-specified state.
2. Identify the character form: baby, good evolution, bad evolution, or a requested variant.
3. Define the art brief before generating:
   - cell size, row/column layout, frame count
   - maximum silhouette footprint and required transparent padding
   - camera angle and pose family
   - body anchor and ground line
   - neutral-gray body palette
   - protected detail colors
   - evolution delta, if making an upgraded or majestic form
   - allowed motion changes per frame
   - an explicit anatomy inventory with realistic counts for every repeated body part
4. Generate or edit non-idle sprites as a complete coherent sheet, not as unrelated single images. For idle sprites, follow Canonical-Base Idle Generation and assemble the sheet deterministically.
5. Split or inspect the generated sheet by cell before background removal when possible. If any subject crosses a cell boundary, regenerate with smaller character scale, larger cells, or stronger "contained in cell" instructions.
6. Remove backgrounds and separators.
7. Run the automated QA gate with `tools/qa_sprite_sheet.py`, then inspect the animated loop visually.
8. If any blocking QA check fails, reject the output before showing it to the user. Correct the source or regenerate the canonical pose, rebuild the sheet, and rerun QA. Cropping, recentering, or normalizing independently redrawn frames does not cure anatomy or silhouette jitter.
9. Deliver the lossless WebP plus the passing QA result and any required frame metadata or implementation notes.

## Prompt Pattern

Use a precise prompt like this when generating:

```text
Create a transparent sprite sheet for the Loems Android game. Deliver the production asset as lossless WebP; a generated PNG is only an intermediate source.
Layout: 6 frames, 3 columns x 2 rows, each cell 512 x 512 px.
Character: [baby/good evolution/bad evolution], [animation state].
Style: cute rounded 2D game sprite, consistent line weight, consistent lighting, same camera angle in all frames.
Alignment: body center stays on the same X coordinate in every frame; lowest body/feet point stays on the same ground line; body scale stays constant.
Anatomy continuity: declare the canonical counts for heads, eyes, horns, wings, arms/hands or forelegs/front paws, legs/feet or hind legs/hind paws, tails, fingers/toes, and any other repeated parts. Every frame must preserve those exact realistic counts and attachment sides. Perspective overlap may occlude a part, but no part may appear, disappear, duplicate, fuse into impossible anatomy, or change sides.
Containment: each frame is isolated inside its own cell. Keep at least 48 px transparent margin to every cell edge. No body, prop, food, wing, horn, tail, outline, antialiasing, glow, or effect pixel may touch a cell edge or appear in a neighboring cell.
Recoloring: main body is neutral light gray with neutral gray shading only; eyes, teeth, outline, cheeks, props, food, and effects use distinct non-gray colors so only the body can be recolored at runtime.
Wing and body color continuity: wings, membranes, wing tips, horns, tail, and every permanent anatomical surface use the same neutral-gray body palette as the torso. Test the completed sheet after runtime recoloring with contrasting colors; no fixed warm or skin-colored anatomical fragment may remain.
Background: fully transparent, no grid, no separator lines, no labels, no watermark, no stray pixels.
Motion: [describe each frame's intended pose change]. For normal hit, specify one recoil and one recovery only; a second pulse is allowed only for an explicitly named double-hit animation.
```

For upgraded/evolved forms, add a concrete delta:

```text
Evolution delta: make the character read clearly more majestic than the reference through a taller proud posture, more confident chest, wings about 25% larger, and horns about 30% taller/cleaner, while keeping the full silhouette including wing tips, horn tips, outlines, and antialiasing inside every cell with 48 px padding.
```

If the generator tends to crop, add:

```text
Scale the character down if needed. It is better for the sprite to be slightly smaller with safe margins than large and clipped. Do not trim frames to content; keep the full transparent cell for every frame.
```

## Editing Existing Sheets

When replacing or modifying existing Loems sprite sheets:

- Inspect current assets in `app/src/main/res/drawable-nodpi/`.
- Inspect the matching `SpriteFrame(...)` constants in `app/src/main/java/de/loems/app/ui/LoemsApp.kt`.
- Prefer preserving the existing sheet dimensions, frame count, animation timing assumptions, and asset names.
- If frame rectangles change, update the matching `SpriteFrame` constants together with the asset.
- Make every new sprite version selectable in debug builds. Do not consider a new version complete until there is a debug-only way to preview it in the app alongside the other versions.
- When adding a new evolution tier, add the complete visible state set used by gameplay: idle, hungry, sleep, melon feeding, ham feeding, battle victory, and battle defeat. The new tier must be wired into normal state selection and also listed separately in debug selection.
- Keep debug-only sprite selection out of release behavior. Use existing `BuildConfig.DEBUG` UI patterns and avoid changing normal gameplay evolution rules unless the user explicitly asks for that.
- Do not change unrelated gameplay or UI behavior while working on sprite art.

## QA Checklist

Run QA after generation or editing. If any blocking item fails, reject the asset and regenerate or re-edit from the previous valid source.

### Mandatory Pre-Delivery QA Gate

- Run `tools/qa_sprite_sheet.py <sheet.webp>` for every generated sheet before presenting it.
- Generate and watch a looping preview that includes the last-to-first transition.
- After every sprite generation or revision, always create and show the user a GIF preview. Never present only the raw WebP or sprite sheet.
- For a canonical single-pose design approval, show a static looping GIF of that approved pose. For an animation, show the complete ordered frame loop, including its last-to-first transition.
- Save the GIF beside the corresponding WebP with a clear `_preview.gif` suffix so the reviewed preview remains traceable to its source asset.
- Do not present, recommend, integrate, or ask for approval of a sheet that fails automated or visual QA.
- QA is part of generation, not an optional later review. Iterate internally until it passes or report that no valid result could be produced.
- Prompt constraints alone never count as verification.

Blocking checks:

- Split the sheet into individual cells or inspect a visible cell grid overlay. Confirm nothing is clipped and nothing from one frame intrudes into another frame's cell.
- Each frame has at least 48 px transparent padding on every side for new generated sheets. Legacy edits may use 28 px only when existing dimensions make 48 px impossible.
- Visible alpha bounds never touch or cross a cell boundary; `left`, `top`, `right`, and `bottom` margins must all be above the padding threshold.
- Body center X and ground Y are stable across frames unless a deliberate full-body idle bob is specified. For full-body bob, report the bob range and verify the app crop, scale, and horizontal position remain stable.
- Body size is stable across frames. Measure body bounds, not just full bounds, when wings or effects move.
- The full silhouette does not jitter unintentionally. For idle loops, torso center may shift by at most 2 px, ground line by 0 px, and body height by at most 2%.
- For idle loops, compare consecutive frames with a pixel-difference or overlay check. Differences should exist so the sprite feels alive, but they must be limited to the intended local animation area or a tiny controlled breathing area. If the wings, horns, feet, tail, or main body outline differ substantially between idle frames, treat it as twitch and rebuild the loop from a locked-anchor base.
- If idle wing/tail motion is used, run an additional edge/foot stability check. Foot pixels should not differ across frames, and any changed edge pixels must be deliberate, contained, and free of white seams, ghost cuts, or antialiasing leftovers.
- The apparent on-screen position stays stable. Flip through the exact frame sequence used by the app, not only the raw sheet order. If a wing-open frame makes the Loem look like it slides left/right, either regenerate the frame or remove it from the idle sequence.
- App `SpriteFrame` rectangles are stable and appropriately cropped. Full safety cells are a QA failure when they make the rendered sprite noticeably smaller than comparable existing Loems.
- Rendered visible alpha width and height are comparable to the nearest existing sprite at the same app `sizeDp`; accept only small intentional differences caused by the silhouette.
- For multi-state evolution work, compare every state variant against the idle crop. Width, height, center X, and ground Y should stay stable unless the state intentionally adds a prop inside the already-approved crop.
- Horn placement is stable across frames. Horn tips may follow the intended head bob, but they must not flicker, change shape, swap size, touch the cell edge, or read as different anatomy from frame to frame.
- Perform a frame-by-frame anatomy inventory audit against the canonical design. Confirm identical, realistic counts and attachment sides for heads, eyes, horns, wings, arms/hands or forelegs/front paws, legs/feet or hind legs/hind paws, tails, fingers/toes, and all other repeated parts. Occluded parts must be explainable by the pose; unexplained missing, extra, fused, duplicated, or side-swapped anatomy is a blocking failure.
- The animation reads smoothly when frames are flipped through in sequence.
- Feeding loops visibly pass through open, closed-bite/chew, and reopened mouth phases; reject any eating loop whose mouth silhouette remains static.
- Feeding edible-food area is monotonically non-increasing, every completed bite produces a smaller edible remainder, no larger edible stage reappears, and the final production frame contains no food. For bone-in food, measure meat separately from the revealed invariant bone. Reject ping-pong keyframe orders and sequences that end with remaining food.
- Feeding props retain one constant source scale and holding anchor; their loss of area must come from cumulative visible bite cutouts with believable tooth-scalloped contours, never from whole-prop downscaling.
- The progressive bite edge starts on the side physically touching the mouth and advances away from that contact side in one consistent direction.
- Every feeding cell is a flattened, isolated full-body frame with no rectangular partial-character overlay, mixed body pose, neighboring-cell bleed, doubled anatomy, or nonzero hidden RGB beneath fully transparent pixels.
- Every closed-mouth feeding frame passes the compact, rounded cheek-to-neck jaw-continuity check with no slab-like lower chin, detached outline, or clipping overlap.
- The body recolor mask will not capture eyes, teeth, outline, cheeks, food, props, or effects.
- Render every revised battle sheet with at least two contrasting runtime body colors and inspect all frames for unrecolored anatomical fragments, especially wing tips, membranes, horns, tails, and defeated poses. Record this as a recolor-consistency pass/fail result.
- For every normal hit sheet and its runtime playback, inspect frame offsets and rendered motion for recoil extrema. Exactly one impact/recoil pulse is allowed; report a blocking failure for a second pulse. Double-hit sheets are reviewed separately and may contain two pulses only.
- No hidden background remains in semi-transparent pixels.
- The final production file is a lossless WebP with transparency and must decode pixel-identically to its approved source.
- If used in the app, the relevant Android build or targeted compile still succeeds.

Required final QA report:

- Report image size, cell size, frame count, and alpha mode.
- Report each frame's alpha bounding box, margins, center X, and ground Y.
- Report the app `SpriteFrame` rectangles used for rendering and compare their visible fill/size against the closest existing sprite.
- Report pass/fail for clipping, neighbor bleed, jitter, recolor safety, and visual evolution/readability.
- Report the canonical anatomy inventory and pass/fail for frame-by-frame anatomy continuity.
- Report whether the sprite is registered for debug selection, or explicitly say it is an art-only draft not yet wired into the debug picker.
- Include a visual inspection note after viewing the final transparent sheet, not just numeric measurements. Numeric alignment can pass while art is visibly clipped or too weakly evolved.
