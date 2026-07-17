# Loems project instructions

When creating, regenerating, repairing, or updating a Loems evolution graphic or evolution tree, read and follow `docs/evolution-graphic.md` completely before editing the visualization. Start from `docs/evolution-graphic-template.html` when the requested layout is the established Loems diagram. Always run `tools/embed_evolution_graphic_sprites.py` and perform the required rendered-image checks before delivery.

Every fourth evolution tier (`evolution == 3`) must have both a male and a female visual variant. Both gender variants share identical evolution requirements, time windows, weight profiles, battle values, and gameplay behavior; only their names and sprite resources differ. New tier-4 forms are incomplete until idle, hungry, sleep, melon, ham, and all battle states exist for both genders and are wired into normal and debug selection.
