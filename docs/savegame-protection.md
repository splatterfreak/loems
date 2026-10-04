# Spielstandschutz und Wiederherstellung

Stand: 17.09.2026. Implementiert, nicht automatisch im Play Store veröffentlicht.

## Befund zum gemeldeten G2-Verlust

Auf zwei Geräten mit Play-Store-App erschien ohne vorausgehendes Update direkt die
Namensabfrage für Generation 1; auch der Stammbaum fehlte und der Zustand blieb
nach Neustart erhalten. Der ursprüngliche Auslöser des fehlenden Spielstands ist
ohne Geräteprotokolle und betroffene Speicherdateien **nicht nachgewiesen**.

Ein konkreter Folgefehler im bisherigen Code erklärt die fehlende Ei-Anzeige:
`LoemNotificationWorker` rief `refreshWorld` auch ohne initialisierten Spielstand auf.
`readState` bildete leere Preferences auf ein neues unbenanntes G1-Löm ab, und
`refreshWorld` speicherte dieses dauerhaft. Wurde die App mindestens fünf Minuten
später geöffnet, war das Löm bereits geschlüpft. Dieser Ablauf setzt bereits fehlende
Daten voraus; er erklärt nicht, warum ein intakter G2-Spielstand ursprünglich fehlte.
Ein regulärer Generationswechsel erhöht die Generation und bewahrt die Vorfahren.

## Implementierter Schutz

- `ProtectedGameStore` ist die gemeinsame Zugriffsstelle aller Repository-Instanzen.
  Vordergrund und Worker teilen einen Mutex und denselben DataStore.
- Spielzustände werden erst nach geprüfter Initialisierung/Wiederherstellung an die UI
  veröffentlicht. `SaveProtectionGate` hält bei Speicherfehlern das Spiel an, bietet
  erneute Prüfung an und zeigt keinen falschen Schlupf- oder Namensbildschirm.
- Nur der erste Vordergrundstart darf bei tatsächlich leerer Installation ein Ei anlegen.
  Der Worker darf das ausdrücklich nicht. Vorhandene Schutzmarkierungen, beschädigte
  Sicherungen oder alte Benachrichtigungsmarker verhindern einen stillen Erststart.
- Zwei vollständige, SHA-256-geprüfte Sicherungen liegen unabhängig von der Hauptdatei
  unter `files/save_safety/`. Sie enthalten auch Namen, Stammbaum und Einstellungen.
  Sicherungen werden über temporäre Dateien, Dateisynchronisierung und atomaren Austausch
  geschrieben. Unbestätigte neue Spielaktionen werden nicht vorab als Sicherung gespeichert.
- Jeder Spielverlauf erhält eine unveränderliche interne Kennung sowie einen
  generationsübergreifenden Lebenszähler und Schreibzähler. Zwei getrennte
  Höchststandsmarken verhindern, dass ein technisch jüngerer, logisch aber älterer
  Stand unbemerkt als neuester Stand gilt.
- Zusätzlich zu den rotierenden Kopien gibt es einen monotonen Fortschrittsstand,
  einen Startanker je Generation und einen Abschlussstand vergangener Generationen.
  Der Fortschrittsstand akzeptiert innerhalb derselben Generation keine Rückschritte.
- Der letzte gültige Stand wird vor Änderungen gesichert, der bestätigte neue danach.
  Fehler beim Sichern oder Schreiben blockieren weitere Spielaktionen.
- Generation und Stammbaum dürfen nicht zurückgesetzt werden. Ein Generationswechsel
  muss den Eltern-Eintrag behalten und ohne Bonusalter als unbenanntes Ei beginnen.
- Innerhalb derselben Generation dürfen Entwicklung, Kampf-Erfahrung, Kampflevel-Basis
  und -Grenze, Siege, Niederlagen, Training, Fütterungen, Bonusalter, Pflegehistorie,
  bestätigter Name und feste Charaktermerkmale nicht zurückgehen oder wechseln.
  Ein verdächtiger Stand wird als Diagnosebeleg erhalten, aber nicht hochgestuft.
