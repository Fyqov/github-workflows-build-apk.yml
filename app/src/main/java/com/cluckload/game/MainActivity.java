package com.cluckload.game;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

/** Hosts the game (assets/index.html) full-screen in a WebView. The game runs fully offline. */
public class MainActivity extends Activity {

    private WebView web;
    private LanLink lan;
    private volatile int wantHz = 0;      // what the game last asked for: 0 = the fastest the screen can do

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        web = new WebView(this);
        web.setBackgroundColor(0xFF223319);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setLongClickable(false);
        web.setHapticFeedbackEnabled(false);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true); // keeps the best score between sessions
        s.setMediaPlaybackRequiresUserGesture(false);

        web.addJavascriptInterface(new NativeBridge(), "CluckNative");
        web.setWebViewClient(new WebViewClient());
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
        applyRefreshRate(0);              // until the game says otherwise, ask for the fastest screen mode
    }

    /** Called from the game (Settings > Frame rate): asks the phone for a matching screen refresh rate. */
    private class NativeBridge {
        @JavascriptInterface
        public void setFrameRate(final int hz) {
            runOnUiThread(() -> applyRefreshRate(hz));
        }

        /** {"cur":120,"max":120,"modes":[60,90,120],"sdk":35}: what the screen is doing now and what it can do (settings shows it). */
        @JavascriptInterface
        public String displayInfo() {
            try {
                Display d = defaultDisplay();
                if (d == null) return "{}";
                Display.Mode cur = d.getMode();
                float max = cur.getRefreshRate();
                StringBuilder modes = new StringBuilder("[");
                boolean first = true;
                for (Display.Mode m : d.getSupportedModes()) {
                    if (m.getPhysicalWidth() != cur.getPhysicalWidth() || m.getPhysicalHeight() != cur.getPhysicalHeight()) continue;
                    float r = m.getRefreshRate();
                    if (r > max) max = r;
                    if (!first) modes.append(',');
                    first = false;
                    modes.append(Math.round(r));
                }
                modes.append(']');
                return "{\"cur\":" + Math.round(d.getRefreshRate()) + ",\"max\":" + Math.round(max) + ",\"modes\":" + modes + ",\"sdk\":" + Build.VERSION.SDK_INT + "}";
            } catch (Throwable t) {
                return "{}";
            }
        }

        // ----- play with a friend over Wi-Fi: the game (JavaScript) drives these, events come back through window.__netEv -----
        @JavascriptInterface
        public String netLocalIps() { return link().localIps(); }

        @JavascriptInterface
        public void netHost(int port, String name) { link().host(port, name); }

        @JavascriptInterface
        public void netJoin(String ip, int port) { link().join(ip, port); }

        @JavascriptInterface
        public void netSend(String line) { link().send(line); }

        @JavascriptInterface
        public void netScan() { link().scan(); }

        @JavascriptInterface
        public void netClose() { link().close(); }
    }

    private synchronized LanLink link() {
        if (lan == null) {
            lan = new LanLink((kind, data) -> {
                final WebView w = web;
                if (w == null) return;
                final String js = "window.__netEv&&window.__netEv(" + JSONObject.quote(kind) + "," + JSONObject.quote(data) + ")";
                w.post(() -> w.evaluateJavascript(js, null));
            });
        }
        return lan;
    }

    private Display defaultDisplay() {
        Display d = null;
        if (Build.VERSION.SDK_INT >= 30) {
            try { d = getDisplay(); } catch (Throwable ignored) { /* not attached yet */ }
        }
        if (d == null) d = getWindowManager().getDefaultDisplay();
        return d;
    }

    /**
     * Asks the phone for a screen mode. hz <= 0 means "the fastest one". Phones differ in what they listen to
     * (display mode id, preferred rate, min/max range, Android 15 frame-rate request), so every hint is given.
     */
    private void applyRefreshRate(int hz) {
        wantHz = hz;
        try {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            float target = 0f;
            Display d = defaultDisplay();
            if (d != null) {
                Display.Mode cur = d.getMode();
                Display.Mode best = null;
                for (Display.Mode m : d.getSupportedModes()) {
                    if (m.getPhysicalWidth() != cur.getPhysicalWidth() || m.getPhysicalHeight() != cur.getPhysicalHeight()) continue;
                    if (best == null) { best = m; continue; }
                    float rm = m.getRefreshRate(), rb = best.getRefreshRate();
                    if (hz <= 0) {
                        if (rm > rb) best = m;
                    } else {
                        float dm = Math.abs(rm - hz), db = Math.abs(rb - hz);
                        if (dm < db - 0.01f || (Math.abs(dm - db) <= 0.01f && rm > rb)) best = m;
                    }
                }
                if (best != null) {
                    lp.preferredDisplayModeId = best.getModeId();
                    target = best.getRefreshRate();
                }
            }
            if (target <= 0f) target = hz > 0 ? hz : 120f;
            lp.preferredRefreshRate = target;
            setRefreshRange(lp, target);
            getWindow().setAttributes(lp);
            requestFrameRate(target);
        } catch (Throwable ignored) {
            // the game keeps working at the screen's default rate
        }
    }

    /** Android 11+ min/max preferred rate (public from API 34): reached by reflection so older SDK stubs still compile. */
    private void setRefreshRange(WindowManager.LayoutParams lp, float rate) {
        if (Build.VERSION.SDK_INT < 30) return;
        try {
            WindowManager.LayoutParams.class.getField("preferredMinDisplayRefreshRate").setFloat(lp, rate);
            WindowManager.LayoutParams.class.getField("preferredMaxDisplayRefreshRate").setFloat(lp, rate);
        } catch (Throwable ignored) {
            // not available on this phone
        }
    }

    /** Android 15 (API 35): views tell the system how fast they want to be drawn; without it a WebView can be held at 60 Hz. */
    private void requestFrameRate(float rate) {
        if (Build.VERSION.SDK_INT < 35) return;
        try {
            java.lang.reflect.Method m = View.class.getMethod("setRequestedFrameRate", float.class);
            m.invoke(getWindow().getDecorView(), rate);
            if (web != null) m.invoke(web, rate);
        } catch (Throwable ignored) {
            // not available on this phone
        }
    }

    @SuppressWarnings("deprecation")
    private void goImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
     * Back button: ask the game first (pause / return to menu). Only when the game says
     * "not handled" (i.e. we are on the main menu) does the app actually close.
     */
    private void handleBack() {
        if (web == null) { finish(); return; }
        web.evaluateJavascript(
                "(function(){try{return window.__back?!!window.__back():false;}catch(e){return false;}})()",
                value -> {
                    if (!"true".equals(value)) finish();
                });
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        handleBack();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            goImmersive();
            applyRefreshRate(wantHz);       // some phones reset the mode when the window loses focus
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // let the game write its run checkpoint before the WebView is frozen
        if (web != null) web.evaluateJavascript("if(window.__save)window.__save()", null);
        if (web != null) web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
        applyRefreshRate(wantHz);
    }

    @Override
    protected void onDestroy() {
        if (lan != null) lan.close();
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
