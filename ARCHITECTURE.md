# ARCHITECTURE.md - WorkoutGen System Architecture

## Overview

WorkoutGen follows **Clean Architecture** principles with three distinct layers, ensuring separation of concerns, testability, and maintainability.

```
┌────────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   Screens    │  │  ViewModels  │  │    Theme     │         │
│  │  (Compose)   │  │ (StateFlow)  │  │  (Material3) │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
├────────────────────────────────────────────────────────────────┤
│                        DOMAIN LAYER                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │    Models    │  │  Use Cases   │  │ Repository   │         │
│  │ (Data Class) │  │  (Business)  │  │ (Interfaces) │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
├────────────────────────────────────────────────────────────────┤
│                         DATA LAYER                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   Entities   │  │     DAOs     │  │ Repository   │         │
│  │   (Room)     │  │   (Room)     │  │   (Impl)     │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└────────────────────────────────────────────────────────────────┘
```

## Layer Details

### Presentation Layer (`ui/`)

**Responsibility**: Display data and handle user interactions

**Components**:
- **Screens**: Jetpack Compose UI components
- **ViewModels**: State holders with business logic orchestration
- **Navigation**: Single NavHost with type-safe routes
- **Theme**: Material 3 theming with dynamic colors

**Data Flow**:
```
User Action → ViewModel.function() → Update StateFlow → Recompose UI
```

**State Management**:
```kotlin
data class UiState(
    val isLoading: Boolean = false,
    val data: List<T> = emptyList(),
    val error: String? = null
)

class ViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
}
```

### Domain Layer (`domain/`)

**Responsibility**: Business logic, independent of frameworks

**Components**:
- **Models**: Pure Kotlin data classes representing business entities
- **Use Cases**: Single-responsibility business operations
- **Repository Interfaces**: Contracts for data access

**Key Models**:
```
Exercise          - Single exercise definition
Workout           - Collection of exercises with parameters
WorkoutExercise   - Exercise with sets/reps/weight for a workout
UserProfile       - User preferences and stats
WorkoutLog        - Completed workout record
TrainingProgram   - Structured multi-week program
```

**AI Workout Generator Flow**:
```
1. Receive AIWorkoutParams
2. Load user profile
3. Calculate muscle recovery status
4. Select target muscles (based on split/recovery)
5. Filter exercises (equipment/difficulty)
6. Calculate sets/reps/weight (based on history + progression)
7. Generate warmup/cooldown
8. Return GeneratedWorkout
```

### Data Layer (`data/`)

**Responsibility**: Data persistence and retrieval

**Components**:
- **Entities**: Room database tables
- **DAOs**: Database access objects
- **Repository Implementations**: Concrete data operations

**Database Schema**:
```
┌─────────────────┐     ┌─────────────────┐
│    exercises    │     │    workouts     │
├─────────────────┤     ├─────────────────┤
│ id (PK)         │     │ id (PK)         │
│ name            │     │ name            │
│ description     │     │ description     │
│ instructions    │     │ exercises (JSON)│
│ primaryMuscles  │     │ targetMuscles   │
│ secondaryMuscles│     │ difficulty      │
│ equipment       │     │ goal            │
│ type            │     │ createdAt       │
│ difficulty      │     │ isFavorite      │
│ imageUrl        │     └─────────────────┘
└─────────────────┘
        │
        │ referenced by
        ▼
┌─────────────────┐     ┌─────────────────┐
│  workout_logs   │     │  user_profile   │
├─────────────────┤     ├─────────────────┤
│ id (PK)         │     │ id (PK)         │
│ workoutId (FK)  │     │ name            │
│ workoutName     │     │ experienceLevel │
│ startTime       │     │ fitnessGoals    │
│ endTime         │     │ equipment       │
│ exerciseLogs    │     │ workoutDuration │
│ totalVolume     │     │ daysPerWeek     │
│ rating          │     │ preferredSplit  │
│ notes           │     │ bodyWeight      │
└─────────────────┘     │ maxes (JSON)    │
                        └─────────────────┘
```

## Dependency Injection

Using **Hilt** for dependency injection:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WorkoutDatabase
    
    @Provides
    fun provideExerciseDao(db: WorkoutDatabase): ExerciseDao
    
    @Provides
    @Singleton
    fun provideExerciseRepository(dao: ExerciseDao): ExerciseRepository
}
```

**Injection Graph**:
```
Application
    └── WorkoutDatabase (Singleton)
            ├── ExerciseDao
            │       └── ExerciseRepository
            │               └── AIWorkoutGenerator
            │                       └── AIGenerateViewModel
            ├── WorkoutDao
            │       └── WorkoutRepository
            └── WorkoutLogDao
                    └── WorkoutLogRepository
