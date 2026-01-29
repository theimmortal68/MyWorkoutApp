package com.workoutgen.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.workoutgen.domain.model.Workout
import com.workoutgen.domain.model.WorkoutLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGenerateWorkout: () -> Unit,
    onViewHistory: () -> Unit,
    onViewProfile: () -> Unit,
    onViewExercises: () -> Unit,
    onViewAnalytics: () -> Unit,
    onWorkoutClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WorkoutGen AI") },
                actions = {
                    IconButton(onClick = onViewHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onViewProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onGenerateWorkout,
                icon = { Icon(Icons.Default.AutoAwesome, "Generate") },
                text = { Text("AI Workout") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Quick Stats Section
            item {
                QuickStatsCard(
                    workoutsThisWeek = uiState.workoutsThisWeek,
                    totalMinutes = uiState.totalMinutesThisWeek,
                    currentStreak = uiState.currentStreak
                )
            }
            
            // Recent Workouts Section
            if (uiState.recentWorkouts.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Workouts",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.recentWorkouts) { workout ->
                            WorkoutCard(
                                workout = workout,
                                onClick = { onWorkoutClick(workout.id) }
                            )
                        }
                    }
                }
            }
            
            // Favorite Workouts Section
            if (uiState.favoriteWorkouts.isNotEmpty()) {
                item {
                    Text(
                        text = "Favorites",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(uiState.favoriteWorkouts) { workout ->
                    WorkoutListItem(
                        workout = workout,
                        onClick = { onWorkoutClick(workout.id) }
                    )
                }
            }
            
            // Quick Actions
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MenuBook,
                        label = "Exercises",
                        onClick = onViewExercises
                    )
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Analytics,
                        label = "Analytics",
                        onClick = onViewAnalytics
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.History,
                        label = "History",
                        onClick = onViewHistory
                    )
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        onClick = onViewProfile
                    )
                }
            }
            
            // Spacer for FAB
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun QuickStatsCard(
    workoutsThisWeek: Int,
    totalMinutes: Int,
    currentStreak: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = workoutsThisWeek.toString(),
                label = "This Week",
                icon = Icons.Default.FitnessCenter
            )
            StatItem(
                value = "$totalMinutes min",
                label = "Active Time",
                icon = Icons.Default.Timer
            )
            StatItem(
                value = "$currentStreak days",
                label = "Streak",
                icon = Icons.Default.LocalFireDepartment
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutCard(
    workout: Workout,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(200.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = workout.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${workout.exercises.size} exercises • ${workout.estimatedDurationMinutes} min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                workout.targetMuscles.take(3).forEach { muscle ->
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = muscle.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutListItem(
    workout: Workout,
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
            Icon(
                imageVector = if (workout.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (workout.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${workout.exercises.size} exercises • ${workout.estimatedDurationMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
