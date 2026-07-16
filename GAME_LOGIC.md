# Löms – Spielmechaniken und Formeln

Diese Datei beschreibt den aktuellen Stand der Implementierung. Zeitangaben beziehen sich auf
reale verstrichene Zeit, sofern nicht ausdrücklich der Debug-Modus erwähnt wird.

## 1. Aktualisierung der Spielwelt

- Beim App-Start wird die Welt sofort aktualisiert.
- Solange die App geöffnet ist, erfolgt danach ungefähr alle 60 Sekunden ein Welt-Update.
- Der Benachrichtigungs-Worker aktualisiert die Welt im Hintergrund ungefähr alle 15 Minuten.
- Füttern, Training und andere Aktionen berechnen zunächst die seit dem letzten gespeicherten
  Zeitpunkt verstrichenen Werte. Die Anzeige kann deshalb auch zwischen Welt-Updates den
  aktuellen Zustand darstellen.
- Zeitdifferenzen kleiner als null werden wie `0` behandelt.

Der Pflege-Score wird bei einem Welt-Update nur fortgeschrieben, wenn mindestens eine Minute
seit dem vorherigen Pflege-Update vergangen ist.

## 2. Erzeugung, Ei und Grundwerte

Ein neuer Spielstand beginnt mit:

| Wert | Startwert |
|---|---:|
| Hunger | 15 % |
| Gewicht | 5.000 g |
| Zufriedenheit | 50 % |
| Gesundheit | 100 % |
| Mahlzeiten | 0 |
| Trainingseinheiten | 0 |
| gewonnene Trainings für den Stärkebonus | 0 |
| Siege / Niederlagen | 0 / 0 |
| Kampf-Erfahrung | 0 EP |
| Generation | 1 |
| Heilspritzen | 0 |

Das Ei schlüpft nach fünf Minuten:

```text
Alter = max(aktuelle Zeit − Geburtszeit, 0) + Debug-Bonusalter
geschlüpft = Alter ≥ 5 Minuten
```

Füttern und Training sind erst nach dem Schlüpfen verfügbar.

### Generation

Jedes neu erzeugte Löm gehört zunächst zu `Generation 1`, startet auf Kampf-Level `1` und kann
maximal Kampf-Level `9` erreichen. Beim Generationswechsel werden Trainingssiege und das
aktuelle Trainingsfenster vollständig gelöscht. Die neue Generation übernimmt stattdessen
zufällig `15–20 %` des zuvor erreichten Kampf-Levels als Zusatzlevel zu ihrem Basislevel `1`.
Das dadurch entstehende Start-Level besitzt sofort die normalen Werte dieses Levels.

Das neue Maximallevel ergibt sich aus dem bisherigen Maximallevel plus dem vererbten Start-Level
plus einem weiteren Generationslevel. Beispiel: G1 erreicht Level und Maximum `9`. Bei
`15 %` Vererbung startet G2 auf Level `2` und kann Level `12` erreichen; bei `20 %` startet sie
auf Level `3` und kann Level `13` erreichen. Dieselbe Formel wird für jede spätere Generation
erneut auf deren tatsächlich erreichtes Level und deren bisheriges Maximum angewendet.

### Zufällige Eigenschaften

- Geschlecht: männlich oder weiblich, gleichverteilt.
- Element: Feuer, Wind, Wasser oder Erde, gleichverteilt.
- Die neun normalen Farben haben jeweils ein Losgewicht von `10`.
- Weiß und Schwarz haben jeweils ein Losgewicht von `3`.
- Gesamtgewicht der Farblotterie: `96`.

Damit besitzt jede normale Farbe eine Wahrscheinlichkeit von `10 / 96`, Weiß und Schwarz
jeweils `3 / 96`.

Der Name kann geändert werden, wird getrimmt und auf maximal 20 Zeichen begrenzt. Ein leerer
Name wird nicht übernommen.

## 3. Hunger

Hunger steigt kontinuierlich um fünf Prozentpunkte pro Stunde:

```text
verstrichene Stunden = max(jetzt − letztes Vitalwerte-Update, 0) / 1 Stunde
Hunger = clamp(gespeicherter Hunger + verstrichene Stunden × 5, 0, 100)
```

Ab `70 %` gilt das Löm als hungrig. Dieser Grenzwert steuert:

- den hungrigen Sprite,
- Hunger-Benachrichtigungen,
- den Beginn beziehungsweise Fortbestand eines langfristig schlechten Zustands.

## 4. Gewicht

Ohne Aktion sinkt das Gewicht um 20 g pro Stunde:

```text
Gewicht = max(
    gespeichertes Gewicht − verstrichene Stunden × 20 g,
    Minimalgewicht der Form
)
```

Beim Speichern wird das berechnete Gewicht auf ganze Gramm abgeschnitten. Das Minimalgewicht
ist die Hälfte, die technische Obergrenze das Dreifache des gesunden Richtgewichts.
Diese Grenzen gelten zusätzlich beim Laden und Schreiben des Spielstands. Ältere oder
fehlerhafte Werte außerhalb des gültigen Bereichs werden sofort auf die Grenze der aktuellen
Form korrigiert.

