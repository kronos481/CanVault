# CANVAULT Preisgrundlage

Stand: 3. August 2026

## Verwendung beim Hinzufügen

Der Preis im Formular ist ein vorausgefüllter EUR-Einzelpreis inklusive der vom Händler ausgewiesenen Umsatzsteuer, ohne Versand und ohne Mengenrabatte. Die Eingabe bleibt jederzeit überschreibbar.

CANVAULT wählt den Preis in dieser Reihenfolge:

1. Durchschnitt der beobachteten Händlerpreise für exakt dieselbe Dosenlinie und dasselbe Volumen.
2. Auf das Volumen hochgerechneter Richtwert derselben Dosenlinie.
3. Durchschnitt derselben Marke und desselben Dosenformats.
4. Durchschnitt vergleichbarer Dosen mit demselben Volumen.
5. Volumenbasierter Markt-Richtwert, falls für ein seltenes Format keine direkte Beobachtung existiert.

Durchschnittswerte über mehrere Linien werden aus den jeweiligen Linien-Durchschnitten gebildet. Dadurch erhält eine Linie nicht nur deshalb mehr Gewicht, weil für sie mehr Händlerseiten erfasst wurden.

## Abdeckung

Der Offline-Katalog enthält aktuell 62 Preisbeobachtungen für 31 von 38 Dosenlinien. Zu den neu ergänzten Preisreihen gehören:

| Dosenlinie | Volumen | Quellenbeispiele |
|---|---:|---|
| MTN Mega | 600 ml | [Graffitibox](https://graffitibox.de/spruehdosen/mtn-mega/), [BETTERRUN](https://www.betterrun.shop/en/spray-cans/action-spray-cans/mtn-cans-mega-colors-600ml-20-colors), [Spectrum](https://spectrumstore.com/en-eu/collections/montana-mtn-mega) |
| MTN Alien | 250 ml | [Impulse Innovation](https://shop.impulse-innovation.de/Spruehlack-MTN-ALIEN-Black-White-250ml), [Nicolaas Verf](https://www.nicolaasverf.nl/product/mtn-alien-250ml/), [Art & Colour](https://www.artcolour.gr/en/shop/craft-materials/graffiti-en/graffiti-spraycans/spray-paint-montana-alien-250-ml/), [Flow Control](https://flow-control.at/eshop/graffiti/mtn/spruehdosen-mtn/mtn-spruehdosen/mtn-alien-250ml-2/) |
| Montana Tarblack | 500 ml | [Dedicated Store](https://www.dedicated-store.com/startseite/902-montana-tarblack-500ml.html), [Graffitilager](https://graffitilager.de/montana-tarblack-500ml), [BETTERRUN](https://www.betterrun.shop/action-cans/) |
| Montana Blackout Tarblack | 400 ml | [BETTERRUN](https://www.betterrun.shop/spruehdosen/action-cans/montana-cans-blackout-400ml-schwarz), [OVERKILL](https://www.overkillshop.com/products/montana-blackout-400-ml-mon401435), [Graffiti Shop Berlin](https://www.graffitishop-berlin.de/montana-blackout-tarblack-400ml-spruehdose.html) |
| Montana Ultra Wide | 750 ml | [Graffitilager](https://graffitilager.de/en/Spray-cans/Montana-Cans/Ultrawide/), [Ultra Wide Barcelona](https://ultrawide.es/shop/montana-cans/montana-ultra-wide-750ml-spray-graffiti/), [BETTERRUN](https://www.betterrun.shop/en/montana-bombing-cans/) |
| NBQ Slow | 400 ml | [Dedicated Store](https://www.dedicated-store.com/startseite/1886-nbq-slow-400ml.html), [Allcity](https://www.allcity.fr/nbq-slow-400ml.html), [Graffitibox](https://graffitibox.de/spruehdosen/standard/10909/nbq-new-slow-pro-spraypaint-400ml) |
| Dope Action | 600 ml | [BETTERRUN](https://www.betterrun.shop/dope-cans-spruehdosen/) |
| Clash | 400 ml | [Clash Paint](https://www.clashpaint.com/it/spray/clash-400-ml), [Graffitibox](https://graffitibox.de/spruehdosen/standard/158/clash-paint-400ml) |

Alle 62 Einzelbeobachtungen mit Händler, URL, Centbetrag und Erfassungsdatum stehen im Offline-Katalog `VerifiedCatalog.kt`. Im Can-Markt können die Quellen einer direkt erfassten Dosenlinie geöffnet werden.

## Qualitätsgrenze

Preise ändern sich und können regional abweichen. Für Dosenlinien ohne belastbare aktuelle EUR-Händlerdaten zeigt das Hinzufügen-Formular deshalb ausdrücklich einen Marken- oder Format-Richtwert und keinen scheinbar exakten Linienpreis. Eine eigene Eingabe hat immer Vorrang.
