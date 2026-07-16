# Sturmkaiser-Löm – vollständiges Sprite- und Integrations-QA

## Produktionsformat

- 10 transparente RGBA-PNGs, jeweils 1536 × 1024 px.
- 6 Frames pro Animation, 3 × 2 Zellen mit je 512 × 512 px.
- Quell-Sicherheitsrand: mindestens 49 px über das gesamte Set; kein Sheet liegt unter dem geforderten 48-px-Minimum.
- App-Zuschnitte für alle Zustände: `(44,40,424,424)`, `(556,40,424,424)`, `(1068,40,424,424)`, `(44,552,424,424)`, `(556,552,424,424)`, `(1068,552,424,424)`.
- Vergleich Quell-Alpha/App-Zuschnitt: Kein sichtbares Pixel wird abgeschnitten. Der kleinste zusätzliche Rand innerhalb des App-Zuschnitts beträgt 5 px.

| Animation | Transparent | Teilalpha | Opaque | Center-Drift | Boden-Drift | Höhenänderung | Loop-IoU | Ergebnis |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| Idle | 1.135.674 | 63.538 | 373.652 | 0,5 px | 0 px | 1,8 % | 0,957 | PASS |
| Hungrig | 1.134.761 | 64.688 | 373.415 | 0,6 px | 0 px | 1,8 % | 0,956 | PASS |
| Schlaf | 1.133.542 | 38.007 | 401.315 | 0,5 px | 0 px | 1,6 % | 0,983 | PASS |
| Melone | 1.249.032 | 49.177 | 274.655 | 13,6 px* | 1 px | 1,4 % | 0,797 | PASS |
| Schinken | 1.176.464 | 50.719 | 345.681 | 12,4 px* | 1 px | 5,1 % | 0,690 | PASS |
| Angriff | 1.227.109 | 44.128 | 301.627 | 8,3 px | 0 px | 39,3 % | 0,426 | PASS |
| Treffer | 1.192.607 | 46.303 | 333.954 | 2,5 px | 0 px | 26,0 % | 0,537 | PASS |
| Doppelangriff | 1.268.955 | 45.331 | 258.578 | 3,0 px | 1 px | 24,1 % | 0,534 | PASS |
| Doppeltreffer | 1.270.131 | 37.875 | 264.858 | 3,9 px | 1 px | 46,0 % | 0,409 | PASS |
| Sieg | 1.270.503 | 44.949 | 257.412 | 2,2 px | 0 px | 12,0 % | 0,589 | PASS |

`*` Bei den Fütterungen enthält die Messung das schrumpfende Futter. Der am neutral-grauen Körper ausgerichtete Körperanker bleibt stabil.

## Frame-Messwerte

Notation je Frame: `bbox(x0,y0,x1,y1); Ränder(L,O,R,U); Schwerpunkt(x,y); Boden` in lokalen 512er-Zellkoordinaten.

### Idle

- F1 `50,126,462,448; 50,126,50,64; 254,5/293,9; 447`
- F2 `50,123,461,448; 50,123,51,64; 254,0/292,4; 447`
- F3 `51,121,461,448; 51,121,51,64; 254,5/291,6; 447`
- F4 `50,123,461,448; 50,123,51,64; 254,0/292,4; 447`
- F5 `50,126,462,448; 50,126,50,64; 254,5/293,9; 447`
- F6 `49,127,462,448; 49,127,50,64; 254,1/294,4; 447`

### Hungrig

- F1 `50,126,462,448; 50,126,50,64; 254,7/293,6; 447`
- F2 `50,123,461,448; 50,123,51,64; 254,1/292,1; 447`
- F3 `51,121,461,448; 51,121,51,64; 254,6/291,2; 447`
- F4 `50,123,461,448; 50,123,51,64; 254,1/292,1; 447`
- F5 `50,126,462,448; 50,126,50,64; 254,7/293,6; 447`
- F6 `49,127,462,448; 49,127,50,64; 254,2/294,0; 447`

### Schlaf

- F1 `50,205,462,448; 50,205,50,64; 262,1/330,7; 447`
- F2 `50,203,461,448; 50,203,51,64; 261,6/329,7; 447`
- F3 `51,201,461,448; 51,201,51,64; 262,1/328,8; 447`
- F4 `50,203,461,448; 50,203,51,64; 261,6/329,7; 447`
- F5 `50,205,462,448; 50,205,50,64; 262,1/330,7; 447`
- F6 `49,205,462,448; 49,205,50,64; 261,7/330,7; 447`

### Melone

- F1 `63,167,426,449; 63,167,86,63; 267,1/317,7; 448`
- F2 `63,167,430,449; 63,167,82,63; 268,0/317,8; 448`
- F3 `53,170,421,449; 53,170,91,63; 265,2/321,9; 448`
- F4 `50,171,416,449; 50,171,96,63; 263,0/321,7; 448`
- F5 `54,171,402,449; 54,171,110,63; 256,5/319,1; 448`
- F6 `53,170,398,448; 53,170,114,64; 254,4/318,7; 447`

### Schinken

