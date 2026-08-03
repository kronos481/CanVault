# CANVAULT

CANVAULT is an Android app for keeping track of spray paint cans. It shows what is currently available, how much paint is left, which colors are in the collection, and which cans have already been used or archived.

The app is made for people who want one clear place for their cans instead of relying on notes, photos, or memory. The maintained Android project is located in [`native-android`](native-android/).

## Features

### Inventory

- Add cans manually or with the built-in barcode scanner.
- Store the brand, product line, color name, color code, volume, price, quantity, fill level, and an optional photo.
- Search the inventory and filter it by status.
- Sort cans by name, brand, fill level, date, or color.
- See the real can color in previews and fill-level bars when an exact color value is available.

### Barcode scanner

The scanner runs inside the app and supports EAN-13, EAN-8, UPC-A, UPC-E, Code 128, Code 39, Code 93, ITF, and Codabar.

Known products can fill in their product details automatically. If a barcode is not known yet, it can be linked to a can after the first scan and recognized again later. CANVAULT QR codes can also be created and scanned for sharing can records.

### Color Combo

Color Combo builds palettes from the exact colors in the inventory and assigns them to practical graffiti roles such as:

- background
- outline and second outline
- fill, secondary fill, and fill fade
- inline or highlight

“Inventory Only” uses cans that are already available. “Add Color” can suggest missing colors from the product catalog. Every palette includes a 0–100% harmony score based on color relationships, visual separation, edge contrast, and the amount of paint available. Difficult color sets are still shown as best-effort ideas instead of being hidden.

### Can Market

The Can Market is a browsable catalog of supported spray can lines. It includes product images, brand information, reference prices, and selectable currencies such as EUR and USD.

### Storage and history

- Archive cans without losing their information.
- Restore archived cans or permanently delete individual entries.
- View every current and archived can in the full history.
- Review collection statistics, spending, used volume, remaining paint, favorite brands, and monthly activity.
- Export the collection as a CSV file through Android Share.

### Local-first

Inventory data and personal can photos are stored on the device. Barcode images are processed locally and camera frames are not uploaded. A shared verified barcode catalog can be enabled separately, but the app remains usable without an account.

### Android experience

CANVAULT uses a native Material 3 interface with dark styling, small animations, touch feedback, and short interface sounds. Product photos and brand logos are included for supported lines, with a neutral can illustration used as a fallback.

## Install on Android

The simplest option is to download the latest APK from the repository's Releases page and open it on the phone. Android may ask for permission to install apps from the browser or file manager used to open the APK.

To build and install CANVAULT directly from a Windows PC:

1. Install JDK 17 or newer.
2. Enable Developer Options and USB debugging on the Android phone.
3. Connect and unlock the phone, then approve the USB debugging prompt.
4. From the repository root, double-click `INSTALL_CANVAULT.cmd` or run:

```powershell
powershell -ExecutionPolicy Bypass -File ".\INSTALL_CANVAULT.ps1"
```

The first run downloads Google's Android command-line tools, builds the APK, installs it, and starts the app. Existing CANVAULT data is preserved when the app is updated.

## Build and test

```powershell
cd .\native-android
$env:ANDROID_HOME="$env:LOCALAPPDATA\CANVAULT\AndroidSdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat testDebugUnitTest lintDebug assembleRelease
```

The release APK is created at:

```text
native-android/app/build/outputs/apk/release/app-release.apk
```

## Tech

- Kotlin and Jetpack Compose
- Material 3
- Preferences DataStore and Kotlin Serialization
- CameraX and Google ML Kit Barcode Scanning
- ZXing for QR code generation
- Coil for local photos
- JUnit and Android Lint

CANVAULT currently targets Android only. The application ID is `com.canvault.app`.
