plugins {
    id("com.android.application")
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
}

dependencies {
    implementation(project(":core"))
    implementation(project(":converter-swift"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
}