| Form | Minimalgewicht | Gesundes Richtgewicht | Obergrenze |
|---|---:|---:|---:|
| Junges Löm | 2,5 kg | 5 kg | 15 kg |
| Flügel-Löm | 7,5 kg | 15 kg | 45 kg |
| Majestätisches Flügel-Löm | 10 kg | 20 kg | 60 kg |
| Sturmkaiser-Löm | 15 kg | 30 kg | 90 kg |
| Wurst-Löm | 5 kg | 10 kg | 30 kg |
| Prunkschlangen-Löm | 5 kg | 10 kg | 30 kg |
| Haufen-Löm | 12,5 kg | 25 kg | 75 kg |

Der gesunde Bereich liegt inklusive Grenzen bei `80–130 %` des Richtgewichts:

```text
gesund = Gewicht ∈ [Richtgewicht × 0,80; Richtgewicht × 1,30]
```

Die Statusfarbe verwendet einen zusätzlichen gelben Warnbereich von `60–170 %`. Außerhalb
davon wird der Gewichtsbalken rot.

Bei einer Evolution wird das Gewicht proportional auf das neue Profil übertragen:

```text
neues Gewicht = altes Gewicht × neues Richtgewicht / altes Richtgewicht
neues Gewicht = clamp(neues Gewicht, neues Minimalgewicht, neue Obergrenze)
```

## 5. Futter

| Futter | Hunger | Gewicht | Zufriedenheit | Gesundheit |
|---|---:|---:|---:|---:|
| Schinken | −25 | +150 g | −2 | ±0 |
| Melone | −15 | +60 g | +2 | ±0 |
| Heilspritze | unverändert | unverändert | unverändert | auf 100 |

Für jede Mahlzeit gilt:

- Hunger fällt nicht unter `0`.
- Gewicht steigt nicht über die Obergrenze der aktuellen Form.
- Zufriedenheit wird auf `0–100` begrenzt; normales Futter verändert Gesundheit nicht.
- Gesamtmahlzeiten und der Zähler der jeweiligen Futtersorte steigen um eins.

Wird ein schlafendes Löm gefüttert, gelten zusätzlich die Regeln aus „Schlaf und Wecken“.

### Heilspritze

Für jeweils drei volle gealterte Tage (`72` Altersstunden) wird eine Heilspritze verfügbar.
Das Alter umfasst dabei wie bei Evolutionen auch ein im Debug-Modus hinzugefügtes Bonusalter.
Spritzen sind nicht stapelbar: Ist beim nächsten Dreitages-Meilenstein bereits eine vorhanden,
bleibt es bei einer Spritze und der Meilenstein gilt dennoch als verarbeitet. Nach dem Verbrauch
kann daher erst ein späterer Dreitages-Meilenstein wieder eine Spritze vergeben.

Die Spritze erscheint im Fütterungsbildschirm als dritte Auswahl. Nach dem Ziehen auf das Löm
erklärt ein Bestätigungsdialog, dass sie die Gesundheit vollständig auf `100` setzt und danach
verbraucht ist. Abbrechen verändert den Spielstand nicht. Bestätigen verbraucht genau eine
Spritze; Hunger, Gewicht, Zufriedenheit und Mahlzeitenzähler bleiben unverändert. Es gibt keine
eigene Fütterungsanimation. Anschließend bestätigt ein zweiter Hinweis die vollständige Heilung.

Mit dem Start der Fütterungsanimation wird ein kurzer Soundeffekt über den Android-Spielkanal
abgespielt. Schinken verwendet ein trockenes, 1,25 Sekunden langes Knusper-/Kaugeräusch;
Melone ein helleres, 1,48 Sekunden langes Biss-, Schmatz- und Schlürfgeräusch. Beide Sounds
werden mit 75 Prozent ihrer Sample-Lautstärke wiedergegeben und folgen der Medienlautstärke
des Geräts.

## 6. Zufriedenheit

Die natürliche Zufriedenheit sinkt pro voller Stunde um einen Punkt:

```text
Zufriedenheit = max(gespeicherte Zufriedenheit − volle Stunden, 0)
```

Liegt ein Kackhaufen herum, sinkt sie zusätzlich um einen Punkt pro voller schmutziger Stunde.
Damit kann sie bei Schmutz insgesamt um zwei Punkte pro Stunde fallen. Schlafstrafen und
Weckkosten können zusätzlich hinzukommen.

Statusfarben:

- Grün: mindestens `70`.
- Gelb: `40–69`.
- Rot: unter `40`.

## 7. Gesundheit und langfristig schlechte Zustände

Gesundheit liegt immer zwischen `0` und `100`. Ein herumliegender Kackhaufen kostet zwei
Gesundheitspunkte pro voller schmutziger Stunde.

Solange die Zufriedenheit im grünen Bereich bei mindestens `70` liegt, regeneriert das Löm
unabhängig vom Schlaf einen Gesundheitspunkt pro voller Stunde. Normales Futter und das
Training verändern die Gesundheit nicht; die zeitbasierte Regeneration wird auch nach einem
Neustart anhand der vergangenen vollen Stunden verrechnet.

