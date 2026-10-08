plugins {
    id("com.android.application")
}

val rustJniLibsDir = layout.buildDirectory.dir("generated/rustJniLibs").get().asFile
val rustTargetDir = layout.buildDirectory.dir("rust-target").get().asFile
val rustManifest = rootProject.file("converter-rust/Cargo.toml")
val rustSources = rootProject.file("converter-rust/src")
val rustWorkingDirectory = rootProject.file("converter-rust")

val buildRustPrediction = tasks.register<org.gradle.api.tasks.Exec>("buildRustPrediction") {
    group = "build"
    description = "Builds the Rust prediction JNI library for Android."

    workingDir(rustWorkingDirectory)
    inputs.file(rustManifest)
    inputs.dir(rustSources)
    outputs.dir(rustJniLibsDir)

    environment(
        "CARGO_TARGET_DIR",
        rustTargetDir.absolutePath,
    )
    commandLine(
        "cargo",
        "ndk",
        "--platform",
        "28",
        "-t",
        "arm64-v8a",
        "-t",
        "armeabi-v7a",
        "-t",
        "x86_64",
        "-o",
        rustJniLibsDir.absolutePath,
        "build",
        "--release",
        "--manifest-path",
        rustManifest.absolutePath,
    )
}

android {
    namespace = "dev.turboboo.azookey"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.turboboo.azookey"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main").jniLibs.srcDir(rustJniLibsDir)
    }
}

tasks.named("preBuild").configure {
    dependsOn(buildRustPrediction)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":converter-swift"))
    implementation("androidx.activity:activity:1.10.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
}
