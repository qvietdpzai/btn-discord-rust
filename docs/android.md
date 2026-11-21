# Android

Btn Discord's Android build is a scaffold, not a client yet. This page says what
runs on a phone today, what stops the rest, and the order to take the stops in.

## What exists

- `android/` is a Gradle project with one activity. It loads
  `libbtn_android.so` if the APK has one and shows what that library answers;
  without it, the screen says how to build it.
- `android/rust/` is `btn-android`, a JNI crate in a workspace of its own. It
  keeps the library out of the desktop workspace, whose default build is the
  iced window.
- `.github/workflows/android.yml` cross-compiles the library for `arm64-v8a`,
  `armeabi-v7a` and `x86_64` with `cargo ndk`, packs `assembleDebug`, and keeps
  the APK as a build artifact.

What that workflow builds now: the bridge's host test passes; the three
`libbtn_android.so` files are in the APK (645 KB arm64, 395 KB armv7, 603 KB
x86_64); `app-debug.apk` is 1.7 MB; and the `discord` feature job compiles
`yapfast-discord` for arm64, with voice, the OS credential store and game
detection turned off. The protocol crate builds on Android, but nothing calls
it yet: the shell still draws its own screen.

The Rust bridge exposes three calls:

| Java (`Native.java`) | Rust | Answer |
| --- | --- | --- |
| `version()` | `Java_io_github_btndiscord_Native_version` | The crate's version |
| `capabilities()` | `..._capabilities` | One JSON object: what this build can do |
| `initialize(directory)` | `..._initialize` | Starts logcat's log, checks the directory is writable |

Build it by hand:

```sh
rustup target add aarch64-linux-android
cargo install cargo-ndk --locked
export ANDROID_NDK_HOME=/path/to/ndk
cargo ndk -t arm64-v8a -o android/app/src/main/jniLibs \
  --manifest-path android/rust/Cargo.toml build --release
gradle -p android assembleDebug
```

The APK lands in `android/app/build/outputs/apk/debug/`.

## What runs, and what does not

The program is split in two: `yapfast-core` holds the model (`AppState`, the
effects a screen dispatches) and the Discord protocol lives in
`yapfast-discord`; `crates/yapfast-iced` draws it. The model has no
UI-framework dependency, which is what makes another shell possible at all
([architecture](architecture.md#adding-another-native-ui)). It still reaches
the operating system in four places, and each is a stop on Android:

1. **The window.** iced renders through winit, and winit has no Android
   backend. Nothing of `yapfast-iced` compiles for Android, and the shell must
   draw `AppState` itself: iced's `Application` loop becomes an Android view
   tree, and the effects the model returns (`Start`, `Command`) are run by the
   shell. This is the largest piece of work in the port.
2. **Token storage.** `crates/yapfast-discord/src/token.rs` uses `keyring`,
   whose 4.x release has no Android platform. The token must go to
   `EncryptedSharedPreferences` (Android Keystore) behind the same functions.
   Until then, build `btn-android` without its `discord` feature.
3. **The machine's details.** `game_detection.rs` reads `sysinfo` to name the
   game in Rich Presence. Android has no desktop processes to scan; the module
   should compile to nothing there.
4. **Sound and calls.** `audio_io.rs` has PulseAudio on Linux, WASAPI on
   Windows, and silence everywhere else, so the core compiles without audio.
   Live sound needs an AAudio/OpenSL ES backend behind `Input`/`Output`, and
   calls need `webrtc-sys`, whose vendored copy already carries
   `src/android.rs` and an Android archive; it expects the NDK and a JNI
   `Context` at runtime.

The desktop-only crates (`ksni`, `zbus`, `wayland-*`, `arboard`, `winit`,
`windows-sys`) are behind the root package's `desktop` feature. The Android
build never enables it.

## The order to take them in

Each step ends with something that runs on a phone.

1. **Protocol.** `yapfast-discord` now builds for `aarch64-linux-android`:
   voice (Opus and libwebrtc), the OS credential store and process-based game
   detection are default-on features, turned off on Android, where the vendored
   libwebrtc patch does not match the archive and there is no credential store
   or desktop to detect for. `cargo ndk build --features discord --manifest-path
   android/rust/Cargo.toml` is the check, and the CI job with the same name
   runs it. What remains is using it: JNI calls that start a `Backend`, hand
   its events to Kotlin, and store the token in `EncryptedSharedPreferences`.
2. **Sign-in and the gateway.** Add JNI calls that start a `Backend`, hand its
   events to Kotlin through a callback, and replace the token store. The first
   screen that matters shows a server's channel list from real events.
3. **Reading.** Draw a channel's timeline from `AppState` in a RecyclerView:
   text, emoji, attachments. Sending comes next; both use the model's own
   drafts and effects rather than new code.
4. **Sound.** Add the AAudio backend for voice playback and the microphone
   meter; calls stay out until the WebRTC side has a `Context`.
5. **Release.** Sign with a keystore, split per ABI, and publish the APK
   beside the desktop releases.

Steps 1 and 2 are what the `discord` feature of `android/rust` marks out; it is
off by default so the scaffold's CI stays green while it is unfinished.

## What this fork changed

- The repository is `btn-discord-rust`, a fork of
  [Yapfast](https://github.com/yapfast/yapfast) under the same licence,
  GPL-3.0-or-later. The crate names `yapfast-*` stay as they are: every
  `use yapfast_core::…`, the `YAPFAST_*` build variables and the packaging
  scripts name them, and renaming them would rename the whole build for no
  gain on a phone. The product name the shell shows is Btn Discord.
- `android/`, `docs/android.md` and the two workflows are new here.
