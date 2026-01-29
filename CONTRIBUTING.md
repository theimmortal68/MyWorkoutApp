# CONTRIBUTING.md - Contribution Guidelines

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17+
- Android SDK 35
- Git

### Setup

```bash
# Clone the repository
git clone <repository-url>
cd workout-generator-scaffold

# Open in Android Studio
# File → Open → Select project folder

# Sync Gradle
# Android Studio will prompt to sync automatically

# Run on emulator or device
# Select device in toolbar → Click Run (▶)
```

### First Run

1. Build the project: `./gradlew assembleDebug`
2. Run on emulator/device
3. The app will seed 870+ exercises on first launch
4. Set up your profile in Settings

---

## Development Workflow

### Branch Naming

```
feature/[ticket-id]-short-description
bugfix/[ticket-id]-short-description
refactor/[ticket-id]-short-description
docs/short-description
```

Examples:
- `feature/WG-123-add-rest-timer`
- `bugfix/WG-456-fix-workout-crash`
- `refactor/WG-789-extract-calculator`

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): description

[optional body]

[optional footer]
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `docs`: Documentation only
- `style`: Formatting, missing semicolons, etc.
- `test`: Adding or fixing tests
- `chore`: Maintenance tasks

**Examples**:
```
feat(workout): add rest timer with notifications

- Implement countdown timer service
- Add notification with pause/resume actions
- Play sound on timer complete

Closes #123
```

```
fix(generator): prevent crash when no equipment selected

The AI generator now defaults to bodyweight exercises
when the user has no equipment configured.

Fixes #456
```

### Pull Request Process

1. Create feature branch from `main`
2. Make changes following code style
3. Write/update tests
4. Update documentation if needed
5. Run `./gradlew check` to verify
6. Create PR with description template
7. Request review from maintainers
8. Address feedback
9. Squash and merge when approved

---

## Code Style

### Kotlin

Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) with these additions:

```kotlin
// Use trailing commas
data class Exercise(
    val id: String,
    val name: String,
    val muscles: List<MuscleGroup>,  // trailing comma
)

// Use expression bodies for simple functions
fun isValid(): Boolean = name.isNotBlank() && muscles.isNotEmpty()

// Use named arguments for clarity
generateWorkout(
    duration = 45,
    goal = FitnessGoal.BUILD_MUSCLE,
    includeWarmup = true,
)

// Use scope functions appropriately
exercise?.let { 
    saveExercise(it)
    showToast("Saved ${it.name}")
}

// Prefer immutable data
val exercises: List<Exercise>  // not MutableList
```

### Compose

```kotlin
// Composable naming: PascalCase, noun/noun phrase
@Composable
fun ExerciseCard(...)  // ✓
fun exerciseCard(...)  // ✗
fun ShowExercise(...)  // ✗

// Modifier as first optional parameter
@Composable
fun ExerciseCard(
    exercise: Exercise,
    modifier: Modifier = Modifier,  // first optional param
    onClick: () -> Unit,
) { ... }

// Use remember for expensive computations
val filteredList = remember(exercises, filter) {
    exercises.filter { it.matches(filter) }
}

// Extract large composables
@Composable
fun ExerciseLibraryScreen(...) {
    Scaffold(...) { padding ->
        ExerciseList(...)  // extracted
    }
}

@Composable
private fun ExerciseList(...) { ... }
```

### File Organization

```kotlin
// File: ExerciseCard.kt

package com.workoutgen.ui.components

// 1. Imports (sorted, no wildcards)
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*

// 2. Public composables
@Composable
fun ExerciseCard(
    exercise: Exercise,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // ...
}

// 3. Private composables
@Composable
private fun ExerciseImage(...) { ... }

// 4. Preview functions
@Preview(showBackground = true)
@Composable
private fun ExerciseCardPreview() {
    WorkoutGenTheme {
        ExerciseCard(
            exercise = previewExercise,
            onClick = {},
        )
    }
}

// 5. Preview data
private val previewExercise = Exercise(
    id = "preview",
    name = "Bench Press",
    // ...
)
```

---

## Testing Requirements

### Unit Tests

**Required coverage**:
- All use cases
- All calculators/utilities
- ViewModel state changes

```kotlin
class OneRepMaxCalculatorTest {
    
    @Test
    fun `brzycki returns correct 1RM for known values`() {
        // Given
        val weight = 100f
        val reps = 5
        
        // When
        val result = OneRepMaxCalculator.brzycki(weight, reps)
        
        // Then
        assertThat(result).isWithin(0.1f).of(112.5f)
    }
    
    @Test
    fun `calculate returns weight when reps is 1`() {
        val result = OneRepMaxCalculator.calculate(100f, 1)
        assertThat(result).isEqualTo(100f)
    }
}
```

### Integration Tests

**Required for**:
- Repository + Database operations
- Complex data flows

```kotlin
@RunWith(AndroidJUnit4::class)
class ExerciseRepositoryTest {
    
    private lateinit var database: WorkoutDatabase
    private lateinit var repository: ExerciseRepository
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java
        ).build()
        repository = ExerciseRepositoryImpl(database.exerciseDao())
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun getExercisesByMuscle_returnsFilteredList() = runTest {
        // Given
        val exercises = listOf(
            testExercise(muscles = listOf(MuscleGroup.CHEST)),
            testExercise(muscles = listOf(MuscleGroup.BACK)),
        )
        exercises.forEach { repository.insert(it) }
        
        // When
        val result = repository.getExercisesByMuscleGroup(MuscleGroup.CHEST).first()
        
        // Then
        assertThat(result).hasSize(1)
        assertThat(result.first().primaryMuscles).contains(MuscleGroup.CHEST)
    }
}
```

### UI Tests

**Required for**:
- Critical user flows
- Accessibility compliance

```kotlin
class GenerateWorkoutScreenTest {
    
    @get:Rule
    val composeRule = createComposeRule()
    
    @Test
    fun clickingGenerateButton_showsLoadingIndicator() {
        composeRule.setContent {
            AIGenerateWorkoutScreen(
                onWorkoutGenerated = {},
                onBack = {},
            )
        }
        
        composeRule.onNodeWithText("Generate AI Workout").performClick()
        composeRule.onNode(hasProgressBar()).assertIsDisplayed()
    }
}
```

---

## Documentation

### Code Comments

```kotlin
/**
 * Generates a personalized workout based on user profile and preferences.
 *
 * The algorithm:
 * 1. Determines target muscles based on recovery status
 * 2. Selects exercises matching available equipment
 * 3. Calculates weights using progression history
 *
 * @param params Configuration for workout generation
 * @return Generated workout or error
 * @throws IllegalStateException if user profile is not configured
 */
suspend fun generateWorkout(params: AIWorkoutParams): GeneratedWorkout
```

### README Updates

Update README.md when:
- Adding new major features
- Changing setup instructions
- Modifying API/interfaces

### Architecture Docs

Update ARCHITECTURE.md when:
- Adding new layers/modules
- Changing data flow patterns
- Modifying database schema

---

## Review Checklist

Before submitting PR, verify:

- [ ] Code compiles without warnings
- [ ] All tests pass (`./gradlew check`)
- [ ] New code has tests
- [ ] Documentation updated
- [ ] No hardcoded strings (use resources)
- [ ] Accessibility labels added
- [ ] Dark mode works
- [ ] Handles configuration changes
- [ ] No memory leaks
- [ ] Follows code style

---

## Getting Help

- Check existing code for patterns
- Review `CLAUDE.md` for project guidance
- Review `AGENTS.md` for AI agent instructions
- Review `ARCHITECTURE.md` for system design
- Ask questions in PR comments

---

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.
