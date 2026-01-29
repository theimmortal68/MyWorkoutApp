package com.workoutgen.ui.screens.programs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutgen.domain.model.*
import com.workoutgen.domain.repository.UserProfileRepository
import com.workoutgen.domain.usecase.AIProgramGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ============================================================================
// VIEW MODEL
// ============================================================================

data class GenerateProgramUiState(
    val daysPerWeek: Int = 4,
    val selectedGoal: FitnessGoal = FitnessGoal.BUILD_MUSCLE,
    val durationWeeks: Int = 12,
    val programName: String = "",
    val emphasisMuscles: Set<MuscleGroup> = emptySet(),
    val preferredSplit: WorkoutSplit? = null,
    val includeDeloads: Boolean = true,
    val deloadFrequency: Int = 4,
    val isGenerating: Boolean = false,
    val generatedProgram: AIGeneratedProgram? = null,
    val showProgramPreview: Boolean = false,
    val programStarted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class GenerateProgramViewModel @Inject constructor(
    private val aiProgramGenerator: AIProgramGenerator,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenerateProgramUiState())
    val uiState: StateFlow<GenerateProgramUiState> = _uiState.asStateFlow()

    private var userProfile: UserProfile = UserProfile()

    init {
        viewModelScope.launch {
            userProfileRepository.getUserProfile().collect { profile ->
                userProfile = profile ?: UserProfile()
                _uiState.update { state ->
                    state.copy(
                        daysPerWeek = userProfile.workoutDaysPerWeek.coerceIn(2, 6),
                        selectedGoal = userProfile.fitnessGoals.firstOrNull() ?: FitnessGoal.BUILD_MUSCLE
                    )
                }
            }
        }
    }

    fun setDaysPerWeek(days: Int) {
        _uiState.update { it.copy(daysPerWeek = days.coerceIn(2, 6), error = null) }
    }

    fun setGoal(goal: FitnessGoal) {
        _uiState.update { it.copy(selectedGoal = goal, error = null) }
    }

    fun setDurationWeeks(weeks: Int) {
        _uiState.update { it.copy(durationWeeks = weeks, error = null) }
    }

    fun setProgramName(name: String) {
        _uiState.update { it.copy(programName = name) }
    }

    fun toggleEmphasisMuscle(muscle: MuscleGroup) {
        _uiState.update { state ->
            val newMuscles = if (muscle in state.emphasisMuscles) {
                state.emphasisMuscles - muscle
            } else {
                if (state.emphasisMuscles.size < 3) {
                    state.emphasisMuscles + muscle
                } else {
                    state.emphasisMuscles
                }
            }
            state.copy(emphasisMuscles = newMuscles, error = null)
        }
    }

    fun setPreferredSplit(split: WorkoutSplit?) {
        _uiState.update { it.copy(preferredSplit = split, error = null) }
    }

    fun setIncludeDeloads(include: Boolean) {
        _uiState.update { it.copy(includeDeloads = include) }
    }

    fun setDeloadFrequency(weeks: Int) {
        _uiState.update { it.copy(deloadFrequency = weeks.coerceIn(3, 6)) }
    }

    fun generateProgram() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }

            val params = AIProgramGeneratorParams(
                daysPerWeek = _uiState.value.daysPerWeek,
                primaryGoal = _uiState.value.selectedGoal,
                durationWeeks = _uiState.value.durationWeeks,
                programName = _uiState.value.programName.ifBlank { null },
                emphasisMuscles = _uiState.value.emphasisMuscles.toList(),
                preferredSplit = _uiState.value.preferredSplit,
                includeDeloads = _uiState.value.includeDeloads,
                deloadFrequency = _uiState.value.deloadFrequency
            )

            when (val result = aiProgramGenerator.generateProgram(params)) {
                is GeneratedProgram.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isGenerating = false,
                            generatedProgram = result.result,
                            showProgramPreview = true
                        )
                    }
                }
                is GeneratedProgram.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            isGenerating = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun dismissPreview() {
        _uiState.update { it.copy(showProgramPreview = false) }
    }

    fun startProgram() {
        viewModelScope.launch {
            val program = _uiState.value.generatedProgram?.program ?: return@launch

            userProfileRepository.getUserProfile().first()?.let { profile ->
                val updated = profile.copy(
                    currentProgramId = program.id,
                    programStartDate = System.currentTimeMillis(),
                    currentWeek = 1,
                    currentDay = 1
                )
                userProfileRepository.updateProfile(updated)
            }

            _uiState.update { it.copy(programStarted = true, showProgramPreview = false) }
        }
    }

    fun getRecommendedSplit(): WorkoutSplit {
        return aiProgramGenerator.selectOptimalSplit(
            _uiState.value.daysPerWeek,
            _uiState.value.selectedGoal,
            userProfile.experienceLevel
        )
    }
}

