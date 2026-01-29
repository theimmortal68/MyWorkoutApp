package com.workoutgen.ui.screens.programs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutgen.data.local.BuiltInPrograms
import com.workoutgen.domain.model.*
import com.workoutgen.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ============================================================================
// VIEW MODEL
// ============================================================================

data class ProgramsUiState(
    val programs: List<TrainingProgram> = emptyList(),
    val filteredPrograms: List<TrainingProgram> = emptyList(),
    val selectedDifficulty: Difficulty? = null,
    val selectedGoal: FitnessGoal? = null,
    val currentProgramId: String? = null,
    val selectedProgram: TrainingProgram? = null,
    val showProgramDetail: Boolean = false
)

@HiltViewModel
class ProgramsViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgramsUiState())
    val uiState: StateFlow<ProgramsUiState> = _uiState.asStateFlow()

    init {
        loadPrograms()
    }

    private fun loadPrograms() {
        viewModelScope.launch {
            val programs = BuiltInPrograms.allPrograms
            _uiState.update { 
                it.copy(
                    programs = programs,
                    filteredPrograms = programs
                )
            }
            
            userProfileRepository.getProfile().collect { profile ->
                _uiState.update { it.copy(currentProgramId = profile?.currentProgramId) }
            }
        }
    }

    fun filterByDifficulty(difficulty: Difficulty?) {
        _uiState.update { state ->
            val filtered = applyFilters(state.programs, difficulty, state.selectedGoal)
            state.copy(selectedDifficulty = difficulty, filteredPrograms = filtered)
        }
    }

    fun filterByGoal(goal: FitnessGoal?) {
        _uiState.update { state ->
            val filtered = applyFilters(state.programs, state.selectedDifficulty, goal)
            state.copy(selectedGoal = goal, filteredPrograms = filtered)
        }
    }

    private fun applyFilters(
        programs: List<TrainingProgram>,
        difficulty: Difficulty?,
        goal: FitnessGoal?
    ): List<TrainingProgram> {
        return programs.filter { program ->
            (difficulty == null || program.difficulty == difficulty) &&
            (goal == null || program.goals.contains(goal))
        }
    }

    fun selectProgram(program: TrainingProgram) {
        _uiState.update { it.copy(selectedProgram = program, showProgramDetail = true) }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(showProgramDetail = false) }
    }

    fun startProgram(programId: String) {
        viewModelScope.launch {
            userProfileRepository.getProfile().first()?.let { profile ->
                val updated = profile.copy(
                    currentProgramId = programId,
                    programStartDate = System.currentTimeMillis(),
                    currentWeek = 1,
                    currentDay = 1
                )
                userProfileRepository.updateProfile(updated)
            }
            _uiState.update { it.copy(currentProgramId = programId, showProgramDetail = false) }
        }
    }
}

// ============================================================================
// UI COMPOSABLES
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsScreen(
    onNavigateBack: () -> Unit,
    onCreateAIProgram: () -> Unit = {},
    viewModel: ProgramsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training Programs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // AI Program Generator Card
            CreateAIProgramCard(
                onClick = onCreateAIProgram,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter chips
            FilterSection(
                selectedDifficulty = uiState.selectedDifficulty,
                selectedGoal = uiState.selectedGoal,
                onDifficultySelected = { viewModel.filterByDifficulty(it) },
                onGoalSelected = { viewModel.filterByGoal(it) }
            )

            // Program list
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.filteredPrograms) { program ->
                    ProgramCard(
                        program = program,
                        isActive = program.id == uiState.currentProgramId,
                        onClick = { viewModel.selectProgram(program) }
                    )
                }
            }
        }

        // Program detail bottom sheet
        if (uiState.showProgramDetail && uiState.selectedProgram != null) {
            ProgramDetailSheet(
                program = uiState.selectedProgram!!,
                isActive = uiState.selectedProgram?.id == uiState.currentProgramId,
                onDismiss = { viewModel.dismissDetail() },
                onStartProgram = { viewModel.startProgram(uiState.selectedProgram!!.id) }
            )
        }
    }
}

@Composable
private fun FilterSection(
    selectedDifficulty: Difficulty?,
    selectedGoal: FitnessGoal?,
    onDifficultySelected: (Difficulty?) -> Unit,
    onGoalSelected: (FitnessGoal?) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Difficulty",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedDifficulty == null,
                    onClick = { onDifficultySelected(null) },
                    label = { Text("All") }
                )
            }
            items(Difficulty.entries.toList()) { difficulty ->
                FilterChip(
                    selected = selectedDifficulty == difficulty,
                    onClick = { onDifficultySelected(difficulty) },
                    label = { Text(difficulty.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Goal",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedGoal == null,
                    onClick = { onGoalSelected(null) },
                    label = { Text("All") }
                )
            }
            items(listOf(
                FitnessGoal.BUILD_STRENGTH,
                FitnessGoal.BUILD_MUSCLE,
                FitnessGoal.GENERAL_FITNESS
            )) { goal ->
                FilterChip(
                    selected = selectedGoal == goal,
                    onClick = { onGoalSelected(goal) },
                    label = { Text(goal.displayName) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
    }
}

@Composable
private fun ProgramCard(
    program: TrainingProgram,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = if (isActive) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = program.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isActive) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Active") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp)) }
                    )
                }
            }

            Text(
                text = program.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
                maxLines = 2
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("${program.daysPerWeek} days/week") },
                    icon = { Icon(Icons.Default.CalendarMonth, null, Modifier.size(16.dp)) }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text(program.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    icon = { Icon(Icons.Default.FitnessCenter, null, Modifier.size(16.dp)) }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text(program.split.displayName) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgramDetailSheet(
    program: TrainingProgram,
    isActive: Boolean,
    onDismiss: () -> Unit,
    onStartProgram: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = program.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "by ${program.author}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = program.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Program details
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DetailRow("Days per week", "${program.daysPerWeek}")
                    DetailRow("Split", program.split.displayName)
                    DetailRow("Progression", program.periodization.displayName)
                    if (program.durationWeeks > 0) {
                        DetailRow("Duration", "${program.durationWeeks} weeks")
                    }
                    if (program.deloadFrequency > 0) {
                        DetailRow("Deload", "Every ${program.deloadFrequency} weeks")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Workout days preview
            Text(
                "Workout Schedule",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            program.programDays.forEach { day ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                day.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${day.exercises.size} exercises",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            day.targetMuscles.take(3).joinToString { it.displayName },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onStartProgram,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isActive
            ) {
                Icon(
                    if (isActive) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                    null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isActive) "Currently Active" else "Start Program")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
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
private fun CreateAIProgramCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Create AI Program",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Generate a personalized multi-week training program",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
