package com.reeled.quizoverlay.ui.dashboard

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
)

class DashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(sampleUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun showPinPrompt() {
        // Hook for disable overlay flow.
    }

    fun openFeedback() {
        // Hook for feedback flow.
    }

    private fun sampleUiState(): DashboardUiState {
        return DashboardUiState(
            todaySummary = TodaySummary(
                shown = 8,
                answered = 6,
                correct = 4,
                dismissed = 2,
                avgResponseSeconds = 9.4f,
            ),
            weekData = listOf(
                DaySummary(dayLabel = "Mon", shown = 10, correct = 8),
                DaySummary(dayLabel = "Tue", shown = 10, correct = 6),
                DaySummary(dayLabel = "Wed", shown = 0, correct = 0),
                DaySummary(dayLabel = "Thu", shown = 10, correct = 9, isToday = true),
                DaySummary(dayLabel = "Fri", shown = 0, correct = 0),
                DaySummary(dayLabel = "Sat", shown = 0, correct = 0),
                DaySummary(dayLabel = "Sun", shown = 0, correct = 0),
            ),
            todayBySubject = listOf(
                SubjectStat(subject = "Math", correct = 3, shown = 4),
                SubjectStat(subject = "English", correct = 1, shown = 2),
                SubjectStat(subject = "General", correct = 0, shown = 2),
            ),
            recentAttempts = listOf(
                AttemptPreview(
                    questionText = "What is 6×7?",
                    responseSeconds = 3.2f,
                    subject = "Math",
                    status = AttemptStatus.CORRECT,
                ),
                AttemptPreview(
                    questionText = "Capital of France?",
                    responseSeconds = 12f,
                    subject = "General",
                    status = AttemptStatus.WRONG,
                ),
                AttemptPreview(
                    questionText = "Fill: The ___ star",
                    responseSeconds = 7.8f,
                    subject = "English",
                    status = AttemptStatus.CORRECT,
                ),
                AttemptPreview(
                    questionText = "Dismissed",
                    responseSeconds = 0f,
                    subject = "—",
                    status = AttemptStatus.DISMISSED,
                ),
                AttemptPreview(
                    questionText = "Match animals",
                    responseSeconds = 15f,
                    subject = "Science",
                    status = AttemptStatus.CORRECT,
                ),
            ),
        )
    }
}
