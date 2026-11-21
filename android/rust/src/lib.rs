//! The JNI bridge between Btn Discord's Android shell and Rust.
//!
//! The shell loads `libbtn_android.so`, which `cargo ndk` builds from this
//! crate. It carries the part of the program that runs on Android today and
//! says, in `capabilities`, what a Discord client still needs here: the
//! desktop window is iced on winit, and neither has an Android backend, so the
//! shell draws its own screens and asks Rust for what it cannot do itself.
//!
//! Nothing here talks to Discord yet. The porting steps, in order, are in
//! `docs/android.md`.

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{JNI_TRUE, jboolean, jstring};

const VERSION: &str = env!("CARGO_PKG_VERSION");
const LOG_FILE: &str = "btn-discord.log";

/// Answers `Native.version()` with the crate's version, which the release
/// build's tag names too.
#[unsafe(no_mangle)]
pub extern "system" fn Java_io_github_btndiscord_Native_version<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
) -> jstring {
    string(&mut env, VERSION)
}

/// Answers `Native.capabilities()` with one JSON object: what this build of
/// the library can do, and what the shell should tell the user it cannot.
#[unsafe(no_mangle)]
pub extern "system" fn Java_io_github_btndiscord_Native_capabilities<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
) -> jstring {
    string(&mut env, &capabilities())
}

/// Answers `Native.initialize(logDirectory)` after starting Android's log and
/// writing one line into the directory the shell keeps its files in. A false
/// answer means the directory is not writable, which the shell shows instead
/// of failing later, on the first message it cannot cache.
#[unsafe(no_mangle)]
pub extern "system" fn Java_io_github_btndiscord_Native_initialize<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    log_directory: JString<'local>,
) -> jboolean {
    let Ok(directory) = env.get_string(&log_directory) else {
        return 0;
    };
    let directory = std::path::PathBuf::from(String::from(directory));
    if std::fs::create_dir_all(&directory).is_err() {
        return 0;
    }
    if std::fs::write(directory.join(LOG_FILE), format!("btn-discord {VERSION}\n")).is_err() {
        return 0;
    }
    start_logging();
    JNI_TRUE
}

/// What this build of the library can do, as the JSON one line the shell
/// shows. Built by hand rather than with serde: every value is a constant, so
/// there is nothing to escape.
fn capabilities() -> String {
    format!(
        concat!(
            "{{\"version\":\"{}\",\"os\":\"{}\",\"arch\":\"{}\",",
            "\"discordProtocol\":{},\"desktopUi\":false,\"voice\":false,",
            "\"missing\":[\"Android UI for AppState\",\"token storage\",",
            "\"microphone and speakers\",\"libwebrtc for calls\"]}}"
        ),
        VERSION,
        std::env::consts::OS,
        std::env::consts::ARCH,
        cfg!(feature = "discord"),
    )
}

/// Turns on Android's log, under one tag, so the shell's logcat shows what
/// Rust says. Other platforms have no logcat; there the call does nothing.
fn start_logging() {
    #[cfg(target_os = "android")]
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Info)
            .with_tag("btn-discord"),
    );
    log::info!("btn-discord {VERSION} started");
}

/// A Rust string as a Java string, or null if the JVM is out of memory.
fn string(env: &mut JNIEnv<'_>, value: &str) -> jstring {
    match env.new_string(value) {
        Ok(string) => string.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[cfg(test)]
mod tests {
    use super::capabilities;

    #[test]
    fn capabilities_name_every_missing_piece() {
        let json = capabilities();
        assert!(json.starts_with("{\"version\":\""));
        assert!(json.contains("\"desktopUi\":false"));
        // One JSON object: the shell hands the string to a parser as is.
        assert_eq!(json.matches('{').count(), 1);
        assert_eq!(json.matches('}').count(), 1);
    }
}