Ein langfristig schlechter Zustand liegt vor, wenn mindestens eine Bedingung erfüllt ist:

- Hunger mindestens `70 %`,
- Zufriedenheit unter `40 %`,
- Gewicht über `130 %` des Richtgewichts.

Untergewicht löst aktuell keine langfristige Gesundheitsstrafe aus, beeinflusst aber den
Pflege-Score negativ.

Beim erstmaligen Eintritt beginnt eine zweistündige Schonfrist. Danach kostet jede weitere
volle Stunde einen Gesundheitspunkt:

```text
Strafbeginn = Zeitpunkt des schlechten Zustands + 2 Stunden
Gesundheitsverlust = volle Stunden seit dem letzten verrechneten Strafzeitpunkt
```

Sind alle drei Bedingungen wieder unkritisch, werden Beginn und Strafzeitpunkt vollständig
zurückgesetzt. Ein späterer schlechter Zustand erhält wieder die volle Schonfrist.

Schmutz-, Schlaf- und Langzeitstrafen sind voneinander unabhängig und können sich addieren.

## 8. Schlaf, Licht und Wecken

Die Schlafzeit hängt von der aktuellen Form ab:

| Form | Schlafzeit |
|---|---|
| Junges Löm, Evolution 0 | 20:00–08:00 Uhr |
| Alle entwickelten Löms, ab Evolution 1 | 21:00–08:00 Uhr |

Ein geschlüpftes Löm schläft in seinem Zeitraum, sofern seine Weckzeit abgelaufen ist.

```text
schläft = geschlüpft UND Nachtzeit UND jetzt ≥ wach-bis
```

Im Debug-Build kann Schlaf unabhängig von Uhrzeit und Weckzeit erzwungen werden. Der Schalter
befindet sich unter den Debug-Werkzeugen und kann den erzwungenen Schlaf auch wieder beenden.
Bei einem ungeschlüpften Ei ist er deaktiviert. Release-Builds ignorieren einen eventuell im
Debug-Spielstand gespeicherten erzwungenen Schlafzustand.

### Licht

- Licht aus in der Nacht gibt `+1` Pflegepunkt.
- Licht an in der Nacht gibt `−1` Pflegepunkt.
- Für jede volle Schlafstunde mit eingeschaltetem Licht verliert das Löm zusätzlich
  `1` Zufriedenheit und `1` Gesundheit.
- Für jede volle Schlafstunde mit ausgeschaltetem Licht regeneriert das Löm `1` Gesundheit
  (höchstens bis `100`).
- Während das Löm schläft, kann der wiederverwendbare Schlaf-Teddy vom Icon neben dem
  Lichtschalter auf das Löm gezogen werden. Er schwebt anschließend als separates Overlay über
  jeder Löm-Form; dafür werden keine zusätzlichen Form-Sprites benötigt.
- Der Teddy erhöht die Heilung bei ausgeschaltetem Licht für diese Schlafphase zufällig um
  `10–15 %`. Prozentreste werden gespeichert und in späteren Schlafstunden weiterverrechnet,
  damit der Bonus trotz ganzzahlig angezeigter Gesundheit nicht durch Rundung verloren geht.
- Bei eingeschaltetem Licht gibt der Teddy keine Heilung. Sobald die Schlafphase endet, wird
  seine Platzierung einschließlich des zufälligen Bonus zurückgesetzt und er kann beim nächsten
  Schlaf erneut hineingezogen werden.
- Außerhalb der Nacht schaltet ein Welt-Update ein eventuell ausgeschaltetes Licht wieder ein.

### Wecken

Füttern oder Training während des Schlafs:

- weckt das Löm für 15 Minuten,
- kostet zusätzlich `5` Zufriedenheit,
- kostet zusätzlich `3` Gesundheit,
- setzt eine laufende Licht-an-Schlafmessung zurück.

Nach den 15 Minuten schläft es wieder, falls weiterhin Nachtzeit ist.

## 9. Sauberkeit

Nach Spielstart beziehungsweise nach dem Wegspülen wird der nächste Kackhaufen zufällig nach
6 bis 12 Stunden geplant. Die Grenzen sind ganzzahlige Stunden und beide inklusive.

- Es kann höchstens ein Haufen gleichzeitig existieren.
- Wird die App erst später geöffnet, gilt der ursprünglich geplante Zeitpunkt als Beginn der
  Verschmutzung. Die bis dahin entstandenen Verluste werden nachberechnet.
- Wegspülen aktualisiert zuerst Zufriedenheit und Gesundheit, entfernt den Haufen und plant
  den nächsten Zeitpunkt.
- Im Debug-Modus kann sofort ein Haufen erzeugt werden.

## 10. Training und Trainingsspiel

Das Treffertraining besteht aus drei Angriffen. Der Marker bewegt sich in `1.150 ms` von einem
Rand zum anderen und anschließend zurück.

Pro Angriff wird der Abstand `d` des Markers zur Mitte (`0,5`) ausgewertet:

| Abstand zur Mitte | Punkte |
|---|---:|
| `d ≤ 0,10` | 3 |
| `0,10 < d ≤ 0,25` | 2 |
| `d > 0,25` | 0 |

