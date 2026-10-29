// The VpnService host, JNI facade, Keystore and LAN handling, compiled from
// apps/mobile/modules/tunnet-native so both apps run the same native layer.
// Only the Expo bridge (expo/modules/tunnetnative) is left out.
plugins {
    id("com.android.library")
    id("com.google.protobuf")
}

val workspaceRoot: File = rootProject.projectDir.parentFile.parentFile
val shared = File(workspaceRoot, "apps/mobile/modules/tunnet-native/android/src")

val NDK_VERSION = "30.0.16248370"
val NDK_PLATFORM = 26
val AGENT_CRATE = "tunnet-mobile"
val CARGO_NDK_VERSION = "4.1.2"

val debugAbis = listOf("arm64-v8a", "x86_64")
val releaseAbis = listOf("arm64-v8a")

// `-Ptunnet.abis=arm64-v8a` builds one ABI, halving the Rust build for a phone.
val abiOverride: List<String>? = providers.gradleProperty("tunnet.abis").orNull
    ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)

android {
    // The shared sources import `expo.modules.tunnetnative.R`.
    namespace = "expo.modules.tunnetnative"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }
    ndkVersion = NDK_VERSION

    defaultConfig {
        minSdk = 26
    }

    sourceSets {
        getByName("main") {
            // Kotlin does not tie source paths to packages, so pointing at the
            // package directory leaves out the sibling Expo bridge.
            kotlin.directories.add(File(shared, "main/java/io/tunnet/android").path)
            res.directories.add(File(shared, "main/res").path)
            manifest.srcFile(File(shared, "main/AndroidManifest.xml"))
            (this as ExtensionAware).extensions.getByName<SourceDirectorySet>("proto")
                .srcDir(File(workspaceRoot, "proto"))
        }
        getByName("test") {
            kotlin.directories.add(File(shared, "test/java/io/tunnet/android").path)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        abortOnError = true
        disable += "ChromeOsAbiSupport"
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.36.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    api("com.google.protobuf:protobuf-javalite:4.36.1")
    testImplementation("junit:junit:4.13.2")
}

fun androidSdkDir(): File {
    val env = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
    if (env != null && File(env).isDirectory) return File(env)
    val props = rootProject.file("local.properties")
    if (props.isFile) {
        val sdk = props.readLines()
            .firstOrNull { it.trim().startsWith("sdk.dir=") }
            ?.substringAfter("=")
            ?.trim()
            ?.replace("\\\\", "\\")
        if (sdk != null && File(sdk).isDirectory) return File(sdk)
    }
    throw GradleException("ANDROID_HOME / ANDROID_SDK_ROOT is not set and local.properties has no sdk.dir")
}

val rustInputs = files(
    fileTree(workspaceRoot) {
        include(
            "Cargo.toml",
            "Cargo.lock",
            "rust-toolchain.toml",
            "crates/**/*.rs",
            "crates/**/Cargo.toml",
            "proto/**/*.proto",
        )
        exclude("**/target/**", "**/build/**")
    },
)

androidComponents {
    onVariants { variant ->
        val release = variant.buildType == "release"
        val taskName = "cargoNdk${variant.name.replaceFirstChar { it.uppercase() }}"
        val cargo = tasks.register<CargoNdkBuild>(taskName) {
            group = "rust"
            description = "Build $AGENT_CRATE for the ${variant.name} variant via cargo-ndk."
            crate.set(AGENT_CRATE)
            // A debug agent is ~800 MB per ABI unstripped; ship the release
            // profile in every variant.
            cargoRelease.set(true)
            platform.set(NDK_PLATFORM)
            ndkHome.set(File(androidSdkDir(), "ndk/$NDK_VERSION").absolutePath)
            ndkVersion.set(NDK_VERSION)
            cargoNdkVersion.set(CARGO_NDK_VERSION)
            abis.set(abiOverride ?: if (release) releaseAbis else debugAbis)
            repoRoot.set(workspaceRoot)
            outDir.set(layout.buildDirectory.dir("rustJniLibs/${variant.name}"))
            rustSources.from(rustInputs)
        }
        variant.sources.jniLibs?.addGeneratedSourceDirectory(cargo, CargoNdkBuild::outDir)
    }
}