- Ein zweifach gespeicherter höchster Generationszähler verhindert G2→G1 auch dann,
  wenn die eigentlichen Sicherungen beschädigt sind. Eine Markierung liegt zusätzlich
  in `noBackupFilesDir`, damit eine alte Android-Wiederherstellung auf demselben Gerät
  nicht unbemerkt eine niedrigere Generation aktiviert.
- Die Wiederherstellung bewertet Generation, Revision und Konsistenz der Geburtsidentität
  und Vorfahren. Beschädigte Hauptdateien werden niemals durch leere Preferences ersetzt.
- Vor einer Reparatur werden die ursprünglichen Preferences bzw. beschädigten Rohbytes
  aufbewahrt. `diagnostics.log` hält maximal 100 Ereignisse ohne Charakternamen fest.
- Übrig gebliebene `.tmp`-/`.bak`-Dateien werden vor dem ersten DataStore-Zugriff
  separat gesichert. Ohne gültigen Hauptstand/Sicherung wird dann blockiert; ein
  möglicherweise nicht bestätigter Schreibvorgang wird nicht automatisch aktiviert.

## Bereits betroffene Installationen

1. Ist ein vollständiger gültiger Sicherungsstand vorhanden, wird dieser automatisch
   wiederhergestellt. Seit dem Sicherungszeitpunkt kann Fortschritt fehlen.
2. Existieren noch gültige Vorfahren, lässt sich ein fehlender oder zu niedriger
   Generationszähler aus deren höchster Generation ergänzen. Charakterdaten werden
   dabei nicht erfunden. Dies ist keine Rekonstruktion verlorener Namen oder Werte.
3. Ein freigeschalteter Stammbaum ohne passende Daten beweist einen Widerspruch, aber
   nicht die genaue frühere Generation. Ohne bessere Daten wird blockiert statt geraten.
4. Ein bereits vollständig überschriebener G1-Stand ohne alte Daten oder Sicherungen
   ist nicht zuverlässig von einer legitimen ersten Generation unterscheidbar. Die alte
   App hatte diese Sicherungskopien noch nicht; das Update kann sie nicht rückwirkend erzeugen.

Kein lokaler Schutz kann eine vollständige Löschung sämtlicher App-Daten oder einen
Geräteverlust überleben. Android-Backup ist keine garantierte externe Spielstandsicherung.
Die genaue Ursache der beiden ursprünglichen Verluste muss weiterhin am Gerät untersucht
werden. App nicht deinstallieren und Daten nicht löschen.

## Prüfung und Auslieferung

`SaveGameProtectionTest` prüft unter anderem Erststart, Worker ohne Stand, G2-Erhalt,
Reparatur aus Vorfahren, fehlende/beschädigte Hauptdatei, beschädigte Sicherung,
Generationsmarkierungen, Schreibfehler, konkurrierende Aktionen und unveränderte
Unicode-/Typ-Wiederherstellung sowie Rückschritte bei Level, Erfahrung, Entwicklung,
Kämpfen, Training und Fütterungen. Mehrere Tests verwenden echte
Preferences-DataStore-Dateien inklusive Schließen und erneutem Öffnen; zusätzlich
laufen die bisherigen Domänen-Tests. Der absichtlich korrupte Real-Datei-Test wird
unter Windows übersprungen, weil DataStore 1.2 dort die Testdatei nicht atomar über
ein vorhandenes NTFS-Ziel ersetzen kann; der Produktivpfad läuft auf Android/Linux.

Lokal geprüft: 145 Debug-Unit-Tests ohne Fehler (144 ausgeführt, ein oben erklärter
Windows-Plattformtest übersprungen), davon 34 Spielstandschutz-Tests, Debug-APK-Bau
und Release-Kotlin-Kompilierung.
Kein Test auf einem der betroffenen Handys; kein solches Gerät war verbunden.

Die Änderung muss als reguläres Update derselben Play-Store-App ausgeliefert werden.
Eine Debug-APK nutzt `de.loems.app.debug` und kann den privaten Spielstand von
`de.loems.app` nicht reparieren. Versionsnummer und Play-Veröffentlichung wurden
mit dieser Änderung nicht verändert. Keinesfalls zur Reparatur deinstallieren.
