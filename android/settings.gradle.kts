// The Android shell of btn-discord-rust. The Rust half is built by cargo-ndk
// into app/src/main/jniLibs; see README.md here and docs/android.md.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "btn-discord-rust"
include(":app")
