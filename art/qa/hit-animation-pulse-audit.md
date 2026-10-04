# Treffer-Puls-Audit

Prüft nur normale `battle_hit`-Sheets. Ein Treffer darf genau einen Rückstoß und eine Erholung enthalten; `battle_double_hit` ist absichtlich ausgeschlossen.

| Sheet | X-Mittelpunkte je Frame | Zweiter Rückstoß im Sheet |
|---|---|---|
| `loem_armageddon_serpent_female_battle_hit_sheet.webp` | 256.0, 250.0, 244.0, 250.0, 254.0, 256.0 | nein |
| `loem_armageddon_serpent_male_battle_hit_sheet.webp` | 256.0, 250.0, 244.0, 250.0, 254.0, 256.0 | nein |
| `loem_bad_battle_hit_sheet.webp` | 256.0, 250.0, 245.0, 251.0, 254.0, 256.0 | nein |
| `loem_gloom_wizard_poop_female_battle_hit_sheet.webp` | 255.5, 250.0, 245.0, 251.0, 254.0, 255.5 | nein |
| `loem_gloom_wizard_poop_male_battle_hit_sheet.webp` | 255.5, 250.0, 245.0, 251.0, 254.0, 255.5 | nein |
| `loem_good_battle_hit_sheet.webp` | 256.0, 255.5, 256.0, 255.5, 255.5, 255.5 | nein |
| `loem_mud_toad_battle_hit_sheet.webp` | 256.0, 248.0, 242.0, 248.0, 253.0, 256.0 | nein |
| `loem_poop_battle_hit_sheet.webp` | 251.0, 251.0, 250.0, 244.0, 248.5, 257.0 | nein |
| `loem_serpent_battle_hit_sheet.webp` | 256.0, 256.0, 255.5, 256.5, 256.0, 255.5 | nein |
| `loem_space_rift_archmage_poop_battle_hit_sheet.webp` | 256.0, 256.0, 252.0, 247.0, 244.0, 250.0, 253.0, 255.0, 256.0, 256.0, 256.0, 256.0 | nein |
| `loem_space_rift_urtoad_battle_hit_sheet.webp` | 256.0, 256.0, 252.0, 247.0, 244.0, 249.0, 253.0, 255.0, 256.0, 256.0, 256.0, 256.0 | nein |
| `loem_space_rift_world_serpent_battle_hit_sheet.webp` | 256.0, 256.0, 251.5, 246.5, 243.5, 248.5, 252.5, 254.5, 255.5, 255.5, 256.0, 256.0 | nein |
| `loem_stormkaiser_battle_hit_sheet.webp` | 249.0, 258.5, 238.0, 224.0, 242.0, 258.0 | nein |
| `loem_stormkaiser_female_battle_hit_sheet.webp` | 256.0, 250.0, 244.0, 250.0, 254.0, 256.0 | nein |
| `loem_ultra_cosmic_battle_hit_sheet.webp` | 256.0, 256.0, 252.0, 247.0, 244.0, 250.0, 253.0, 255.0, 256.0, 256.0, 256.0, 256.0 | nein |
| `loem_wart_emperor_female_battle_hit_sheet.webp` | 256.0, 251.0, 245.0, 251.0, 254.0, 256.0 | nein |
| `loem_wart_emperor_male_battle_hit_sheet.webp` | 256.0, 251.0, 245.0, 251.0, 254.0, 256.0 | nein |
| `loem_wing_evolution_battle_hit_sheet.webp` | 252.0, 244.0, 235.5, 229.5, 231.5, 250.5 | nein |

## Ergebnis

- Sheets mit zweitem Rückstoß: keine
- Die Laufzeitbewegung wird zusätzlich im `BattleSequence` geprüft: ein normaler Treffer nutzt eine einzige Sinus-Halbwelle; damit kann sie keinen zweiten Puls erzeugen.
