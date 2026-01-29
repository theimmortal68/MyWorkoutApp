package com.workoutgen.data.local

import com.workoutgen.domain.model.*

/**
 * Sample exercise data to pre-populate the database.
 * In a production app, this would come from a backend API.
 */
object SampleExerciseData {
    
    val exercises = listOf(
        // CHEST EXERCISES
        Exercise(
            id = "bench_press",
            name = "Barbell Bench Press",
            description = "The king of chest exercises. A compound movement that primarily targets the pectorals.",
            instructions = listOf(
                "Lie flat on bench with feet planted firmly",
                "Grip bar slightly wider than shoulder-width",
                "Lower bar to mid-chest with control",
                "Press bar back up explosively"
            ),
            primaryMuscles = listOf(MuscleGroup.CHEST),
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
            equipmentRequired = listOf(Equipment.BARBELL, Equipment.BENCH),
            type = ExerciseType.COMPOUND,
            difficulty = Difficulty.INTERMEDIATE,
            tips = listOf("Keep wrists straight", "Drive feet into floor")
        ),
        Exercise(
            id = "push_ups",
            name = "Push-Ups",
            description = "Fundamental bodyweight chest exercise.",
            instructions = listOf(
                "Start in plank position",
                "Lower chest to floor",
                "Push back up"
            ),
            primaryMuscles = listOf(MuscleGroup.CHEST),
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS, MuscleGroup.CORE),
            equipmentRequired = listOf(Equipment.NONE),
            type = ExerciseType.COMPOUND,
            difficulty = Difficulty.BEGINNER,
            tips = listOf("Keep core tight", "Don't flare elbows")
        ),
        Exercise(
            id = "dumbbell_flyes",
            name = "Dumbbell Flyes",
            description = "Isolation exercise for chest stretch and contraction.",
            instructions = listOf(
                "Lie on bench with dumbbells above chest",
                "Lower weights in wide arc to sides",
                "Squeeze chest to bring weights together"
            ),
            primaryMuscles = listOf(MuscleGroup.CHEST),
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS),
            equipmentRequired = listOf(Equipment.DUMBBELLS, Equipment.BENCH),
            type = ExerciseType.ISOLATION,
            difficulty = Difficulty.INTERMEDIATE,
            tips = listOf("Keep slight bend in elbows", "Focus on stretch")
        ),
        
        // BACK EXERCISES
        Exercise(
            id = "deadlift",
            name = "Conventional Deadlift",
            description = "Ultimate full-body strength builder targeting posterior chain.",
            instructions = listOf(
                "Stand with feet hip-width, bar over mid-foot",
                "Hinge at hips and grip bar",
                "Brace core and stand up",
                "Lower with control"
            ),
            primaryMuscles = listOf(MuscleGroup.BACK, MuscleGroup.HAMSTRINGS),
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.CORE, MuscleGroup.FOREARMS),
            equipmentRequired = listOf(Equipment.BARBELL),
            type = ExerciseType.COMPOUND,
            difficulty = Difficulty.INTERMEDIATE,
            tips = listOf("Keep bar close to body", "Don't round lower back")
        ),
        Exercise(
            id = "pull_ups",
            name = "Pull-Ups",
            description = "Gold standard for lat development.",
            instructions = listOf(
                "Hang from bar with palms facing away",
                "Pull up until chin clears bar",
                "Lower with control"
            ),
            primaryMuscles = listOf(MuscleGroup.BACK),
            secondaryMuscles = listOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
            equipmentRequired = listOf(Equipment.PULL_UP_BAR),
            type = ExerciseType.COMPOUND,
            difficulty = Difficulty.INTERMEDIATE,
            tips = listOf("Don't swing", "Drive elbows down")
        ),
        Exercise(
            id = "barbell_rows",
            name = "Barbell Bent-Over Rows",
            description = "Horizontal pulling for back thickness.",
            instructions = listOf(
                "Hinge forward holding barbell",
                "Pull bar to lower chest",
                "Lower with control"
            ),
            primaryMuscles = listOf(MuscleGroup.BACK),
            secondaryMuscles = listOf(MuscleGroup.BICEPS, MuscleGroup.CORE),
            equipmentRequired = listOf(Equipment.BARBELL),
            type = ExerciseType.COMPOUND,
            difficulty = Difficulty.INTERMEDIATE,
            tips = listOf("Keep core braced", "Don't use momentum")
        ),
        Exercise(
            id = "lat_pulldown",
            name = "Lat Pulldown",
            description = "Machine lat exercise, great for pull-up progression.",
            instructions = listOf(
                "Sit at machine with thighs secured",
                "Pull bar to upper chest",
                "Control weight back up"
            ),
            primaryMuscles = listOf(MuscleGroup.BACK),
            secondaryMuscles = listOf(MuscleGroup.BICEPS),
            equipmentRequired = listOf(Equipment.LAT_PULLDOWN),
            type = ExerciseType.COMPOUND,
            difficulty = Difficulty.BEGINNER,
            tips = listOf("Don't lean too far back", "Squeeze lats")
        ),
        
        // LEG EXERCISES
        Exercise(
            id = "barbell_squat",
            name = "Barbell Back Squat",
            description = "King of leg exercises for overall lower body development.",
            instructions = listOf(
                "Position bar on upper back",
                "Squat down until thighs parallel",
                "Drive through heels to stand"
            ),
            primaryMuscles = listOf(MuscleGroup.QUADRICEPS),
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
            equipmentRequired = listOf(Equipment.BARBELL),
            type = ExerciseType.COMPOUND,
            difficulty = Difficulty.INTERMEDIATE,
            tips = listOf("Keep chest up", "Knees track over toes")
        ),
        Exercise(
            id = "lunges",
            name = "Walking Lunges",
            description = "Unilateral leg exercise for strength and balance.",
            instructions = listOf(
                "Step forward into lunge",
                "Both knees bend to 90 degrees",
                "Push through front foot to continue"
            ),
            primaryMuscles = listOf(MuscleGroup.QUADRICEPS, MuscleGroup.GLUTES),
            secondaryMuscles = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
            equipmentRequired = listOf(Equipment.NONE),
            type = ExerciseType.COMPOUND,
            difficulty = Difficulty.BEGINNER,
            tips = listOf("Keep torso upright", "Control the movement")
        ),
        Exercise(
            id = "romanian_deadlift",
            name = "Romanian Deadlift",
            description = "Hip hinge for hamstrings and glutes.",
            instructions = listOf(
                "Stand holding barbell at hips",
                "Push hips back, lowering bar along legs",
                "Drive hips forward to stand"
            ),
            primaryMuscles = listOf(MuscleGroup.HAMSTRINGS),
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.BACK),
            equipmentRequired = listOf(Equipment.BARBELL),
            type = ExerciseType.COMPOUND,
            difficulty = Difficulty.INTERMEDIATE,
            tips = listOf("Keep back flat", "Feel hamstring stretch")
        ),
        Exercise(
            id = "goblet_squat",
            name = "Goblet Squat",
            description = "Beginner-friendly squat variation with dumbbell or kettlebell.",
            instructions = listOf(
                "Hold weight at chest level",
                "Squat down keeping torso upright",
                "Stand back up"
            ),
            primaryMuscles = listOf(MuscleGroup.QUADRICEPS),
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.CORE),
            equipmentRequired = listOf(Equipment.DUMBBELLS),
            type = ExerciseType.COMPOUND,
            difficulty = Difficulty.BEGINNER,
            tips = listOf("Elbows between knees at bottom", "Keep heels down")
        ),
        Exercise(
            id = "calf_raises",
            name = "Standing Calf Raises",
            description = "Isolation exercise for calf development.",
            instructions = listOf(
                "Stand on edge of step or platform",
                "Rise up on toes as high as possible",
                "Lower heels below platform level"
            ),
            primaryMuscles = listOf(MuscleGroup.CALVES),
            secondaryMuscles = emptyList(),
            equipmentRequired = listOf(Equipment.NONE),
            type = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER,
            tips = listOf("Full range of motion", "Pause at top")
        ),
        
        // SHOULDER EXERCISES
        Exercise(
            id = "overhead_press",
            name = "Overhead Press",
            description = "Fundamental vertical press for shoulder development.",
            instructions = listOf(
                "Stand with bar at shoulder level",
                "Press bar overhead until arms extended",
                "Lower with control"
            ),
            primaryMuscles = listOf(MuscleGroup.SHOULDERS),
            secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.CORE),
            equipmentRequired = listOf(Equipment.BARBELL),
            type = ExerciseType.COMPOUND,
            difficulty = Difficulty.INTERMEDIATE,
            tips = listOf("Squeeze glutes for stability", "Don't lean back")
        ),
        Exercise(
            id = "lateral_raises",
            name = "Lateral Raises",
            description = "Isolation for lateral deltoids and shoulder width.",
            instructions = listOf(
                "Stand with dumbbells at sides",
                "Raise arms to sides until parallel",
                "Lower with control"
            ),
            primaryMuscles = listOf(MuscleGroup.SHOULDERS),
            secondaryMuscles = emptyList(),
            equipmentRequired = listOf(Equipment.DUMBBELLS),
            type = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER,
            tips = listOf("Don't swing", "Lead with elbows")
        ),
        Exercise(
            id = "face_pulls",
            name = "Face Pulls",
            description = "Rear delt and rotator cuff exercise for shoulder health.",
            instructions = listOf(
                "Set cable at face height",
                "Pull rope to face, separating hands",
                "Squeeze shoulder blades together"
            ),
            primaryMuscles = listOf(MuscleGroup.SHOULDERS),
            secondaryMuscles = listOf(MuscleGroup.BACK),
            equipmentRequired = listOf(Equipment.CABLE_MACHINE),
            type = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER,
            tips = listOf("External rotate at end", "Don't use too much weight")
        ),
        
        // ARM EXERCISES
        Exercise(
            id = "barbell_curl",
            name = "Barbell Bicep Curl",
            description = "Classic bicep builder.",
            instructions = listOf(
                "Stand holding barbell underhand",
                "Curl weight up by flexing elbows",
                "Lower with control"
            ),
            primaryMuscles = listOf(MuscleGroup.BICEPS),
            secondaryMuscles = listOf(MuscleGroup.FOREARMS),
            equipmentRequired = listOf(Equipment.BARBELL),
            type = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER,
            tips = listOf("Keep elbows pinned", "Don't swing")
        ),
        Exercise(
            id = "hammer_curls",
            name = "Hammer Curls",
            description = "Bicep curl variation targeting brachialis.",
            instructions = listOf(
                "Hold dumbbells with neutral grip",
                "Curl weights up",
                "Lower with control"
            ),
            primaryMuscles = listOf(MuscleGroup.BICEPS),
            secondaryMuscles = listOf(MuscleGroup.FOREARMS),
            equipmentRequired = listOf(Equipment.DUMBBELLS),
            type = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER,
            tips = listOf("Thumbs up position", "Controlled movement")
        ),
        Exercise(
            id = "tricep_pushdown",
            name = "Tricep Pushdown",
            description = "Cable isolation for triceps.",
            instructions = listOf(
                "Stand at cable machine",
                "Push handle down until arms straight",
                "Control weight back up"
            ),
            primaryMuscles = listOf(MuscleGroup.TRICEPS),
            secondaryMuscles = emptyList(),
            equipmentRequired = listOf(Equipment.CABLE_MACHINE),
            type = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER,
            tips = listOf("Keep elbows at sides", "Squeeze at bottom")
        ),
        Exercise(
            id = "tricep_dips",
            name = "Tricep Dips",
            description = "Bodyweight tricep exercise.",
            instructions = listOf(
                "Position on dip bars or bench",
                "Lower body by bending elbows",
                "Press back up"
            ),
            primaryMuscles = listOf(MuscleGroup.TRICEPS),
            secondaryMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS),
            equipmentRequired = listOf(Equipment.NONE),
            type = ExerciseType.COMPOUND,
            difficulty = Difficulty.INTERMEDIATE,
            tips = listOf("Keep elbows close for tricep focus", "Don't go too deep")
        ),
        
        // CORE EXERCISES
        Exercise(
            id = "plank",
            name = "Plank",
            description = "Isometric core stability exercise.",
            instructions = listOf(
                "Hold forearm plank position",
                "Keep body in straight line",
                "Engage core and glutes"
            ),
            primaryMuscles = listOf(MuscleGroup.CORE),
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS),
            equipmentRequired = listOf(Equipment.NONE),
            type = ExerciseType.ISOMETRIC,
            difficulty = Difficulty.BEGINNER,
            tips = listOf("Don't let hips sag", "Breathe steadily")
        ),
        Exercise(
            id = "hanging_leg_raises",
            name = "Hanging Leg Raises",
            description = "Advanced lower ab exercise.",
            instructions = listOf(
                "Hang from pull-up bar",
                "Raise legs to parallel",
                "Lower with control"
            ),
            primaryMuscles = listOf(MuscleGroup.CORE),
            secondaryMuscles = listOf(MuscleGroup.FOREARMS),
            equipmentRequired = listOf(Equipment.PULL_UP_BAR),
            type = ExerciseType.ISOLATION,
            difficulty = Difficulty.ADVANCED,
            tips = listOf("Start with knee raises", "Don't swing")
        ),
        Exercise(
            id = "russian_twist",
            name = "Russian Twist",
            description = "Rotational core exercise.",
            instructions = listOf(
                "Sit with knees bent, lean back slightly",
                "Rotate torso side to side",
                "Touch floor beside hips"
            ),
            primaryMuscles = listOf(MuscleGroup.CORE),
            secondaryMuscles = emptyList(),
            equipmentRequired = listOf(Equipment.NONE),
            type = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER,
            tips = listOf("Keep core engaged", "Control the rotation")
        ),
        Exercise(
            id = "dead_bug",
            name = "Dead Bug",
            description = "Core stability exercise protecting the lower back.",
            instructions = listOf(
                "Lie on back with arms up and knees bent 90 degrees",
                "Lower opposite arm and leg toward floor",
                "Return and repeat other side"
            ),
            primaryMuscles = listOf(MuscleGroup.CORE),
            secondaryMuscles = emptyList(),
            equipmentRequired = listOf(Equipment.NONE),
            type = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER,
            tips = listOf("Keep lower back pressed to floor", "Move slowly")
        ),
        
        // GLUTE EXERCISES
        Exercise(
            id = "hip_thrust",
            name = "Hip Thrust",
            description = "Primary glute builder.",
            instructions = listOf(
                "Sit with upper back against bench",
                "Drive hips up squeezing glutes",
                "Lower with control"
            ),
            primaryMuscles = listOf(MuscleGroup.GLUTES),
            secondaryMuscles = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
            equipmentRequired = listOf(Equipment.BARBELL, Equipment.BENCH),
            type = ExerciseType.COMPOUND,
            difficulty = Difficulty.INTERMEDIATE,
            tips = listOf("Chin tucked at top", "Full hip extension")
        ),
        Exercise(
            id = "glute_bridge",
            name = "Glute Bridge",
            description = "Bodyweight glute activation.",
            instructions = listOf(
                "Lie on back with knees bent",
                "Drive hips up squeezing glutes",
                "Lower with control"
            ),
            primaryMuscles = listOf(MuscleGroup.GLUTES),
            secondaryMuscles = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
            equipmentRequired = listOf(Equipment.NONE),
            type = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER,
            tips = listOf("Squeeze glutes at top", "Don't hyperextend back")
        )
    )
}
