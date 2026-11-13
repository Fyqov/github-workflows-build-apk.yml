package com.cluckload.game;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * One Bluetooth (classic RFCOMM) link between two phones. Same shape as LanLink: single text lines in both
 * directions, events go back to the game as (kind, data). The game (JavaScript) decides what the lines mean.
 * Every public method is safe to call from any thread and never throws.
 *
 * Events: listening (name|0), open, msg, close, error, room (address|0|name), scanend.
 */
final class BtLink {

    interface Sink {
        void event(String kind, String data);
    }

    /** Both phones use this id so they find each other's "Cluck & Load" service. */
    static final UUID APP_UUID = UUID.fromString("8f3c6a52-5b1e-4d2a-9c71-2b6f0c4a1d90");

    private final Context ctx;
    private final Sink sink;
    private final Object lock = new Object();
    private final BluetoothAdapter adapter;
    private BluetoothServerSocket server;
    private BluetoothSocket sock;
    private BufferedOutputStream out;
    private volatile int gen = 0;                 // bumps on every host/join/close so stale threads stay quiet
    private BroadcastReceiver finder;

    BtLink(Context ctx, Sink sink) {
        this.ctx = ctx.getApplicationContext();
        this.sink = sink;
        BluetoothAdapter a = null;
        try {
            BluetoothManager m = (BluetoothManager) this.ctx.getSystemService(Context.BLUETOOTH_SERVICE);
            if (m != null) a = m.getAdapter();
        } catch (Throwable ignored) {
        }
        this.adapter = a;
    }

    private void emit(String kind, String data) {
        try {
            sink.event(kind, data == null ? "" : data);
        } catch (Throwable ignored) {
        }
    }

