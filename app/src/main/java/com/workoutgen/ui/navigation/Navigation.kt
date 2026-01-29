package com.workoutgen.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.workoutgen.ui.screens.analytics.AnalyticsScreen
import com.workoutgen.ui.screens.exercise.ExerciseDetailScreen
import com.workoutgen.ui.screens.exercise.ExerciseLibraryScreen
import com.workoutgen.ui.screens.home.HomeScreen
import com.workoutgen.ui.screens.history.HistoryScreen
import com.workoutgen.ui.screens.profile.ProfileScreen
import com.workoutgen.ui.screens.programs.AIGenerateProgramScreen
import com.workoutgen.ui.screens.programs.ProgramsScreen
import com.workoutgen.ui.screens.workout.ActiveWorkoutScreen
import com.workoutgen.ui.screens.workout.GenerateWorkoutScreen
import com.workoutgen.ui.screens.workout.WorkoutDetailScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object GenerateWorkout : Screen("generate_workout")
    object WorkoutDetail : Screen("workout/{workoutId}") {
        fun createRoute(workoutId: String) = "workout/$workoutId"
    }
    object ActiveWorkout : Screen("active_workout/{workoutId}") {
        fun createRoute(workoutId: String) = "active_workout/$workoutId"
    }
    object ExerciseLibrary : Screen("exercises")
    object ExerciseDetail : Screen("exercise/{exerciseId}") {
        fun createRoute(exerciseId: String) = "exercise/$exerciseId"
    }
    object History : Screen("history")
    object Profile : Screen("profile")
    object Analytics : Screen("analytics")
    object Programs : Screen("programs")
    object GenerateProgram : Screen("generate_program")
}

@Composable
fun WorkoutGenNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // Home - Dashboard with quick actions
        composable(Screen.Home.route) {
            HomeScreen(
                onGenerateWorkout = { navController.navigate(Screen.GenerateWorkout.route) },
                onViewHistory = { navController.navigate(Screen.History.route) },
                onViewProfile = { navController.navigate(Screen.Profile.route) },
                onViewExercises = { navController.navigate(Screen.ExerciseLibrary.route) },
                onViewAnalytics = { navController.navigate(Screen.Analytics.route) },
                onWorkoutClick = { workoutId ->
                    navController.navigate(Screen.WorkoutDetail.createRoute(workoutId))
                }
            )
        }
        
        // Generate Workout - AI-powered workout generation
        composable(Screen.GenerateWorkout.route) {
            GenerateWorkoutScreen(
                onWorkoutGenerated = { workoutId ->
                    navController.navigate(Screen.WorkoutDetail.createRoute(workoutId)) {
                        popUpTo(Screen.GenerateWorkout.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Workout Detail - View generated workout before starting
        composable(
            route = Screen.WorkoutDetail.route,
            arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString("workoutId") ?: return@composable
            WorkoutDetailScreen(
                workoutId = workoutId,
                onStartWorkout = {
                    navController.navigate(Screen.ActiveWorkout.createRoute(workoutId))
                },
                onBack = { navController.popBackStack() },
                onExerciseClick = { exerciseId ->
                    navController.navigate(Screen.ExerciseDetail.createRoute(exerciseId))
                }
            )
        }
        
        // Active Workout - Track workout in progress
        composable(
            route = Screen.ActiveWorkout.route,
            arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString("workoutId") ?: return@composable
            ActiveWorkoutScreen(
                workoutId = workoutId,
                onWorkoutComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Exercise Library - Browse all exercises
        composable(Screen.ExerciseLibrary.route) {
            ExerciseLibraryScreen(
                onExerciseClick = { exerciseId ->
                    navController.navigate(Screen.ExerciseDetail.createRoute(exerciseId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Exercise Detail - View exercise instructions
        composable(
            route = Screen.ExerciseDetail.route,
            arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: return@composable
            ExerciseDetailScreen(
                exerciseId = exerciseId,
                onBack = { navController.popBackStack() }
            )
        }
        
        // History - View past workouts
        composable(Screen.History.route) {
            HistoryScreen(
                onLogClick = { logId ->
                    // Could navigate to log detail
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Profile - User settings and equipment
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Analytics - View stats and progress
        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Programs - Browse training programs
        composable(Screen.Programs.route) {
            ProgramsScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreateAIProgram = { navController.navigate(Screen.GenerateProgram.route) }
            )
        }

        // Generate Program - AI program generator
        composable(Screen.GenerateProgram.route) {
            AIGenerateProgramScreen(
                onProgramStarted = {
                    navController.navigate(Screen.Programs.route) {
                        popUpTo(Screen.GenerateProgram.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
