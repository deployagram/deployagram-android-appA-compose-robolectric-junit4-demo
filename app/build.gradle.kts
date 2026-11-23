plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.deployagram.demo.appa"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.deployagram.demo.appa"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    testOptions {
        unitTests {
            // Robolectric needs the Android resources and manifest on the unit-test classpath.
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    // Lets Compose test rules launch an activity in the debug manifest.
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation(platform("androidx.compose:compose-bom:2025.06.01"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.18.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")
    // Deployagram: the JUnit 4 rule, and the MockWebServer recorder. Published on Maven Central.
    testImplementation("com.deployagram:deployagram-junit4:1.1.0")
    testImplementation("com.deployagram:deployagram-mockwebserver:1.1.0")
}

// Robolectric runs Android SDK 36 on the JVM, and that needs Java 21. The app itself still compiles for Java 17.
tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}
