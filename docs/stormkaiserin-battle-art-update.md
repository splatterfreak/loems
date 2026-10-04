# Sturmkaiserin-Löm — Kampfanimationen

Sturmkaiserin-Löm ist vollständig: Angriff, Treffer, Doppelangriff, Doppeltreffer, Sieg und Niederlage sind als eigene WebP-Sheets vorhanden und in Gameplay sowie Debug-Auswahl verdrahtet. Sieg und Niederlage spielen einmal und halten das Schlussbild.

Die sechs Posen basieren auf der vorhandenen weiblichen Idle-Referenz und bewahren einheitlich Kopf, Augen, Hörner, Flügel, vier Füße und Schwanz. Die Körperfarbe bleibt neutralgrau und damit recolorierbar. Es gibt keine eingebauten Geschosse, Strahlen oder Schussspuren; diese kommen ausschließlich vom BattleProjectileEffect.

Technik: 3×2 Zellen à 512 px, 1536×1024 lossless RGBA-WebP, transparente Pixel bereinigt, mindestens 48 px Sicherheitsrand. Alle sechs Sheets bestehen die `qa_sprite_sheet.py`-Prüfung mit den bewusst erlaubten Kampfbewegungen; der bestehende App-Crop `STORMKAISER_STATE_FRAMES` `(44,40,424,424)` bleibt unverändert.

Vorschauen: [Angriff](../art/previews/loem_stormkaiser_female_battle_attack_preview.gif), [Treffer](../art/previews/loem_stormkaiser_female_battle_hit_preview.gif), [Doppelangriff](../art/previews/loem_stormkaiser_female_battle_double_attack_preview.gif), [Doppeltreffer](../art/previews/loem_stormkaiser_female_battle_double_hit_preview.gif), [Sieg](../art/previews/loem_stormkaiser_female_battle_victory_preview.gif), [Niederlage](../art/previews/loem_stormkaiser_female_battle_defeat_preview.gif).

Nachtrag 13.09.2026: Die weibliche Niederlage wurde im normalen Kampf irrtümlich dem männlichen Sturmkaiser zugeordnet; diese Zuordnung ist korrigiert. Auch die damals noch fehlenden Zustände von Wurst-Löm und beiden Trübsal-Zauberhaufen-Varianten sind inzwischen ergänzt. Aktueller Stand: [Abschlussbericht](battle-animation-completion.md) und [Bestandsliste](animation-inventory.md).
