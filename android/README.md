# The Android shell

A Gradle project whose one activity shows what `libbtn_android.so` answers.
The Rust half is `android/rust`, a JNI crate in its own workspace, built with
`cargo ndk`. What runs, what does not, and the order to fix it:
[docs/android.md](../docs/android.md).

## Build

The Rust library first, for the phone's ABI:

```sh
export ANDROID_NDK_HOME=/path/to/ndk     # or ANDROID_NDK_LATEST_HOME on CI
rustup target add aarch64-linux-android
cargo install cargo-ndk --locked          # once
cargo ndk -t arm64-v8a -o android/app/src/main/jniLibs \
  --manifest-path android/rust/Cargo.toml build --release
```

Then the APK, with Gradle 8.11 or newer and JDK 17 or newer:

```sh
gradle -p android assembleDebug
```

The APK is `android/app/build/outputs/apk/debug/app-debug.apk`. Without the
library the app still starts, and its screen says the `.so` is missing.

The wrapper's `gradle-wrapper.properties` names Gradle 8.11.1 but the wrapper
jar is not committed; `gradle wrapper` writes it, and CI uses
`gradle/actions/setup-gradle` instead.

## Layout

- `app/src/main/java/io/github/btndiscord/MainActivity.java` — the screen.
- `app/src/main/java/io/github/btndiscord/Native.java` — the three JNI calls.
- `app/src/main/jniLibs/<abi>/libbtn_android.so` — where cargo-ndk writes the
  library; ignored by Git.
- `rust/src/lib.rs` — the bridge itself.
