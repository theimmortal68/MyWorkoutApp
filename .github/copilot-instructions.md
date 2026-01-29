# GitHub Copilot Instructions for WorkoutGen

## Project Overview
Android fitness app with AI workout generation using Kotlin + Jetpack Compose.

## Tech Stack
- Kotlin 2.0, Jetpack Compose, Material 3
- Hilt for DI, Room for database
- Clean Architecture (MVVM)
- Coroutines + Flow for async

## Code Generation Guidelines

### For ViewModels
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()
}
```

### For Composables
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    onNavigateBack: () -> Unit,
    viewModel: MyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(topBar = { ... }) { padding -> ... }
}
```

### For Repositories
```kotlin
interface MyRepository {
    fun getItems(): Flow<List<Item>>
    suspend fun saveItem(item: Item)
}
```

## Key Domain Models
- Exercise, Workout, WorkoutExercise
- UserProfile, WorkoutLog
- MuscleGroup, Equipment, Difficulty (enums)

## Important Patterns
- Use StateFlow not LiveData
- Use Flow for streams, suspend for one-shot
- Use @Inject constructor for Hilt
- Add Modifier as first optional param in composables

## File Locations
- Screens: ui/screens/[feature]/
- ViewModels: same folder as screen
- Models: domain/model/Models.kt
- Repositories: domain/repository/ (interface), data/repository/ (impl)
