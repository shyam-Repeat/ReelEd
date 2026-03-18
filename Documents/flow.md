The Trigger Flow (YouTube Example)


  When the OverlayForegroundService is running, it wakes up every 10 seconds (POLLING_INTERVAL_MS) and asks the TriggerEngine to evaluate if a quiz should be
  shown.

  When the user opens YouTube, here is the gauntlet of conditions the engine checks:


   1. Overlay Active Check: Is a quiz already currently showing? If yes, skip.
     (Dev Mode Log: EVENT • trigger_skip → {"reason":"overlay_active"})


   2. Foreground App Check: Is the current app in the TARGET_PACKAGES list (which includes com.google.android.youtube)? If not, skip and clear the session.
     (Dev Mode Log: EVENT • trigger_skip → {"reason":"target_app_not_foreground"})

   3. Daily Cap Check: Has the user already seen 6 quizzes today (MAX_DAILY)? If yes, skip.
     (Dev Mode Log: EVENT • trigger_skip → {"reason":"daily_cap_reached"})


   4. Cooldown Check: Was a quiz answered or dismissed very recently? The base cooldown is 3 minutes (COOLDOWN_MS). This increases by 5 minutes if they
      dismissed the last quiz, or decreases by 1 minute if they answered the last one correctly. If we are still in this cooldown window, skip.
     (Dev Mode Log: EVENT • trigger_skip → {"reason":"cooldown_active (150s)"})


   5. Warmup Check: Has the user been watching YouTube for at least 30 seconds (WARMUP_MS) during this session? We don't want to instantly hit them with a quiz
      the millisecond they open the app.
     (Dev Mode Log: EVENT • trigger_skip → {"reason":"warmup_not_done (25s)"})


   6. Interval Check: Has it been at least 2 minutes (INTERVAL_MS) since the last quiz was shown during this specific viewing session? If no, skip.
     (Dev Mode Log: EVENT • trigger_skip → {"reason":"interval_not_elapsed (110s)"})


   7. Interrupt Score (Video Playback): The engine checks if the user is actively watching a video with sound on and phone muted (a "poor" interrupt moment
      where score = 0). If they still have plenty of quizzes left in their daily quota, it decides to politely wait for a better moment and skips.
     (Dev Mode Log: EVENT • trigger_skip → {"reason":"poor_interrupt_moment"})

   8. Content Availability: Do we have cached questions in the local database that haven't been shown yet? If no, skip.
     (Dev Mode Log: EVENT • trigger_skip → {"reason":"no_questions_cached"})


  The "Fire" Event
  If YouTube is open and all 8 of the above checks pass (meaning they've watched for at least 30 seconds, no cooldowns are active, and it's an okay time to
  interrupt):


   1. The TriggerEngine selects a question and returns TriggerDecision.Fire.
   2. The Service logs that the quiz is being rendered.
     (Dev Mode Log: EVENT • quiz_shown → {"question_id":"...","source_app":"com.google.android.youtube"})
   3. The Compose UI is attached to the screen via WindowManager.


  User Interaction Logs
  Once the UI is on screen, the user must interact with it. Depending on what they do, you will see one of the following attempt logs in Dev Mode:


   * If they answer correctly:
    (Dev Mode Log: ATTEMPT • math • correct → What is 2+2? (4.5s))
    (Dev Mode Log: EVENT • quiz_answered → {"question_id":"...","correct":true,"response_ms":4500,"source_app":"com.google.android.youtube"})


   * If they answer incorrectly:
    (Dev Mode Log: ATTEMPT • math • wrong → What is 2+2? (3.2s))
    (Dev Mode Log: EVENT • quiz_answered → {"question_id":"...","correct":false,"response_ms":3200,"source_app":"com.google.android.youtube"})


   * If they press the Dismiss button:
    (Dev Mode Log: ATTEMPT • math • dismissed → What is 2+2? (0.0s))
    (Dev Mode Log: EVENT • quiz_dismissed → {"question_id":"...","correct":false,"response_ms":0,"source_app":"com.google.android.youtube"})


  By opening the Dev Mode screen in your app, you will see exactly which of the "skip" barriers is holding the quiz back, or if it successfully fired.