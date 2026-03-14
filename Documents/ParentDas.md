# SECTION 2: PARENT DASHBOARD
## Simple In-App View

---

### 2.1 DESIGN RULE

Parent dashboard = 1 screen, no navigation.
Everything a parent needs to validate "is my kid learning?" 
fits in a single scrollable screen.
No graphs, no charts, no pagination for MVP.

---

### 2.2 SCREEN LAYOUT

```
PARENT DASHBOARD SCREEN — Single scroll view
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌─────────────────────────────────────────┐
│  Today's Summary                        │
│  ─────────────────────────────────────  │
│  Quizzes shown      :  8                │
│  Answered           :  6    (75%)       │
│  Correct            :  4    (67%)       │
│  Dismissed          :  2                │
│  Avg response time  :  9.4 sec          │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  This Week                              │
│  ─────────────────────────────────────  │
│  Mon  ████████░░  8/10 correct          │
│  Tue  ██████░░░░  6/10 correct          │
│  Wed  ░░░░░░░░░░  No session            │
│  Thu  █████████░  9/10 correct  ← today │
│  Fri  ──                                │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  By Subject — Today                     │
│  ─────────────────────────────────────  │
│  Math        :  3 correct / 4 shown     │
│  English     :  1 correct / 2 shown     │
│  General     :  0 correct / 2 shown     │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  Recent Attempts (last 5)               │
│  ─────────────────────────────────────  │
│  ✓  What is 6×7?          3.2s  Math    │
│  ✗  Capital of France?    12s   General │
│  ✓  Fill: The ___ star    7.8s  English │
│  —  (dismissed)                         │
│  ✓  Match animals         15s   Science │
└─────────────────────────────────────────┘

[ DISABLE OVERLAY  ▶ ]   [ SEND FEEDBACK ]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### 2.3 COMPOSABLE FUNCTION DEFINITIONS

```kotlin
@Composable
fun ParentDashboardScreen(
    viewModel: ParentDashboardViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    LazyColumn {
        item { TodaySummaryCard(uiState.todaySummary) }
        item { WeekBarCard(uiState.weekData) }
        item { SubjectBreakdownCard(uiState.todayBySubject) }
        item { RecentAttemptsCard(uiState.recentAttempts) }
        item { ActionButtons(onDisable = viewModel::showPinPrompt,
                             onFeedback = viewModel::openFeedback) }
    }
}
```

```kotlin
@Composable
fun TodaySummaryCard(summary: TodaySummary) {
    // Shows: shown, answered, correct, dismissed, avg response time
    // All from Room query: SELECT * FROM quiz_attempts WHERE date(shown_at) = today
    // No network call — all local Room data
    Card { ... }
}
```

```kotlin
@Composable
fun WeekBarCard(weekData: List<DaySummary>) {
    // Simple horizontal bar per day using Box + fillMaxWidth(fraction)
    // fraction = correctCount / shownCount
    // No chart library needed — pure Compose Box layout
    // 7 rows max (Mon–Sun)
    Card { ... }
}
```

```kotlin
@Composable
fun SubjectBreakdownCard(subjects: List<SubjectStat>) {
    // Simple text rows: "Math: 3 correct / 4 shown"
    // Derived from quiz_attempts JOIN quiz_questions ON category
    // Local Room query only
    Card { ... }
}
```

```kotlin
@Composable
fun RecentAttemptsCard(attempts: List<AttemptPreview>) {
    // Last 5 attempts from Room, newest first
    // Each row: icon (✓/✗/—) + question_text truncated + response_time + category
    Card { ... }
}
```

---

### 2.4 VIEWMODEL + DATA QUERIES

```kotlin
class ParentDashboardViewModel(private val repo: QuizRepository) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repo.getTodaySummary(),
        repo.getWeekSummary(),
        repo.getTodayBySubject(),
        repo.getRecentAttempts(limit = 5)
    ) { today, week, subjects, recent ->
        DashboardUiState(today, week, subjects, recent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState.Empty)

}
```

All queries are Room Flows — reactive, no polling, no network.

```kotlin
// Room DAO queries needed:

@Query("""
    SELECT COUNT(*) as shown,
           SUM(CASE WHEN dismissed = 0 THEN 1 ELSE 0 END) as answered,
           SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) as correct,
           SUM(CASE WHEN dismissed = 1 THEN 1 ELSE 0 END) as dismissed,
           AVG(response_time_ms) as avg_response_ms
    FROM quiz_attempts
    WHERE date(shown_at / 1000, 'unixepoch') = date('now')
""")
fun getTodaySummary(): Flow<TodaySummary>

@Query("""
    SELECT date(shown_at / 1000, 'unixepoch') as day,
           COUNT(*) as shown,
           SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) as correct
    FROM quiz_attempts
    WHERE shown_at >= :weekStartMs
    GROUP BY day ORDER BY day ASC
""")
fun getWeekSummary(weekStartMs: Long): Flow<List<DaySummary>>

@Query("""
    SELECT q.category, COUNT(*) as shown,
           SUM(CASE WHEN a.is_correct = 1 THEN 1 ELSE 0 END) as correct
    FROM quiz_attempts a
    JOIN quiz_questions q ON a.question_id = q.id
    WHERE date(a.shown_at / 1000, 'unixepoch') = date('now')
    GROUP BY q.category
""")
fun getTodayBySubject(): Flow<List<SubjectStat>>

@Query("""
    SELECT a.*, q.question_text, q.category
    FROM quiz_attempts a
    JOIN quiz_questions q ON a.question_id = q.id
    ORDER BY a.shown_at DESC LIMIT :limit
""")
fun getRecentAttempts(limit: Int): Flow<List<AttemptPreview>>
```

---

---