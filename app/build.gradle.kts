import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

abstract class VerifyReleaseConfigurationTask : DefaultTask() {
    @get:Input
    abstract val signingConfigured: Property<Boolean>

    @get:Input
    abstract val keystorePath: Property<String>

    @get:Input
    abstract val configuredPrivacyPolicyUrl: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val privacyPolicyFile: RegularFileProperty

    @TaskAction
    fun verifyConfiguration() {
        check(signingConfigured.get()) {
            "Release signing is not configured. Use Android Studio's Build > Generate Signed Bundle / APK " +
                "wizard, or inject signing credentials through the release CI environment."
        }
        check(File(keystorePath.get()).isFile) {
            "The selected release keystore is not a readable file."
        }
        val policyUrl = configuredPrivacyPolicyUrl.get()
        check(policyUrl.isNotBlank()) {
            "The checked-in privacy policy URL must not be blank."
        }
        check(policyUrl.startsWith("https://")) {
            "The checked-in privacy policy URL must use public HTTPS."
        }
        val policy = privacyPolicyFile.get().asFile
        check(policy.isFile && policy.readText().contains("MazeBloom Privacy Policy")) {
            "docs/index.html is missing the MazeBloom Privacy Policy."
        }
    }
}

abstract class VerifyProductionAdsConfigurationTask : DefaultTask() {
    @get:Input
    abstract val adsConfigured: Property<Boolean>

    @TaskAction
    fun verifyConfiguration() {
        check(adsConfigured.get()) {
            "The checked-in production AdMob identifiers are missing, malformed, or still use Google test inventory."
        }
    }
}

val googleTestAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
val googleTestAppOpenId = "ca-app-pub-3940256099942544/9257395921"
val googleTestInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val googleTestRewardedId = "ca-app-pub-3940256099942544/5224354917"

fun externalValue(name: String): String = providers.gradleProperty(name)
    .orElse(providers.environmentVariable(name))
    .orNull
    .orEmpty()
    .trim()

fun protectedEnvironmentValue(name: String): String = providers.environmentVariable(name)
    .orNull
    .orEmpty()
    .trim()

val productionAdMobAppId = "ca-app-pub-7742442202074564~4220443427"
val productionAppOpenId = "ca-app-pub-7742442202074564/6679085805"
val productionInterstitialId = "ca-app-pub-7742442202074564/1818580712"
val productionRewardedId = "ca-app-pub-7742442202074564/5123400902"
val adMobAppIdPattern = Regex("ca-app-pub-\\d{16}~\\d{10}")
val adMobUnitIdPattern = Regex("ca-app-pub-\\d{16}/\\d{10}")
val productionAdsConfigured = adMobAppIdPattern.matches(productionAdMobAppId) &&
    adMobUnitIdPattern.matches(productionAppOpenId) &&
    adMobUnitIdPattern.matches(productionInterstitialId) &&
    adMobUnitIdPattern.matches(productionRewardedId) &&
    productionAdMobAppId != googleTestAdMobAppId &&
    productionAppOpenId != googleTestAppOpenId &&
    productionInterstitialId != googleTestInterstitialId &&
    productionRewardedId != googleTestRewardedId
