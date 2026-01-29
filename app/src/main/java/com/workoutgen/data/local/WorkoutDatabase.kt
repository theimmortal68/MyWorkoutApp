package com.workoutgen.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.workoutgen.data.local.dao.*
import com.workoutgen.data.local.entity.*

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutLogEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase : RoomDatabase() {
    
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun userProfileDao(): UserProfileDao
    
    companion object {
        const val DATABASE_NAME = "workout_gen_db"
    }
}
