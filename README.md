# StockLedger

Android app for managing shop stock and ledgers, built with Kotlin, Jetpack Compose and Firebase.

## Features

- **Inventory** — products with stock levels, reorder thresholds, suppliers and barcodes
- **Smart reordering** — suggested order quantities when stock runs low
- **Customers** — credit limits, balances, purchase and payment history
- **Invoices & payments** — create invoices, track partial and full payments and balances due
- **QR code scanning** — scan barcodes and QR codes with the camera
- **Dashboard** — stock, sales and ledger overview at a glance
- **Sign-in with Firebase Auth** — every user's data lives in their own Cloud Firestore space (`/users/{userId}/…`)

## Tech stack

- Kotlin, Jetpack Compose (Material 3)
- Firebase: Cloud Firestore, Firebase Auth (BOM 34.17.0)
- CameraX, ZXing (QR codes), Coroutines, Lifecycle ViewModel
- Gradle with Kotlin DSL and a version catalog (`gradle/libs.versions.toml`)

## Requirements

- Android Studio with JDK 11
- A Firebase project with Firestore and Authentication enabled
- `minSdk 24`, `targetSdk 36`, current version 1.0

## Setup

1. Clone the repo.
2. Add your `google-services.json` from your Firebase project into `app/` — the file is intentionally not in the repo.
3. Optional: copy `.env.example` to `.env` and add your Gemini API key.
4. Open in Android Studio and run, or build from the command line:

```bash
./gradlew assembleDebug
```

## Project structure

- `app/src/main/java/com/example/` — `MainActivity`, data models, repository, UI screens (auth, dashboard, inventory, customers, invoices, reorder), view model
- `firebase-blueprint.json` — Cloud Firestore data model (products, customers, invoices, payments, reorders)
- `firestore.rules` — Firestore security rules

## License

MIT
