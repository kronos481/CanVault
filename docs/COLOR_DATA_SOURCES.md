# CANVAULT Farbquellen

Stand: 3. August 2026

CANVAULT speichert ausschließlich HEX-Werte, die als digitale Farbfelder direkt auf einer Hersteller- oder Marken-Shopseite veröffentlicht sind. Farbkarten, die nur als Foto oder PDF vorliegen, werden nicht automatisch in HEX umgerechnet. Dadurch bleibt ein unbekannter Farbwert sichtbar unbekannt, statt als scheinbar exakte Schätzung im Inventar zu erscheinen.

Die veröffentlichten HEX-Werte sind Bildschirmreferenzen. Sie beschreiben nicht garantiert den physischen Lack unter jedem Licht und auf jedem Untergrund. Montana Cans weist ausdrücklich darauf hin, dass RGB/HEX der Orientierung dienen und für einen physischen Vergleich ein Echtfarbmuster beziehungsweise Testsprühen nötig ist.

## Lokal integrierte Digitalwerte

| Dosenlinie | In der App | Quelle |
|---|---:|---|
| MTN 94 | 217 | [MTN Shop](https://www.mtn-shop.de/mtn-94-ex0140241m) |
| MTN Hardcore | 142 | [MTN Shop](https://www.mtn-shop.de/mtn-hardcore) |
| MTN Vice | 50 | [MTN Shop](https://www.mtn-shop.de/mtn-vice-ex014vi0009) |
| MTN Water Based 400 | 83 | [MTN Shop](https://www.mtn-shop.de/mtn-water-based-400) |
| MTN Mega | 16 | [MTN Shop](https://www.mtn-shop.de/mtn-mega-colors) |
| Montana BLACK | 187 | [Montana BLACK](https://www.montana-cans.com/de/Montana-BLACK-400ml/263507), [Infra Colors](https://www.montana-cans.com/de/Montana-BLACK-400ml-Infra-Colors/352249) |
| Montana GOLD | 215 | [Colors](https://www.montana-cans.com/de/Montana-GOLD-400ml-Colors/284502), [Transparent](https://www.montana-cans.com/de/Montana-GOLD-400ml-Transparent-Colors/419362), [Fluorescent](https://www.montana-cans.com/de/Montana-GOLD-400ml-Fluorescent-Colors/521409), [Chrome](https://www.montana-cans.com/de/Montana-GOLD-400ml-Chrome-Effect-Colors/285936), [Metallic](https://www.montana-cans.com/de/Montana-GOLD-400ml-Metallic-Colors/369759) |
| Montana Ultra Wide | 12 | [Montana Cans](https://www.montana-cans.com/de/Montana-ULTRA-WIDE-750ml/486968) |
| Molotow Premium | 240 aktuell auslesbare Varianten | [Molotow Shop](https://shop.molotow.com/produkt/molotow-premium/) |
| Molotow Burner | 1 direkt als HEX veröffentlichte Variante | [Molotow Shop](https://shop.molotow.com/produkt/burner-spraydose/) |
| Molotow CoversAll | 1 direkt als HEX veröffentlichte Variante | [Molotow Shop](https://shop.molotow.com/produkt/coversall-color/) |
| Loop 400 | 218 aktuell auslesbare Datensätze | [Loop Colors](https://loopcolors.com/product/loop-400ml/) |
| Loop Asphalt | 2 | [600 ml](https://loopcolors.com/product/asphalt-600-ml/), [400 ml](https://loopcolors.com/product/asphalt-400ml/) |
| Flame Blue | 115 aktuell auslesbare Varianten | [Molotow Shop](https://shop.molotow.com/produkt/flame-blue/) |
| Flame Orange | 133 aktuell auslesbare Varianten | [Molotow Shop](https://shop.molotow.com/produkt/flame-orange/) |
| Ironlak 400 | 101 aktuell im Shop veröffentlichte Swatches | [Ironlak](https://ironlak.com/product/ironlak-acrlyic-spray-paint-400ml/) |
| Double A | 143 | [Double A](https://doublea-spraypaint.com/products/double-a-spraypaint-400ml-143-farben) |

Der Generator unter `scripts/update-official-color-catalog.ps1` lädt diese Seiten, prüft Name und `#RRGGBB`, entfernt Dubletten und erzeugt den Offline-Katalog für Android. Die App braucht danach keine Internetverbindung für die Farberkennung.

## Recherchiert, aber ohne maschinenlesbaren Hersteller-HEX

| Dosenlinie | Veröffentlichte Information | Verhalten in CANVAULT |
|---|---|---|
| MTN Alien | Aktuell Schwarz und Weiß; offizielle technische Farbkarte ohne HEX | Kein geratenes HEX |
| Montana White | Keine aktuelle Varianten-Seite mit digitalen HEX-Feldern gefunden | Kein geratenes HEX |
| Montana Tarblack | Eine schwarze Farbe, aber kein HEX-Feld auf der Produktseite | Kein geratenes HEX |
| Montana Blackout Tarblack | Eine schwarze Farbe, aber kein HEX-Feld auf der Produktseite | Kein geratenes HEX |
| Burner Chrome/Gold/Copper/Black 600 ml | Namen und Artikelnummern; Metallic-Swatches werden als Produktbilder veröffentlicht | Kein aus dem Bild geschätztes HEX |
| Kobra HP / LP | Offizielle Farbkarte als PDF | Kein aus dem PDF geschätztes HEX |
| Sugar Artists Acrylic | Offizielle Farbkarte als PDF | Kein aus dem PDF geschätztes HEX |
| NBQ Fast / Slow | Farbkarte als Bild | Kein aus dem Bild geschätztes HEX |
| Dope Action / Classic | Sortimentsangaben und Bild-Farbkarte | Kein aus dem Bild geschätztes HEX |
| Clash | Download-Farbkarte und Namen/Codes, keine maschinenlesbare HEX-Zuordnung | Kein geratenes HEX |
| Dang Prime / Hi-Flow | Keine belastbare aktuelle Marken-Seite mit Name-Code-HEX-Zuordnung gefunden | Kein geratenes HEX |
| Beat | Keine belastbare aktuelle Marken-Seite mit Name-Code-HEX-Zuordnung gefunden | Kein geratenes HEX |
| Scribo | Keine belastbare aktuelle Marken-Seite mit Name-Code-HEX-Zuordnung gefunden | Kein geratenes HEX |
| Krink K-750 | Produktvarianten vorhanden, aber keine direkte HEX-Zuordnung | Kein geratenes HEX |

Eigene HEX-Werte bleiben als bewusste Nutzereingabe möglich. Sobald Name oder Code exakt einem veröffentlichten Katalogeintrag entspricht, hat der Herstellerwert Vorrang und wird für Vorschau, Inventarbalken und Color Combo verwendet.