// ============================================================================
// UI COMPOSABLES
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AIGenerateProgramScreen(
    onProgramStarted: () -> Unit,
    onBack: () -> Unit,
    viewModel: GenerateProgramViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.programStarted) {
        if (uiState.programStarted) {
            onProgramStarted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create AI Program") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isGenerating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Generating your program...")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Days per week
                item {
                    Text(
                        text = "How many days per week?",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items((2..6).toList()) { days ->
                            FilterChip(
                                selected = days == uiState.daysPerWeek,
                                onClick = { viewModel.setDaysPerWeek(days) },
                                label = { Text("$days days") },
                                leadingIcon = if (days == uiState.daysPerWeek) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // Primary Goal
                item {
                    Text(
                        text = "What's your primary goal?",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(
                            FitnessGoal.BUILD_MUSCLE,
                            FitnessGoal.BUILD_STRENGTH,
                            FitnessGoal.LOSE_WEIGHT,
                            FitnessGoal.POWERBUILDING,
                            FitnessGoal.GENERAL_FITNESS
                        )) { goal ->
                            FilterChip(
                                selected = goal == uiState.selectedGoal,
                                onClick = { viewModel.setGoal(goal) },
                                label = { Text(goal.displayName) },
                                leadingIcon = if (goal == uiState.selectedGoal) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // Duration
                item {
                    Text(
                        text = "Program duration",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(4, 8, 12, 16)) { weeks ->
                            FilterChip(
                                selected = weeks == uiState.durationWeeks,
                                onClick = { viewModel.setDurationWeeks(weeks) },
                                label = { Text("$weeks weeks") }
                            )
                        }
                    }
                }

                // Split Override (Optional)
                item {
                    val recommendedSplit = viewModel.getRecommendedSplit()

                    Text(
                        text = "Workout split (optional)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Recommended: ${recommendedSplit.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.preferredSplit == null,
                            onClick = { viewModel.setPreferredSplit(null) },
                            label = { Text("Auto") }
                        )
                        listOf(
                            WorkoutSplit.FULL_BODY,
                            WorkoutSplit.UPPER_LOWER,
                            WorkoutSplit.PUSH_PULL_LEGS,
                            WorkoutSplit.ARNOLD_SPLIT
                        ).filter { uiState.daysPerWeek in it.daysPerWeek }.forEach { split ->
                            FilterChip(
                                selected = uiState.preferredSplit == split,
                                onClick = { viewModel.setPreferredSplit(split) },
                                label = { Text(split.displayName) }
                            )
                        }
                    }
                }

                // Emphasis Muscles (Optional)
                item {
                    Text(
                        text = "Emphasis muscles (optional, max 3)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MuscleGroup.entries.filter { it != MuscleGroup.FULL_BODY }.forEach { muscle ->
                            FilterChip(
                                selected = muscle in uiState.emphasisMuscles,
                                onClick = { viewModel.toggleEmphasisMuscle(muscle) },
                                label = { Text(muscle.displayName) }
                            )
                        }
                    }
                }

                // Deload Options
                item {
                    Text(
                        text = "Deload settings",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include deload weeks")
                        Switch(
                            checked = uiState.includeDeloads,
                            onCheckedChange = { viewModel.setIncludeDeloads(it) }
                        )
                    }
                    if (uiState.includeDeloads) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Deload every ${uiState.deloadFrequency} weeks",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = uiState.deloadFrequency.toFloat(),
                            onValueChange = { viewModel.setDeloadFrequency(it.toInt()) },
                            valueRange = 3f..6f,
                            steps = 2
                        )
                    }
                }

                // Program Name (Optional)
                item {
                    Text(
                        text = "Program name (optional)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.programName,
                        onValueChange = { viewModel.setProgramName(it) },
                        placeholder = { Text("AI will generate a name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Error message
                if (uiState.error != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiState.error ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Generate Button
                item {
                    Button(
                        onClick = { viewModel.generateProgram() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isGenerating
                    ) {
                        Icon(Icons.Default.AutoAwesome, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Program")
                    }
                }

                // Spacer
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }

        // Program Preview Bottom Sheet
        if (uiState.showProgramPreview && uiState.generatedProgram != null) {
            ProgramPreviewSheet(
                generatedProgram = uiState.generatedProgram!!,
                onDismiss = { viewModel.dismissPreview() },
                onStartProgram = { viewModel.startProgram() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgramPreviewSheet(
    generatedProgram: AIGeneratedProgram,
    onDismiss: () -> Unit,
    onStartProgram: () -> Unit
) {
    val program = generatedProgram.program
    val reasoning = generatedProgram.reasoning

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Text(
                    text = program.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = program.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Program Details Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ProgramDetailRow("Split", program.split.displayName)
                        ProgramDetailRow("Days/Week", "${program.daysPerWeek}")
                        ProgramDetailRow("Duration", "${program.durationWeeks} weeks")
                        ProgramDetailRow("Progression", program.periodization.displayName)
                        if (program.deloadFrequency > 0) {
                            ProgramDetailRow("Deload", "Every ${program.deloadFrequency} weeks")
                        }
                    }
                }
            }

            // AI Reasoning Section
            item {
                Text(
                    text = "AI Reasoning",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ReasoningItem("Split Selection", reasoning.splitSelection)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ReasoningItem("Periodization", reasoning.periodizationSelection)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ReasoningItem("Volume", reasoning.volumeDistribution)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ReasoningItem("Progression", reasoning.progressionStrategy)
                        if (reasoning.limitationAdjustments.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "Adjustments for Physical Limitations:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            reasoning.limitationAdjustments.forEach { adjustment ->
                                Text(
                                    text = "• $adjustment",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Workout Schedule
            item {
                Text(
                    text = "Workout Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(program.programDays) { day ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Day ${day.dayNumber}: ${day.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${day.exercises.size} exercises",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = day.targetMuscles.take(4).joinToString(", ") { it.displayName },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        day.exercises.take(4).forEach { exercise ->
                            Text(
                                text = "• ${exercise.name} - ${exercise.sets}×${exercise.reps.first}-${exercise.reps.last}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (day.exercises.size > 4) {
                            Text(
                                text = "  + ${day.exercises.size - 4} more...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Start Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onStartProgram,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Program")
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ProgramDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ReasoningItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
