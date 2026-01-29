# AGENTS.md - AI Coding Agent Instructions

This document provides instructions for AI coding agents (Claude Code, Codex, Gemini, Cursor, Copilot, etc.) working on the WorkoutGen Android project.

---

## Quick Context

- **Project**: WorkoutGen - AI-powered workout generator for Android
- **Language**: Kotlin with Jetpack Compose
- **Architecture**: Clean Architecture with MVVM
- **Key Dependencies**: Hilt (DI), Room (DB), Coroutines/Flow (async)

---

## Before Making Changes

### 1. Understand the Architecture

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                          │
│  (Compose Screens + ViewModels)                     │
├─────────────────────────────────────────────────────┤
│                  Domain Layer                        │
│  (Use Cases + Models + Repository Interfaces)       │
├─────────────────────────────────────────────────────┤
│                   Data Layer                         │
│  (Room DB + Repository Implementations)             │
└─────────────────────────────────────────────────────┘
```

**Data flows UP through interfaces. Dependencies point DOWN.**

### 2. Locate Relevant Files

| Task | Primary Files |
|------|---------------|
| Add exercise | `data/local/ExerciseLibrary.kt` |
| Modify workout generation | `domain/usecase/AIWorkoutGenerator.kt` |
| Add training program | `data/local/BuiltInPrograms.kt` |
| Modify data models | `domain/model/Models.kt` |
| Add new screen | `ui/screens/[feature]/`, `ui/navigation/Navigation.kt` |
| Modify database | `data/local/entity/Entities.kt`, `data/local/dao/Daos.kt` |
| Change DI | `di/AppModule.kt` |

### 3. Check Existing Patterns

Before implementing, look at similar existing code:
- For new screens → check `ui/screens/home/HomeScreen.kt`
- For new ViewModels → check `ui/screens/workout/AIGenerateViewModel`
- For new use cases → check `domain/usecase/AIWorkoutGenerator.kt`
- For new entities → check `data/local/entity/Entities.kt`

---

## Code Patterns to Follow

### ViewModel Pattern
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    fun doAction() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val result = repository.getData()
                _uiState.update { it.copy(data = result, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}

data class MyUiState(
    val isLoading: Boolean = false,
    val data: List<Item> = emptyList(),
    val error: String? = null
)
```

### Compose Screen Pattern
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    onNavigateBack: () -> Unit,
    onItemClick: (String) -> Unit,
    viewModel: MyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Title") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        // Content
    }
}
```

### Repository Pattern
```kotlin
// Interface in domain/repository/
interface MyRepository {
    fun getItems(): Flow<List<Item>>
    suspend fun saveItem(item: Item)
}

// Implementation in data/repository/
class MyRepositoryImpl @Inject constructor(
    private val dao: MyDao
) : MyRepository {
    override fun getItems(): Flow<List<Item>> = 
        dao.getAll().map { entities -> entities.map { it.toDomain() } }
    
    override suspend fun saveItem(item: Item) = 
        dao.insert(item.toEntity())
}
```

---

## DO's and DON'Ts

### ✅ DO

- Use `StateFlow` for UI state in ViewModels
- Use `Flow` for observable data from repositories
- Use `suspend` for one-shot operations
- Use Hilt `@Inject constructor` for dependencies
- Use Material 3 components from `androidx.compose.material3`
- Use the existing `MuscleGroup`, `Equipment`, `Difficulty` enums
- Follow existing naming conventions
- Add `@Preview` functions for new composables
- Use `Modifier` as first parameter in composables
- Handle loading and error states in UI

### ❌ DON'T

- Don't use `LiveData` - we use `StateFlow`
- Don't use XML layouts - this is Compose-only
- Don't use `findViewById` or View binding
- Don't create singletons manually - use Hilt
- Don't hardcode strings - use `strings.xml` for user-facing text
- Don't ignore null safety - handle nulls explicitly
- Don't block the main thread - use coroutines
- Don't access Room directly from ViewModels - go through repositories

---

## Adding New Features

### New Screen Checklist

1. [ ] Create data class for UI state
2. [ ] Create ViewModel with `@HiltViewModel`
3. [ ] Create `@Composable` screen function
4. [ ] Add route to `Screen` sealed class in `Navigation.kt`
5. [ ] Add composable to `NavHost`
6. [ ] Add navigation callback to calling screen
7. [ ] Create preview function

### New Entity Checklist

1. [ ] Create entity in `data/local/entity/Entities.kt`
2. [ ] Create domain model in `domain/model/Models.kt`
3. [ ] Add mapping functions `toEntity()` and `toDomain()`
4. [ ] Create DAO in `data/local/dao/Daos.kt`
5. [ ] Add DAO to `WorkoutDatabase`
6. [ ] Increment database version and add migration
7. [ ] Create repository interface
8. [ ] Create repository implementation
9. [ ] Add to Hilt module

### New Use Case Checklist

1. [ ] Create class in `domain/usecase/`
2. [ ] Inject required repositories
3. [ ] Create data classes for params and results
4. [ ] Implement business logic
5. [ ] Add to ViewModel via injection

---

## Common Gotchas

### 1. Room Database Migrations
When modifying entities, you MUST handle migrations:
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE exercises ADD COLUMN new_column TEXT")
    }
}
```

