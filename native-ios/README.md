# CANVAULT for iOS

This folder contains the native SwiftUI version of CANVAULT. It mirrors the Android 1.9.3 feature set while using iOS-native navigation, camera, photo picker, sharing, accessibility, haptics and motion.

## Requirements

- macOS with Xcode 15.4 or newer
- iOS 16 or newer
- A free or paid Apple ID configured in Xcode
- A physical iPhone for real barcode-scanner testing

## Open and run

1. Open `CANVAULT.xcodeproj` in Xcode.
2. Select the CANVAULT target, open **Signing & Capabilities**, and choose your Apple Development Team.
3. If Xcode reports that the bundle identifier is already taken, change `com.kronos481.canvault` to a unique identifier.
4. Connect and trust your iPhone, select it as the run destination, then press **Run**.

The simulator can be used for layout testing, but it cannot reproduce the rear-camera barcode workflow.

## Included features

- Local spray-can inventory with exact fill levels, quantity, price, status and photos
- In-app AVFoundation scanner for EAN, UPC, Code 39/93/128, ITF-14, Data Matrix, PDF417, Aztec and CANVAULT QR
- Verified and locally learned barcode matches
- 1,876 exact manufacturer color swatches exported from the Android source catalog
- Inventory-only and Add Color palette generation with role-aware contrast scoring
- Can Market with EUR, USD, GBP and CHF display plus source details
- Archive, complete history, permanent per-can deletion, statistics and CSV export
- Native sharing, Dynamic Type, VoiceOver labels, reduced-motion support, haptics and subtle generated UI sounds

## Version

- Marketing version: `1.9.3`
- Build number: `23`
- Bundle identifier: `com.kronos481.canvault`
