import java.util.Properties
import java.time.Instant

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

val signingProps = Properties().apply {
    val signingFile = rootProject.file("signing.properties")
    if (signingFile.exists()) {
        signingFile.inputStream().use { load(it) }
    }
}

fun readSigningValue(key: String): String? {
    val fromProject = (project.findProperty(key) as String?)?.trim().orEmpty()
    if (fromProject.isNotEmpty()) return fromProject

    val fromEnv = System.getenv(key)?.trim().orEmpty()
    if (fromEnv.isNotEmpty()) return fromEnv

    val fromFile = signingProps.getProperty(key)?.trim().orEmpty()
    if (fromFile.isNotEmpty()) return fromFile

    return null
}

fun promptSigningValue(key: String, secret: Boolean): String? {
    val console = System.console() ?: return null
    return if (secret) {
        console.readPassword("Enter %s: ", key)?.concatToString()?.trim()?.takeIf { it.isNotEmpty() }
    } else {
        console.readLine("Enter %s: ", key)?.trim()?.takeIf { it.isNotEmpty() }
    }
}

val signingStoreFile = readSigningValue("SIGNING_STORE_FILE")
    ?: promptSigningValue("SIGNING_STORE_FILE", secret = false)
val signingStorePassword = readSigningValue("SIGNING_STORE_PASSWORD")
    ?: promptSigningValue("SIGNING_STORE_PASSWORD", secret = true)
val signingKeyAlias = readSigningValue("SIGNING_KEY_ALIAS")
    ?: promptSigningValue("SIGNING_KEY_ALIAS", secret = false)
val signingKeyPassword = readSigningValue("SIGNING_KEY_PASSWORD")
    ?: promptSigningValue("SIGNING_KEY_PASSWORD", secret = true)

val hasCompleteReleaseSigning =
    !signingStoreFile.isNullOrBlank() &&
        !signingStorePassword.isNullOrBlank() &&
        !signingKeyAlias.isNullOrBlank() &&
        !signingKeyPassword.isNullOrBlank()

val releaseTaskRequested = gradle.startParameter.taskNames.any { task ->
    val lowered = task.lowercase()
    lowered.contains("release")
}

val autoVersionCode = (project.findProperty("VERSION_CODE") as String?)
    ?.toIntOrNull()
    ?: System.getenv("VERSION_CODE")?.toIntOrNull()
    ?: Instant.now().epochSecond.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

android {
    namespace = "com.reeled.quizoverlay"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.reeled.quizoverlay"
        minSdk = 26
        targetSdk = 34
        versionCode = autoVersionCode
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasCompleteReleaseSigning) {
            create("release") {
                storeFile = file(signingStoreFile!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasCompleteReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

fun String.escapeForBuildConfig(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

val supabaseUrl = (project.findProperty("SUPABASE_URL") as String?)
    ?.trim()
    ?.removeSuffix("/")
    .orEmpty()
val supabaseAnonKey = (project.findProperty("SUPABASE_ANON_KEY") as String?)
    ?.trim()
    .orEmpty()

android.defaultConfig {
    buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl.escapeForBuildConfig()}\"")
    buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supabaseAnonKey.escapeForBuildConfig()}\"")
}

if (releaseTaskRequested && !hasCompleteReleaseSigning) {
    throw GradleException(
        """
        Missing release signing configuration.
        Provide all of:
        - SIGNING_STORE_FILE
        - SIGNING_STORE_PASSWORD
        - SIGNING_KEY_ALIAS
        - SIGNING_KEY_PASSWORD

        You can set them in one of:
        1) ./signing.properties (recommended, local only)
        2) environment variables
        3) -P Gradle properties
        """.trimIndent()
    )
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.google.android.material:material:1.11.0")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Retrofit / OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Rive
    implementation("app.rive:rive-android:9.1.0")
    implementation("androidx.startup:startup-runtime:1.2.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
