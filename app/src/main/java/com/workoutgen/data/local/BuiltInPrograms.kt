package com.workoutgen.data.local

import com.workoutgen.domain.model.*

/**
 * Built-in training programs inspired by popular programs like:
 * - StrongLifts 5x5
 * - GreySkull LP
 * - Push/Pull/Legs
 * - nSuns 5/3/1
 * - GZCLP
 * - Wendler 5/3/1
 */
object BuiltInPrograms {

    // ========================================================================
    // STRONGLIFTS 5x5
    // ========================================================================
    val strongLifts5x5 = TrainingProgram(
        id = "stronglifts_5x5",
        name = "StrongLifts 5×5",
        description = "The classic beginner strength program. Squat every workout, alternate between Workout A and B. Add 2.5kg/5lb every session.",
        author = "Mehdi Hadim",
        difficulty = Difficulty.BEGINNER,
        goals = listOf(FitnessGoal.BUILD_STRENGTH),
        durationWeeks = 0, // Indefinite
        daysPerWeek = 3,
        split = WorkoutSplit.FULL_BODY,
        periodization = PeriodizationType.LINEAR,
        requiredEquipment = setOf(Equipment.BARBELL, Equipment.BENCH),
        deloadFrequency = 0, // Deload on failure
        programDays = listOf(
            ProgramDay(
                dayNumber = 1,
                name = "Workout A",
                targetMuscles = listOf(MuscleGroup.QUADRICEPS, MuscleGroup.CHEST, MuscleGroup.BACK),
                exercises = listOf(
                    ProgramExercise("barbell_squat", "Barbell Squat", 5, 5..5, null, null, 180, "Main lift - add 2.5kg each session"),
                    ProgramExercise("bench_press", "Bench Press", 5, 5..5, null, null, 180, "Add 2.5kg each session"),
                    ProgramExercise("barbell_rows", "Barbell Row", 5, 5..5, null, null, 180, "Add 2.5kg each session")
                )
            ),
            ProgramDay(
                dayNumber = 2,
                name = "Workout B",
                targetMuscles = listOf(MuscleGroup.QUADRICEPS, MuscleGroup.SHOULDERS, MuscleGroup.BACK),
                exercises = listOf(
                    ProgramExercise("barbell_squat", "Barbell Squat", 5, 5..5, null, null, 180, "Main lift - add 2.5kg each session"),
                    ProgramExercise("overhead_press", "Overhead Press", 5, 5..5, null, null, 180, "Add 2.5kg each session"),
                    ProgramExercise("deadlift", "Deadlift", 1, 5..5, null, null, 300, "Add 5kg each session")
                )
            )
        ),
        progressionRules = ProgressionRules(
            weightIncrementKg = mapOf(
                MuscleGroup.QUADRICEPS to 2.5f,
                MuscleGroup.BACK to 5f, // Deadlift
                MuscleGroup.CHEST to 2.5f,
                MuscleGroup.SHOULDERS to 2.5f
            ),
            progressionTrigger = ProgressionTrigger.ALL_SETS_COMPLETED,
            failureThreshold = 3,
            deloadPercentage = 0.1f
        )
    )