Ab sechs Gesamtpunkten ist das Training gewonnen. Jede abgeschlossene Runde erhöht weiterhin
den Trainingseinheiten-Zähler, unabhängig vom Ergebnis. Für den Battle-Stärkebonus werden
jedoch ausschließlich gewonnene Trainings gezählt. Die Lebenszeit ist in feste
Sechs-Stunden-Fenster geteilt. Pro Fenster können höchstens drei gewonnene Trainings für die
Stärke gutgeschrieben werden. Weitere Trainingssiege wirken sich im selben Fenster weiterhin
normal auf die übrigen Trainingsfolgen aus, erhöhen aber nicht den Stärkebonus. Ungenutzte
Gutschriften werden nicht in spätere Fenster übertragen.

Die über alle Fenster gesammelten, anrechenbaren Siege bleiben innerhalb derselben Generation
dauerhaft erhalten und besitzen dort keine globale Obergrenze. Nur der Zuwachs je
Sechs-Stunden-Fenster ist auf drei begrenzt. Mit einer neuen Generation werden sämtliche
Trainingssiege und der daraus berechnete Trainingsbonus vollständig gelöscht.

| Auswirkung | Gewonnen | Verloren |
|---|---:|---:|
| Zufriedenheit | +10 | ±0 |
| Gesundheit | ±0 | ±0 |
| Hunger | +10 | +10 |
| Gewicht | −100 g | −100 g |

Hunger ist auf `100`, Gewicht am Minimalgewicht begrenzt. Nachts kommen die Weckkosten hinzu.

Training erhöht außerdem die Battle-Stärke über eine stark abflachende logarithmische Formel. Alte
Spielstände besitzen noch keinen getrennten Sieg-Zähler. Bei der ersten Übernahme werden ihre
bisherigen Trainingseinheiten deshalb als Siege behandelt, aber rückwirkend auf höchstens drei
Siege je begonnenem Sechs-Stunden-Fenster begrenzt. Die tatsächliche Gesamtzahl der
Trainingseinheiten bleibt für die Statistik erhalten.

## 11. Pflege-Score

Der Pflege-Score ist ein zeitgewichteter Durchschnitt. Bei jedem Pflege-Update wird ein
Momentwert berechnet:

| Bereich | Zustand | Punkte |
|---|---|---:|
| Hunger | `0–35` | +2 |
| Hunger | `>35–60` | +1 |
| Hunger | `>60–80` | −1 |
| Hunger | `>80–100` | −2 |
| Zufriedenheit | `70–100` | +2 |
| Zufriedenheit | `50–69` | +1 |
| Zufriedenheit | `30–49` | −1 |
| Zufriedenheit | `0–29` | −2 |
| Gesundheit | `70–100` | +2 |
| Gesundheit | `40–69` | 0 |
| Gesundheit | `0–39` | −2 |
| Gewicht | gesund (`80–130 %`) | +1 |
| Gewicht | ungesund | −1 |
| Nacht | Licht aus | +1 |
| Nacht | Licht an | −1 |
| Tag | unabhängig vom Licht | 0 |
| Sauberkeit | Kackhaufen vorhanden | −2 |
| Sauberkeit | sauber | 0 |

Der theoretische Momentwert liegt zwischen `−10` und `+8`.

```text
verstrichene Pflegezeit = Stunden seit letztem Pflege-Update
Pflegesumme_neu = Pflegesumme_alt + Momentwert × verstrichene Pflegezeit
Pflegestunden_neu = Pflegestunden_alt + verstrichene Pflegezeit
Pflegedurchschnitt = Pflegesumme / Pflegestunden
```

Solange noch keine Pflegestunden gespeichert sind, wird direkt der aktuelle Momentwert benutzt.
Der Debug-Button „+1 Stunde“ addiert den aktuellen Momentwert mit genau einer Pflegestunde.

## 12. Evolution

### Erste Evolution

Nach 72 Lebensstunden entwickelt sich das junge Löm beim nächsten Welt-Update:

```text
Pflegedurchschnitt ≥ 1,0 → Flügel-Löm
Pflegedurchschnitt < 1,0 → Wurst-Löm
```

### Zweite Evolution

Der Zeitpunkt liegt deterministisch zwischen dem fünften und siebten Lebenstag:

```text
Offset = floorMod(Geburtszeit in Epoch-Stunden, 49)
Evolutionsalter = 120 Stunden + Offset
```

Der Offset liegt damit zwischen `0` und `48` Stunden.

- Ein Flügel-Löm entwickelt sich zu diesem Zeitpunkt automatisch zum majestätischen
  Flügel-Löm.
- Beim persönlichen Evolutionszeitpunkt wird das Wurst-Löm genau einmal anhand seines dann
  geltenden Pflegedurchschnitts bewertet: mindestens `1,0` ergibt das Prunkschlangen-Löm,
  ein Wert unter `1,0` ergibt sofort das Haufen-Löm.
- Nach dieser Entscheidung findet aktuell kein späterer Wechsel zwischen Prunkschlangen- und
  Haufen-Löm statt.

