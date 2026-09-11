import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
// Build inputs come from a Gradle property, an environment variable, or
// local.properties, in that order, so the same build works on a workstation
// and on CI without checking any secret into the repository.
// A blank value counts as absent: CI sets an unset secret to an empty string,
// which should fall through to the default rather than win the lookup.
fun buildInput(name: String, default: String = ""): String = listOf(
    providers.gradleProperty(name).orNull,
    providers.environmentVariable(name).orNull,
    localProperties.getProperty(name),
).firstOrNull { !it.isNullOrBlank() }?.trim() ?: default

val mapTilerKey = buildInput("MAPTILER_KEY")
val mapStyleUrlOverride = buildInput("MAP_STYLE_URL_OVERRIDE")

// Release signing. The APK must be signed with a stable key or Android will
// refuse to install it as an upgrade over an earlier build.
val releaseKeystore = buildInput("RMP_KEYSTORE_FILE").let { if (it.isEmpty()) null else rootProject.file(it) }
val releaseKeystorePassword = buildInput("RMP_KEYSTORE_PASSWORD")
val releaseKeyAlias = buildInput("RMP_KEY_ALIAS")
val releaseKeyPassword = buildInput("RMP_KEY_PASSWORD")
val releaseSigningReady = releaseKeystore?.isFile == true &&
    releaseKeystorePassword.isNotEmpty() &&
    releaseKeyAlias.isNotEmpty() &&
    releaseKeyPassword.isNotEmpty()

// A tagged release overrides these so the published APK carries the tag it was
// cut from; a plain local build keeps the defaults. The release workflow derives
// the version code from the version name as major * 10000 + minor * 100 + patch,
// so the defaults below follow the same scheme to stay consistent with it.
val appVersionName = buildInput("RMP_VERSION_NAME", "1.1.0")
val appVersionCode = buildInput("RMP_VERSION_CODE", "10100").let {
    it.toIntOrNull() ?: error("RMP_VERSION_CODE must be an integer, but was \"$it\"")
}

android {
    namespace = "com.mike.rmpfinder"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mike.rmpfinder"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MAPTILER_KEY", "\"${mapTilerKey.replace("\"", "\\\"")}\"")
        buildConfigField("String", "MAP_STYLE_URL_OVERRIDE", "\"${mapStyleUrlOverride.replace("\"", "\\\"")}\"")
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            // Left unsigned on purpose when no key is supplied: a silently
            // unsigned APK that cannot install is worse than an obvious gap,
            // and the release workflow fails the build when signing is missing.
            signingConfig = if (releaseSigningReady) signingConfigs.getByName("release") else null

            // MapLibre's renderer ships a native library per ABI, and four of
            // them were 49 MB of a 70 MB APK. The release targets one personal
            // arm64 phone: x86 and x86_64 only ever run on an emulator, and
            // armeabi-v7a is 32-bit ARM the device does not use. Debug builds
            // deliberately keep every ABI so emulator-based instrumented tests
            // in src/androidTest still run.
            ndk {
                abiFilters.add("arm64-v8a")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

abstract class SyncRmpAssets : DefaultTask() {
    @get:InputFile
    abstract val restaurantsFile: RegularFileProperty

    @get:InputFile
    abstract val manifestFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun sync() {
        val output = outputDirectory.get().asFile.apply { mkdirs() }
        restaurantsFile.get().asFile.copyTo(output.resolve("restaurants.json"), overwrite = true)
        manifestFile.get().asFile.copyTo(output.resolve("manifest.json"), overwrite = true)
    }
}

val syncBundledData = tasks.register<SyncRmpAssets>("syncBundledData") {
    restaurantsFile.set(rootProject.layout.projectDirectory.file("data/restaurants.json"))
    manifestFile.set(rootProject.layout.projectDirectory.file("data/manifest.json"))
    outputDirectory.set(layout.buildDirectory.dir("generated/rmpAssets"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            syncBundledData,
            SyncRmpAssets::outputDirectory,
        )
    }
}

dependencies {
    constraints {
        // lintVitalRelease fails assembleRelease with
        // InvalidFragmentVersionForActivityResult because an old
        // androidx.fragment (1.0.0, pulled in transitively by
        // play-services-location) is on the classpath, and that FragmentActivity
        // mishandles ActivityResult permission requests. MainActivity is a
        // ComponentActivity so it is not itself affected, but the floor is worth
        // raising rather than suppressing the check. This is a minimum, not a
        // pin: anything newer already in the graph still wins.
        implementation("androidx.fragment:fragment") {
            version { require("1.3.0") }
            because("ActivityResult APIs need FragmentActivity 1.3.0 or newer")
        }
    }

    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")

    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("com.google.android.gms:play-services-location:21.4.0")
    implementation("org.maplibre.gl:android-sdk:13.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
