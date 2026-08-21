plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.rameshta.mazebloom"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.rameshta.mazebloom"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.register<Exec>("verifyBundledContent") {
    group = "verification"
    description = "Verifies the exact campaign and daily assets packaged by the app."
    workingDir = rootProject.projectDir
    environment("PATH", "/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:${System.getenv("PATH").orEmpty()}")
    commandLine("/usr/bin/env", "node", "tools/verify-content.mjs")
    inputs.files(
        layout.projectDirectory.file("src/main/assets/content/campaign.jsonl"),
        layout.projectDirectory.file("src/main/assets/content/daily.jsonl"),
        layout.projectDirectory.file("src/main/assets/content/manifest.json"),
        rootProject.layout.projectDirectory.file("tools/verify-content.mjs"),
    )
}

tasks.register<Exec>("generateBundledContent") {
    group = "mazebloom content"
    description = "Deterministically regenerates campaign and daily assets from fixed seeds."
    workingDir = rootProject.projectDir
    environment("PATH", "/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:${System.getenv("PATH").orEmpty()}")
    commandLine("/usr/bin/env", "node", "tools/generate-content.mjs")
}
tasks.register("generateCampaign") { group = "mazebloom content"; dependsOn("generateBundledContent") }
tasks.register("generateDailyPool") { group = "mazebloom content"; dependsOn("generateBundledContent") }
tasks.register("certifyCampaign") { group = "verification"; dependsOn("verifyBundledContent", "testDebugUnitTest") }

listOf("solve", "analyze", "dedupe", "replay", "render").forEach { command ->
    tasks.register<Exec>("${command}Content") {
        group = "mazebloom content"
        description = "Runs the MazeBloom content CLI '$command' command (campaign-001 by default)."
        workingDir = rootProject.projectDir
        environment("PATH", "/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:${System.getenv("PATH").orEmpty()}")
        commandLine("/usr/bin/env", "node", "tools/content-cli.mjs", command, "campaign-001")
    }
}

tasks.named("check") { dependsOn("verifyBundledContent") }
tasks.matching { it.name == "assembleDebug" || it.name == "bundleRelease" }.configureEach {
    dependsOn("verifyBundledContent")
}
