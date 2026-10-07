plugins {
    id("com.android.application")
}

val rustJniLibsDir = layout.buildDirectory.dir("generated/rustJniLibs")
val rustTargetDir = layout.buildDirectory.dir("rust-target")

val buildRustPrediction = tasks.register<org.gradle.api.tasks.Exec>("buildRustPrediction") {
    group = "build"
    description = "Builds the Rust prediction JNI library for Android."

    val manifest = rootProject.file("converter-rust/Cargo.toml")
    val sources = rootProject.file("converter-rust/src")

    workingDir(rootProject.file("converter-rust"))
    inputs.file(manifest)
    inputs.dir(sources)
    outputs.dir(rustJniLibsDir)

    doFirst {
        rustJniLibsDir.get().asFile.deleteRecursively()
    }

    environment(
        "CARGO_TARGET_DIR",
        rustTargetDir.get().asFile.absolutePath,
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
        rustJniLibsDir.get().asFile.absolutePath,
        "build",
        "--release",
        "--manifest-path",
        manifest.absolutePath,
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
        getByName("main").jniLibs.srcDir(rustJniLibsDir.get().asFile)
    }
}

tasks.named("preBuild").configure {
    dependsOn(buildRustPrediction)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":converter-swift"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
}
