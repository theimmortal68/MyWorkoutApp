package com.workoutgen.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.workoutgen.domain.model.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddLimitationDialog by remember { mutableStateOf(false) }

    if (showAddLimitationDialog) {
        AddLimitationDialog(
            existingLimitations = uiState.profile.physicalLimitations.map { it.limitation },
            onDismiss = { showAddLimitationDialog = false },
            onAdd = { limitation ->
                viewModel.addLimitation(limitation)
                showAddLimitationDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Experience Level
            item {
                Text("Experience Level", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    Difficulty.entries.forEachIndexed { index, level ->
                        SegmentedButton(
                            selected = level == uiState.profile.experienceLevel,
                            onClick = { viewModel.setExperience(level) },
                            shape = SegmentedButtonDefaults.itemShape(index, Difficulty.entries.size)
                        ) {
                            Text(level.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }
            
            // Fitness Goals
            item {
                Text("Fitness Goals", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FitnessGoal.entries.forEach { goal ->
                        FilterChip(
                            selected = goal in uiState.profile.fitnessGoals,
                            onClick = { viewModel.toggleGoal(goal) },
                            label = { Text(goal.toDisplayName()) }
                        )
                    }
                }
            }
            
            // Available Equipment
            item {
                Text("Available Equipment", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Equipment.entries.forEach { equipment ->
                        FilterChip(
                            selected = equipment in uiState.profile.availableEquipment,
                            onClick = { viewModel.toggleEquipment(equipment) },
                            label = { Text(equipment.toDisplayName()) }
                        )
                    }
                }
            }
            
            // Preferred Workout Duration
            item {
                Text("Preferred Workout Duration", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${uiState.profile.preferredWorkoutDuration} minutes")
                }
                Slider(
                    value = uiState.profile.preferredWorkoutDuration.toFloat(),
                    onValueChange = { viewModel.setDuration(it.toInt()) },
                    valueRange = 15f..120f,
                    steps = 20
                )
            }
            
            // Workouts Per Week
            item {
                Text("Workouts Per Week", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${uiState.profile.workoutDaysPerWeek} days")
                }
                Slider(
                    value = uiState.profile.workoutDaysPerWeek.toFloat(),
                    onValueChange = { viewModel.setWorkoutDays(it.toInt()) },
                    valueRange = 1f..7f,
                    steps = 5
                )
            }
            
            // Preferred Split
            item {
                Text("Preferred Split", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                WorkoutSplit.entries.forEach { split ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = split == uiState.profile.preferredSplit,
                            onClick = { viewModel.setSplit(split) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(split.toDisplayName())
                    }
                }
            }

            // Physical Limitations
            item {
                Text("Physical Limitations", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Add any injuries or conditions to filter out exercises that may aggravate them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.profile.physicalLimitations.isEmpty()) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "No limitations added. The full exercise library will be available.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.profile.physicalLimitations.forEach { userLimitation ->
                            InputChip(
                                selected = true,
                                onClick = { },
                                label = {
                                    Column {
                                        Text(
                                            userLimitation.limitation.displayName,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            userLimitation.severity.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when (userLimitation.severity) {
                                                LimitationSeverity.MILD -> MaterialTheme.colorScheme.tertiary
                                                LimitationSeverity.MODERATE -> MaterialTheme.colorScheme.secondary
                                                LimitationSeverity.SEVERE -> MaterialTheme.colorScheme.error
                                            }
                                        )
                                    }
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { viewModel.removeLimitation(userLimitation.limitation) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove ${userLimitation.limitation.displayName}",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showAddLimitationDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Limitation")
                }
            }

            // Unit preference
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Use Metric Units (kg)")
                    Switch(
                        checked = uiState.profile.useMetricUnits,
                        onCheckedChange = { viewModel.setMetricUnits(it) }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

private fun FitnessGoal.toDisplayName(): String = when (this) {
    FitnessGoal.BUILD_MUSCLE -> "Build Muscle"
    FitnessGoal.LOSE_WEIGHT -> "Lose Weight"
    FitnessGoal.BUILD_STRENGTH -> "Build Strength"
    FitnessGoal.IMPROVE_ENDURANCE -> "Endurance"
    FitnessGoal.INCREASE_FLEXIBILITY -> "Flexibility"
    FitnessGoal.GENERAL_FITNESS -> "General Fitness"
    FitnessGoal.ATHLETIC_PERFORMANCE -> "Athletic Performance"
    FitnessGoal.POWERBUILDING -> "Powerbuilding"
}

private fun Equipment.toDisplayName(): String = name.lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }

private fun WorkoutSplit.toDisplayName(): String = when (this) {
    WorkoutSplit.FULL_BODY -> "Full Body"
    WorkoutSplit.UPPER_LOWER -> "Upper/Lower"
    WorkoutSplit.PUSH_PULL_LEGS -> "Push/Pull/Legs"
    WorkoutSplit.BRO_SPLIT -> "Body Part Split"
    WorkoutSplit.ARNOLD_SPLIT -> "Arnold Split"
    WorkoutSplit.POWERLIFTING -> "Powerlifting"
    WorkoutSplit.CUSTOM -> "Custom"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddLimitationDialog(
    existingLimitations: List<PhysicalLimitation>,
    onDismiss: () -> Unit,
    onAdd: (UserLimitation) -> Unit
) {
    var selectedLimitation by remember { mutableStateOf<PhysicalLimitation?>(null) }
    var selectedSeverity by remember { mutableStateOf(LimitationSeverity.MODERATE) }
    var notes by remember { mutableStateOf("") }
    var showLimitationPicker by remember { mutableStateOf(true) }

    val availableLimitations = remember(existingLimitations) {
        PhysicalLimitation.entries.filter { it !in existingLimitations }
    }

    // Group limitations by category for easier navigation
    val limitationsByCategory = remember(availableLimitations) {
        mapOf(
            "Knee" to availableLimitations.filter {
                it in listOf(
                    PhysicalLimitation.ACL_REPAIR,
                    PhysicalLimitation.MENISCUS_REPAIR,
                    PhysicalLimitation.KNEE_ARTHRITIS,
                    PhysicalLimitation.PATELLA_TENDINITIS
                )
            },
            "Shoulder" to availableLimitations.filter {
                it in listOf(
                    PhysicalLimitation.SHOULDER_BURSITIS,
                    PhysicalLimitation.ROTATOR_CUFF_INJURY,
                    PhysicalLimitation.SHOULDER_IMPINGEMENT,
                    PhysicalLimitation.LABRUM_TEAR,
                    PhysicalLimitation.FROZEN_SHOULDER
                )
            },
            "Back" to availableLimitations.filter {
                it in listOf(
                    PhysicalLimitation.LOWER_BACK_PAIN,
                    PhysicalLimitation.HERNIATED_DISC,
                    PhysicalLimitation.SCIATICA,
                    PhysicalLimitation.SPONDYLOLISTHESIS
                )
            },
            "Hip" to availableLimitations.filter {
                it in listOf(
                    PhysicalLimitation.HIP_BURSITIS,
                    PhysicalLimitation.HIP_LABRUM_TEAR,
                    PhysicalLimitation.HIP_REPLACEMENT
                )
            },
            "Arm/Wrist" to availableLimitations.filter {
                it in listOf(
                    PhysicalLimitation.TENNIS_ELBOW,
                    PhysicalLimitation.GOLFERS_ELBOW,
                    PhysicalLimitation.CARPAL_TUNNEL
                )
            },
            "Lower Leg/Foot" to availableLimitations.filter {
                it in listOf(
                    PhysicalLimitation.ANKLE_INSTABILITY,
                    PhysicalLimitation.PLANTAR_FASCIITIS
                )
            },
            "General" to availableLimitations.filter {
                it in listOf(
                    PhysicalLimitation.OSTEOPOROSIS,
                    PhysicalLimitation.PREGNANCY,
                    PhysicalLimitation.HIGH_BLOOD_PRESSURE
                )
            }
        ).filterValues { it.isNotEmpty() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (showLimitationPicker) "Add Physical Limitation"
                else "Set Severity"
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (showLimitationPicker) {
                    if (availableLimitations.isEmpty()) {
                        Text(
                            "All available limitations have already been added.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            limitationsByCategory.forEach { (category, limitations) ->
                                Text(
                                    category,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                limitations.forEach { limitation ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedLimitation = limitation
                                                showLimitationPicker = false
                                            }
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedLimitation == limitation,
                                            onClick = {
                                                selectedLimitation = limitation
                                                showLimitationPicker = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                limitation.displayName,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                limitation.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                } else {
                    // Severity selection
                    selectedLimitation?.let { limitation ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    limitation.displayName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    limitation.description,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Severity Level",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LimitationSeverity.entries.forEach { severity ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSeverity = severity }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedSeverity == severity,
                                    onClick = { selectedSeverity = severity }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        severity.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = when (severity) {
                                            LimitationSeverity.MILD -> MaterialTheme.colorScheme.tertiary
                                            LimitationSeverity.MODERATE -> MaterialTheme.colorScheme.secondary
                                            LimitationSeverity.SEVERE -> MaterialTheme.colorScheme.error
                                        }
                                    )
                                    Text(
                                        severity.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes (optional)") },
                            placeholder = { Text("e.g., surgery date, doctor's advice") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!showLimitationPicker && selectedLimitation != null) {
                TextButton(
                    onClick = {
                        selectedLimitation?.let { limitation ->
                            onAdd(
                                UserLimitation(
                                    limitation = limitation,
                                    severity = selectedSeverity,
                                    notes = notes.takeIf { it.isNotBlank() }
                                )
                            )
                        }
                    }
                ) {
                    Text("Add")
                }
            }
        },
        dismissButton = {
            if (showLimitationPicker) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            } else {
                TextButton(onClick = { showLimitationPicker = true }) {
                    Text("Back")
                }
            }
        }
    )
}