### Erwachsene Sturmkaiser-Form

Männliche und weibliche majestätische Flügel-Löms besitzen dieselbe dritte Evolution. Der
Zeitpunkt liegt für beide deterministisch zwischen dem 14. und 16. Lebenstag:

```text
Offset = floorMod(Geburtszeit in Epoch-Stunden + 17, 49)
Evolutionsalter = 336 Stunden + Offset
```

Der Offset liegt zwischen `0` und `48` Stunden. Sobald das Löm die Evolutionsstufe `2` und den
guten Pfad besitzt und dieses Alter erreicht, entwickelt es sich beim nächsten Welt-Update
automatisch zum Sturmkaiser-Löm beziehungsweise zur Sturmkaiserin-Löm. Voraussetzungen,
Gewichtsprofil und Statuswerte sind für beide Geschlechter identisch; nur die Sprites und der
Anzeigename unterscheiden sich. Prunkschlangen- und Haufen-Löms besitzen aktuell weiterhin
keine dritte automatische Evolution.

Die technische Evolutionsstufe ist auf `3` begrenzt.

## 13. Battle-Werte

### Basiswerte

| Form | Basisstärke | Basisverteidigung |
|---|---:|---:|
| Junges Löm | 5 | 5 |
| Flügel-Löm | 12 | 9 |
| Wurst-Löm | 8 | 14 |
| Majestätisches Flügel-Löm | 18 | 15 |
| Sturmkaiser-Löm | 20 | 16 |
| Prunkschlangen-Löm | 15 | 18 |
| Haufen-Löm | 4 | 6 |

### Pflegefaktor

```text
Pflegefaktor = clamp(1 + Pflegedurchschnitt / 8 × 0,10; 0,90; 1,10)
```

Pflege kann Stärke und Verteidigung damit um höchstens zehn Prozent senken oder erhöhen.

### Training und Erfahrung

```text
Trainingsbonus = floor(ln(1 + anrechenbare gewonnene Trainings))
Level-EP-Bedarf = 100 + 50 × (Level − 1) + 25 × (Level − 1)²
Stärkebonus durch Level = (Level − 1) × 2
Verteidigungsbonus durch Level = Level − 1

Stärke = floor(Basisstärke × Pflegefaktor)
        + Trainingsbonus
        + Level-Stärkebonus

Verteidigung = floor(Basisverteidigung × Pflegefaktor)
             + Level-Verteidigungsbonus

angezeigte Kampfstufe = Stärke
```

Generation 1 beginnt auf Kampf-Level `1`. Spätere Generationen können durch die Vererbung auf
einem höheren Level beginnen. Kampf-EP allein verändern keine Kampfwerte. Erst wenn
der vollständige EP-Bedarf des aktuellen Levels erreicht ist, steigt das Löm ein Level auf und
erhält dauerhaft `+2` Stärke und `+1` Verteidigung. Überschüssige EP werden in das nächste
Level übernommen. Der quadratisch steigende Bedarf sorgt dafür, dass spätere Level zunehmend
länger dauern: Level 1 benötigt `100 EP`, Level 2 `175 EP`, Level 3 `300 EP`, Level 4 `475 EP`.
Generation 1 besitzt Maximallevel `9`, das nach insgesamt `5.700 EP` erreicht wird. In späteren
Generationen steigt die Levelgrenze gemäß der Vererbungsregel. Am jeweiligen Maximallevel
werden keine weiteren Kampf-EP gesammelt; die Statusanzeige zeigt Level und aktuelle Grenze.

## 14. Elemente

Der Elementfaktor wirkt direkt auf die kampfentscheidende Stärke:

| Angreifer | Verteidiger | Faktor |
|---|---|---:|
| Wasser | Feuer | 1,03 |
| Feuer | Wind | 1,03 |
| Wind | Erde | 1,03 |
| Erde | Wasser | 1,03 |
| umgekehrte Richtung | jeweiliges Vorteilselement | 0,97 |
| Feuer ↔ Erde | neutral | 1,00 |
| Wasser ↔ Wind | neutral | 1,00 |
| gleiches Element | neutral | 1,00 |

## 15. Battle-Ergebnis

Ein Löm ist erst ab Evolutionsstufe `1` und mit mindestens `15` Gesundheit kampffähig. Das
junge oder zu stark angeschlagene Löm kann weder sichtbar geschaltet werden noch
Herausforderungen senden oder annehmen. Sinkt die Gesundheit während der WLAN-Sichtbarkeit
unter diese Grenze, wird der lokale Kampfmodus beendet.

Zu Kampfbeginn werden Name, Element, Form, Stärke, Verteidigung und Siege als Snapshot fixiert.
Erst nachdem beide Snapshots vorliegen, werden die beiden angepassten Stärkewerte berechnet:

```text
angepasste Stärke = Stärke × Elementfaktor
Rohchance A = angepasste Stärke A ÷ (angepasste Stärke A + angepasste Stärke B)
Gewinnchance A = clamp(Rohchance A; 0,20; 0,80)
Gewinnchance B = 1 − Gewinnchance A
```

