# CANVAULT design system

## Direction

Dark-first industrial precision: quiet graphite surfaces, sharp information hierarchy, restrained mint signal color and temporary user-entered can color. Urban character comes from material contrast and rhythm, not graffiti fonts or decorative spray textures.

## Tokens

- Background `#090B0E`; raised background `#0D1014`
- Surface `#14181E`; raised surface `#1A2028`
- Primary text `#F5F7FA`; muted text `#AEB8C4`
- Primary accent `#58E4C2`; danger `#FF7B7B`; warning `#F5BF60`
- Spacing follows 4/8-point increments; section rhythm 16/24/32/48
- Radius 8/14/20; no arbitrary per-screen radii
- Touch targets: 44 pt iOS, 48 dp elsewhere
- Motion: 150/220/300 ms, meaningful and reduced-motion compatible

## Interaction

- Five labeled top-level tabs maximum.
- One primary call to action per screen.
- Every pressable has visible pressed state, semantic role and label.
- Color is never the only signal: names, codes, status text and progress values remain visible.
- Archive is confirmed, reversible and never presented as permanent deletion.
- Form labels remain visible; errors sit next to their field.

## Can card

A colored top bar anchors the card, followed by a quiet studio window with the user-provided product cutout for the selected catalog line. A normalized transparent 512 x 768 canvas keeps every can visually stable; generic project-owned illustrations are used only when no mapped product asset exists. User-supplied photos override catalog artwork. The official-shaped brand asset appears in a deterministic all-white variant without changing proportions, always on a graphite badge so contrast remains stable in light and dark themes. The card always shows line, color name, optional code, fill value and textual status. User-entered HEX is described as an approximation.

## Scanner

- Full-screen rear camera with an explicit torch control and two clearly separated modes.
- Product barcode is the default mode and uses a wide guidance frame for EAN, UPC and common linear formats; CANVAULT QR has its own square mode.
- A barcode must be decoded consistently across multiple camera frames before the app accepts it.
- Every accepted scan pauses the camera and opens a confirmation card; saving never happens during detection.
- Unknown product barcodes enter a real local learning flow. Once the completed product is saved, future scans restore its product data.
- Scanner motion uses one transform-only line animation and freezes when the operating system requests reduced motion.

## Motion

- Press feedback: 150 ms scale-down, 220 ms recovery; bounds never change.
- Screen/card entrances: 220–260 ms opacity/vertical motion.
- Snackbar exit is faster than entry at 150 ms.
- Every Reanimated entrance and continuous scanner animation respects the system reduced-motion preference.

## Avoid

No emojis as navigation icons, low-contrast gray-on-gray text, third-party brand-logo invention, decorative motion, glass blur as decoration, tiny icon hit areas, color-only states or unverified manufacturer imagery.
