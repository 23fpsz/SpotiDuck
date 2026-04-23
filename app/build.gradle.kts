import java.util.Properties
import java.io.FileInputStream
import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
}

// Function to get the current Git commit hash using ProcessBuilder
fun getGitCommitHash(): String {
    return try {
        val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
            .start()
        val hash = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        if (hash.isNotEmpty()) hash else "unknown"
    } catch (e: Exception) {
        "unknown"
    }
}

android {
    namespace = "com.spotifuck.music"
    compileSdk = 35

    // Read signing properties from local.properties
    val keystoreProperties = Properties()
    val keystorePropertiesFile = rootProject.file("local.properties")
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    defaultConfig {
        applicationId = "com.spotifuck.music"
        minSdk = 24
        targetSdk = 34
        versionCode = 14
        versionName = "1.1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Priority: Environment Variables (GitHub CI) -> local.properties (Local PC)
            val envStoreFile = System.getenv("RELEASE_STORE_FILE")
            if (envStoreFile != null) {
                storeFile = file(envStoreFile)
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            } else {
                val path = keystoreProperties["signing.storeFile"] as String?
                if (path != null) {
                    storeFile = file(path)
                    storePassword = keystoreProperties["signing.storePassword"] as String?
                    keyAlias = keystoreProperties["signing.keyAlias"] as String?
                    keyPassword = keystoreProperties["signing.keyPassword"] as String?
                }
            }
        }
    }

    buildFeatures {
        resValues = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appName"] = "SpotiDuck β"
            resValue("string", "app_name", "SpotiDuck β")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")

            manifestPlaceholders["appName"] = "SpotiDuck"
            resValue("string", "app_name", "SpotiDuck")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Rename APKs using androidComponents for AGP 8.0+
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                val isCI = System.getenv("GITHUB_ACTIONS") == "true"
                val versionName = android.defaultConfig.versionName
                val baseName = "SpotiDuck-v$versionName"
                
                if (isCI && variant.name == "debug") {
                    val commitHash = getGitCommitHash()
                    output.outputFileName.set("$baseName-$commitHash.apk")
                } else if (variant.name == "debug") {
                    output.outputFileName.set("$baseName-debug.apk")
                } else {
                    output.outputFileName.set("$baseName.apk")
                }
            }
        }
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.media)
    implementation(libs.picasso)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