    /** The runtime permissions this phone's Android version asks for. */
    static String[] neededPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            return new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE};
        }
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};      // Android 8-11: finding devices needs "location"
    }

    boolean hasPermissions() {
        for (String p : neededPermissions()) {
            if (ctx.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    /** "none" (no Bluetooth in this phone), "perm" (permission missing), "off" (switched off) or "ok". */
    String state() {
        if (adapter == null) return "none";
        if (!hasPermissions()) return "perm";
        try {
            if (!adapter.isEnabled()) return "off";
        } catch (Throwable e) {
            return "perm";
        }
        return "ok";
    }

    /** This phone's Bluetooth name (what the friend sees in the list). */
    String name() {
        try {
            String n = adapter == null ? null : adapter.getName();
            return n == null || n.isEmpty() ? "PHONE" : n;
        } catch (Throwable e) {
            return "PHONE";
        }
    }

    // ---------- being found and waiting for the friend ----------

    void host() {
        close();
        final int g = ++gen;
        new Thread(() -> {
            BluetoothServerSocket ss = null;
            try {
                ss = adapter.listenUsingInsecureRfcommWithServiceRecord("CluckLoad", APP_UUID);      // "insecure" = no pairing needed; the game PIN is the check
                synchronized (lock) {
                    if (g != gen) {
                        ss.close();
                        return;
                    }
                    server = ss;
                }
                emit("listening", name() + "|0");
                while (g == gen) {
                    BluetoothSocket s = ss.accept();            // blocks until a phone connects (or close() is called)
                    boolean busy;
                    synchronized (lock) {
                        busy = (sock != null);
                    }
                    if (busy || g != gen) {
                        try {
                            s.close();
                        } catch (IOException ignored) {
                        }
                        continue;
                    }
                    attach(s, g);
                }
            } catch (Throwable e) {
                if (g == gen) emit("error", "host: " + e.getMessage());
            } finally {
                try {
                    if (ss != null && g != gen) ss.close();
                } catch (IOException ignored) {
                }
            }
        }, "bt-host").start();
    }

    // ---------- connecting to a friend ----------

    void join(final String address) {
        close();
        final int g = ++gen;
        new Thread(() -> {
            BluetoothSocket s = null;
            Throwable last = null;
            for (int attempt = 0; attempt < 2 && g == gen; attempt++) {      // the first try often fails while the phones are still pairing
                try {
                    try {
                        adapter.cancelDiscovery();                           // discovery slows a connection down a lot
                    } catch (Throwable ignored) {
                    }
                    BluetoothDevice d = adapter.getRemoteDevice(address);
                    s = attempt == 0 ? d.createInsecureRfcommSocketToServiceRecord(APP_UUID)      // first without pairing,
                            : d.createRfcommSocketToServiceRecord(APP_UUID);                      // then the normal way (the system may ask to pair)
                    s.connect();
                    last = null;
                    break;
                } catch (Throwable e) {
                    last = e;
                    try {
                        if (s != null) s.close();
                    } catch (IOException ignored) {
                    }
                    s = null;
                    try {
                        Thread.sleep(700);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
            if (g != gen) {
                try {
                    if (s != null) s.close();
                } catch (IOException ignored) {
                }
                return;
            }
            if (s == null) {
                emit("error", "join: " + (last == null ? "failed" : last.getMessage()));
                return;
            }
            attach(s, g);
        }, "bt-join").start();
    }

    private void attach(BluetoothSocket s, final int g) {
        try {
            synchronized (lock) {
                sock = s;
                out = new BufferedOutputStream(s.getOutputStream(), 4096);
            }
            emit("open", "");
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8), 16384);
            String line;
            while (g == gen && (line = br.readLine()) != null) {
                emit("msg", line);
            }
        } catch (Throwable ignored) {
        } finally {
            boolean mine;
            synchronized (lock) {
                mine = (sock == s);
                if (mine) {
                    sock = null;
                    out = null;
                }
            }
            try {
                s.close();
            } catch (IOException ignored) {
            }
            if (mine && g == gen) emit("close", "");
        }
    }

    void send(String line) {
        BufferedOutputStream o;
        synchronized (lock) {
            o = out;
        }
        if (o == null) return;
        try {
            synchronized (lock) {
                o.write(line.getBytes(StandardCharsets.UTF_8));
                o.write('\n');
                o.flush();
            }
        } catch (Throwable e) {
            BluetoothSocket s;
            synchronized (lock) {
                s = sock;
            }
            try {
                if (s != null) s.close();           // the reader thread notices and reports "close"
            } catch (IOException ignored) {
            }
        }
    }

    void close() {
        gen++;
        stopFinder();
        BluetoothSocket s;
        BluetoothServerSocket ss;
        synchronized (lock) {
            s = sock;
            ss = server;
            sock = null;
            server = null;
            out = null;
        }
        try {
            if (s != null) s.close();
        } catch (IOException ignored) {
        }
        try {
            if (ss != null) ss.close();
        } catch (IOException ignored) {
        }
    }

    // ---------- finding the friend's phone ----------

    /** Phones, tablets and phones that do not say what they are; headphones, TVs and watches stay out of the list. */
    private static boolean looksLikePhone(BluetoothDevice d) {
        try {
            BluetoothClass c = d.getBluetoothClass();
            if (c == null) return true;
            int m = c.getMajorDeviceClass();
            return m == BluetoothClass.Device.Major.PHONE || m == BluetoothClass.Device.Major.COMPUTER || m == BluetoothClass.Device.Major.UNCATEGORIZED;
        } catch (Throwable e) {
            return true;
        }
    }

    private static String deviceName(BluetoothDevice d) {
        String n = null;
        try {
            n = d.getName();
        } catch (Throwable ignored) {
        }
        if (n == null || n.isEmpty()) n = d.getAddress();
        return n.replace('|', ' ').replace('\n', ' ');
    }

    /** Reports the already paired phones at once, then the ones that turn up in a short search, as "room" events. */
    void scan() {
        stopFinder();
        final int g = gen;
        if (adapter == null) {
            emit("scanend", "");
            return;
        }
        final Set<String> seen = new HashSet<>();
        try {
            for (BluetoothDevice d : adapter.getBondedDevices()) {
                if (looksLikePhone(d) && seen.add(d.getAddress())) emit("room", d.getAddress() + "|0|" + deviceName(d));
            }
        } catch (Throwable ignored) {
        }
        final BroadcastReceiver r = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                try {
                    String a = i.getAction();
                    if (BluetoothDevice.ACTION_FOUND.equals(a)) {
                        BluetoothDevice d;
                        if (Build.VERSION.SDK_INT >= 33) d = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                        else d = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (d != null && looksLikePhone(d) && seen.add(d.getAddress())) emit("room", d.getAddress() + "|0|" + deviceName(d));
                    } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(a)) {
                        stopFinder();
                        if (g == gen) emit("scanend", "");
                    }
                } catch (Throwable ignored) {
                }
            }
        };
        try {
            IntentFilter f = new IntentFilter();
            f.addAction(BluetoothDevice.ACTION_FOUND);
            f.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            if (Build.VERSION.SDK_INT >= 33) ctx.registerReceiver(r, f, Context.RECEIVER_EXPORTED);
            else ctx.registerReceiver(r, f);
            synchronized (lock) {
                finder = r;
            }
            if (!adapter.startDiscovery()) {
                stopFinder();
                emit("scanend", "");
            }
        } catch (Throwable e) {
            stopFinder();
            emit("scanend", "");
        }
    }

    private void stopFinder() {
        BroadcastReceiver r;
        synchronized (lock) {
            r = finder;
            finder = null;
        }
        if (r == null) return;
        try {
            ctx.unregisterReceiver(r);
        } catch (Throwable ignored) {
        }
        try {
            if (adapter != null) adapter.cancelDiscovery();
        } catch (Throwable ignored) {
        }
    }
}