```

## Navigation Architecture

Using **Navigation Compose** with type-safe routes:

```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object GenerateWorkout : Screen("generate_workout")
    object WorkoutDetail : Screen("workout/{workoutId}")
    object ActiveWorkout : Screen("active_workout/{workoutId}")
    object ExerciseLibrary : Screen("exercises")
    object ExerciseDetail : Screen("exercise/{exerciseId}")
    object History : Screen("history")
    object Profile : Screen("profile")
    object Programs : Screen("programs")
    object Analytics : Screen("analytics")
}
```

**Navigation Flow**:
```
Home ─────────────────────────────────────────────┐
  │                                               │
  ├──► GenerateWorkout ──► WorkoutDetail ──► ActiveWorkout ──► Home
  │                              │
  ├──► ExerciseLibrary ──► ExerciseDetail
  │
  ├──► Programs ──► ProgramDetail
  │
  ├──► History ──► LogDetail
  │
  ├──► Analytics
  │
  └──► Profile
```

## AI Workout Generation Algorithm

### Muscle Selection Logic

```
Input: UserProfile, RecentWorkoutLogs, WorkoutSplit

1. Calculate recovery status for each muscle group:
   - RECOVERED: >72h since last trained (large muscles) or >48h (small)
   - RECOVERING: 50-100% of recovery time
   - FATIGUED: <50% of recovery time

2. Select muscles based on split:
   - PUSH_PULL_LEGS: Rotate through Push→Pull→Legs
   - UPPER_LOWER: Alternate Upper↔Lower
   - FULL_BODY: 1 push + 1 pull + 1 legs + core
   - BRO_SPLIT: One muscle group per day

3. Prioritize recovered muscles, warn if training fatigued muscles
```

### Exercise Selection Logic

```
Input: TargetMuscles, AvailableEquipment, ExerciseCount, UserDifficulty

1. Filter exercises by:
   - Equipment availability
   - Difficulty ≤ User level (beginners get beginner/intermediate)
   - Not in user's excluded list

2. Sort by priority:
   - Compound movements first (if prioritizeCompound=true)
   - Primary muscle match
   - Difficulty match

3. Select N exercises ensuring variety:
   - At least 1 compound per major muscle
   - Mix of movement patterns
```

### Weight Progression Logic

```
Input: ExerciseHistory, ProgressionStyle, TargetReps

LINEAR:
  weight = lastWeight + increment

DOUBLE_PROGRESSION:
  if lastReps >= targetReps + 2:
    weight = lastWeight + increment
  else:
    weight = lastWeight (aim for more reps)

WAVE:
  cyclePosition = sessionCount % 3
  weight = lastWeight × [1.05, 0.9, 1.0][cyclePosition]

DAILY_UNDULATING:
  if targetReps ≤ 5: weight × 1.05  (strength)
  if targetReps ≤ 10: weight        (hypertrophy)
  else: weight × 0.85               (endurance)
```

## Error Handling Strategy

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// In ViewModel
fun loadData() {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        
        when (val result = repository.getData()) {
            is Result.Success -> _uiState.update { 
                it.copy(data = result.data, isLoading = false) 
            }
            is Result.Error -> _uiState.update { 
                it.copy(error = result.message, isLoading = false) 
            }
        }
    }
}
```

## Caching Strategy

1. **Exercise Library**: Pre-loaded in memory at app start (ExerciseLibrary object)
2. **User Profile**: Cached in DataStore, synced to Room
3. **Workout History**: Room with Flow for reactive updates
4. **Images**: Coil with disk cache (to be implemented)

## Testing Architecture

```
┌─────────────────────────────────────────────────┐
│              Integration Tests                   │
│  (Repository + Database + Real Data)            │
├─────────────────────────────────────────────────┤
│                 Unit Tests                       │
│  (Use Cases, ViewModels with Mocked Repos)      │
├─────────────────────────────────────────────────┤
│                  UI Tests                        │
│  (Compose Testing, Screenshot Tests)            │
└─────────────────────────────────────────────────┘
```

## Future Architecture Considerations

1. **Remote Sync**: Add Retrofit layer for cloud backup
2. **Offline-First**: Implement sync queue for offline changes
3. **Multi-Module**: Split into :core, :feature-*, :data modules
4. **Wear OS**: Add :wear module for watch companion
5. **Widget**: Add Glance widget for quick workout start
