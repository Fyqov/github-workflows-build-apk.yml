package com.cluckload.game;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

/** Hosts the game (assets/index.html) full-screen in a WebView. The game runs fully offline. */
public class MainActivity extends Activity {
    private static final int ICON_COUNT = 10;                       // v6.4: activity-aliases Icon0..Icon9 in the manifest

    private ComponentName iconComponent(int i) {
        return new ComponentName(this, "com.cluckload.game.Icon" + i);
    }


    private WebView web;
    private LanLink lan;
    private BtLink bt;
    private static final int REQ_BT_PERM = 71, REQ_BT_ON = 72, REQ_BT_DISC = 73;
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
        // v6.2: nothing in this app caps the CPU (no sustained-performance mode, no throttling); these only ask Android to treat the game like a game
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        if (Build.VERSION.SDK_INT >= 26) web.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true);   // keep the web renderer at foreground priority while the game is shown
        try { android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY); } catch (Throwable t) { /* ignore */ }

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
        /** Settings > Screen: 0 = turn with the phone (even if the system rotation lock is on), 1 = landscape, 2 = portrait. */
        @JavascriptInterface
        public void setOrientation(final int mode) {
            runOnUiThread(() -> setRequestedOrientation(mode == 1 ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    : mode == 2 ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR));
        }

        /** v6.3: "YES, EXIT" on the main menu: closes the game and removes it from the recent apps. */
        @JavascriptInterface
        public void exitApp() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 21) finishAndRemoveTask(); else finish();
            });
        }

        /** v6.4: which launcher icon is active (0-9). The aliases Icon0..Icon9 in the manifest each have one icon; exactly one is enabled. */
        @JavascriptInterface
        public int getIcon() {
            try {
                PackageManager pm = getPackageManager();
                for (int i = 0; i < ICON_COUNT; i++) {
                    int st = pm.getComponentEnabledSetting(iconComponent(i));
                    if (st == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || (st == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && i == 0)) return i;
                }
            } catch (Exception e) { }
            return 0;
        }

        /** v6.4: Settings > App icon. Turns the chosen alias on first, then the others off, without killing the game. */
        @JavascriptInterface
        public boolean setIcon(final int n) {
            if (n < 0 || n >= ICON_COUNT) return false;
            try {
                PackageManager pm = getPackageManager();
                pm.setComponentEnabledSetting(iconComponent(n), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                for (int i = 0; i < ICON_COUNT; i++) {
                    if (i != n) pm.setComponentEnabledSetting(iconComponent(i), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
                return true;
            } catch (Exception e) { return false; }
        }

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

        // ----- play with a friend over Bluetooth: same events as the Wi-Fi link, plus "btstate" (ok / perm / off / none / denied) -----
        /** Checks the permission and the Bluetooth switch (asking the person when needed); the answer comes back as a "btstate" event. */
        @JavascriptInterface
        public void btEnsure() { runOnUiThread(() -> btEnsureUi()); }

        @JavascriptInterface
        public String btName() { return btLink().name(); }

        @JavascriptInterface
        public void btHost(final boolean discoverable) {
            btLink().host();
            if (!discoverable) return;           // only the first time: after a dropped friend the room just opens again
            runOnUiThread(() -> {
                try {                      // the system asks "let other phones see you for 5 minutes?"
                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                            .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300), REQ_BT_DISC);
                } catch (Throwable ignored) {
                }
            });
        }

        @JavascriptInterface
        public void btJoin(String address) { btLink().join(address); }

        @JavascriptInterface
        public void btSend(String line) { btLink().send(line); }

        @JavascriptInterface
        public void btScan() { btLink().scan(); }

        @JavascriptInterface
        public void btClose() { btLink().close(); }

        /** Opens the phone's Bluetooth settings (to pair with the friend's phone). */
        @JavascriptInterface
        public void btSettings() {
            runOnUiThread(() -> {
                try {
                    startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                } catch (Throwable ignored) {
                }
            });
        }
    }

    private void netJs(String kind, String data) {
        final WebView w = web;
        if (w == null) return;
        final String js = "window.__netEv&&window.__netEv(" + JSONObject.quote(kind) + "," + JSONObject.quote(data) + ")";
        w.post(() -> w.evaluateJavascript(js, null));
    }

    private synchronized BtLink btLink() {
        if (bt == null) bt = new BtLink(this, this::netJs);
        return bt;
    }

    /** Permission first, then the Bluetooth switch; reports "btstate" when it is clear whether Bluetooth can be used. */
    private void btEnsureUi() {
        String st = btLink().state();
        if (st.equals("perm")) {
            try {
                requestPermissions(BtLink.neededPermissions(), REQ_BT_PERM);
            } catch (Throwable e) {
                netJs("btstate", "denied");
            }
        } else if (st.equals("off")) {
            try {
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_BT_ON);
            } catch (Throwable e) {
                netJs("btstate", "off");
            }
        } else {
            netJs("btstate", st);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_BT_PERM) return;
        boolean all = grantResults.length > 0;
        for (int r : grantResults) all &= (r == PackageManager.PERMISSION_GRANTED);
        if (all) btEnsureUi(); else netJs("btstate", "denied");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_BT_ON) {
            if (resultCode == RESULT_OK) btEnsureUi(); else netJs("btstate", "off");
        }
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
        if (bt != null) bt.close();
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
