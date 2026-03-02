# PokeScan - Pokemon Card Collector

An Android app for managing your Pokemon TCG card collection.

## Features

### Core
- **Camera Scanning** – Point your camera at any Pokemon card; ML Kit OCR reads the card name automatically and searches the Pokemon TCG API for matches
- **Manual Search** – Type any card name to search online and add it to your collection
- **Online Prices** – Fetches live market prices from [pokemontcg.io](https://pokemontcg.io) (TCGPlayer data)
- **Offline-first** – All cards are stored locally in a Room database; prices can be refreshed on demand

### Collection Management
- Grid view of all your cards with cover art
- Track **quantity**, **condition** (NM / LP / MP / HP / DMG), **foil**, and **notes** per card
- **Sort by**: Date Added · Name · Value · Set · Type · Rarity
- **Search** your local collection by card name or set

### Stats
- Total card count & unique sets owned
- Total collection value (sum of market prices × quantity)
- Distribution charts: cards by set, type, and rarity

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| DI | Hilt |
| Database | Room |
| Networking | Retrofit + OkHttp + Gson |
| Camera | CameraX |
| OCR | ML Kit Text Recognition |
| Images | Coil |
| State | ViewModel + StateFlow |

## API

Card data and prices are sourced from the [Pokemon TCG API](https://pokemontcg.io/).

The free tier allows 1 000 requests/day. For higher limits, get a free API key at
[pokemontcg.io](https://pokemontcg.io) and set it in `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "POKEMON_TCG_API_KEY", "\"YOUR_KEY_HERE\"")
```

## Building

```bash
./gradlew assembleDebug
```

Minimum SDK: 26 (Android 8.0)
Target SDK: 35 (Android 15)
