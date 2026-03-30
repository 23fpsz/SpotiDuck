import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
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
        versionCode = 11
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Use properties from local.properties if they exist, otherwise use defaults
            val path = keystoreProperties["signing.storeFile"] as String? ?: "../Apk Key"
            storeFile = file(path)
            storePassword = keystoreProperties["signing.storePassword"] as String? ?: "Overlord@2001"
            keyAlias = keystoreProperties["signing.keyAlias"] as String? ?: "Spotifuck"
            keyPassword = keystoreProperties["signing.keyPassword"] as String? ?: "Overlord@2001"
        }
    }

    buildFeatures {
        resValues = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            //versionNameSuffix = " β"
            manifestPlaceholders["appName"] = "Spotifuck β"
            resValue("string", "app_name", "Spotifuck β")
        }
        release {
            isMinifyEnabled = true // Enables code shrinking, obfuscation, and optimization
            isShrinkResources = true // Enables resource shrinking (removes unused icons/layouts)
            signingConfig = signingConfigs.getByName("release")

            manifestPlaceholders["appName"] = "Spotifuck"
            resValue("string", "app_name", "Spotifuck")
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
                val baseName = "Spotifuck-v${android.defaultConfig.versionName}"
                output.outputFileName.set(if (variant.name == "debug") {
                    // If you want the debug APK to still have "-debug" in the filename, keep this.
                    // Otherwise, change to "$baseName.apk"
                    "$baseName-debug.apk"
                } else {
                    "$baseName.apk"
                })
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
