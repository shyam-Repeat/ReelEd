# ReelEd
Reel Education

## Supabase setup

Set these Gradle properties (in `~/.gradle/gradle.properties` or project `gradle.properties`) before running the app:

```properties
SUPABASE_URL=https://<your-project-id>.supabase.co
SUPABASE_ANON_KEY=<your-anon-key>
```

If these are missing, remote fetch/sync is skipped and the app continues with local-only data.

## Release signing

Release builds now require signing values, so APK updates install without uninstalling.
`versionCode` is now auto-generated on each build (from current epoch seconds), so new APKs install as updates.

Provide all keys using one of these:
- `signing.properties` in project root (recommended)
- environment variables
- Gradle `-P` properties

Required keys:

```properties
SIGNING_STORE_FILE=/absolute/path/to/your-release-key.jks
SIGNING_STORE_PASSWORD=your_store_password
SIGNING_KEY_ALIAS=your_key_alias
SIGNING_KEY_PASSWORD=your_key_password
```

Quick setup:
1. Copy `signing.properties.example` to `signing.properties`.
2. Fill your real values.
3. Build release: `./gradlew assembleRelease`.

If you run from a terminal and any value is missing, Gradle will prompt for it when possible.

You can override version code manually if needed:

```bash
./gradlew assembleRelease -PVERSION_CODE=123456
```

## Create a JKS file

A `.jks` file is your app signing key store. You must keep this file and passwords safe forever.
If you lose it, you cannot ship updates for apps already signed with it.

Generate one (example):

```bash
keytool -genkeypair \
  -v \
  -keystore my-release-key.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias reelEdRelease
```

Then set:

```properties
SIGNING_STORE_FILE=/absolute/path/to/my-release-key.jks
SIGNING_STORE_PASSWORD=<password you entered>
SIGNING_KEY_ALIAS=reelEdRelease
SIGNING_KEY_PASSWORD=<password for the alias>
```