- F1 `50,132,445,447; 50,132,67,65; 264,2/296,7; 446`
- F2 `61,135,444,448; 61,135,68,64; 267,9/292,3; 447`
- F3 `53,148,446,447; 53,148,66,65; 267,2/302,8; 446`
- F4 `50,142,444,448; 50,142,68,64; 263,6/296,8; 447`
- F5 `58,142,437,448; 58,142,75,64; 260,3/296,7; 447`
- F6 `59,140,418,448; 59,140,94,64; 255,5/297,2; 447`

### Angriff

- F1 `51,125,463,448; 51,125,49,64; 254,3/293,3; 447`
- F2 `62,249,423,448; 62,249,89,64; 254,3/353,6; 447`
- F3 `62,182,447,448; 62,182,65,64; 262,3/332,6; 447`
- F4 `55,252,434,448; 55,252,78,64; 255,0/352,5; 447`
- F5 `51,179,424,448; 51,179,88,64; 258,0/321,8; 447`
- F6 `51,125,462,448; 51,125,50,64; 254,0/293,3; 447`

### Treffer

- F1 `56,129,442,448; 56,129,70,64; 253,6/294,9; 447`
- F2 `55,131,461,448; 55,131,51,64; 254,9/295,3; 447`
- F3 `67,169,410,448; 67,169,102,64; 255,5/313,2; 447`
- F4 `51,212,398,448; 51,212,114,64; 253,0/341,2; 447`
- F5 `73,147,412,448; 73,147,100,64; 253,7/305,4; 447`
- F6 `56,130,460,448; 56,130,52,64; 254,4/295,4; 447`

### Doppelangriff

- F1 `74,174,435,449; 74,174,77,63; 255,2/315,3; 448`
- F2 `59,199,418,449; 59,199,94,63; 257,9/337,6; 448`
- F3 `79,237,401,448; 79,237,111,64; 257,2/350,8; 447`
- F4 `55,193,443,448; 55,193,69,64; 257,9/325,6; 447`
- F5 `50,170,404,448; 50,170,108,64; 256,4/313,5; 447`
- F6 `80,181,429,449; 80,181,83,63; 255,0/319,5; 448`

### Doppeltreffer

- F1 `60,140,449,449; 60,140,63,63; 254,6/300,5; 448`
- F2 `70,145,402,448; 70,145,110,64; 253,6/309,9; 447`
- F3 `62,151,411,449; 62,151,101,63; 255,5/302,5; 448`
- F4 `50,224,394,449; 50,224,118,63; 255,9/356,6; 448`
- F5 `79,282,390,449; 79,282,122,63; 256,6/379,9; 448`
- F6 `63,218,386,449; 63,218,126,63; 252,7/354,1; 448`

### Sieg

- F1 `67,191,408,448; 67,191,104,64; 254,7/327,1; 447`
- F2 `74,190,427,448; 74,190,85,64; 254,5/321,9; 447`
- F3 `73,165,461,448; 73,165,51,64; 255,2/312,0; 447`
- F4 `75,167,449,448; 75,167,63,64; 256,2/309,6; 447`
- F5 `76,195,429,448; 76,195,83,64; 255,4/325,5; 447`
- F6 `66,199,407,448; 66,199,105,64; 254,1/330,8; 447`

## Visuelle und funktionale Prüfung

| Prüfung | Ergebnis |
|---|---|
| Ultraflügel, Hörner, Schwanzspitze und vier Beine vollständig | PASS |
| Kein Zell-Bleed oder abgeschnittene Alpha-Silhouette | PASS |
| Idle/Hunger/Schlaf ohne anatomisches Flackern | PASS |
| Melone und Schinken mit echter Kieferbewegung und sichtbaren Bissstufen | PASS |
| Doppelaktionen als zwei getrennte Treffer lesbar | PASS |
| Körper neutral-grau und laufzeitfärbbar; Details geschützt | PASS |
| Männliche Entwicklung Tag 14–16 verdrahtet | PASS |
| Weibliche majestätische Form bleibt unverändert | PASS |
| Leicht erhöhte Kampfwerte 20/16 statt 18/15 | PASS |
| Alle 10 Varianten in Debug-Auswahl registriert | PASS |
| Normale Zustände, Fütterung und Kampf verwenden die neuen Assets | PASS |

Visuelle Notiz: Die Silhouette ist schon bei kleiner Darstellung deutlich erwachsen: breitere Brust, längere Hörner, Speerschwanz und doppellagige Ultraflügel. Schlaf faltet die Flügel schützend um den Körper; Sieg öffnet sie maximal, ohne Zellgrenzen zu berühren. Die einheitlichen 424er App-Zuschnitte halten alle Zustände groß und stabil im Bild.

## Erzeugung

- Modus: eingebautes Imagegen mit dem freigegebenen Sturmkaiser-Kanon als Identitätsreferenz; anschließend Chroma-Key-Entfernung, Despill, Alpha-Bereinigung und lokale Anker-/Boden-Ausrichtung.
- Prompt-Set: zwei kanonische Zustandsposen (hungrig, Schlaf), zwei anatomische Fütterungssequenzen und fünf getrennte Kampfsequenzen; Idle wurde deterministisch aus dem freigegebenen Kanon aufgebaut.
