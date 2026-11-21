package io.github.btndiscord;

/**
 * The Rust half of the shell, loaded from {@code libbtn_android.so}.
 *
 * <p>The library is optional while the port is unfinished: an APK packed
 * without it still starts and says so, instead of dying in the launcher.
 */
public final class Native {
    private static final boolean LOADED = load();

    private Native() {}

    private static boolean load() {
        try {
            System.loadLibrary("btn_android");
            return true;
        } catch (UnsatisfiedLinkError error) {
            return false;
        }
    }

    /** Whether {@code libbtn_android.so} was found and loaded. */
    public static boolean loaded() {
        return LOADED;
    }

    /** The Rust crate's version. */
    public static native String version();

    /** One JSON object naming what this build can and cannot do. */
    public static native String capabilities();

    /**
     * Starts Rust's log and checks that {@code logDirectory} is writable.
     *
     * @return whether the shell may keep its files there
     */
    public static native boolean initialize(String logDirectory);
}
