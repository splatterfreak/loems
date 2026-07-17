# Armageddon-Prunkschlangen-Tier – Produktions-QA

Status: **PASS und vollständig integriert**

## Varianten und Zustände

- Männlich: Armageddon-Prunkschlangenkaiser-Löm
- Weiblich: Armageddon-Prunkschlangenkaiserin-Löm
- Je Variante: Idle, hungrig, Schlaf, Melone, Schinken, Angriff, Treffer, Doppelangriff, Doppeltreffer und Sieg
- Alle 20 Produktions-Sheets liegen unter `app/src/main/res/drawable-nodpi/` und besitzen eine GIF-Vorschau unter `art/previews/`.

## Technische Geometrie

- Bildgröße je Sheet: 1536 × 1024 px
- Layout: 3 × 2 Frames
- Zellgröße: 512 × 512 px
- Alphamodus: RGBA mit transparentem Hintergrund
- App-Renderausschnitte: `(48,144,416,320)`, `(560,144,416,320)`, `(1072,144,416,320)`, `(48,656,416,320)`, `(560,656,416,320)`, `(1072,656,416,320)`
- Männliches Idle je Frame: Alpha-BBox `(52,171,460,448)`, Ränder `(52,171,52,64)`, Mittelpunkt `(285,8;332,0)`, Bodenlinie `447`
- Weibliches Idle je Frame: Alpha-BBox `(52,171,460,448)`, Ränder `(52,171,52,64)`, Mittelpunkt `(285,9;331,8)`, Bodenlinie `447`
- Idle-/Schlaf-/Hunger-/Kampfdrift: `0,0 px`; Bodenliniendrift: `0 px`; Loop-IoU: `1,000`
- Fütterung: Körper pixelstabil; nur das Futter bewegt sich. Maximale Gesamt-Alpha-Drift durch den Schinken: männlich `2,6 px`, weiblich `2,2 px`.

## Vergleich zur Prunkschlange

- Identischer App-Crop von 416 × 320 px, dadurch kein Größenpuls beim Evolutionswechsel.
- Prunkschlange: etwa 400 × 292 px sichtbare Alpha-Fläche.
- Armageddon-Form: etwa 408 × 277 px sichtbare Alpha-Fläche, im normalen UI zusätzlich größer mit 250 dp statt 220 dp und im Kampf mit 305 dp statt 245 dp gerendert.
- Die breitere Panzer-/Flügelsilhouette und größere UI-Darstellung machen die Weiterentwicklung klar lesbar.

## Anatomieinventar

- 1 Kopf
- 2 Augen, wobei das hintere Auge perspektivisch verdeckt sein darf
- 2 Hörner
- 2 Flügel
- 4 Beine und 4 Füße
- 1 Schwanz
- 1 gespaltene Zunge

Anatomiekontinuität: **PASS**. Die feste Basis wird in allen sechs Frames wiederverwendet. Es gibt keine zusätzlichen, fehlenden oder abgeschnittenen Gliedmaßen.

## Sichtprüfung

- Keine abgeschnittenen Hörner, Flügel, Füße, Zungen, Futterteile oder Antialiasing-Ränder
- Kein Überlaufen in Nachbarzellen
- Neutralgrauer Körper bleibt für Laufzeit-Einfärbung geeignet; Augen, Zähne, Wangen, Panzerung, Zunge und Futter bleiben außerhalb der Körpermaske
- Männlich: schwere gold-/orangefarbene Endboss-Panzerung
- Weiblich: gleich mächtig, mit eleganten violett-roségoldenen Akzenten und Wimpern
- Schlaf wird über geschlossene Augen und entspannte Mimik dargestellt; Hunger/Fütterung über einen tatsächlich geöffneten Kiefer

## Gameplay-Integration

- Automatische Entwicklung der Prunkschlange bei Evolution 2 im Erwachsenenfenster 336–384 Stunden
- Geschlechtsabhängige Namen und Ressourcen bei identischen Voraussetzungen und Werten
- Gewichtprofil: 42.000 g
- Kampfwerte: Stärke 26, Verteidigung 24
- Normale Auswahl: Idle, Hunger, Schlaf, Melone, Schinken und alle Kampfphasen
- Debug-Auswahl: alle 20 Varianten einzeln registriert sowie Evolution 3 für den geschlechtsabhängigen Armageddon-Pfad
- `testDebugUnitTest`: **PASS**
- `assembleDebug`: **PASS**

## Generierung

- Werkzeug: integrierte OpenAI-Bildgenerierung; lokale Chroma-Key-Freistellung und deterministische Loems-Sheet-Normalisierung
- Referenz: `loem_serpent_evolution_idle_sheet.png` sowie der freigegebene männliche Armageddon-Entwurf
- Kernvorgabe: imposante vierte Prunkschlangen-Stufe im Loems-Stil mit neutralgrauem Körper, goldener Panzerung, exakt zwei Hörnern, zwei Flügeln, vier Beinen/Füßen, einem Schwanz und einer gespaltenen Zunge; weibliche Variante mit identischer Anatomie und eleganten violett-roségoldenen Akzenten.