Bei angepassten Stärken von 40 und 30 ergeben sich damit vor der Deckelung `40 / 70 = 57,14 %`
und `30 / 70 = 42,86 %`. Auch ein extrem überlegenes Löm erhält nie mehr als 80 Prozent;
entsprechend bleiben dem schwächeren Löm immer mindestens 20 Prozent.

Die gemeinsame Kampf-ID erzeugt genau einen deterministischen Ergebniswert zwischen `0` und
`1`:

```text
Ergebniswert < Gewinnchance des Gastgebers → Gastgeber gewinnt
sonst → Herausforderer gewinnt
```

Das Gastgebergerät führt diesen einzigen Wurf aus und sendet die exakt gespiegelte Sicht samt
Stärkewerten und Chancen an den Gegner. Es gibt keine getrennten Würfe auf beiden Geräten;
deshalb können niemals beide gewinnen oder beide verlieren. Der bisherige zusätzliche
15-Prozent-Glückstreffer, die Verteidigung als Teil der Kampfkraft sowie die zufällige
Stärkeschwankung entfallen vollständig.

### Belohnung, Bilanz und Gesundheit

Nur der Gewinner erhält Kampf-EP. Entscheidend ist das Verhältnis der bereits inklusive
Element berechneten Stärken:

```text
EP = round(20 × gegnerische Kampfkraft ÷ eigene Kampfkraft)
EP-Untergrenze = 10
EP-Obergrenze = 40
```

Ein gleich starker Gegner gibt somit `20 EP`, ein 50 Prozent stärkerer etwa `30 EP`. Der
Verlierer erhält keine EP.

Verteidigung beeinflusst ausschließlich den Gesundheitsverlust nach dem Kampf. Der
Grundverlust beträgt beim Verlierer `8` und beim Gewinner `4`. Jeder Verteidigungspunkt senkt
diesen Verlust um zwei Prozentpunkte; die Reduktion ist auf 50 Prozent begrenzt:

```text
Verteidigungsreduktion = min(Verteidigung × 0,02; 0,50)
Gesundheitsverlust = round(Grundverlust × (1 − Verteidigungsreduktion))
```

Die vorhandenen Verteidigungswerte liegen je nach Form, Pflege und Level typischerweise etwa
zwischen 9 und 26. Dadurch sind schon 18 bis 36 Prozent Reduktion im normalen Bereich spürbar,
während 25 Verteidigung das Maximum erreicht. Der Gewinner verliert somit mindestens `2`, der
Verlierer mindestens `4` Gesundheit. Gesundheit bleibt auf `0–100` begrenzt.

Im Status stehen Kampf-Level und EP-Fortschritt des aktuellen Levels als `vorhanden / benötigt`,
die Gewinnquote sowie direkt darunter die absolute Zahl gewonnener und verlorener Kämpfe.
Ohne bisherige Kämpfe beträgt die Gewinnquote `0,0 %`;
ansonsten gilt `Siege ÷ (Siege + Niederlagen) × 100`.

### Kampfanimationen

Flügel-Löm, majestätisches Flügel-Löm, Sturmkaiser-Löm, Prunkschlangen-Löm und Haufen-Löm
besitzen jeweils eigene Kampf-Sheets für Angriff, Treffer, Doppelangriff, Doppeltreffer und
Sieg. Die Elementgeschosse werden getrennt von der Figur gerendert, damit die vier Elemente
nicht je Form neu gezeichnet werden müssen. Auf Evolutionsstufe `3` im guten Pfad wird das
männliche oder weibliche Sturmkaiser-Set passend zum Geschlecht gewählt. Beide Sets verwenden
dieselben Kampfwerte und dieselben zeitlichen Abläufe.

Ein Kampf dauert ungefähr 20 Sekunden und besteht aus vier Runden zu je fünf Sekunden:

| Runde | 0–2 Sekunden | 2–3 Sekunden | 3–5 Sekunden |
|---|---|---|---|
| 1–3 | eigenes Elementgeschoss abschießen | Flugpause | gegnerischen Treffer einstecken |
| 4, Gewinner | zwei eigene Geschosse | Flugpause | Siegesbewegung |
| 4, Verlierer | auf Endangriff vorbereiten | Vorbereitung | zwei Treffer einstecken |

Feuer erscheint als Flammenkugel, Wasser als Wasserball, Wind als Luftwirbel und Erde als
Steingeschoss. Das Kampfergebnis steht vor Beginn der Animation bereits fest; die vier Runden
sind dessen visuelle Darstellung und berechnen keine zusätzlichen Schadenspunkte.

Bevor die Animation sichtbar wird, speichert jedes Gerät Ergebnis, Bilanz, Kampf-EP,
Gesundheitsverlust und Freigabezeit gemeinsam in einer dauerhaften Transaktion. Wird die App
während des Kampfes beendet, bleibt die Navigation nach dem Neustart für die restliche
23-Sekunden-Sequenz gesperrt und die Animation wird an der zeitlich passenden Stelle
fortgesetzt. Erst danach wird Sieg oder Niederlage angezeigt.

