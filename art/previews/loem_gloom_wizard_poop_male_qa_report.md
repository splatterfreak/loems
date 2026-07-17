# Trübsal-Zauberhaufen-Löm und -Lömin – Produktions-QA

Status: **PASS und vollständig integriert**.

## Spielform

- Ausgangsform: Haufen-Löm (`evolution == 2`, Pfad `BAD`)
- Zielstufe: vierte Evolution (`evolution == 3`)
- Zeitraum: zufällig-deterministisch zwischen 336 und 384 Stunden nach Geburt
- Namen: Trübsal-Zauberhaufen-Löm (männlich), Trübsal-Zauberhaufen-Lömin (weiblich)
- Gesundes Gewicht: 32.000 g
- Basis-Kampfwerte: Stärke 24, Verteidigung 18
- Anforderungen, Zeitraum, Gewicht, Werte und Verhalten sind für beide Geschlechter identisch.

## Feste Anatomie und Gestaltung

- 1 Kopf-/Haufenkörper, 2 Augen, 2 Arme, 2 Hände und 2 Füße
- 1 Magierhut, 1 Umhang, 1 Zauberstab und 1 fest sitzender Kristall
- Keine Hörner, Flügel oder Schwänze
- **Keine Tränen oder Tropfen in irgendeinem Zustand**
- Niedergeschlagene Stimmung ausschließlich durch Augen, Mund, Körperhaltung und gesenkten Stab

## Vollständige Zustände

Für männlich und weiblich vorhanden und in Normal- sowie Debug-Auswahl verdrahtet:

- Idle, hungrig, Schlaf, Melone und Schinken
- Angriff, Treffer, Doppelangriff, Doppeltreffer und Sieg

Die Kampfbewegung wird wie bei den übrigen Loems zusätzlich durch Compose-Translation, Rotation und Skalierung erzeugt. Die zugrunde liegenden Frames bleiben anatomisch gesperrt.

## Technische Prüfung

- 20 Produktions-Sheets, jeweils 1536 × 1024 px, RGBA
- Layout: 3 × 2 Frames mit 512 × 512 px pro Zelle
- App-Crop: 416 × 416 px ab `(48,48)` je Zelle
- Männliche Alpha-BBox: ungefähr `(59,52,452,448)`
- Weibliche Alpha-BBox: ungefähr `(66,52,445,448)`
- Mittelpunktdrift: 0,0 px
- Bodenliniendrift: 0 px
- Höhenschwankung: 0,0 %
- Loop-IoU: 1,000
- Automatische Sprite-QA: **20/20 PASS**
- Sichtprüfung von Idle, Schlaf, Melone und Schinken beider Varianten: **PASS**
- `testDebugUnitTest`: **PASS**
- `assembleDebug`: **PASS**

## Reproduzierbare Generierung

- Generator: `tools/generate_gloom_wizard_poop_state_sheets.py`
- Referenzbasis: Haufen-Löm und die freigegebenen tränenlosen männlichen/weiblichen Canonical-Sheets
- Der Generator fixiert sechs Frames, transparente Hintergründe, Bodenlinie, sichere Ränder sowie die realistische und in jedem Frame identische Anzahl aller Körperteile und Ausrüstungsgegenstände.