val configuredAdMobAppId = productionAdMobAppId.takeIf { productionAdsConfigured }.orEmpty()
val configuredAppOpenId = productionAppOpenId.takeIf { productionAdsConfigured }.orEmpty()
val configuredInterstitialId = productionInterstitialId.takeIf { productionAdsConfigured }.orEmpty()
val configuredRewardedId = productionRewardedId.takeIf { productionAdsConfigured }.orEmpty()
val releaseKeystorePath = protectedEnvironmentValue("MAZEBLOOM_KEYSTORE_FILE")
val releaseStorePassword = protectedEnvironmentValue("MAZEBLOOM_KEYSTORE_PASSWORD")
val releaseKeyAlias = protectedEnvironmentValue("MAZEBLOOM_KEY_ALIAS")
val releaseKeyPassword = protectedEnvironmentValue("MAZEBLOOM_KEY_PASSWORD")
val releaseSigningConfigured = listOf(
    releaseKeystorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all(String::isNotBlank)
val injectedKeystorePath = externalValue("android.injected.signing.store.file")
val injectedSigningConfigured = listOf(
    injectedKeystorePath,
    externalValue("android.injected.signing.store.password"),
    externalValue("android.injected.signing.key.alias"),
    externalValue("android.injected.signing.key.password"),
).all(String::isNotBlank)
val bundleSigningConfigured = releaseSigningConfigured || injectedSigningConfigured
val bundleKeystorePath = releaseKeystorePath.ifBlank { injectedKeystorePath }
val privacyPolicyUrl = "https://naimish0.github.io/MazeBloom/"
val appVersionCode = externalValue("MAZEBLOOM_VERSION_CODE")
    .ifBlank { "1" }
    .toIntOrNull()
    ?.takeIf { it > 0 }
    ?: error("MAZEBLOOM_VERSION_CODE must be a positive integer.")
val appVersionName = externalValue("MAZEBLOOM_VERSION_NAME").ifBlank { "1.0" }

fun String.asBuildConfigString(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.rameshta.mazebloom"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.rameshta.mazebloom"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "PRIVACY_POLICY_URL", privacyPolicyUrl.asBuildConfigString())
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = rootProject.file(releaseKeystorePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["ADMOB_APP_ID"] = googleTestAdMobAppId
            manifestPlaceholders["MOBILE_ADS_PROVIDER_ENABLED"] = "true"
            buildConfigField("boolean", "ADMOB_ENABLED", "true")
            buildConfigField("String", "ADMOB_APP_OPEN_ID", googleTestAppOpenId.asBuildConfigString())
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", googleTestInterstitialId.asBuildConfigString())
            buildConfigField("String", "ADMOB_REWARDED_ID", googleTestRewardedId.asBuildConfigString())
        }
        release {
            isDebuggable = false
            isJniDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "src/main/keepRules/mazebloom.keep",
            )
            signingConfig = signingConfigs.findByName("release")
            buildConfigField("boolean", "ADMOB_ENABLED", "false")
            buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"\"")
        }
        create("production") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            manifestPlaceholders["ADMOB_APP_ID"] = configuredAdMobAppId
            manifestPlaceholders["MOBILE_ADS_PROVIDER_ENABLED"] = productionAdsConfigured.toString()
            buildConfigField("boolean", "ADMOB_ENABLED", productionAdsConfigured.toString())
            buildConfigField("String", "ADMOB_APP_OPEN_ID", configuredAppOpenId.asBuildConfigString())
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", configuredInterstitialId.asBuildConfigString())
            buildConfigField("String", "ADMOB_REWARDED_ID", configuredRewardedId.asBuildConfigString())
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets {
        getByName("debug").kotlin.directories.add("src/ads/java")
        getByName("debug").manifest.srcFile("src/ads/AndroidManifest.xml")
        getByName("production").kotlin.directories.add("src/ads/java")
        getByName("production").manifest.srcFile("src/ads/AndroidManifest.xml")
        getByName("release").kotlin.directories.add("src/offline/java")
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
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
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    add("debugImplementation", libs.google.mobile.ads)
    add("debugImplementation", libs.google.user.messaging.platform)
    add("productionImplementation", libs.google.mobile.ads)
    add("productionImplementation", libs.google.user.messaging.platform)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

// Room 2.8.4's migration bundle is compiled against serialization 1.8.1; newer
// SavedState currently contributes an older BOM. Keep the runtime ABI coherent.
configurations.configureEach {
    resolutionStrategy.force(
        "org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.1",
    )
}

tasks.register<Exec>("verifyBundledContentFast") {
    group = "verification"
    description = "Validates the exact packaged catalog, 100 shards, Daily and Progressive pools, roots, and full-certification stamp."
    workingDir = rootProject.projectDir
    environment("PATH", "/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:${System.getenv("PATH").orEmpty()}")
    commandLine("/usr/bin/env", "node", "tools/verify-content.mjs")
    inputs.files(
        layout.projectDirectory.file("src/main/assets/content/campaign.jsonl"),
        layout.projectDirectory.file("src/main/assets/content/daily.jsonl"),
        layout.projectDirectory.file("src/main/assets/content/progressive.jsonl"),
        layout.projectDirectory.file("src/main/assets/content/manifest.json"),
        layout.projectDirectory.file("src/main/assets/content/certification-stamp.json"),
        fileTree(layout.projectDirectory.dir("src/main/assets/content/campaign")),
        rootProject.layout.projectDirectory.file("tools/verify-content.mjs"),
    )
}

tasks.register<Exec>("verifyEndlessBaseline") {
    group = "verification"
    description = "Pins the exact 2,220-record Endless uniqueness baseline and canonical collision evidence."
    workingDir = rootProject.projectDir
    environment("PATH", "/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:${System.getenv("PATH").orEmpty()}")
    commandLine("/usr/bin/env", "node", "tools/verify-endless-baseline.mjs")
    inputs.files(
        layout.projectDirectory.file("src/main/assets/content/manifest.json"),
        layout.projectDirectory.file("src/main/assets/content/endless-baseline.json"),
        rootProject.layout.projectDirectory.file("tools/verify-endless-baseline.mjs"),
    )
}

tasks.register<Exec>("verifyEndlessHorizonStamp") {
    group = "verification"
    description = "Fails closed unless the exact current tooling has a passing independent 1,000-level Endless horizon stamp."
    workingDir = rootProject.projectDir
    environment("PATH", "/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:${System.getenv("PATH").orEmpty()}")
    commandLine("/usr/bin/env", "node", "tools/verify-endless-horizon-stamp.mjs")
    inputs.files(
        layout.projectDirectory.file("src/main/assets/content/endless-baseline.json"),
        layout.projectDirectory.file("src/main/assets/content/endless-horizon-stamp.json"),
        rootProject.layout.projectDirectory.file("tools/generate-content.mjs"),
        rootProject.layout.projectDirectory.file("tools/certify-endless-horizon.mjs"),
        rootProject.layout.projectDirectory.file("tools/verify-endless-horizon-stamp.mjs"),
    )
}

tasks.register<Exec>("certifyAutoProgressiveHorizon") {
    group = "verification"
    description = "Checkpoint-generates and independently compares the first 1,000 runtime Endless levels against the full 2,220 baseline."
    workingDir = rootProject.projectDir
    environment("PATH", "/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:${System.getenv("PATH").orEmpty()}")
    commandLine("/usr/bin/env", "node", "--max-old-space-size=512", "tools/certify-endless-horizon.mjs", "--count=1000", "--generator=15")
    inputs.files(
        layout.projectDirectory.file("src/main/assets/content/endless-baseline.json"),
        layout.projectDirectory.file("src/main/assets/content/manifest.json"),
        layout.projectDirectory.file("src/main/assets/content/daily.jsonl"),
        layout.projectDirectory.file("src/main/assets/content/progressive.jsonl"),
        fileTree(layout.projectDirectory.dir("src/main/assets/content/campaign")),
        rootProject.layout.projectDirectory.file("tools/generate-content.mjs"),
        rootProject.layout.projectDirectory.file("tools/certify-endless-horizon.mjs"),
    )
    outputs.files(
        layout.projectDirectory.file("src/main/assets/content/endless-horizon-stamp.json"),
        rootProject.layout.projectDirectory.file("reports/content/endless-horizon-1000.json"),
    )
    outputs.upToDateWhen { false }
}

tasks.register<Exec>("certifyCampaignFull") {
    group = "verification"
    description = "Re-solves all 2,000 Campaign, 120 Daily, and 100 Progressive levels and reruns exhaustive strict uniqueness."
    workingDir = rootProject.projectDir
    environment("PATH", "/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:${System.getenv("PATH").orEmpty()}")
    commandLine("/usr/bin/env", "node", "--max-old-space-size=512", "tools/verify-content.mjs", "--full")
    inputs.files(
        layout.projectDirectory.file("src/main/assets/content/campaign.jsonl"),
        layout.projectDirectory.file("src/main/assets/content/daily.jsonl"),
        layout.projectDirectory.file("src/main/assets/content/progressive.jsonl"),
        layout.projectDirectory.file("src/main/assets/content/manifest.json"),
        fileTree(layout.projectDirectory.dir("src/main/assets/content/campaign")),
        rootProject.layout.projectDirectory.file("tools/verify-content.mjs"),
        rootProject.layout.projectDirectory.file("tools/generate-content.mjs"),
    )
}

tasks.register("verifyBundledContent") {
    group = "verification"
    description = "Runs the fast packaged-content gate and full current certification."
    dependsOn("verifyBundledContentFast", "certifyCampaignFull")
}

tasks.register<Exec>("generateBundledContent") {
    group = "mazebloom content"
    description = "Deterministically regenerates Campaign, Daily, and Auto Progressive assets from fixed seeds."
    workingDir = rootProject.projectDir
    environment("PATH", "/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:${System.getenv("PATH").orEmpty()}")
    commandLine("/usr/bin/env", "node", "--max-old-space-size=512", "tools/generate-content.mjs")
}
tasks.register("generateCampaign") { group = "mazebloom content"; dependsOn("generateBundledContent") }
tasks.register("generateDailyPool") { group = "mazebloom content"; dependsOn("generateBundledContent") }
tasks.register("verifyLegacyPrefix") { group = "verification"; dependsOn("verifyBundledContentFast") }
tasks.register("generateGarden") { group = "mazebloom content"; dependsOn("generateBundledContent") }
tasks.register("certifyGarden") { group = "verification"; dependsOn("verifyBundledContentFast") }
tasks.register("certifyCampaignIncremental") { group = "verification"; dependsOn("verifyBundledContentFast") }
tasks.register("certifyCampaign") { group = "verification"; dependsOn("certifyCampaignFull", "testDebugUnitTest") }

listOf("solve", "analyze", "dedupe", "replay", "render").forEach { command ->
    tasks.register<Exec>("${command}Content") {
        group = "mazebloom content"
        description = "Runs the MazeBloom content CLI '$command' command (campaign-001 by default)."
        workingDir = rootProject.projectDir
        environment("PATH", "/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:${System.getenv("PATH").orEmpty()}")
        commandLine("/usr/bin/env", "node", "tools/content-cli.mjs", command, "campaign-001")
    }
}

val verifyReleaseConfiguration = tasks.register<VerifyReleaseConfigurationTask>("verifyReleaseConfiguration") {
    group = "verification"
    description = "Fails closed unless the Play artifact has dialog/CI signing and a public in-app privacy policy configured."
    signingConfigured.set(bundleSigningConfigured)
    keystorePath.set(rootProject.file(bundleKeystorePath.ifBlank { ".missing-release-keystore" }).absolutePath)
    configuredPrivacyPolicyUrl.set(privacyPolicyUrl)
    privacyPolicyFile.set(rootProject.layout.projectDirectory.file("docs/index.html"))
}

val verifyProductionAdsConfiguration = tasks.register<VerifyProductionAdsConfigurationTask>("verifyProductionAdsConfiguration") {
    group = "verification"
    description = "Fails closed unless all production AdMob identifiers are valid and are not Google test identifiers."
    adsConfigured.set(productionAdsConfigured)
}

tasks.named("check") {
    dependsOn(
        "verifyBundledContentFast",
        "verifyEndlessBaseline",
        verifyProductionAdsConfiguration,
    )
}
tasks.matching { it.name == "assembleDebug" }.configureEach { dependsOn("verifyBundledContentFast", "verifyEndlessBaseline") }
tasks.matching { it.name == "bundleRelease" || it.name == "bundleProduction" }.configureEach {
    dependsOn("verifyBundledContent", "verifyEndlessBaseline", "verifyEndlessHorizonStamp")
}
tasks.matching { it.name == "bundleRelease" }.configureEach {
    dependsOn(verifyReleaseConfiguration)
}
tasks.matching { it.name == "bundleProduction" }.configureEach {
    dependsOn(verifyReleaseConfiguration, verifyProductionAdsConfiguration)
}
