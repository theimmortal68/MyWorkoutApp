package com.workoutgen.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutgen.domain.model.*
import com.workoutgen.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            userProfileRepository.getUserProfile().collect { profile ->
                _uiState.update { it.copy(profile = profile ?: UserProfile(), isLoading = false) }
            }
        }
    }
    
    fun setExperience(level: Difficulty) {
        updateProfile { it.copy(experienceLevel = level) }
    }
    
    fun toggleGoal(goal: FitnessGoal) {
        updateProfile { profile ->
            val newGoals = if (goal in profile.fitnessGoals) {
                profile.fitnessGoals - goal
            } else {
                profile.fitnessGoals + goal
            }
            profile.copy(fitnessGoals = newGoals.ifEmpty { listOf(FitnessGoal.GENERAL_FITNESS) })
        }
    }
    
    fun toggleEquipment(equipment: Equipment) {
        updateProfile { profile ->
            val newEquipment = if (equipment in profile.availableEquipment) {
                profile.availableEquipment - equipment
            } else {
                profile.availableEquipment + equipment
            }
            profile.copy(availableEquipment = newEquipment.ifEmpty { setOf(Equipment.NONE) })
        }
    }
    
    fun setDuration(minutes: Int) {
        updateProfile { it.copy(preferredWorkoutDuration = minutes) }
    }
    
    fun setWorkoutDays(days: Int) {
        updateProfile { it.copy(workoutDaysPerWeek = days) }
    }
    
    fun setSplit(split: WorkoutSplit) {
        updateProfile { it.copy(preferredSplit = split) }
    }
    
    fun setMetricUnits(useMetric: Boolean) {
        updateProfile { it.copy(useMetricUnits = useMetric) }
    }

    fun addLimitation(limitation: UserLimitation) {
        updateProfile { profile ->
            // Avoid duplicates
            if (profile.physicalLimitations.any { it.limitation == limitation.limitation }) {
                profile
            } else {
                profile.copy(physicalLimitations = profile.physicalLimitations + limitation)
            }
        }
    }

    fun removeLimitation(limitation: PhysicalLimitation) {
        updateProfile { profile ->
            profile.copy(
                physicalLimitations = profile.physicalLimitations.filter {
                    it.limitation != limitation
                }
            )
        }
    }

    private fun updateProfile(update: (UserProfile) -> UserProfile) {
        viewModelScope.launch {
            val currentProfile = _uiState.value.profile
            val updatedProfile = update(currentProfile)
            userProfileRepository.updateProfile(updatedProfile)
        }
    }
}
