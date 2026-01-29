# CLAUDE.md - WorkoutGen AI Project Guide

This file provides guidance for Claude Code CLI when working on this project.

## Project Overview

**WorkoutGen** is an Android fitness application that uses AI to generate personalized workout routines. The app features:
- AI-powered workout generation based on user goals, equipment, and history
- 870+ exercise library with comprehensive filtering (muscle, equipment, difficulty, type)
- Multiple progression systems (linear, double progression, wave, DUP)
- Built-in training programs (StrongLifts, PPL, GZCLP, etc.)
- Workout logging with progressive overload tracking
- **Smart exercise swapping** with recommendations based on swap reason
- **Real-time workout adjustments** when failing to meet prescribed reps
- Analytics and personal record tracking

## Tech Stack

- **Language**: Kotlin 2.0.21
- **UI**: Jetpack Compose with Material 3
- **Architecture**: Clean Architecture (MVVM)
- **DI**: Hilt 2.53.1
- **Database**: Room 2.6.1
- **Navigation**: Navigation Compose 2.8.5
- **Async**: Kotlin Coroutines & Flow
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35

## Project Structure

```
app/src/main/java/com/workoutgen/
├── data/                    # Data layer
│   ├── local/              
│   │   ├── dao/            # Room DAOs
│   │   ├── entity/         # Room entities
│   │   ├── ExerciseLibrary.kt    # 870+ exercises
│   │   ├── BuiltInPrograms.kt    # Training programs
│   │   └── WorkoutDatabase.kt
│   └── repository/         # Repository implementations
├── domain/                  # Domain layer
│   ├── model/              # Domain models & enums
│   ├── repository/         # Repository interfaces
│   └── usecase/            # Business logic
│       ├── AIWorkoutGenerator.kt      # Core AI logic
│       ├── ExerciseSwapManager.kt     # Smart exercise swapping
│       ├── WorkoutAdjustmentEngine.kt # Real-time adjustments
│       └── GenerateWorkoutUseCase.kt
├── ui/                      # Presentation layer
│   ├── navigation/         # Nav graph
│   ├── screens/            # Feature screens
│   │   ├── analytics/      # Stats & progress
│   │   ├── exercise/       # Exercise library
│   │   ├── history/        # Workout logs
│   │   ├── home/           # Dashboard
│   │   ├── profile/        # User settings
│   │   ├── programs/       # Training programs
│   │   └── workout/        # Generation & active workout
│   └── theme/              # Material theme
├── di/                      # Hilt modules
├── MainActivity.kt
└── WorkoutGenApp.kt        # Application class
```

## Key Files to Understand

### Core AI Logic
- `domain/usecase/AIWorkoutGenerator.kt` - The brain of workout generation
  - Selects target muscles based on recovery and split
  - Calculates optimal exercises based on equipment
  - Auto-adjusts weights using progression algorithms
  - Generates warmup/cooldown routines

### Domain Models
- `domain/model/Models.kt` - All data classes and enums including:
  - `Exercise`, `Workout`, `WorkoutExercise`
  - `UserProfile`, `WorkoutLog`, `ExerciseLog`
  - `TrainingProgram`, `ProgramDay`, `ProgressionRules`
  - Utility calculators: `OneRepMaxCalculator`, `WarmupCalculator`, `VolumeCalculator`

### Exercise Data
- `data/local/ExerciseLibrary.kt` - 870+ exercises with:
  - Primary/secondary muscles
  - Equipment requirements
  - Difficulty levels
  - Step-by-step instructions
  - Image URLs

### UI Screens
- `ui/screens/workout/AIGenerateWorkoutScreen.kt` - Main generation UI
- `ui/screens/workout/ActiveWorkoutScreen.kt` - In-progress workout tracking
- `ui/screens/programs/ProgramsScreen.kt` - Browse training programs

## Common Development Tasks

### Adding a New Exercise
```kotlin
// In ExerciseLibrary.kt, add to the exercises list:
Exercise(
    id = "unique_id",
    name = "Exercise Name",
    description = "Description",
    instructions = listOf("Step 1", "Step 2"),
    primaryMuscles = listOf(MuscleGroup.CHEST),
    secondaryMuscles = listOf(MuscleGroup.TRICEPS),
    equipmentRequired = listOf(Equipment.BARBELL),
    type = ExerciseType.COMPOUND,
    difficulty = Difficulty.INTERMEDIATE,
    imageUrl = "https://..."
)
```

### Adding a New Training Program
```kotlin
// In BuiltInPrograms.kt:
val myProgram = TrainingProgram(
    id = "my_program",
    name = "My Program",
    description = "Description",
    author = "Author Name",
    difficulty = Difficulty.INTERMEDIATE,
    goals = listOf(FitnessGoal.BUILD_MUSCLE),
    durationWeeks = 12,
    daysPerWeek = 4,
    split = WorkoutSplit.UPPER_LOWER,
    periodization = PeriodizationType.LINEAR,
    programDays = listOf(/* ProgramDay objects */),
    progressionRules = ProgressionRules(),
    requiredEquipment = setOf(Equipment.BARBELL, Equipment.DUMBBELLS)
)
```

### Modifying Workout Generation Logic
The AI generation flow in `AIWorkoutGenerator.kt`:
1. `generateWorkout()` - Entry point
2. `selectTargetMuscles()` - Picks muscles based on recovery/split
3. `selectExercises()` - Filters by equipment, difficulty
4. `createWorkoutExercise()` - Sets reps/weight based on history
5. `calculateSuggestedWeight()` - Applies progression algorithm

### Adding a New Screen
1. Create screen composable in `ui/screens/[feature]/`
2. Create ViewModel with `@HiltViewModel`
3. Add route to `Screen` sealed class in `Navigation.kt`
4. Add composable to `NavHost`

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run Android tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Check for dependency updates
./gradlew dependencyUpdates
```

## Code Style Guidelines

- Use Kotlin idioms (scope functions, null safety, data classes)
- Follow Material 3 design guidelines
- ViewModels expose `StateFlow<UiState>` to composables
- Use `@Composable` preview functions for UI components
- Repository functions return `Flow<T>` for observable data
- Use `suspend` functions for one-shot operations
- Prefer composition over inheritance

## Testing Strategy

- **Unit tests**: Domain logic, calculators, use cases
- **Integration tests**: Repository + Database
- **UI tests**: Compose testing for screens
- **Screenshot tests**: Visual regression with Paparazzi

## Known Issues & TODOs

1. [ ] Exercise images need caching with Coil
2. [ ] Implement rest timer with notifications
3. [ ] Add workout sharing/export feature
4. [ ] Implement cloud backup
5. [ ] Add exercise video playback
6. [ ] Implement plate calculator for barbell exercises
7. [ ] Add social features (leaderboards, challenges)

## Environment Setup

1. Android Studio Hedgehog or newer
2. JDK 17+
3. Android SDK 35
4. Enable Kotlin 2.0 in Android Studio settings

## Useful Resources

- [Compose Documentation](https://developer.android.com/jetpack/compose)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Material 3 Components](https://m3.material.io/components)