### 2. Compose Recomposition
Avoid creating objects in composition:
```kotlin
// ❌ Bad - creates new lambda every recomposition
Button(onClick = { viewModel.doThing(item.id) })

// ✅ Good - stable lambda reference
val onClick = remember(item.id) { { viewModel.doThing(item.id) } }
Button(onClick = onClick)
```

### 3. Flow Collection
Always collect flows in lifecycle-aware manner:
```kotlin
// In Composable
val state by viewModel.uiState.collectAsState()

// In ViewModel, use viewModelScope
viewModelScope.launch {
    repository.getItems().collect { items ->
        _uiState.update { it.copy(items = items) }
    }
}
```

### 4. Hilt Injection in Composables
Use `hiltViewModel()` only in screen-level composables:
```kotlin
// ✅ Screen level
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel())

// ❌ Don't do this in child composables
@Composable
fun MyButton(viewModel: MyViewModel = hiltViewModel()) // Wrong!
```

---

## Testing Guidelines

### Unit Test Structure
```kotlin
class AIWorkoutGeneratorTest {
    
    private lateinit var generator: AIWorkoutGenerator
    private val mockExerciseRepo = mockk<ExerciseRepository>()
    
    @Before
    fun setup() {
        generator = AIWorkoutGenerator(mockExerciseRepo, ...)
    }
    
    @Test
    fun `generateWorkout returns correct exercise count for 45 min workout`() = runTest {
        // Given
        coEvery { mockExerciseRepo.getAllExercises() } returns flowOf(testExercises)
        
        // When
        val result = generator.generateWorkout(AIWorkoutParams(durationMinutes = 45))
        
        // Then
        assertThat(result).isInstanceOf(GeneratedWorkout.Success::class.java)
        assertThat((result as GeneratedWorkout.Success).workout.exercises.size).isIn(6..8)
    }
}
```

### Compose UI Test Structure
```kotlin
class HomeScreenTest {
    
    @get:Rule
    val composeRule = createComposeRule()
    
    @Test
    fun homeScreen_displaysGenerateButton() {
        composeRule.setContent {
            HomeScreen(
                onGenerateWorkout = {},
                onViewHistory = {},
                // ...
            )
        }
        
        composeRule.onNodeWithText("Generate Workout").assertIsDisplayed()
    }
}
```

---

## Performance Considerations

1. **Exercise Library Loading**: The 870+ exercise list is large. Use lazy loading and pagination in the Exercise Library screen.

2. **Image Caching**: Exercise images should be cached. Implement Coil with disk caching:
```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(exercise.imageUrl)
        .crossfade(true)
        .build(),
    contentDescription = null
)
```

3. **Database Queries**: Use Room's `@Query` with specific columns instead of `SELECT *` when possible.

4. **Compose Lists**: Always use `LazyColumn`/`LazyRow` with `key` parameter:
```kotlin
LazyColumn {
    items(exercises, key = { it.id }) { exercise ->
        ExerciseItem(exercise)
    }
}
```

---

## Deployment Notes

- **Signing**: Release builds need signing config in `app/build.gradle.kts`
- **ProGuard**: Rules are in `app/proguard-rules.pro`
- **Version**: Update `versionCode` and `versionName` in `build.gradle.kts`

---

## Getting Help

- Check `CLAUDE.md` for detailed project documentation
- Review existing implementations for patterns
- Run `./gradlew tasks` to see available Gradle tasks
- Check `gradle/libs.versions.toml` for dependency versions

---

## Summary Prompt for Quick Context

When starting work, use this as context:

> This is an Android Kotlin + Jetpack Compose app for workout generation. It uses Clean Architecture with Hilt DI and Room database. Key files: `AIWorkoutGenerator.kt` (workout logic), `Models.kt` (data classes), `ExerciseLibrary.kt` (870+ exercises). Follow existing patterns for ViewModels (StateFlow), Composables (Material 3), and Repositories (Flow for streams, suspend for one-shot).
