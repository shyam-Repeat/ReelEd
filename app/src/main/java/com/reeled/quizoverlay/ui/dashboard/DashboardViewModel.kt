package com.reeled.quizoverlay.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reeled.quizoverlay.data.local.dao.QuizAttemptDao
import com.reeled.quizoverlay.data.repository.QuizRepository
import com.reeled.quizoverlay.util.TimeUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar


data class TodaySummary(
    val shown: Int = 0,
    val answered: Int = 0,
    val correct: Int = 0,
    val dismissed: Int = 0,
    val avgResponseSeconds: Float = 0f,
)

data class DaySummary(
    val dayLabel: String,
    val shown: Int,
    val correct: Int,
    val isToday: Boolean = false,
) {
    val progress: Float = if (shown <= 0) 0f else correct.toFloat() / shown.toFloat()
    val hasSession: Boolean = shown > 0
}

data class SubjectStat(
    val subject: String,
    val correct: Int,
    val shown: Int,
)

enum class AttemptStatus {
    CORRECT,
    WRONG,
    DISMISSED,
}

data class AttemptPreview(
    val questionText: String,
    val responseSeconds: Float,
    val subject: String,
    val status: AttemptStatus,
)

data class DashboardUiState(
    val todaySummary: TodaySummary = TodaySummary(),
    val weekData: List<DaySummary> = emptyList(),
    val todayBySubject: List<SubjectStat> = emptyList(),
    val recentAttempts: List<AttemptPreview> = emptyList(),
    val isOverlayEnabled: Boolean = true,
)

sealed class DashboardAction {
    object ShowPinPrompt : DashboardAction()
    object OpenFeedback : DashboardAction()
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuizRepository(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<DashboardAction>()
    val actions: SharedFlow<DashboardAction> = _actions.asSharedFlow()

    init {
        refreshDashboard()
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            val startOfDay = TimeUtils.getStartOfDay()
            val shown = repository.getShownCountToday(startOfDay)
            val answered = repository.getAnsweredCountToday(startOfDay)
            val correct = repository.getCorrectCountToday(startOfDay)
            val dismissed = (shown - answered).coerceAtLeast(0)
            val avgMs = repository.getAvgResponseTimeToday(startOfDay) ?: 0f

            val recent = repository.getRecentAttemptDetails(limit = 200)
            val todayRecent = recent.filter { it.shownAt >= startOfDay }

            val bySubject = todayRecent
                .groupBy { it.subject }
                .map { (subject, attempts) ->
                    SubjectStat(
                        subject = subject,
                        shown = attempts.size,
                        correct = attempts.count { it.isCorrect && !it.dismissed }
                    )
                }
                .sortedByDescending { it.shown }

            val recentPreview = recent.take(20).map { attempt ->
                AttemptPreview(
                    questionText = attempt.questionText,
                    responseSeconds = attempt.responseTimeMs / 1000f,
                    subject = attempt.subject,
                    status = when {
                        attempt.dismissed -> AttemptStatus.DISMISSED
                        attempt.isCorrect -> AttemptStatus.CORRECT
                        else -> AttemptStatus.WRONG
                    }
                )
            }

            _uiState.value = _uiState.value.copy(
                todaySummary = TodaySummary(
                    shown = shown,
                    answered = answered,
                    correct = correct,
                    dismissed = dismissed,
                    avgResponseSeconds = avgMs / 1000f,
                ),
                weekData = buildWeekSummaries(recent),
                todayBySubject = bySubject,
                recentAttempts = recentPreview,
            )
        }
    }

    fun showPinPrompt() {
        viewModelScope.launch {
            _actions.emit(DashboardAction.ShowPinPrompt)
        }
    }

    fun openFeedback() {
        viewModelScope.launch {
            _actions.emit(DashboardAction.OpenFeedback)
        }
    }

    private fun buildWeekSummaries(recent: List<QuizAttemptDao.RecentAttemptDetail>): List<DaySummary> {
        val now = Calendar.getInstance()
        val dayStarts = mutableListOf<Pair<String, Long>>()
        repeat(7) { index ->
            val cal = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -(6 - index))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayLabel = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, java.util.Locale.getDefault()) ?: "-"
            dayStarts += dayLabel to cal.timeInMillis
        }

        val dayRanges = dayStarts.mapIndexed { index, (label, start) ->
            val end = dayStarts.getOrNull(index + 1)?.second ?: Long.MAX_VALUE
            Triple(label, start, end)
        }

        return dayRanges.mapIndexed { idx, (label, start, end) ->
            val attempts = recent.filter { it.shownAt in start until end }
            DaySummary(
                dayLabel = label,
                shown = attempts.size,
                correct = attempts.count { it.isCorrect && !it.dismissed },
                isToday = idx == dayRanges.lastIndex,
            )
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(application) as T
                }
            }
    }
}
            )
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(application) as T
                }
            }
    }
}
