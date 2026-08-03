# Native Android build

CANVAULT ist eine native Kotlin/Jetpack-Compose-App unter `native-android/`. Paket-ID: `com.canvault.app`; minSdk 26; target/compile SDK 36.

## Einfachste Installation

`INSTALL_CANVAULT.cmd` doppelklicken oder aus PowerShell starten:

```powershell
powershell -ExecutionPolicy Bypass -File "C:\Users\janni\Documents\Invenfitti\INSTALL_CANVAULT.ps1"
```

Das Installationsskript:

1. lädt und verifiziert Googles Android Command-Line Tools,
2. installiert Platform Tools, Platform 36 und Build Tools 36.0.0,
3. baut eine signierte Debug-APK,
4. speichert sie als `C:\Users\janni\Downloads\CANVAULT.apk`,
5. installiert und startet sie über ADB auf dem freigegebenen USB-Gerät.

## Manuell bauen

```powershell
cd C:\Users\janni\Documents\Invenfitti\native-android
$env:ANDROID_HOME="$env:LOCALAPPDATA\CANVAULT\AndroidSdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Ausgabe: `native-android/app/build/outputs/apk/debug/app-debug.apk`.

## Xiaomi/HyperOS

Bei `INSTALL_FAILED_USER_RESTRICTED` in den Entwickleroptionen `Install via USB` aktivieren und die Sicherheitsabfrage auf dem entsperrten Handy bestätigen. Alternativ liegt die bereits übertragene APK unter `Download/CANVAULT.apk` und kann im Xiaomi-Dateimanager angetippt werden.

Die Debug-Signatur ist für direkte lokale Installation gedacht. Für eine spätere Play-Store-Veröffentlichung wird ein eigener, sicher verwahrter Release-Key und ein signiertes Android App Bundle benötigt.
