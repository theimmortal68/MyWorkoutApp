package com.workoutgen.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.workoutgen.data.local.ExerciseLibrary
import com.workoutgen.data.local.WorkoutDatabase
import com.workoutgen.data.local.dao.*
import com.workoutgen.data.local.entity.toEntity
import com.workoutgen.data.repository.*
import com.workoutgen.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        exerciseDaoProvider: Provider<ExerciseDao>
    ): WorkoutDatabase {
        return Room.databaseBuilder(
            context,
            WorkoutDatabase::class.java,
            WorkoutDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Seed the database with sample exercises on first creation
                    CoroutineScope(Dispatchers.IO).launch {
                        val exerciseDao = exerciseDaoProvider.get()
                        val exercises = ExerciseLibrary.exercises.map { it.toEntity() }
                        exerciseDao.insertExercises(exercises)
                    }
                }
            })
            .build()
    }
    
    @Provides
    @Singleton
    fun provideExerciseDao(database: WorkoutDatabase): ExerciseDao = database.exerciseDao()
    
    @Provides
    @Singleton
    fun provideWorkoutDao(database: WorkoutDatabase): WorkoutDao = database.workoutDao()
    
    @Provides
    @Singleton
    fun provideWorkoutLogDao(database: WorkoutDatabase): WorkoutLogDao = database.workoutLogDao()
    
    @Provides
    @Singleton
    fun provideUserProfileDao(database: WorkoutDatabase): UserProfileDao = database.userProfileDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindExerciseRepository(
        impl: ExerciseRepositoryImpl
    ): ExerciseRepository
    
    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(
        impl: WorkoutRepositoryImpl
    ): WorkoutRepository
    
    @Binds
    @Singleton
    abstract fun bindWorkoutLogRepository(
        impl: WorkoutLogRepositoryImpl
    ): WorkoutLogRepository
    
    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(
        impl: UserProfileRepositoryImpl
    ): UserProfileRepository
}
