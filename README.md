# WorkoutGen AI - Intelligent Workout Generator

<p align="center">
  <strong>100% AI-powered personalized workouts that learn from your performance</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#getting-started">Getting Started</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#ai-engine">AI Engine</a> •
  <a href="#contributing">Contributing</a>
</p>

---

## Features

### 🤖 Pure AI Workout Generation
No templates, no pre-built programs - every workout is generated specifically for YOU based on:
- Your training history and performance data
- Recovery status of each muscle group
- Available equipment
- Time constraints
- Personal goals and preferences
- What worked (and didn't work) in past sessions

### 📊 Intelligent Learning System
The app learns from every workout:
- Tracks which exercises you swap out and why
- Monitors rep completion rates and adjusts accordingly
- Identifies your weak points and strong areas
- Learns your recovery patterns
- Adapts progression speed to your actual progress

### ⚡ Real-Time Workout Adjustments
During your workout, the AI adjusts on the fly:
- **Failed a set?** AI recommends weight reduction or rep adjustment
- **Exercise unavailable?** Smart swap suggestions based on your reason
- **Feeling strong?** AI notes it for future sessions
- **Pain/discomfort?** Immediate safer alternatives

### 📚 Comprehensive Exercise Library
- **870+ exercises** with detailed instructions
- Filter by muscle, equipment, difficulty, type
- Bodyweight-only option
- Multi-criteria advanced filtering

### 🔄 Smart Exercise Swapping
When you need to swap an exercise, the AI:
- Asks WHY (equipment, difficulty, injury, preference, etc.)
- Scores replacements based on muscle match, equipment, difficulty
- Learns from your swaps to improve future workouts
- Tracks patterns to avoid problematic exercises

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17+
- Android SDK 35
- Android device or emulator (API 26+)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/workout-generator.git
   cd workout-generator
   ```

2. **Open in Android Studio**
   - File → Open → Select project folder
   - Wait for Gradle sync to complete

3. **Run the app**
   - Select device/emulator from toolbar
   - Click Run (▶) or press Shift+F10

4. **First launch**
   - The app will seed 870+ exercises (takes ~5 seconds)
   - Set up your profile in Settings
   - Configure your available equipment
   - Start generating workouts!

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Full check (lint + tests)
./gradlew check
```

---

## Architecture

WorkoutGen follows **Clean Architecture** with three layers:

```
┌─────────────────────────────────────────┐
│           Presentation Layer            │
│   (Compose Screens + ViewModels)        │
├─────────────────────────────────────────┤
│             Domain Layer                │
│   (Use Cases + Models + Interfaces)     │
├─────────────────────────────────────────┤
│              Data Layer                 │
│   (Room DB + Repository Impl)           │
└─────────────────────────────────────────┘
```

### Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Database | Room |
| Async | Coroutines + Flow |
| Navigation | Navigation Compose |

### Project Structure

```
app/src/main/java/com/workoutgen/
├── data/                    # Data layer
│   ├── local/              
│   │   ├── dao/            # Room DAOs
│   │   ├── entity/         # Room entities
│   │   ├── ExerciseLibrary.kt
│   │   └── BuiltInPrograms.kt
│   └── repository/         # Repository implementations
├── domain/                  # Domain layer
│   ├── model/              # Business models
│   ├── repository/         # Repository interfaces
│   └── usecase/            # Business logic
│       └── AIWorkoutGenerator.kt
├── ui/                      # Presentation layer
│   ├── navigation/
│   ├── screens/
│   └── theme/
└── di/                      # Dependency injection
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed documentation.

---

## AI Engine

The AI workout generator (`AIWorkoutGenerator.kt`) uses several algorithms:

### Muscle Selection
Based on split type and recovery status:
- Calculates time since each muscle was trained
- Considers muscle size (larger muscles need more recovery)
- Rotates through split pattern (PPL, Upper/Lower, etc.)

### Exercise Selection
Filters and prioritizes exercises:
1. Filter by available equipment
2. Filter by user difficulty level
3. Prioritize compound movements
4. Ensure variety (different movement patterns)

### Weight Progression
Multiple progression algorithms:

| Style | Logic |
|-------|-------|
| Linear | Add weight each session |
| Double Progression | Increase reps → then weight |
| Wave | Heavy → Light → Medium cycling |
| DUP | Vary rep ranges daily |

### Volume Scaling
Adjusts exercise count based on duration:
- 30 min → 3-4 exercises
- 45 min → 5-6 exercises
- 60 min → 7-8 exercises
- 90 min → 9-10 exercises

---

## Documentation

| Document | Description |
|----------|-------------|
| [CLAUDE.md](CLAUDE.md) | Guide for Claude Code CLI |
| [AGENTS.md](AGENTS.md) | Instructions for AI coding agents |
| [ARCHITECTURE.md](ARCHITECTURE.md) | System architecture details |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Contribution guidelines |

---

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Development workflow
- Code style guidelines
- Testing requirements
- Pull request process

---

## Roadmap

- [ ] Rest timer with notifications
- [ ] Exercise video playback
- [ ] Cloud backup/sync
- [ ] Plate calculator
- [ ] Social features
- [ ] Wear OS companion app
- [ ] Home screen widget

---

## Acknowledgments

- Exercise data from [free-exercise-db](https://github.com/yuhonas/free-exercise-db)
- Inspired by Fitbod, Strong, and Boostcamp
- Built with [Jetpack Compose](https://developer.android.com/jetpack/compose)

---

<p align="center">
  Made with 💪 for fitness enthusiasts
</p>
