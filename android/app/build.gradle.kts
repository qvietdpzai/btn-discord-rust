plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.btndiscord"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.btndiscord"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // The shell runs unsigned here; signing is the release's job.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // The .so files cargo-ndk writes into app/src/main/jniLibs are the
    // default source set, so no extra configuration names them.
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}
