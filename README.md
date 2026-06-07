# Voice Call Assistant

A native Android Kotlin app that lets you call any phone contact using your voice.

## Features

- **Offline voice recognition** — Uses Android's on-device speech recogniser with `EXTRA_PREFER_OFFLINE`
- **Read phone contacts** — Searches your local contacts by name
- **Voice commands** — Say "Call Mom", "Call Dad", "Call Rahul", etc.
- **Smart contact matching** — Exact → prefix → fuzzy search fallback
- **Quick command chips** — Tap pre-set commands without speaking
- **Contact search bar** — Manual search with live filtering
- **Foreground service** — `VoiceListenerService` keeps the session alive while in the background
- **Modern Material 3 UI** — Clean card-based design with pulsing mic animation

## Building in Android Studio

### Requirements

| Tool | Version |
|------|---------|
| Android Studio | Ladybug (2024.2) or newer |
| JDK | 17+ |
| Android Gradle Plugin | 8.7.3 |
| Compile SDK | 35 (Android 15) |
| Min SDK | 26 (Android 8.0) |

### Steps

1. **Open the project** — File → Open → select the `voice-call-assistant` folder
2. **Sync Gradle** — Android Studio will download all dependencies automatically
3. **Run or build APK**:
   - Press ▶ to run on a connected device or emulator
   - Build → Build Bundle(s) / APK(s) → Build APK(s) to get an installable `.apk`

> **Tip for offline voice:** On the device, go to **Settings → General Management → Language → Text-to-speech** and download the offline speech pack for your language. This ensures voice recognition works without internet.

## Project Structure

```
app/src/main/
├── java/com/voicecallassistant/
│   ├── MainActivity.kt          ← UI, permissions, speech recogniser
│   ├── MainViewModel.kt         ← State management (ViewModel + StateFlow)
│   ├── ContactsHelper.kt        ← Reads & searches device contacts
│   ├── VoiceCommandParser.kt    ← Parses "Call Mom" → name extraction
│   ├── CallHelper.kt            ← Places phone calls via Intent.ACTION_CALL
│   └── VoiceListenerService.kt  ← Foreground service for background listening
├── res/
│   ├── layout/activity_main.xml ← Material 3 UI layout
│   ├── values/colors.xml
│   ├── values/strings.xml
│   ├── values/themes.xml        ← Material3.Light theme
│   └── drawable/                ← Mic & search vector icons
└── AndroidManifest.xml          ← Permissions: RECORD_AUDIO, READ_CONTACTS, CALL_PHONE
```

## Permissions

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Voice recognition |
| `READ_CONTACTS` | Search your contact list |
| `CALL_PHONE` | Place calls without user confirmation dialog |
| `FOREGROUND_SERVICE` | Background voice listening service |

## Supported Voice Commands

| You say | App does |
|---|---|
| "Call Mom" | Finds "Mom" in contacts and calls |
| "Call Rahul Sharma" | Finds best matching contact named Rahul Sharma |
| "Dial office" | Finds a contact named "office" |
| "Phone Dad" | Finds "Dad" and calls |

## Customisation

- **Add more contacts to Quick Chips:** Edit `setupQuickCommandChips()` in `MainActivity.kt`
- **Change theme colour:** Edit `primary` in `res/values/colors.xml`
- **Adjust fuzzy matching:** Edit `findContact()` in `ContactsHelper.kt`
- **Change language:** Set `EXTRA_LANGUAGE` in `startListening()` in `MainActivity.kt`