Jedes Element besitzt einen eigenen Abschuss- und Trefferklang. Der Abschuss startet mit der
Angriffsphase. Der Trefferklang startet nicht beim Erscheinen des anfliegenden Projektils,
sondern nach 1,36 Sekunden der Trefferphase beim sichtbaren Einschlag. Beim Doppelangriff
beziehungsweise Doppeltreffer wird derselbe Klang nach 180 ms ein zweites Mal und leicht höher
abgespielt. Battle-Sounds laufen mit etwa 72–78 Prozent Sample-Lautstärke über den
Medien-/Spielkanal des Geräts.

Nach dem letzten Angriff bleibt der jeweils letzte Ergebnisframe weitere drei Sekunden stehen:
bei einem Sieg der letzte Frame der Siegesanimation, bei einer Niederlage der letzte Frame der
finalen Trefferanimation. Gleichzeitig läuft ein eigener, ebenfalls drei Sekunden langer,
generierter Sieger- beziehungsweise Verlierersound. Erst nach dieser Abschlussphase erscheint
der Ergebnisdialog. Die gesamte Sequenz dauert dadurch 23 Sekunden.

## 16. Lokale Kämpfe

- „Online“ ist aktuell nur ein deaktivierter Platzhalter.
- „Lokal“ verwendet Android Network Service Discovery mit dem Diensttyp
  `_loems-battle._tcp.` im gleichen WLAN.
- Sichtbarkeit und Suche laufen nur bei aktiviertem lokalen Kampfmodus.
- Eine Herausforderung muss innerhalb von 30 Sekunden angenommen werden.
- Socket-Verbindungen besitzen ungefähr 35 Sekunden Timeout.
- Während bereits eine Entscheidung wartet, werden weitere Herausforderungen abgelehnt.
- Verlassen beziehungsweise Deaktivieren des Modus beendet Werbung, Suche und Server.
- Gast-WLANs oder Router mit Client-Isolation können direkte Kämpfe verhindern.

## 17. Benachrichtigungen

Der Hintergrund-Worker läuft als eindeutige periodische Arbeit ungefähr alle 15 Minuten. Das
Betriebssystem darf den tatsächlichen Zeitpunkt verschieben.

Optionale Benachrichtigungen:

- Schlaf: einmal pro Nachtfenster zwischen 20:00 und 07:00 Uhr.
- Evolution: wenn die gespeicherte Evolutionsstufe gestiegen ist.
- Sauberkeit: einmal pro neuem Kackhaufen.
- Hunger: beim erstmaligen Überschreiten von `70 %`; nach Erholung kann später erneut
  benachrichtigt werden.

Ab Android 13 ist dafür die Benachrichtigungsberechtigung erforderlich.

## 18. App-Updates

Der Release-Build fragt beim Start über die offizielle Google-Play-In-App-Update-API ab, ob für
`de.loems.app` eine Version mit höherem Versionscode im erreichbaren Play-Track verfügbar ist.
Falls Google Play ein sofortiges Update erlaubt, erscheint der von Google bereitgestellte
Update-Dialog. Ein bereits gestartetes Update wird beim Zurückkehren in die App fortgesetzt.

Der separat installierbare Debug-Build `de.loems.app.debug` überspringt diese Prüfung, weil
dieser Paketname keinen Eintrag im Play-Testtrack besitzt.

## 19. Debug-Mechaniken

Nur Debug-Builds bieten zusätzliche Eingriffe und Anzeigen:

- eine simulierte Stunde hinzufügen,
- sofort schlüpfen,
- gute oder schlechte Evolution auslösen,
- jede vorhandene Form einschließlich majestätischem Flügel-Löm, Haufen-Löm,
  Prunkschlangen-Löm sowie Sturmkaiser-Löm und Sturmkaiserin-Löm direkt setzen,
- sofort einen Kackhaufen erzeugen,
- Sprite-Zustände direkt auswählen,
- Pflege-Durchschnitt anzeigen,
- Battle-Basiswerte und jede Stufe der Kampfwertformel anzeigen.
- 23-Sekunden-Kampfanimation als Sieg oder Niederlage lokal testen, ohne die Bilanz zu ändern.
- Im Fütterungsbildschirm eine Heilspritze sofort freischalten; der Button ist deaktiviert,
  solange bereits eine Spritze vorhanden ist.

Die simulierte Stunde aktualisiert Hunger, Gewicht, Zufriedenheit und Gesundheit, addiert eine
Pflegestunde und erhöht das Bonusalter um eine Stunde.

## 20. Verbindliche Regeln für neue Löm-Formen

Eine neue Form gilt erst als vollständig umgesetzt, wenn Spielregeln, Darstellung, Debug-Werkzeuge,
Tests und diese Dokumentation gemeinsam angepasst wurden. Ein einzelnes Sprite oder eine nur im
Debug-Modus sichtbare Form zählt nicht als fertige Evolution.

### Evolutions- und Zustandsmodell

Für jede neue Form müssen ausdrücklich festgelegt und implementiert werden:

1. Evolutionsstufe, Evolutionspfad und gegebenenfalls Geschlechtsbedingung.
2. Frühestes Alter, Zeitfenster und die deterministische Berechnung des konkreten Zeitpunkts.
3. Verhalten nicht berechtigter Geschlechter und Pfade; sie dürfen nicht versehentlich dieselbe
   Form erhalten.
4. Anzeigename in Status, Kampf, Evolution und Benachrichtigungen.
5. Kompatibilität mit bestehenden Spielständen. Neue Felder benötigen sichere Standardwerte;
   vorhandene Generationen, Zähler und Werte dürfen nicht stillschweigend gelöscht werden.

Automatische Evolutionen werden zentral beim Welt-Update geprüft. Alter bedeutet immer reales
Alter plus gespeichertes Bonusalter. Dieselbe Bedingung muss als testbare Domain-Funktion
vorliegen und mindestens die Fälle „zu früh“, „berechtigt“ und „nicht berechtigtes Geschlecht
oder falscher Pfad“ abdecken.

### Werte und Balance

Jede neue Form benötigt ein eigenes oder ausdrücklich wiederverwendetes Gewichtsprofil mit
Minimalgewicht, Richtgewicht und Obergrenze. Die proportionale Gewichtsumrechnung bei der
Evolution muss erhalten bleiben. Außerdem müssen Basisstärke und Basisverteidigung bewusst
festgelegt und durch Tests gegen die direkte Vorstufe abgesichert werden. Eine optisch stärkere
Form erhält nicht automatisch einen großen Kampfvorteil; Stärke, Level, Pflege, Element und
Zufallswurf bleiben Teil der Balance.

### Vollständiges sichtbares Sprite-Set

Jede spielbare neue Form benötigt mindestens diese zehn separat auswählbaren Zustände:

- Idle,
- hungrig,
- Schlaf,
- Melone,
- Schinken,
- Angriff,
- Treffer,
- Doppelangriff,
- Doppeltreffer,
- Sieg.

Alle Sheets verwenden sechs Frames in einem `3 × 2`-Raster mit `512 × 512`-Zellen, sofern eine
bestehende Form nicht nachweislich ein anderes Format benötigt. Neue Sheets brauchen mindestens
`48 px` transparenten Sicherheitsrand je Zelle. Körperanker, Bodenlinie, Maßstab, Hörner,
Flügel, Schwanz und Recoloring-Maske werden automatisiert und visuell geprüft. Für Idle-Zustände
wird eine freigegebene kanonische Pose verwendet; unabhängiges Neuzeichnen aller sechs Frames
ist wegen Anatomieflackern nicht zulässig.

### Verdrahtung in der App

Beim Hinzufügen einer Form müssen gemeinsam aktualisiert werden:

- normale Idle-, Hunger- und Schlafauswahl,
- beide Fütterungsanimationen,
- alle Kampfphasen einschließlich finaler Doppelaktion und Ergebnis-Hold,
- stabile `SpriteFrame`-Ausschnitte und passende Bildschirmgröße,
- Debug-Spriteauswahl für alle zehn Zustände,
- Debug-Evolutionsauswahl für die Form,
- Formtitel, Eigenschaftenanzeige und Evolutionsbenachrichtigung.

Geschlechtsspezifische Formen müssen vor allgemeineren Pfadbedingungen geprüft werden. Eine
Bedingung wie „guter Pfad ab Stufe 2“ darf eine speziellere männliche oder weibliche Form nicht
wieder mit dem Sprite der Vorstufe überschreiben.

### Abnahme und fortlaufende Pflege

Vor Abschluss einer neuen Form sind verpflichtend:

1. Sprite-QA für jedes Sheet einschließlich Alpha-Grenzen, Randabstand, Schwerpunkt,
   Bodenlinie und letztem-zu-erstem Frame.
2. Visuelle GIF-Prüfung sämtlicher Zustände.
3. Domain-Tests für Evolution, Geschlecht, Gewicht und Kampfwerte.
4. Erfolgreicher sauberer Unit-Test- und Debug-APK-Build.
5. Aktualisierung der Tabellen und Regeln in `GAME_LOGIC.md` sowie der Sprite-Vorgaben in
   `SPRITE_GUIDELINES.md`.

Ändert eine spätere Mechanik allgemeine Regeln wie Gesundheit, Schlaf, Fütterung oder Kampf,
müssen alle vorhandenen Formen darauf geprüft werden. Neue Formen dürfen keine eigene
Sonderlogik erhalten, wenn dieselbe Regel sinnvoll zentral für alle Löms gelten kann.

## 21. Wertebegrenzungen

| Wert | Untergrenze | Obergrenze |
|---|---:|---:|
| Hunger | 0 | 100 |
| Zufriedenheit | 0 | 100 |
| Gesundheit | 0 | 100 |
| Gewicht | 50 % des Richtgewichts | 300 % des Richtgewichts |
| Evolution | 0 | 3 |

Zähler wie Mahlzeiten, Training, Siege, Niederlagen und Kampf-EP besitzen aktuell im lokalen Spielstand
keine künstliche Obergrenze. Über das WLAN-Protokoll empfangene Kampfwerte werden aus
Sicherheitsgründen auf plausible Bereiche begrenzt.
