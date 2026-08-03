# CANVAULT Color-Combo-Regeln

Stand: 3. August 2026

## Ziel

Color Combo erzeugt keine zufälligen Farbreihen. Jede Palette wird als Schichtaufbau bewertet:

- Background
- Second Outline
- Outline
- Fill-Schatten
- Fill
- Fill-Fade
- Highlight

Je nach gewählter Tonanzahl werden zwei bis sieben dieser Rollen verwendet. Der Bestandsmodus verwendet ausschließlich vorhandene, exakt aufgelöste Inventarfarben. Add Color bevorzugt vorhandene Farben und ergänzt fehlende Rollen ausschließlich mit realen Einträgen aus dem offiziellen Offline-Farbkatalog.

## Harte Lesbarkeitsregeln

- Fill und Outline benötigen mindestens `3:1` Kontrast.
- Outline, Second Outline und Background benötigen an jeder angrenzenden Kante ebenfalls mindestens `3:1`.
- Zwei dunkle Farben werden an einer solchen Kante immer abgelehnt, selbst wenn ihre Farbtöne verschieden sind.
- Paletten ab drei Tönen benötigen eine klar erkennbare Hell-Dunkel-Spanne.
- Highlights müssen wahrnehmbar heller als der Fill sein.
- Fill-Schatten müssen wahrnehmbar dunkler bleiben.
- Fill-Fades dürfen höchstens ungefähr 55 Grad im Farbton auseinanderliegen und müssen eine sichtbare, aber nicht abrupte Helligkeitsstufe bilden.

Die Grenze `3:1` folgt der [WCAG-Regel für angrenzende nicht-textliche visuelle Informationen](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast). Sie wird hier als robuste Mindestgrenze für die Erkennbarkeit von gemalten Kanten verwendet. Das ist eine Designübertragung, keine Behauptung, dass Graffiti selbst unter WCAG fällt.

## Wahrnehmungsbasierte Berechnung

Farbdifferenzen, Helligkeitsstufen und Fades werden in OKLab/OKLCH berechnet. Die [W3C-Spezifikation CSS Color 4](https://www.w3.org/TR/css-color-4/#ok-lab) beschreibt OKLab als wahrnehmungsgleichmäßiger als ältere RGB-/HSL-basierte Verfahren und empfiehlt OKLCH, wenn bei Übergängen die Farbsättigung erhalten bleiben soll.

Die eingebauten Profile kombinieren etablierte Regeln wie analog, komplementär, triadisch und Split-Komplementär mit Rollen- und Kontrastzwängen. [Adobe](https://helpx.adobe.com/pdf/after_effects_reference.pdf) führt diese Regeln ebenfalls als grundlegende Farbthemen. Aktuelle Inspirationsquellen wie [Color Hunt Popular](https://colorhunt.co/palettes/popular) und [Adobe Color](https://color.adobe.com/de/) betonen außerdem starke Kontraste, neutrale Anker, Retro-Schemata und Warm-Kalt-Kombinationen.

## Mehr als 100.000 Kombinationen

Es werden nicht 100.000 starre Paletten in die APK geschrieben. Stattdessen bildet die Engine dynamisch Kombinationen aus den exakt veröffentlichten Katalogfarben und acht Designprofilen. Bereits die möglichen Zwei-Farb-Paare des Katalogs überschreiten 100.000 deutlich; bei drei bis sieben Rollen wächst der Designraum auf viele Millionen Kombinationen.

Pro Generierung werden daraus die passendsten Kandidaten für den aktuellen Bestand gesucht, durch die harten Rollenregeln geprüft, nach Erkennbarkeit, Harmonie und verfügbarer Farbmenge bewertet und anschließend auf höchstens acht unterschiedliche, verständliche Ergebnisse verdichtet. So bleibt die App schnell und zeigt nicht tausende fast identische Vorschläge.
