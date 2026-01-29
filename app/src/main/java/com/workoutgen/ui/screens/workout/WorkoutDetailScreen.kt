package com.workoutgen.ui.screens.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.workoutgen.domain.model.Workout
import com.workoutgen.domain.model.WorkoutExercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workoutId: String,
    onStartWorkout: () -> Unit,
    onBack: () -> Unit,
    onExerciseClick: (String) -> Unit,
    viewModel: WorkoutDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(workoutId) {
        viewModel.loadWorkout(workoutId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.workout?.name ?: "Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (uiState.workout?.isFavorite == true) 
                                Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (uiState.workout?.isFavorite == true) 
                                MaterialTheme.colorScheme.error else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, "Share")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartWorkout,
                icon = { Icon(Icons.Default.PlayArrow, "Start") },
                text = { Text("Start Workout") }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.workout == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Workout not found")
            }
        } else {
            val workout = uiState.workout!!
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Workout Summary Card
                item {
                    WorkoutSummaryCard(workout = workout)
                }
                
                // Warmup Section
                if (workout.warmup.isNotEmpty()) {
                    item {
                        Text(
                            text = "Warmup",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    itemsIndexed(workout.warmup) { index, exercise ->
                        ExerciseListItem(
                            index = index + 1,
                            workoutExercise = exercise,
                            onClick = { onExerciseClick(exercise.exercise.id) }
                        )
                    }
                }
                
                // Main Workout Section
                item {
                    Text(
                        text = "Main Workout",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                itemsIndexed(workout.exercises) { index, exercise ->
                    ExerciseListItem(
                        index = index + 1,
                        workoutExercise = exercise,
                        onClick = { onExerciseClick(exercise.exercise.id) }
                    )
                }
                
                // Cooldown Section
                if (workout.cooldown.isNotEmpty()) {
                    item {
                        Text(
                            text = "Cooldown",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    itemsIndexed(workout.cooldown) { index, exercise ->
                        ExerciseListItem(
                            index = index + 1,
                            workoutExercise = exercise,
                            onClick = { onExerciseClick(exercise.exercise.id) }
                        )
                    }
                }
                
                // Spacer for FAB
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun WorkoutSummaryCard(workout: Workout) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = workout.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WorkoutStat(
                    icon = Icons.Default.FitnessCenter,
                    value = "${workout.exercises.size}",
                    label = "Exercises"
                )
                WorkoutStat(
                    icon = Icons.Default.Timer,
                    value = "${workout.estimatedDurationMinutes}",
                    label = "Minutes"
                )
                WorkoutStat(
                    icon = Icons.Default.Speed,
                    value = workout.difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
                    label = "Level"
                )
            }
        }
    }
}

@Composable
private fun WorkoutStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseListItem(
    index: Int,
    workoutExercise: WorkoutExercise,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exercise number
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = index.toString(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Exercise details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workoutExercise.exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append("${workoutExercise.sets} sets × ")
                        append("${workoutExercise.reps.first}")
                        if (workoutExercise.reps.first != workoutExercise.reps.last) {
                            append("-${workoutExercise.reps.last}")
                        }
                        append(" reps")
                        workoutExercise.weight?.let { append(" @ ${it}kg") }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Rest: ${workoutExercise.restSeconds}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details"
            )
        }
    }
}
