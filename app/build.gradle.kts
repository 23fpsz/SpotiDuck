import java.util.Properties
import java.io.FileInputStream
import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.android.application)
}

// Function to get the current Git commit hash using ProcessBuilder to avoid Gradle DSL scoping issues
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
        versionCode = 12
        versionName = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Priority: Environment Variables (GitHub CI) -> local.properties (Local PC) -> Defaults
            val envStoreFile = System.getenv("RELEASE_STORE_FILE")
            if (envStoreFile != null) {
                storeFile = file(envStoreFile)
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            } else {
                val path = keystoreProperties["signing.storeFile"] as String? ?: "../Apk Key"
                storeFile = file(path)
                storePassword = keystoreProperties["signing.storePassword"] as String? ?: "Overlord@2001"
                keyAlias = keystoreProperties["signing.keyAlias"] as String? ?: "Spotifuck"
                keyPassword = keystoreProperties["signing.keyPassword"] as String? ?: "Overlord@2001"
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
                    // Automated Debug build gets the commit hash, no -debug suffix
                    val commitHash = getGitCommitHash()
                    output.outputFileName.set("$baseName-$commitHash.apk")
                } else if (variant.name == "debug") {
                    // Local Debug build keeps -debug suffix
                    output.outputFileName.set("$baseName-debug.apk")
                } else {
                    // Release builds (Local or CI) keep -version
                    output.outputFileName.set("$baseName.apk")
                }
            }
        }
    }
}

dependencies {
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