    // ========================================================================
    // GREYSKULL LP
    // ========================================================================
    val greyskullLP = TrainingProgram(
        id = "greyskull_lp",
        name = "GreySkull LP",
        description = "Linear progression with AMRAP final sets. Better for hypertrophy than StrongLifts while still building strength.",
        author = "John Sheaffer",
        difficulty = Difficulty.BEGINNER,
        goals = listOf(FitnessGoal.BUILD_STRENGTH, FitnessGoal.BUILD_MUSCLE),
        durationWeeks = 0,
        daysPerWeek = 3,
        split = WorkoutSplit.FULL_BODY,
        periodization = PeriodizationType.LINEAR,
        requiredEquipment = setOf(Equipment.BARBELL, Equipment.BENCH),
        programDays = listOf(
            ProgramDay(
                dayNumber = 1,
                name = "Day A",
                targetMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADRICEPS),
                exercises = listOf(
                    ProgramExercise("bench_press", "Bench Press", 3, 5..5, null, null, 180, "Last set AMRAP", setType = SetType.AMRAP),
                    ProgramExercise("barbell_rows", "Barbell Row", 3, 5..5, null, null, 180, "Last set AMRAP", setType = SetType.AMRAP),
                    ProgramExercise("barbell_squat", "Barbell Squat", 3, 5..5, null, null, 180, "Last set AMRAP", setType = SetType.AMRAP)
                )
            ),
            ProgramDay(
                dayNumber = 2,
                name = "Day B",
                targetMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK, MuscleGroup.HAMSTRINGS),
                exercises = listOf(
                    ProgramExercise("overhead_press", "Overhead Press", 3, 5..5, null, null, 180, "Last set AMRAP", setType = SetType.AMRAP),
                    ProgramExercise("pull_ups", "Pull-ups / Chin-ups", 3, 5..5, null, null, 180, "Last set AMRAP", setType = SetType.AMRAP),
                    ProgramExercise("deadlift", "Deadlift", 1, 5..5, null, null, 300, "Single AMRAP set", setType = SetType.AMRAP)
                )
            )
        ),
        progressionRules = ProgressionRules(
            progressionTrigger = ProgressionTrigger.AMRAP_BASED,
            failureThreshold = 2,
            deloadPercentage = 0.1f
        )
    )

    // ========================================================================
    // PUSH/PULL/LEGS (6-Day)
    // ========================================================================
    val pushPullLegs = TrainingProgram(
        id = "ppl_6day",
        name = "Push/Pull/Legs (6-Day)",
        description = "Classic bodybuilding split. Each muscle group trained 2x per week for optimal hypertrophy.",
        author = "Community",
        difficulty = Difficulty.INTERMEDIATE,
        goals = listOf(FitnessGoal.BUILD_MUSCLE),
        durationWeeks = 0,
        daysPerWeek = 6,
        split = WorkoutSplit.PUSH_PULL_LEGS,
        periodization = PeriodizationType.DOUBLE_PROGRESSION,
        requiredEquipment = setOf(Equipment.BARBELL, Equipment.DUMBBELLS, Equipment.CABLE_MACHINE, Equipment.BENCH),
        programDays = listOf(
            // PUSH DAY 1
            ProgramDay(
                dayNumber = 1,
                name = "Push A (Chest Focus)",
                targetMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                exercises = listOf(
                    ProgramExercise("bench_press", "Bench Press", 4, 6..8, 7..8, null, 180),
                    ProgramExercise("incline_dumbbell_press", "Incline Dumbbell Press", 3, 8..10, 7..8, null, 120),
                    ProgramExercise("dumbbell_flyes", "Dumbbell Flyes", 3, 10..12, 7..8, null, 90),
                    ProgramExercise("overhead_press", "Overhead Press", 3, 8..10, 7..8, null, 120),
                    ProgramExercise("lateral_raises", "Lateral Raises", 4, 12..15, 7..8, null, 60),
                    ProgramExercise("tricep_pushdown", "Tricep Pushdowns", 3, 10..12, 7..8, null, 60),
                    ProgramExercise("overhead_tricep_extension", "Overhead Tricep Extension", 3, 10..12, 7..8, null, 60)
                )
            ),
            // PULL DAY 1
            ProgramDay(
                dayNumber = 2,
                name = "Pull A (Back Focus)",
                targetMuscles = listOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
                exercises = listOf(
                    ProgramExercise("deadlift", "Deadlift", 3, 5..5, 7..8, null, 180),
                    ProgramExercise("barbell_rows", "Barbell Rows", 4, 6..8, 7..8, null, 120),
                    ProgramExercise("lat_pulldown", "Lat Pulldown", 3, 8..10, 7..8, null, 90),
                    ProgramExercise("cable_rows", "Seated Cable Rows", 3, 10..12, 7..8, null, 90),
                    ProgramExercise("face_pulls", "Face Pulls", 4, 15..20, null, null, 60),
                    ProgramExercise("barbell_curl", "Barbell Curls", 3, 8..10, 7..8, null, 60),
                    ProgramExercise("hammer_curls", "Hammer Curls", 3, 10..12, 7..8, null, 60)
                )
            ),
            // LEGS DAY 1
            ProgramDay(
                dayNumber = 3,
                name = "Legs A (Quad Focus)",
                targetMuscles = listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
                exercises = listOf(
                    ProgramExercise("barbell_squat", "Barbell Squat", 4, 6..8, 7..8, null, 180),
                    ProgramExercise("leg_press", "Leg Press", 3, 10..12, 7..8, null, 120),
                    ProgramExercise("lunges", "Walking Lunges", 3, 10..12, null, null, 90),
                    ProgramExercise("leg_curl", "Leg Curls", 4, 10..12, 7..8, null, 60),
                    ProgramExercise("leg_extension", "Leg Extensions", 3, 12..15, 7..8, null, 60),
                    ProgramExercise("calf_raises", "Standing Calf Raises", 4, 12..15, null, null, 60)
                )
            ),
            // PUSH DAY 2
            ProgramDay(
                dayNumber = 4,
                name = "Push B (Shoulder Focus)",
                targetMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.CHEST, MuscleGroup.TRICEPS),
                exercises = listOf(
                    ProgramExercise("overhead_press", "Overhead Press", 4, 6..8, 7..8, null, 180),
                    ProgramExercise("incline_bench_press", "Incline Bench Press", 3, 8..10, 7..8, null, 120),
                    ProgramExercise("cable_crossover", "Cable Crossovers", 3, 12..15, 7..8, null, 60),
                    ProgramExercise("lateral_raises", "Lateral Raises", 4, 12..15, 7..8, null, 60),
                    ProgramExercise("rear_delt_fly", "Rear Delt Flyes", 4, 12..15, 7..8, null, 60),
                    ProgramExercise("tricep_dips", "Tricep Dips", 3, 8..12, 7..8, null, 90),
                    ProgramExercise("skull_crushers", "Skull Crushers", 3, 10..12, 7..8, null, 60)
                )
            ),
            // PULL DAY 2
            ProgramDay(
                dayNumber = 5,
                name = "Pull B (Width Focus)",
                targetMuscles = listOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
                exercises = listOf(
                    ProgramExercise("pull_ups", "Pull-ups", 4, 6..10, 7..8, null, 120),
                    ProgramExercise("t_bar_rows", "T-Bar Rows", 4, 8..10, 7..8, null, 120),
                    ProgramExercise("straight_arm_pulldown", "Straight Arm Pulldowns", 3, 12..15, 7..8, null, 60),
                    ProgramExercise("single_arm_rows", "Single Arm Dumbbell Rows", 3, 8..10, 7..8, null, 90),
                    ProgramExercise("face_pulls", "Face Pulls", 4, 15..20, null, null, 60),
                    ProgramExercise("incline_curl", "Incline Dumbbell Curls", 3, 10..12, 7..8, null, 60),
                    ProgramExercise("concentration_curl", "Concentration Curls", 3, 10..12, 7..8, null, 60)
                )
            ),
            // LEGS DAY 2
            ProgramDay(
                dayNumber = 6,
                name = "Legs B (Posterior Focus)",
                targetMuscles = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.QUADRICEPS, MuscleGroup.CALVES),
                exercises = listOf(
                    ProgramExercise("romanian_deadlift", "Romanian Deadlift", 4, 8..10, 7..8, null, 120),
                    ProgramExercise("hip_thrust", "Barbell Hip Thrust", 4, 8..12, 7..8, null, 120),
                    ProgramExercise("front_squat", "Front Squat", 3, 8..10, 7..8, null, 150),
                    ProgramExercise("bulgarian_split_squat", "Bulgarian Split Squats", 3, 10..12, 7..8, null, 90),
                    ProgramExercise("leg_curl", "Lying Leg Curls", 4, 10..12, 7..8, null, 60),
                    ProgramExercise("seated_calf_raises", "Seated Calf Raises", 4, 15..20, null, null, 45)
                )
            )
        ),
        progressionRules = ProgressionRules(
            progressionTrigger = ProgressionTrigger.ALL_SETS_COMPLETED,
            failureThreshold = 2
        )
    )

    // ========================================================================
    // UPPER/LOWER (4-Day)
    // ========================================================================
    val upperLower = TrainingProgram(
        id = "upper_lower_4day",
        name = "Upper/Lower Split (4-Day)",
        description = "Balanced 4-day split. Upper and lower body each trained 2x per week. Great for intermediates.",
        author = "Community",
        difficulty = Difficulty.INTERMEDIATE,
        goals = listOf(FitnessGoal.BUILD_MUSCLE, FitnessGoal.BUILD_STRENGTH),
        durationWeeks = 0,
        daysPerWeek = 4,
        split = WorkoutSplit.UPPER_LOWER,
        periodization = PeriodizationType.DOUBLE_PROGRESSION,
        requiredEquipment = setOf(Equipment.BARBELL, Equipment.DUMBBELLS, Equipment.BENCH),
        programDays = listOf(
            ProgramDay(
                dayNumber = 1,
                name = "Upper A (Strength)",
                targetMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS),
                exercises = listOf(
                    ProgramExercise("bench_press", "Bench Press", 4, 5..5, 7..8, null, 180),
                    ProgramExercise("barbell_rows", "Barbell Rows", 4, 5..5, 7..8, null, 180),
                    ProgramExercise("overhead_press", "Overhead Press", 3, 6..8, 7..8, null, 120),
                    ProgramExercise("pull_ups", "Pull-ups", 3, 6..10, null, null, 120),
                    ProgramExercise("barbell_curl", "Barbell Curls", 3, 8..10, null, null, 60),
                    ProgramExercise("skull_crushers", "Skull Crushers", 3, 8..10, null, null, 60)
                )
            ),
            ProgramDay(
                dayNumber = 2,
                name = "Lower A (Strength)",
                targetMuscles = listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
                exercises = listOf(
                    ProgramExercise("barbell_squat", "Barbell Squat", 4, 5..5, 7..8, null, 180),
                    ProgramExercise("romanian_deadlift", "Romanian Deadlift", 3, 8..10, 7..8, null, 120),
                    ProgramExercise("leg_press", "Leg Press", 3, 8..10, 7..8, null, 120),
                    ProgramExercise("leg_curl", "Leg Curls", 3, 10..12, null, null, 60),
                    ProgramExercise("calf_raises", "Calf Raises", 4, 12..15, null, null, 45)
                )
            ),
            ProgramDay(
                dayNumber = 3,
                name = "Upper B (Volume)",
                targetMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS),
                exercises = listOf(
                    ProgramExercise("incline_dumbbell_press", "Incline Dumbbell Press", 4, 8..12, 7..8, null, 90),
                    ProgramExercise("cable_rows", "Cable Rows", 4, 8..12, 7..8, null, 90),
                    ProgramExercise("lateral_raises", "Lateral Raises", 4, 12..15, null, null, 60),
                    ProgramExercise("lat_pulldown", "Lat Pulldown", 3, 10..12, 7..8, null, 90),
                    ProgramExercise("dumbbell_flyes", "Dumbbell Flyes", 3, 12..15, null, null, 60),
                    ProgramExercise("hammer_curls", "Hammer Curls", 3, 10..12, null, null, 60),
                    ProgramExercise("tricep_pushdown", "Tricep Pushdowns", 3, 10..12, null, null, 60)
                )
            ),
            ProgramDay(
                dayNumber = 4,
                name = "Lower B (Volume)",
                targetMuscles = listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
                exercises = listOf(
                    ProgramExercise("deadlift", "Conventional Deadlift", 3, 5..5, 7..8, null, 180),
                    ProgramExercise("front_squat", "Front Squat", 3, 8..10, 7..8, null, 150),
                    ProgramExercise("hip_thrust", "Hip Thrust", 3, 10..12, 7..8, null, 90),
                    ProgramExercise("lunges", "Walking Lunges", 3, 10..12, null, null, 90),
                    ProgramExercise("leg_extension", "Leg Extensions", 3, 12..15, null, null, 60),
                    ProgramExercise("seated_calf_raises", "Seated Calf Raises", 4, 15..20, null, null, 45)
                )
            )
        ),
        progressionRules = ProgressionRules(
            progressionTrigger = ProgressionTrigger.ALL_SETS_COMPLETED
        )
    )

    // ========================================================================
    // BEGINNER FULL BODY (3-Day)
    // ========================================================================
    val beginnerFullBody = TrainingProgram(
        id = "beginner_full_body",
        name = "Beginner Full Body (3-Day)",
        description = "Perfect for beginners. Simple full body routine 3x per week with compound movements. Focus on learning form.",
        author = "Community",
        difficulty = Difficulty.BEGINNER,
        goals = listOf(FitnessGoal.GENERAL_FITNESS, FitnessGoal.BUILD_STRENGTH),
        durationWeeks = 12,
        daysPerWeek = 3,
        split = WorkoutSplit.FULL_BODY,
        periodization = PeriodizationType.LINEAR,
        requiredEquipment = setOf(Equipment.BARBELL, Equipment.DUMBBELLS, Equipment.BENCH),
        programDays = listOf(
            ProgramDay(
                dayNumber = 1,
                name = "Full Body A",
                targetMuscles = listOf(MuscleGroup.QUADRICEPS, MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.CORE),
                exercises = listOf(
                    ProgramExercise("goblet_squat", "Goblet Squat", 3, 8..12, null, null, 90, "Focus on form"),
                    ProgramExercise("push_ups", "Push-ups", 3, 8..15, null, null, 60),
                    ProgramExercise("dumbbell_rows", "Dumbbell Rows", 3, 8..12, null, null, 60),
                    ProgramExercise("plank", "Plank", 3, 30..60, null, null, 45, "Seconds hold")
                )
            ),
            ProgramDay(
                dayNumber = 2,
                name = "Full Body B",
                targetMuscles = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.SHOULDERS, MuscleGroup.BACK, MuscleGroup.CORE),
                exercises = listOf(
                    ProgramExercise("romanian_deadlift", "Dumbbell RDL", 3, 8..12, null, null, 90),
                    ProgramExercise("dumbbell_press", "Dumbbell Shoulder Press", 3, 8..12, null, null, 60),
                    ProgramExercise("lat_pulldown", "Lat Pulldown", 3, 8..12, null, null, 60),
                    ProgramExercise("dead_bug", "Dead Bugs", 3, 10..15, null, null, 45)
                )
            ),
            ProgramDay(
                dayNumber = 3,
                name = "Full Body C",
                targetMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.TRICEPS),
                exercises = listOf(
                    ProgramExercise("glute_bridge", "Glute Bridge", 3, 12..15, null, null, 60),
                    ProgramExercise("incline_push_ups", "Incline Push-ups", 3, 10..15, null, null, 60),
                    ProgramExercise("cable_rows", "Cable Rows", 3, 10..12, null, null, 60),
                    ProgramExercise("dumbbell_curl", "Dumbbell Curls", 2, 10..15, null, null, 45),
                    ProgramExercise("tricep_dips", "Bench Dips", 2, 10..15, null, null, 45)
                )
            )
        ),
        progressionRules = ProgressionRules(
            progressionTrigger = ProgressionTrigger.ALL_SETS_COMPLETED,
            failureThreshold = 3
        )
    )

    // ========================================================================
    // GZCLP
    // ========================================================================
    val gzclp = TrainingProgram(
        id = "gzclp",
        name = "GZCLP",
        description = "GZCL Linear Progression. Tiered approach: T1 (heavy compound), T2 (lighter compound), T3 (accessories).",
        author = "Cody Lefever",
        difficulty = Difficulty.INTERMEDIATE,
        goals = listOf(FitnessGoal.BUILD_STRENGTH, FitnessGoal.BUILD_MUSCLE),
        durationWeeks = 0,
        daysPerWeek = 4,
        split = WorkoutSplit.UPPER_LOWER,
        periodization = PeriodizationType.WAVE,
        requiredEquipment = setOf(Equipment.BARBELL, Equipment.BENCH, Equipment.CABLE_MACHINE),
        programDays = listOf(
            ProgramDay(
                dayNumber = 1,
                name = "Day 1 - Squat/Bench",
                targetMuscles = listOf(MuscleGroup.QUADRICEPS, MuscleGroup.CHEST, MuscleGroup.BACK),
                exercises = listOf(
                    ProgramExercise("barbell_squat", "T1: Squat", 5, 3..3, 8..9, null, 180, "85%+ 1RM"),
                    ProgramExercise("bench_press", "T2: Bench Press", 3, 10..10, 7..8, null, 120, "65-75% 1RM"),
                    ProgramExercise("lat_pulldown", "T3: Lat Pulldown", 3, 15..15, null, null, 60)
                )
            ),
            ProgramDay(
                dayNumber = 2,
                name = "Day 2 - OHP/Deadlift",
                targetMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK, MuscleGroup.HAMSTRINGS),
                exercises = listOf(
                    ProgramExercise("overhead_press", "T1: OHP", 5, 3..3, 8..9, null, 180, "85%+ 1RM"),
                    ProgramExercise("deadlift", "T2: Deadlift", 3, 10..10, 7..8, null, 150, "65-75% 1RM"),
                    ProgramExercise("dumbbell_rows", "T3: Dumbbell Rows", 3, 15..15, null, null, 60)
                )
            ),
            ProgramDay(
                dayNumber = 3,
                name = "Day 3 - Bench/Squat",
                targetMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.QUADRICEPS, MuscleGroup.BACK),
                exercises = listOf(
                    ProgramExercise("bench_press", "T1: Bench Press", 5, 3..3, 8..9, null, 180, "85%+ 1RM"),
                    ProgramExercise("barbell_squat", "T2: Squat", 3, 10..10, 7..8, null, 150, "65-75% 1RM"),
                    ProgramExercise("lat_pulldown", "T3: Lat Pulldown", 3, 15..15, null, null, 60)
                )
            ),
            ProgramDay(
                dayNumber = 4,
                name = "Day 4 - Deadlift/OHP",
                targetMuscles = listOf(MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.SHOULDERS),
                exercises = listOf(
                    ProgramExercise("deadlift", "T1: Deadlift", 5, 3..3, 8..9, null, 180, "85%+ 1RM"),
                    ProgramExercise("overhead_press", "T2: OHP", 3, 10..10, 7..8, null, 120, "65-75% 1RM"),
                    ProgramExercise("dumbbell_rows", "T3: Dumbbell Rows", 3, 15..15, null, null, 60)
                )
            )
        ),
        progressionRules = ProgressionRules(
            weightIncrementKg = mapOf(
                MuscleGroup.QUADRICEPS to 2.5f,
                MuscleGroup.BACK to 2.5f,
                MuscleGroup.CHEST to 2.5f,
                MuscleGroup.SHOULDERS to 1.25f
            ),
            progressionTrigger = ProgressionTrigger.ALL_SETS_COMPLETED,
            failureThreshold = 2
        )
    )

    // ========================================================================
    // ADVANCED PPL (6-Day)
    // ========================================================================
    val pplAdvanced = TrainingProgram(
        id = "ppl_advanced",
        name = "Advanced PPL (6-Day)",
        description = "High volume PPL for advanced lifters. Includes intensity techniques like drop sets and rest-pause.",
        author = "Community",
        difficulty = Difficulty.ADVANCED,
        goals = listOf(FitnessGoal.BUILD_MUSCLE),
        durationWeeks = 0,
        daysPerWeek = 6,
        split = WorkoutSplit.PUSH_PULL_LEGS,
        periodization = PeriodizationType.DAILY_UNDULATING,
        requiredEquipment = setOf(Equipment.BARBELL, Equipment.DUMBBELLS, Equipment.CABLE_MACHINE, Equipment.BENCH),
        programDays = listOf(
            ProgramDay(
                dayNumber = 1,
                name = "Push (Heavy)",
                targetMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                exercises = listOf(
                    ProgramExercise("bench_press", "Bench Press", 5, 4..6, 8..9, null, 180),
                    ProgramExercise("overhead_press", "OHP", 4, 5..7, 8..9, null, 150),
                    ProgramExercise("incline_dumbbell_press", "Incline DB Press", 4, 6..8, 8..8, null, 120),
                    ProgramExercise("cable_crossover", "Cable Flyes", 3, 10..12, 8..9, null, 60),
                    ProgramExercise("lateral_raises", "Lateral Raises", 5, 12..15, 8..9, null, 45),
                    ProgramExercise("close_grip_bench", "Close Grip Bench", 3, 8..10, 8..8, null, 90),
                    ProgramExercise("tricep_pushdown", "Pushdowns (dropset)", 3, 10..12, null, null, 60, setType = SetType.DROPSET)
                )
            ),
            ProgramDay(
                dayNumber = 2,
                name = "Pull (Heavy)",
                targetMuscles = listOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
                exercises = listOf(
                    ProgramExercise("deadlift", "Deadlift", 4, 4..6, 8..9, null, 240),
                    ProgramExercise("weighted_pull_ups", "Weighted Pull-ups", 4, 5..7, 8..9, null, 150),
                    ProgramExercise("barbell_rows", "Barbell Rows", 4, 6..8, 8..8, null, 120),
                    ProgramExercise("chest_supported_rows", "Chest Supported Rows", 3, 8..10, 8..8, null, 90),
                    ProgramExercise("face_pulls", "Face Pulls", 4, 15..20, null, null, 45),
                    ProgramExercise("barbell_curl", "Barbell Curls", 3, 8..10, 8..8, null, 60),
                    ProgramExercise("hammer_curls", "Hammer Curls (dropset)", 3, 10..12, null, null, 60, setType = SetType.DROPSET)
                )
            ),
            ProgramDay(
                dayNumber = 3,
                name = "Legs (Heavy)",
                targetMuscles = listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
                exercises = listOf(
                    ProgramExercise("barbell_squat", "Squat", 5, 4..6, 8..9, null, 180),
                    ProgramExercise("romanian_deadlift", "Romanian Deadlift", 4, 6..8, 8..8, null, 150),
                    ProgramExercise("leg_press", "Leg Press", 4, 8..10, 8..9, null, 120),
                    ProgramExercise("leg_curl", "Leg Curls", 4, 10..12, 8..8, null, 60),
                    ProgramExercise("leg_extension", "Leg Extensions", 3, 12..15, 8..9, null, 60),
                    ProgramExercise("calf_raises", "Standing Calf Raises", 5, 10..15, null, null, 45)
                )
            ),
            ProgramDay(
                dayNumber = 4,
                name = "Push (Volume)",
                targetMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                exercises = listOf(
                    ProgramExercise("dumbbell_bench", "Dumbbell Bench", 4, 8..12, 7..8, null, 90),
                    ProgramExercise("machine_press", "Machine Chest Press", 4, 10..12, 7..8, null, 60),
                    ProgramExercise("dumbbell_flyes", "Dumbbell Flyes", 4, 12..15, 7..8, null, 45),
                    ProgramExercise("arnold_press", "Arnold Press", 4, 10..12, 7..8, null, 60),
                    ProgramExercise("rear_delt_fly", "Rear Delt Fly", 4, 15..20, null, null, 30),
                    ProgramExercise("overhead_tricep", "Overhead Tricep", 4, 12..15, 7..8, null, 45),
                    ProgramExercise("tricep_dips", "Dips", 3, 10..15, null, null, 60)
                )
            ),
            ProgramDay(
                dayNumber = 5,
                name = "Pull (Volume)",
                targetMuscles = listOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
                exercises = listOf(
                    ProgramExercise("pull_ups", "Pull-ups", 4, 8..12, 7..8, null, 90),
                    ProgramExercise("t_bar_rows", "T-Bar Rows", 4, 10..12, 7..8, null, 90),
                    ProgramExercise("lat_pulldown", "Lat Pulldown", 4, 10..12, 7..8, null, 60),
                    ProgramExercise("cable_rows", "Cable Rows", 4, 12..15, 7..8, null, 60),
                    ProgramExercise("straight_arm_pulldown", "Straight Arm Pulldown", 3, 15..20, null, null, 45),
                    ProgramExercise("preacher_curl", "Preacher Curls", 3, 10..12, 7..8, null, 60),
                    ProgramExercise("incline_curl", "Incline Curls", 3, 12..15, 7..8, null, 45)
                )
            ),
            ProgramDay(
                dayNumber = 6,
                name = "Legs (Volume)",
                targetMuscles = listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
                exercises = listOf(
                    ProgramExercise("front_squat", "Front Squat", 4, 8..10, 7..8, null, 150),
                    ProgramExercise("hip_thrust", "Hip Thrust", 4, 10..12, 7..8, null, 90),
                    ProgramExercise("walking_lunges", "Walking Lunges", 4, 12..15, null, null, 60),
                    ProgramExercise("leg_curl", "Lying Leg Curl", 4, 12..15, 7..8, null, 45),
                    ProgramExercise("hack_squat", "Hack Squat", 3, 12..15, 7..8, null, 90),
                    ProgramExercise("seated_calf", "Seated Calf Raises", 5, 15..20, null, null, 30)
                )
            )
        ),
        progressionRules = ProgressionRules(
            progressionTrigger = ProgressionTrigger.RPE_BASED
        )
    )

    // ========================================================================
    // ALL PROGRAMS LIST
    // ========================================================================
    val allPrograms: List<TrainingProgram> = listOf(
        strongLifts5x5,
        greyskullLP,
        pushPullLegs,
        upperLower,
        beginnerFullBody,
        gzclp,
        pplAdvanced
    )
}
