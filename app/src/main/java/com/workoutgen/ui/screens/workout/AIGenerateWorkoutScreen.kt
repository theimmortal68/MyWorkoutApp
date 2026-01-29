package com.workoutgen.ui.screens.workout

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutgen.domain.model.*
import com.workoutgen.domain.usecase.AIWorkoutGenerator
import com.workoutgen.domain.usecase.AIWorkoutParams
import com.workoutgen.domain.usecase.GeneratedWorkout
import com.workoutgen.domain.repository.UserProfileRepository
import com.workoutgen.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ============================================================================
// VIEW MODEL
// ============================================================================

data class AIGenerateUiState(
    // User settings
    val durationMinutes: Int = 45,
    val selectedGoal: FitnessGoal = FitnessGoal.BUILD_MUSCLE,
    val selectedSplit: WorkoutSplit = WorkoutSplit.PUSH_PULL_LEGS,
    val progressionStyle: PeriodizationType = PeriodizationType.DOUBLE_PROGRESSION,
    val selectedMuscles: List<MuscleGroup> = emptyList(), // Empty = auto-select
    val focusAreas: List<MuscleGroup> = emptyList(),
    val prioritizeCompound: Boolean = true,
    val includeWarmup: Boolean = true,
    val includeCooldown: Boolean = true,
    
    // Profile info
    val availableEquipment: Set<Equipment> = emptySet(),
    val experienceLevel: Difficulty = Difficulty.INTERMEDIATE,
    
    // Generation state
    val isGenerating: Boolean = false,
    val generatedWorkout: Workout? = null,
    val workoutReasoning: com.workoutgen.domain.usecase.WorkoutReasoning? = null,
    val error: String? = null,
    
    // UI state
    val showMuscleSelector: Boolean = false,
    val showEquipmentSelector: Boolean = false,
    val showAdvancedOptions: Boolean = false
)

