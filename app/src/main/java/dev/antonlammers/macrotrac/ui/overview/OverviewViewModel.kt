package dev.antonlammers.macrotrac.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.antonlammers.macrotrac.domain.model.DailyGoal
import dev.antonlammers.macrotrac.domain.model.FoodEntry
import dev.antonlammers.macrotrac.domain.repository.FoodEntryRepository
import dev.antonlammers.macrotrac.domain.repository.GoalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val foodEntryRepository: FoodEntryRepository,
    private val goalRepository: GoalRepository,
) : ViewModel() {

    private val _date = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<OverviewUiState> = _date
        .flatMapLatest { date ->
            combine(
                foodEntryRepository.entriesForDate(date),
                goalRepository.goal(),
            ) { entries, goal ->
                OverviewUiState(entries = entries, goal = goal, date = date)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OverviewUiState(),
        )

    fun previousDay() = _date.update { it.minusDays(1) }
    fun nextDay() = _date.update { it.plusDays(1) }
    fun goToToday() = _date.update { LocalDate.now() }

    fun delete(id: Long) {
        viewModelScope.launch { foodEntryRepository.delete(id) }
    }
}

data class OverviewUiState(
    val entries: List<FoodEntry> = emptyList(),
    val goal: DailyGoal = DailyGoal(),
    val date: LocalDate = LocalDate.now(),
) {
    val totalKcal get() = entries.sumOf { it.kcal }
    val totalProtein get() = entries.sumOf { it.proteinG }
    val totalCarbs get() = entries.sumOf { it.carbsG }
    val totalFat get() = entries.sumOf { it.fatG }
    val totalSugar get() = entries.sumOf { it.sugarG }
    val totalFiber get() = entries.sumOf { it.fiberG }
}
