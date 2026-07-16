# Sprite-Regeln für Löms

Damit Animationen nicht zittern oder Reste benachbarter Frames zeigen, gelten für jedes neue
Sprite-Sheet verbindlich diese Regeln:

1. Jeder Frame besitzt eine eigene, gleich große Zelle und bei neuen Sheets mindestens 48 Pixel
   transparenten Sicherheitsabstand zu allen Zellgrenzen. 28 Pixel sind nur für unveränderbare
   ältere Bestands-Sheets zulässig.
2. Kein Pixel eines Körpers, Flügels, Futters oder Effekts darf eine Zellgrenze überschreiten.
3. In allen Frames liegt der Mittelpunkt des Körpers auf derselben X-Koordinate.
4. Füße beziehungsweise der tiefste Körperpunkt liegen in allen Frames auf derselben Bodenlinie.
5. Der Körper behält in allen Frames denselben Maßstab. Nur die beabsichtigte Animation darf
   Silhouette oder Haltung verändern.
6. Vor dem Einbau werden die sichtbaren Alpha-Grenzen jedes Frames gemessen. Die App verwendet
   diese gemessenen Ausschnitte und normalisiert sie auf einen gemeinsamen Anker; sie verlässt
   sich nicht blind auf das Zellraster.
7. Einfarbige Hintergründe und Trennlinien werden vor dem Einbau vollständig entfernt.
8. Körperfarbe bleibt neutral grau; Augen, Zähne, Konturen, Wangen und Requisiten müssen klar
   davon getrennte Farben besitzen, damit nur der Körper zur Laufzeit eingefärbt wird.
9. Vor der Generierung wird ein realistisches Anatomie-Inventar festgelegt. Jeder Frame muss
   dieselbe Anzahl und Anordnung von Köpfen, Augen, Hörnern, Flügeln, Armen/Händen oder
   Vorderbeinen/-pfoten, Beinen/Füßen oder Hinterbeinen/-pfoten, Schwänzen, Fingern/Zehen und
   sonstigen wiederholten Körperteilen besitzen. Perspektivische Überdeckung ist erlaubt, muss
   aber durch die Pose erklärbar bleiben. Körperteile dürfen zwischen Frames niemals
   verschwinden, zusätzlich entstehen, unrealistisch verschmelzen oder die Ansatzseite wechseln.

Diese Prüfung ist bei Idle-, Hunger-, Fütterungs-, Schlaf-, Trainings- und Evolutionsanimationen
gleichermaßen erforderlich.

## Pflichtumfang einer neuen spielbaren Form

Eine neue Form benötigt Idle, hungrig, Schlaf, Melone, Schinken, Angriff, Treffer,
Doppelangriff, Doppeltreffer und Sieg. Alle zehn Zustände müssen sowohl im normalen Spiel als
auch einzeln in der Debug-Spriteauswahl verdrahtet sein. Die allgemeine Evolutions-, Werte-,
Test- und Dokumentations-Checkliste steht in `GAME_LOGIC.md` unter „Verbindliche Regeln für
neue Löm-Formen“.

Vor dem Einbau wird für jedes Sheet `tools/qa_sprite_sheet.py` ausgeführt und eine vollständige
Loop-GIF einschließlich letztem-zu-erstem Frame geprüft. Die App verwendet stabile, für alle
Frames einer Animation gleich große `SpriteFrame`-Ausschnitte. Nach der Verdrahtung müssen
Unit-Tests und ein Debug-APK-Build erfolgreich sein.
Zusätzlich wird jedes Einzelbild visuell gegen das zuvor festgelegte Anatomie-Inventar geprüft;
eine falsche oder inkonsistente Anzahl von Körperteilen blockiert den Einbau.
