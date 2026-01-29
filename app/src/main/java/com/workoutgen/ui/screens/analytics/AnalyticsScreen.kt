package com.workoutgen.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutgen.domain.model.*
import com.workoutgen.domain.repository.WorkoutLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ============================================================================
// VIEW MODEL
// ============================================================================

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val totalWorkouts: Int = 0,
    val totalVolume: Float = 0f,
    val totalMinutes: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val averageWorkoutLength: Int = 0,
    val weeklyWorkoutCounts: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0, 0),
    val personalRecords: List<PersonalRecord> = emptyList(),
    val muscleGroupVolume: Map<MuscleGroup, Float> = emptyMap(),
    val recentPRs: List<PersonalRecord> = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val workoutLogRepository: WorkoutLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            workoutLogRepository.getAllLogs().collect { logs ->
                val totalWorkouts = logs.size
                val totalVolume = logs.sumOf { it.totalVolume.toDouble() }.toFloat()
                val totalMinutes = logs.sumOf { log ->
                    val end = log.endTime ?: log.startTime
                    ((end - log.startTime) / 60000).toInt()
                }
                val avgLength = if (totalWorkouts > 0) totalMinutes / totalWorkouts else 0
                
                // Calculate weekly workout counts (last 8 weeks)
                val weeklyWorkouts = calculateWeeklyWorkouts(logs)
                
                // Calculate streaks
                val (current, longest) = calculateStreaks(logs)
                
                // Get PRs
                val prs = workoutLogRepository.getPersonalRecords()
                val recentPRs = prs.sortedByDescending { it.date }.take(5)
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalWorkouts = totalWorkouts,
                        totalVolume = totalVolume,
                        totalMinutes = totalMinutes,
                        averageWorkoutLength = avgLength,
                        weeklyWorkoutCounts = weeklyWorkouts,
                        currentStreak = current,
                        longestStreak = longest,
                        personalRecords = prs,
                        recentPRs = recentPRs
                    )
                }
            }
        }
    }

    private fun calculateWeeklyWorkouts(logs: List<WorkoutLog>): List<Int> {
        val now = System.currentTimeMillis()
        val weekMs = 7L * 24 * 60 * 60 * 1000
        
        return (0 until 8).map { weekIndex ->
            val weekEnd = now - (weekIndex * weekMs)
            val weekStart = weekEnd - weekMs
            logs.count { it.startTime in weekStart..weekEnd }
        }.reversed()
    }

    private fun calculateStreaks(logs: List<WorkoutLog>): Pair<Int, Int> {
        if (logs.isEmpty()) return Pair(0, 0)
        
        val calendar = Calendar.getInstance()
        val workoutDays = logs.map { log ->
            calendar.timeInMillis = log.startTime
            calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
        }.distinct().sorted()
        
        if (workoutDays.isEmpty()) return Pair(0, 0)
        
        var currentStreak = 1
        var longestStreak = 1
        var tempStreak = 1
        
        for (i in 1 until workoutDays.size) {
            if (workoutDays[i] - workoutDays[i - 1] <= 2) { // Allow 1 day gap
                tempStreak++
                longestStreak = maxOf(longestStreak, tempStreak)
            } else {
                tempStreak = 1
            }
        }
        
        // Check if current streak is still active
        calendar.timeInMillis = System.currentTimeMillis()
        val today = calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
        
        currentStreak = if (workoutDays.last() >= today - 2) tempStreak else 0
        
        return Pair(currentStreak, longestStreak)
    }
}

// ============================================================================
// UI COMPOSABLES
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.FitnessCenter,
                            label = "Workouts",
                            value = "${uiState.totalWorkouts}"
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Timer,
                            label = "Total Time",
                            value = "${uiState.totalMinutes / 60}h ${uiState.totalMinutes % 60}m"
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.LocalFireDepartment,
                            label = "Current Streak",
                            value = "${uiState.currentStreak} days"
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.EmojiEvents,
                            label = "Best Streak",
                            value = "${uiState.longestStreak} days"
                        )
                    }
                }

                // Weekly Activity Chart
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Weekly Activity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            WeeklyChart(weeklyData = uiState.weeklyWorkoutCounts)
                        }
                    }
                }

                // Volume lifted
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total Volume Lifted",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.Scale, null)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = formatVolume(uiState.totalVolume),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Personal Records
                item {
                    Text(
                        "Recent Personal Records",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.recentPRs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No PRs yet",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Complete workouts to track your records!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.recentPRs) { pr ->
                        PRCard(pr = pr)
                    }
                }

                // Quick Stats
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Quick Stats",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            QuickStatRow(
                                label = "Average Workout Length",
                                value = "${uiState.averageWorkoutLength} min"
                            )
                            QuickStatRow(
                                label = "Total PRs",
                                value = "${uiState.personalRecords.size}"
                            )
                            QuickStatRow(
                                label = "This Week",
                                value = "${uiState.weeklyWorkoutCounts.lastOrNull() ?: 0} workouts"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyChart(weeklyData: List<Int>) {
    val maxValue = (weeklyData.maxOrNull() ?: 1).coerceAtLeast(5)
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        weeklyData.forEachIndexed { index, count ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height((80.dp * count / maxValue).coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (index == weeklyData.size - 1) primaryColor
                            else primaryColor.copy(alpha = 0.5f)
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${count}",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "W${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PRCard(pr: PersonalRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EmojiEvents,
                    null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        pr.exerciseName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        formatDate(pr.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${pr.value.toInt()} kg",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (pr.reps != null) {
                    Text(
                        "× ${pr.reps} reps",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatRow(label: String, value: String) {
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

private fun formatVolume(volume: Float): String {
    return when {
        volume >= 1_000_000 -> String.format("%.1fM kg", volume / 1_000_000)
        volume >= 1_000 -> String.format("%.1fK kg", volume / 1_000)
        else -> String.format("%.0f kg", volume)
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