@HiltViewModel
class AIGenerateViewModel @Inject constructor(
    private val aiGenerator: AIWorkoutGenerator,
    private val userProfileRepository: UserProfileRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIGenerateUiState())
    val uiState: StateFlow<AIGenerateUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            userProfileRepository.getUserProfile().collect { profile ->
                profile?.let {
                    _uiState.update { state ->
                        state.copy(
                            availableEquipment = it.availableEquipment,
                            experienceLevel = it.experienceLevel,
                            selectedGoal = it.fitnessGoals.firstOrNull() ?: FitnessGoal.BUILD_MUSCLE,
                            selectedSplit = it.preferredSplit,
                            durationMinutes = it.preferredWorkoutDuration
                        )
                    }
                }
            }
        }
    }

    fun setDuration(minutes: Int) {
        _uiState.update { it.copy(durationMinutes = minutes) }
    }

    fun setGoal(goal: FitnessGoal) {
        _uiState.update { it.copy(selectedGoal = goal) }
    }

    fun setSplit(split: WorkoutSplit) {
        _uiState.update { it.copy(selectedSplit = split) }
    }

    fun setProgressionStyle(style: PeriodizationType) {
        _uiState.update { it.copy(progressionStyle = style) }
    }

    fun toggleMuscle(muscle: MuscleGroup) {
        _uiState.update { state ->
            val newMuscles = if (muscle in state.selectedMuscles) {
                state.selectedMuscles - muscle
            } else {
                state.selectedMuscles + muscle
            }
            state.copy(selectedMuscles = newMuscles)
        }
    }

    fun toggleFocusArea(muscle: MuscleGroup) {
        _uiState.update { state ->
            val newFocus = if (muscle in state.focusAreas) {
                state.focusAreas - muscle
            } else {
                state.focusAreas + muscle
            }
            state.copy(focusAreas = newFocus)
        }
    }

    fun toggleEquipment(equipment: Equipment) {
        viewModelScope.launch {
            userProfileRepository.getUserProfile().first()?.let { profile ->
                val newEquipment = if (equipment in profile.availableEquipment) {
                    profile.availableEquipment - equipment
                } else {
                    profile.availableEquipment + equipment
                }
                userProfileRepository.updateProfile(profile.copy(availableEquipment = newEquipment))
            }
        }
    }

    fun setPrioritizeCompound(value: Boolean) {
        _uiState.update { it.copy(prioritizeCompound = value) }
    }

    fun setIncludeWarmup(value: Boolean) {
        _uiState.update { it.copy(includeWarmup = value) }
    }

    fun setIncludeCooldown(value: Boolean) {
        _uiState.update { it.copy(includeCooldown = value) }
    }

    fun toggleMuscleSelector() {
        _uiState.update { it.copy(showMuscleSelector = !it.showMuscleSelector) }
    }

    fun toggleEquipmentSelector() {
        _uiState.update { it.copy(showEquipmentSelector = !it.showEquipmentSelector) }
    }

    fun toggleAdvancedOptions() {
        _uiState.update { it.copy(showAdvancedOptions = !it.showAdvancedOptions) }
    }

    fun generateWorkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            
            val state = _uiState.value
            val params = AIWorkoutParams(
                durationMinutes = state.durationMinutes,
                goal = state.selectedGoal,
                targetMuscles = state.selectedMuscles.takeIf { it.isNotEmpty() },
                splitType = state.selectedSplit,
                progressionStyle = state.progressionStyle,
                prioritizeCompound = state.prioritizeCompound,
                includeWarmup = state.includeWarmup,
                includeCooldown = state.includeCooldown,
                focusAreas = state.focusAreas
            )
            
            when (val result = aiGenerator.generateWorkout(params)) {
                is GeneratedWorkout.Success -> {
                    // Save the workout
                    workoutRepository.saveWorkout(result.workout)
                    _uiState.update { 
                        it.copy(
                            isGenerating = false,
                            generatedWorkout = result.workout,
                            workoutReasoning = result.reasoning
                        )
                    }
                }
                is GeneratedWorkout.Error -> {
                    _uiState.update { 
                        it.copy(
                            isGenerating = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun clearWorkout() {
        _uiState.update { it.copy(generatedWorkout = null, workoutReasoning = null) }
    }
}

// ============================================================================
// UI COMPOSABLES
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIGenerateWorkoutScreen(
    onWorkoutGenerated: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: AIGenerateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate when workout is generated
    LaunchedEffect(uiState.generatedWorkout) {
        uiState.generatedWorkout?.let { workout ->
            onWorkoutGenerated(workout.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Duration Selector
            item {
                DurationSelector(
                    selectedMinutes = uiState.durationMinutes,
                    onSelect = viewModel::setDuration
                )
            }

            // Goal Selector
            item {
                GoalSelector(
                    selectedGoal = uiState.selectedGoal,
                    onSelect = viewModel::setGoal
                )
            }

            // Split Type Selector
            item {
                SplitSelector(
                    selectedSplit = uiState.selectedSplit,
                    onSelect = viewModel::setSplit
                )
            }

            // Muscle Targeting (optional)
            item {
                MuscleTargetingSection(
                    selectedMuscles = uiState.selectedMuscles,
                    showSelector = uiState.showMuscleSelector,
                    onToggleSelector = viewModel::toggleMuscleSelector,
                    onToggleMuscle = viewModel::toggleMuscle
                )
            }

            // Equipment
            item {
                EquipmentSection(
                    availableEquipment = uiState.availableEquipment,
                    showSelector = uiState.showEquipmentSelector,
                    onToggleSelector = viewModel::toggleEquipmentSelector,
                    onToggleEquipment = viewModel::toggleEquipment
                )
            }

            // Advanced Options
            item {
                AdvancedOptionsSection(
                    showOptions = uiState.showAdvancedOptions,
                    onToggle = viewModel::toggleAdvancedOptions,
                    progressionStyle = uiState.progressionStyle,
                    onProgressionChange = viewModel::setProgressionStyle,
                    prioritizeCompound = uiState.prioritizeCompound,
                    onPrioritizeCompoundChange = viewModel::setPrioritizeCompound,
                    includeWarmup = uiState.includeWarmup,
                    onIncludeWarmupChange = viewModel::setIncludeWarmup,
                    includeCooldown = uiState.includeCooldown,
                    onIncludeCooldownChange = viewModel::setIncludeCooldown,
                    focusAreas = uiState.focusAreas,
                    onToggleFocusArea = viewModel::toggleFocusArea
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
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                uiState.error ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Generate Button
            item {
                Button(
                    onClick = viewModel::generateWorkout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !uiState.isGenerating
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Generating...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate AI Workout")
                    }
                }
            }

            // Spacer at bottom
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun DurationSelector(
    selectedMinutes: Int,
    onSelect: (Int) -> Unit
) {
    val options = listOf(30, 45, 60, 75, 90)
    
    SectionCard(
        title = "Workout Duration",
        icon = Icons.Outlined.Timer
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { minutes ->
                FilterChip(
                    selected = selectedMinutes == minutes,
                    onClick = { onSelect(minutes) },
                    label = { Text("$minutes min") },
                    leadingIcon = if (selectedMinutes == minutes) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun GoalSelector(
    selectedGoal: FitnessGoal,
    onSelect: (FitnessGoal) -> Unit
) {
    val goals = listOf(
        FitnessGoal.BUILD_MUSCLE to Pair(Icons.Outlined.FitnessCenter, "Hypertrophy"),
        FitnessGoal.BUILD_STRENGTH to Pair(Icons.Outlined.Bolt, "Heavy weights"),
        FitnessGoal.LOSE_WEIGHT to Pair(Icons.Outlined.LocalFireDepartment, "High intensity"),
        FitnessGoal.POWERBUILDING to Pair(Icons.Outlined.Shield, "Strength + Size"),
        FitnessGoal.GENERAL_FITNESS to Pair(Icons.Outlined.Favorite, "Balanced")
    )
    
    SectionCard(
        title = "Training Goal",
        icon = Icons.Outlined.Flag
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            goals.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (goal, iconAndDesc) ->
                        GoalChip(
                            modifier = Modifier.weight(1f),
                            goal = goal,
                            icon = iconAndDesc.first,
                            description = iconAndDesc.second,
                            isSelected = selectedGoal == goal,
                            onClick = { onSelect(goal) }
                        )
                    }
                    // Fill remaining space if odd number
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalChip(
    modifier: Modifier = Modifier,
    goal: FitnessGoal,
    icon: ImageVector,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                goal.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun SplitSelector(
    selectedSplit: WorkoutSplit,
    onSelect: (WorkoutSplit) -> Unit
) {
    val splits = listOf(
        WorkoutSplit.PUSH_PULL_LEGS,
        WorkoutSplit.UPPER_LOWER,
        WorkoutSplit.FULL_BODY,
        WorkoutSplit.BRO_SPLIT
    )
    
    SectionCard(
        title = "Training Split",
        icon = Icons.Outlined.ViewWeek
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(splits) { split ->
                FilterChip(
                    selected = selectedSplit == split,
                    onClick = { onSelect(split) },
                    label = { Text(split.displayName) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = when (selectedSplit) {
                WorkoutSplit.PUSH_PULL_LEGS -> "Muscles grouped by movement pattern. Great for 3-6 days/week."
                WorkoutSplit.UPPER_LOWER -> "Alternate between upper and lower body. Good for 4 days/week."
                WorkoutSplit.FULL_BODY -> "Train all muscles each session. Best for 2-3 days/week."
                WorkoutSplit.BRO_SPLIT -> "One muscle group per day. Traditional bodybuilding approach."
                else -> ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun MuscleTargetingSection(
    selectedMuscles: List<MuscleGroup>,
    showSelector: Boolean,
    onToggleSelector: () -> Unit,
    onToggleMuscle: (MuscleGroup) -> Unit
) {
    SectionCard(
        title = "Target Muscles",
        icon = Icons.Outlined.Accessibility,
        subtitle = if (selectedMuscles.isEmpty()) "Auto-select based on split" 
                   else "${selectedMuscles.size} selected"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleSelector)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (selectedMuscles.isEmpty()) "Let AI choose optimal muscles"
                else selectedMuscles.joinToString(", ") { it.displayName },
                style = MaterialTheme.typography.bodyMedium
            )
            Icon(
                if (showSelector) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null
            )
        }
        
        AnimatedVisibility(visible = showSelector) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                
                val muscleRows = MuscleGroup.entries.filter { it != MuscleGroup.FULL_BODY }.chunked(3)
                muscleRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { muscle ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = muscle in selectedMuscles,
                                onClick = { onToggleMuscle(muscle) },
                                label = { 
                                    Text(
                                        muscle.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                if (selectedMuscles.isNotEmpty()) {
                    TextButton(onClick = { selectedMuscles.forEach { onToggleMuscle(it) } }) {
                        Text("Clear Selection")
                    }
                }
            }
        }
    }
}

@Composable
private fun EquipmentSection(
    availableEquipment: Set<Equipment>,
    showSelector: Boolean,
    onToggleSelector: () -> Unit,
    onToggleEquipment: (Equipment) -> Unit
) {
    val commonEquipment = listOf(
        Equipment.BARBELL,
        Equipment.DUMBBELLS,
        Equipment.CABLE_MACHINE,
        Equipment.BENCH,
        Equipment.PULL_UP_BAR,
        Equipment.KETTLEBELL,
        Equipment.RESISTANCE_BANDS
    )
    
    SectionCard(
        title = "Available Equipment",
        icon = Icons.Outlined.Inventory2,
        subtitle = "${availableEquipment.size} items"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleSelector)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (availableEquipment.isEmpty()) "Bodyweight only"
                else availableEquipment.take(3).joinToString(", ") { it.displayName } +
                     if (availableEquipment.size > 3) " +${availableEquipment.size - 3} more" else "",
                style = MaterialTheme.typography.bodyMedium
            )
            Icon(
                if (showSelector) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null
            )
        }
        
        AnimatedVisibility(visible = showSelector) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                
                commonEquipment.forEach { equipment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleEquipment(equipment) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(equipment.displayName)
                        Checkbox(
                            checked = equipment in availableEquipment,
                            onCheckedChange = { onToggleEquipment(equipment) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedOptionsSection(
    showOptions: Boolean,
    onToggle: () -> Unit,
    progressionStyle: PeriodizationType,
    onProgressionChange: (PeriodizationType) -> Unit,
    prioritizeCompound: Boolean,
    onPrioritizeCompoundChange: (Boolean) -> Unit,
    includeWarmup: Boolean,
    onIncludeWarmupChange: (Boolean) -> Unit,
    includeCooldown: Boolean,
    onIncludeCooldownChange: (Boolean) -> Unit,
    focusAreas: List<MuscleGroup>,
    onToggleFocusArea: (MuscleGroup) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Tune, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Advanced Options",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    if (showOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null
                )
            }
            
            AnimatedVisibility(visible = showOptions) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Progression Style
                    Text(
                        "Progression Style",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val progressionOptions = listOf(
                        PeriodizationType.LINEAR to "Add weight each session",
                        PeriodizationType.DOUBLE_PROGRESSION to "Increase reps, then weight",
                        PeriodizationType.WAVE to "Heavy/medium/light cycling",
                        PeriodizationType.DAILY_UNDULATING to "Vary rep ranges daily"
                    )
                    
                    progressionOptions.forEach { (style, description) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProgressionChange(style) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = progressionStyle == style,
                                onClick = { onProgressionChange(style) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(style.displayName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Toggle options
                    SwitchOption(
                        label = "Prioritize Compound Movements",
                        description = "Start with multi-joint exercises",
                        checked = prioritizeCompound,
                        onCheckedChange = onPrioritizeCompoundChange
                    )
                    
                    SwitchOption(
                        label = "Include Warmup",
                        description = "5-10 min warmup routine",
                        checked = includeWarmup,
                        onCheckedChange = onIncludeWarmupChange
                    )
                    
                    SwitchOption(
                        label = "Include Cooldown",
                        description = "Stretching routine",
                        checked = includeCooldown,
                        onCheckedChange = onIncludeCooldownChange
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchOption(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
