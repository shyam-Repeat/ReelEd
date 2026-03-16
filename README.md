# ReelEd
Reel Education

## Supabase setup

Set these Gradle properties (in `~/.gradle/gradle.properties` or project `gradle.properties`) before running the app:

```properties
SUPABASE_URL=https://<your-project-id>.supabase.co
SUPABASE_ANON_KEY=<your-anon-key>
```

If these are missing, remote fetch/sync is skipped and the app continues with local-only data.
