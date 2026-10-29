plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val workspaceRoot: File = rootProject.projectDir.parentFile.parentFile

/** `[workspace.package].version` from the root Cargo.toml, e.g. "0.9.1". */
fun cargoWorkspaceVersion(): String {
    val lines = File(workspaceRoot, "Cargo.toml").readLines()
    val start = lines.indexOfFirst { it.trim() == "[workspace.package]" }
    return lines.drop(start + 1)
        .takeWhile { !it.trim().startsWith("[") }
        .firstNotNullOfOrNull { Regex("""^version\s*=\s*"([^"]+)"""").find(it.trim()) }
        ?.groupValues?.get(1)
        ?: throw GradleException("no [workspace.package] version in Cargo.toml")
}

fun versionCodeOf(version: String): Int {
    val (major, minor, patch) = Regex("""^(\d+)\.(\d+)\.(\d+)""").find(version)
        ?.destructured?.toList()?.map(String::toInt)
        ?: throw GradleException("cannot derive a versionCode from $version")
    return major * 10000 + minor * 100 + patch
}

val tunnetVersion = cargoWorkspaceVersion()

android {
    namespace = "io.tunnet.android"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        // Distinct from the Expo app (io.tunnet.android) so both can be installed.
        applicationId = "io.tunnet.android.lite"
        minSdk = 26
        targetSdk = 37
        versionCode = versionCodeOf(tunnetVersion)
        versionName = tunnetVersion
    }

    signingConfigs {
        val keystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = false
        disable += "ChromeOsAbiSupport"
    }
}

dependencies {
    implementation(project(":native"))
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.core:core-ktx:1.19.1")
    testImplementation("junit:junit:4.13.2")
}
