package io.github.btndiscord;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * The shell's only screen for now: what this build is, and what the port still
 * needs. The desktop window is iced on winit, which has no Android backend, so
 * the chat screens are drawn later against the same {@code AppState} the
 * desktop already renders; see docs/android.md.
 */
public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        float density = getResources().getDisplayMetrics().density;
        int padding = (int) (20 * density);

        TextView text = new TextView(this);
        text.setText(report());
        text.setTextSize(15);
        text.setPadding(padding, padding, padding, padding);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(text, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.TOP);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    /** The screen's text: the library's answer, or why there is none. */
    private String report() {
        StringBuilder report = new StringBuilder();
        report.append("Btn Discord — Android scaffold\n\n");
        if (!Native.loaded()) {
            report.append("libbtn_android.so is not in this APK.\n\n");
            report.append("Build it with:\n");
            report.append("  cargo ndk -t arm64-v8a -o android/app/src/main/jniLibs ")
                    .append("--manifest-path android/rust/Cargo.toml build --release\n");
            return report.toString();
        }

        String directory = getFilesDir().getAbsolutePath();
        report.append("Version: ").append(Native.version()).append('\n');
        report.append("Files: ").append(directory).append('\n');
        report.append("Writable: ").append(Native.initialize(directory)).append("\n\n");
        report.append("Capabilities:\n").append(Native.capabilities()).append("\n\n");
        report.append("This build draws no chat yet. docs/android.md lists what ")
                .append("the port needs, in order: an Android UI for AppState, ")
                .append("token storage, audio, calls, then the release's packaging.");
        return report.toString();
    }
}
