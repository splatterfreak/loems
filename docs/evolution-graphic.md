# Loems evolution graphic

Use these instructions for every Evolutionsgrafik, Evolutionsbaum, Entwicklungsgrafik, evolution-path diagram, or missing-image repair.

## Required workflow

1. Verify names, conditions, and time windows against the current game code. The canonical map below is a starting point, not a substitute for checking code.
2. Copy `docs/evolution-graphic-template.html` to the current thread's visualization directory. Preserve its established left-to-right layout and generous horizontal and vertical space unless the user asks for a redesign.
3. Put the base appearance in every node: use the first cell of the form's 3 x 2 idle sheet, never a whole contact sheet or a non-idle state.
4. Use literal `≥`, `<`, and arrows. Never let escaped text such as `&lt;` appear in the rendered graphic.
5. Embed all images locally by running:

   ```powershell
   python tools/embed_evolution_graphic_sprites.py <absolute-path-to-loems-evolution.html>
   ```

6. Render the result in the browser. Require all fifteen current form images and verify every image reports `naturalWidth > 0` and `naturalHeight > 0`.
7. Inspect both the normal 736 px layout and a narrow layout. Reject overlap, clipping, unreadable labels, broken arrows, or missing nodes.
8. Deliver only after the script and rendered checks both pass.

## Image reliability rules

- Never move base64 through terminal output, tool output, chat text, or an `apply_patch` payload. Long output can be truncated and silently corrupt images.
- Generate and embed base64 inside the local script in one process.
- The script creates transparent 128 x 96 WebP thumbnails at quality 82. Each encoded payload must remain below 20,000 characters.
- Match images by exact `alt` text. Missing, duplicate, invalid, or undecodable images are blocking failures.
- Keep the final diagram self-contained; do not reference temporary files or external image URLs.
- Re-run embedding and browser verification after every template or sprite change.

## Canonical evolution map

| From | To | Time | Condition |
|---|---|---|---|
| Junges Löm | Flügel-Löm | random from 72–96 h, day 3–4 | Care at least 1.0 |
| Junges Löm | Wurst-Löm | random from 72–96 h, day 3–4 | Care below 1.0 |
| Flügel-Löm | Majestätisches Flügel-Löm | 120–168 h, day 5–7 | Care at least 1.0 |
| Flügel-Löm | Matschkröten-Löm | 120–168 h, day 5–7 | Care below 1.0 |
| Wurst-Löm | Prunkschlangen-Löm | 120–168 h, day 5–7 | Care at least 1.0 |
| Wurst-Löm | Haufen-Löm | 120–168 h, day 5–7 | Care below 1.0 |
| Majestätisches Flügel-Löm | Sturmkaiser-Löm | 336–384 h, day 14–16 | Male |
| Majestätisches Flügel-Löm | Sturmkaiserin-Löm | 336–384 h, day 14–16 | Female |
| Matschkröten-Löm | Warzenkaiser-Löm | 336–384 h, day 14–16 | Male |
| Matschkröten-Löm | Warzenkaiserin-Löm | 336–384 h, day 14–16 | Female |
| Prunkschlangen-Löm | Armageddon-Prunkschlangenkaiser-Löm | 336–384 h, day 14–16 | Male |
| Prunkschlangen-Löm | Armageddon-Prunkschlangenkaiserin-Löm | 336–384 h, day 14–16 | Female |
| Haufen-Löm | Trübsal-Zauberhaufen-Löm | 336–384 h, day 14–16 | Male |
| Haufen-Löm | Trübsal-Zauberhaufen-Lömin | 336–384 h, day 14–16 | Female |

## Canonical sprite sources

All sources are in `app/src/main/res/drawable-nodpi/`.

| Diagram image `alt` | Source sheet |
|---|---|
| Junges Löm | `loem_idle_sheet.png` |
| Flügel-Löm | `loem_good_evolution_sheet.png` |
| Wurst-Löm | `loem_bad_evolution_sheet.png` |
| Majestätisches Flügel-Löm | `loem_wing_evolution_idle_sheet.png` |
| Matschkröten-Löm | `loem_mud_toad_idle_sheet.png` |
| Prunkschlangen-Löm | `loem_serpent_evolution_idle_sheet.png` |
| Haufen-Löm | `loem_poop_evolution_idle_sheet.png` |
| Sturmkaiser-Löm | `loem_stormkaiser_idle_sheet.png` |
| Sturmkaiserin-Löm | `loem_stormkaiser_female_idle_sheet.png` |
| Warzenkaiser-Löm | `loem_wart_emperor_male_idle_sheet.png` |
| Warzenkaiserin-Löm | `loem_wart_emperor_female_idle_sheet.png` |
| Armageddon-Prunkschlangenkaiser-Löm | `loem_armageddon_serpent_male_idle_sheet.png` |
| Armageddon-Prunkschlangenkaiserin-Löm | `loem_armageddon_serpent_female_idle_sheet.png` |
| Trübsal-Zauberhaufen-Löm | `loem_gloom_wizard_poop_male_idle_sheet.png` |
| Trübsal-Zauberhaufen-Lömin | `loem_gloom_wizard_poop_female_idle_sheet.png` |
