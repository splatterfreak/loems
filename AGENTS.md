# Loems project instructions

When creating, regenerating, repairing, or updating a Loems evolution graphic or evolution tree, read and follow `docs/evolution-graphic.md` completely before editing the visualization. Start from `docs/evolution-graphic-template.html` when the requested layout is the established Loems diagram. Always run `tools/embed_evolution_graphic_sprites.py` and perform the required rendered-image checks before delivery.

Every fourth evolution tier (`evolution == 3`) must have both a male and a female visual variant. Both gender variants share identical evolution requirements, time windows, weight profiles, battle values, and gameplay behavior; only their names and sprite resources differ. New tier-4 forms are incomplete until idle, hungry, sleep, melon, ham, and all battle states exist for both genders and are wired into normal and debug selection.

All production raster images in `app/src/main/res/` must use lossless WebP, preserving transparency whenever the source has an alpha channel. PNG may be used only as an intermediate generation or editing format and must be converted to pixel-identical lossless WebP before integration. Do not add byte-identical copies for logical animation states; wire those states to the shared bitmap resource until they receive genuinely distinct artwork.
