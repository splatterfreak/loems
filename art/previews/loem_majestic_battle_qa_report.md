# Majestätisches Flügel-Löm – Kampfset QA

## Format und Laufzeit-Zuschnitt

- Alle fünf Produktionsdateien: PNG RGBA, 1536 × 1024 px, 3 × 2 Zellen, 512 × 512 px je Frame, 6 Frames.
- Laufzeit-Zuschnitte: `(36,24,440,440)`, `(548,24,440,440)`, `(1060,24,440,440)`, `(36,536,440,440)`, `(548,536,440,440)`, `(1060,536,440,440)`.
- Vergleich Zuschnitt/Quell-Alpha: Kein nichttransparentes Pixel liegt außerhalb der Laufzeit-Zuschnitte. Kleinster verbleibender Abstand im App-Zuschnitt: 12 px horizontal und 15 px vertikal.
- Körper ist neutral hellgrau und damit weiter über die bestehende Laufzeitmaske färbbar. Augen, Zähne, Mund, Kontur, Wangen, Hörner und Flügelmembranen bleiben eigenfarbig.

| Animation | Transparent | Teilalpha | Opaque | Center-Drift | Boden-Drift | Höhenänderung | Loop-IoU | Ergebnis |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| Angriff | 1.240.177 | 34.074 | 298.613 | 6,3 px | 1 px | 23,3 % | 0,583 | PASS |
| Treffer | 1.233.476 | 9.550 | 329.838 | 6,5 px | 0 px | 33,1 % | 0,588 | PASS |
| Doppelangriff | 1.341.211 | 29.381 | 202.272 | 6,6 px | 0 px | 20,6 % | 0,488 | PASS |
| Doppeltreffer | 1.284.411 | 26.827 | 261.626 | 4,9 px | 0 px | 28,2 % | 0,630 | PASS |
| Sieg | 1.309.870 | 30.774 | 232.220 | 0,6 px | 0 px | 2,6 % | 0,709 | PASS |

## Frame-Messwerte

Notation: `F: bbox(x0,y0,x1,y1); Ränder(L,O,R,U); Schwerpunkt(x,y); Boden` – Koordinaten innerhalb der jeweiligen 512er-Zelle.

### Angriff

- F1: `93,148,413,449`; `93,148,99,63`; `256,8 / 320,5`; `448`
- F2: `55,218,431,449`; `55,218,81,63`; `257,3 / 351,4`; `448`
- F3: `58,189,446,449`; `58,189,66,63`; `263,1 / 326,0`; `448`
- F4: `54,200,445,449`; `54,200,67,63`; `261,6 / 337,6`; `448`
- F5: `48,163,453,449`; `48,163,59,63`; `259,5 / 316,8`; `448`
- F6: `94,148,414,448`; `94,148,98,64`; `257,4 / 320,5`; `447`

### Treffer

- F1: `83,131,421,448`; `83,131,91,64`; `256,7 / 313,6`; `447`
- F2: `64,135,424,448`; `64,135,88,64`; `256,9 / 317,7`; `447`
- F3: `52,141,419,448`; `52,141,93,64`; `250,4 / 315,8`; `447`
- F4: `51,236,408,448`; `51,236,104,64`; `252,5 / 354,2`; `447`
- F5: `66,133,397,448`; `66,133,115,64`; `251,9 / 314,5`; `447`
- F6: `82,131,419,448`; `82,131,93,64`; `256,6 / 313,7`; `447`

### Doppelangriff

- F1: `116,201,385,448`; `116,201,127,64`; `256,5 / 342,9`; `447`
- F2: `79,248,424,448`; `79,248,88,64`; `261,7 / 353,3`; `447`
- F3: `108,226,391,448`; `108,226,121,64`; `256,7 / 353,5`; `447`
- F4: `67,231,435,448`; `67,231,77,64`; `258,4 / 335,5`; `447`
- F5: `48,196,379,448`; `48,196,133,64`; `255,1 / 339,9`; `447`
- F6: `121,216,382,448`; `121,216,130,64`; `256,5 / 350,1`; `447`

### Doppeltreffer

- F1: `97,150,410,448`; `97,150,102,64`; `257,5 / 321,5`; `447`
- F2: `80,169,390,448`; `80,169,122,64`; `256,8 / 337,7`; `447`
- F3: `99,155,395,448`; `99,155,117,64`; `257,8 / 324,1`; `447`
- F4: `79,204,395,448`; `79,204,117,64`; `254,1 / 352,4`; `447`
- F5: `50,234,402,448`; `50,234,110,64`; `252,9 / 362,7`; `447`
- F6: `75,191,389,448`; `75,191,123,64`; `255,2 / 347,1`; `447`

### Sieg

- F1: `109,185,382,449`; `109,185,130,63`; `257,3 / 336,3`; `448`
- F2: `74,185,413,449`; `74,185,99,63`; `257,9 / 335,7`; `448`
- F3: `50,184,458,449`; `50,184,54,63`; `257,5 / 323,7`; `448`
- F4: `80,185,418,449`; `80,185,94,63`; `257,7 / 331,5`; `448`
- F5: `76,189,428,449`; `76,189,84,63`; `257,6 / 334,2`; `448`
- F6: `111,191,378,449`; `111,191,134,63`; `257,3 / 338,6`; `448`

## Visuelle und funktionale Prüfung

| Prüfung | Ergebnis |
|---|---|
| Hörner, Flügel und Schwanz vollständig | PASS |
| Kein Zell-Bleed / keine abgeschnittene Alpha-Silhouette | PASS |
| Bodenlinie und Größenwirkung im Loop stabil | PASS |
| Letzter-zu-erster Frame bei Angriff, Treffer und Sieg lesbar | PASS |
| Doppelaktionen als zwei getrennte Beats lesbar | PASS |
| Trefferbewegungsstriche aus dem Rohbild entfernt | PASS |
| Recoloring-Fläche neutral; Gesicht/Details geschützt | PASS |
| Silhouette auf mobiler Zielgröße lesbar | PASS |
| Alle fünf Einträge in der Debug-Spriteauswahl registriert | PASS |
| Normaler Kampf nutzt Set für Angriff, Treffer, Finale und Ergebnis-Hold | PASS |

Visuelle Notiz: Die weiteste Flügelpose liegt im Sieger-Frame 3 und bleibt mit 50/54 px Quellrand sicher in der Zelle. Doppelangriff und Sieg wirken wegen der breiten Flügel bewusst kompakter; der Kampf rendert das majestätische Löm deshalb mit 275 dp statt 245 dp.

## Erzeugung

- Modus: eingebautes Imagegen mit zwei Identitäts-/Bewegungsreferenzen, anschließend lokales Chroma-Keying, Despill, Alpha-Bereinigung und Anker-/Boden-Ausrichtung.
- Prompt-Set: fünf getrennte, aktionsspezifische Prompts für Angriff, Treffer, Doppelangriff, Doppeltreffer und Sieg; gemeinsame Invarianten für Anatomie, 3×2-Layout, 48-px-Rand, neutrale Recoloring-Fläche und unveränderte Perspektive.
