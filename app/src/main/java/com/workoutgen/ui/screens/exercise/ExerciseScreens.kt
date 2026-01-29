package com.workoutgen.ui.screens.exercise

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.workoutgen.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    onExerciseClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercise Library") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (uiState.activeFilterCount > 0) {
                                Badge { Text("${uiState.activeFilterCount}") }
                            }
                        }
                    ) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Outlined.FilterList, "Filter")
                        }
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
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search exercises...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            // Quick filter chips
            QuickFilterChips(
                selectedMuscle = uiState.selectedMuscle,
                selectedDifficulty = uiState.selectedDifficulty,
                bodyweightOnly = uiState.bodyweightOnly,
                onMuscleClick = viewModel::setMuscleFilter,
                onDifficultyClick = viewModel::setDifficultyFilter,
                onBodyweightToggle = viewModel::toggleBodyweightOnly,
                onClearAll = viewModel::clearAllFilters
            )
            
            // Results count
            Text(
                text = "${uiState.filteredExercises.size} exercises",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Exercise list
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.filteredExercises.isEmpty() -> {
                    EmptyState(
                        message = "No exercises found",
                        description = "Try adjusting your filters",
                        onClearFilters = viewModel::clearAllFilters
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.filteredExercises, key = { it.id }) { exercise ->
                            ExerciseListItem(
                                exercise = exercise,
                                onClick = { onExerciseClick(exercise.id) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showFilterSheet) {
        FilterBottomSheet(
            uiState = uiState,
            onMuscleToggle = viewModel::toggleMuscleFilter,
            onEquipmentToggle = viewModel::toggleEquipmentFilter,
            onDifficultyToggle = viewModel::toggleDifficultyFilter,
            onTypeToggle = viewModel::toggleTypeFilter,
            onBodyweightToggle = viewModel::toggleBodyweightOnly,
            onClearAll = viewModel::clearAllFilters,
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun QuickFilterChips(
    selectedMuscle: MuscleGroup?,
    selectedDifficulty: Difficulty?,
    bodyweightOnly: Boolean,
    onMuscleClick: (MuscleGroup?) -> Unit,
    onDifficultyClick: (Difficulty?) -> Unit,
    onBodyweightToggle: () -> Unit,
    onClearAll: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Bodyweight toggle
        item {
            FilterChip(
                selected = bodyweightOnly,
                onClick = onBodyweightToggle,
                label = { Text("Bodyweight") },
                leadingIcon = if (bodyweightOnly) {
                    { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                } else null
            )
        }
        
        // Muscle dropdown
        item {
            var expanded by remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    selected = selectedMuscle != null,
                    onClick = { expanded = true },
                    label = { Text(selectedMuscle?.displayName ?: "Muscle") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("All Muscles") },
                        onClick = { onMuscleClick(null); expanded = false }
                    )
                    HorizontalDivider()
                    MuscleGroup.entries.filter { it != MuscleGroup.FULL_BODY }.forEach { muscle ->
                        DropdownMenuItem(
                            text = { Text(muscle.displayName) },
                            onClick = { onMuscleClick(muscle); expanded = false },
                            leadingIcon = if (selectedMuscle == muscle) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                    }
                }
            }
        }
        
        // Difficulty dropdown
        item {
            var expanded by remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    selected = selectedDifficulty != null,
                    onClick = { expanded = true },
                    label = { 
                        Text(selectedDifficulty?.name?.lowercase()
                            ?.replaceFirstChar { it.uppercase() } ?: "Level") 
                    },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("All Levels") },
                        onClick = { onDifficultyClick(null); expanded = false }
                    )
                    HorizontalDivider()
                    Difficulty.entries.forEach { diff ->
                        DropdownMenuItem(
                            text = { Text(diff.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = { onDifficultyClick(diff); expanded = false },
                            leadingIcon = if (selectedDifficulty == diff) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                    }
                }
            }
        }
        
        // Clear all
        if (selectedMuscle != null || selectedDifficulty != null || bodyweightOnly) {
            item {
                AssistChip(
                    onClick = onClearAll,
                    label = { Text("Clear") },
                    leadingIcon = { Icon(Icons.Default.Clear, null, Modifier.size(18.dp)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    uiState: ExerciseLibraryUiState,
    onMuscleToggle: (MuscleGroup) -> Unit,
    onEquipmentToggle: (Equipment) -> Unit,
    onDifficultyToggle: (Difficulty) -> Unit,
    onTypeToggle: (ExerciseType) -> Unit,
    onBodyweightToggle: () -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filter Exercises", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onClearAll) { Text("Clear All") }
                }
            }
            
            // Bodyweight toggle
            item {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onBodyweightToggle).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Bodyweight Only", fontWeight = FontWeight.Medium)
                        Text("No equipment needed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(checked = uiState.bodyweightOnly, onCheckedChange = { onBodyweightToggle() })
                }
            }
            
            item { HorizontalDivider() }
            
            // Difficulty
            item {
                Text("Difficulty", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = diff in uiState.selectedDifficulties,
                            onClick = { onDifficultyToggle(diff) },
                            label = { Text(diff.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getDifficultyColor(diff).copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
            
            // Muscle groups
            item {
                Text("Muscle Groups", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                val muscles = MuscleGroup.entries.filter { it != MuscleGroup.FULL_BODY }.chunked(3)
                muscles.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { muscle ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = muscle in uiState.selectedMuscles,
                                onClick = { onMuscleToggle(muscle) },
                                label = { Text(muscle.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            
            // Equipment
            item {
                Text("Equipment", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                val equipment = listOf(Equipment.NONE, Equipment.DUMBBELLS, Equipment.BARBELL, Equipment.CABLE_MACHINE, Equipment.KETTLEBELL, Equipment.BENCH).chunked(2)
                equipment.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { eq ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = eq in uiState.selectedEquipment,
                                onClick = { onEquipmentToggle(eq) },
                                label = { Text(eq.displayName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            
            // Exercise type
            item {
                Text("Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ExerciseType.COMPOUND, ExerciseType.ISOLATION, ExerciseType.CARDIO).forEach { type ->
                        FilterChip(
                            selected = type in uiState.selectedTypes,
                            onClick = { onTypeToggle(type) },
                            label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
            
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun EmptyState(message: String, description: String, onClearFilters: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.SearchOff, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.titleMedium)
        Text(description, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onClearFilters) {
            Icon(Icons.Default.Clear, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Clear Filters")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseListItem(exercise: Exercise, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    exercise.primaryMuscles.joinToString(", ") { it.displayName },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DifficultyBadge(exercise.difficulty)
                    SuggestionChip(onClick = {}, label = { Text(exercise.type.name.lowercase(), style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.height(24.dp))
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: Difficulty) {
    val color = getDifficultyColor(difficulty)
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.15f)) {
        Text(
            difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getDifficultyColor(difficulty: Difficulty) = when (difficulty) {
    Difficulty.BEGINNER -> Color(0xFF4CAF50)
    Difficulty.INTERMEDIATE -> Color(0xFFFF9800)
    Difficulty.ADVANCED -> Color(0xFFF44336)
    Difficulty.EXPERT -> Color(0xFF9C27B0)
}

// ============================================================================
// EXERCISE DETAIL SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onBack: () -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(exerciseId) { viewModel.loadExercise(exerciseId) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.exercise?.name ?: "Exercise") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        val exercise = uiState.exercise
        if (exercise == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Basic info
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DifficultyBadge(exercise.difficulty)
                                SuggestionChip(onClick = {}, label = { Text(exercise.type.name.lowercase()) })
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(exercise.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                
                // Muscles
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Target Muscles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Primary: ${exercise.primaryMuscles.joinToString { it.displayName }}")
                            if (exercise.secondaryMuscles.isNotEmpty()) {
                                Text("Secondary: ${exercise.secondaryMuscles.joinToString { it.displayName }}", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
                
                // Equipment
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Equipment", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            if (exercise.equipmentRequired.isEmpty() || exercise.equipmentRequired == listOf(Equipment.NONE)) {
                                Text("No equipment needed (bodyweight)")
                            } else {
                                exercise.equipmentRequired.forEach { Text("• ${it.displayName}") }
                            }
                        }
                    }
                }
                
                // Instructions
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Instructions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            exercise.instructions.forEachIndexed { index, instruction ->
                                Row(Modifier.padding(vertical = 4.dp)) {
                                    Text("${index + 1}.", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                                    Text(instruction)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
