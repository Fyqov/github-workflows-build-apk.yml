package com.cluckload.game;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One TCP link between two phones on the same Wi-Fi (or one phone's hotspot).
 * Messages are single text lines. The game (JavaScript) decides what they mean.
 * The host also sends a small UDP beacon so the other phone can find it in a list.
 * Every public method is safe to call from any thread and never throws.
 */
final class LanLink {

    interface Sink {
        void event(String kind, String data);
    }

    static final int BEACON_PORT = 47322;

    private final Sink sink;
    private final Object lock = new Object();
    private ServerSocket server;
    private Socket sock;
    private BufferedOutputStream out;
    private volatile int gen = 0;          // bumps on every host/join/close so stale threads stay quiet
    private volatile boolean beaconOn = false;

    LanLink(Sink sink) {
        this.sink = sink;
    }

    private void emit(String kind, String data) {
        try {
            sink.event(kind, data == null ? "" : data);
        } catch (Throwable ignored) {
        }
    }

    /** Local IPv4 addresses, Wi-Fi / hotspot interfaces first, comma separated. */
    String localIps() {
        List<String> first = new ArrayList<>();
        List<String> rest = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                String n = ni.getName().toLowerCase();
                boolean wifi = n.startsWith("wlan") || n.startsWith("ap") || n.startsWith("swlan") || n.startsWith("wifi") || n.startsWith("eth");
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (!(a instanceof Inet4Address) || a.isLoopbackAddress()) continue;
                    (wifi ? first : rest).add(a.getHostAddress());
                }
            }
        } catch (Throwable ignored) {
        }
        first.addAll(rest);
        StringBuilder sb = new StringBuilder();
        for (String s : first) {
            if (sb.length() > 0) sb.append(',');
            sb.append(s);
        }
        return sb.toString();
    }

    void host(final int port, final String name) {
        close();
        final int g = ++gen;
        new Thread(() -> {
            ServerSocket ss = null;
            try {
                ss = new ServerSocket();
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress(port));
                synchronized (lock) {
                    if (g != gen) {
                        ss.close();
                        return;
                    }
                    server = ss;
                }
                emit("listening", localIps() + "|" + port);
                startBeacon(g, name, port);
                while (g == gen) {
                    Socket s = ss.accept();
                    boolean busy;
                    synchronized (lock) {
                        busy = (sock != null && !sock.isClosed());
                    }
                    if (busy || g != gen) {
                        try {
                            s.close();
                        } catch (IOException ignored) {
                        }
                        continue;
                    }
                    beaconOn = false;
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
        }, "lan-host").start();
    }

    void join(final String ip, final int port) {
        close();
        final int g = ++gen;
        new Thread(() -> {
            Socket s = new Socket();
            try {
                s.connect(new InetSocketAddress(ip, port), 6000);
                if (g != gen) {
                    s.close();
                    return;
                }
                attach(s, g);
            } catch (Throwable e) {
                try {
                    s.close();
                } catch (IOException ignored) {
                }
                if (g == gen) emit("error", "join: " + e.getMessage());
            }
        }, "lan-join").start();
    }

    private void attach(Socket s, final int g) {
        try {
            s.setTcpNoDelay(true);
            s.setKeepAlive(true);
            synchronized (lock) {
                sock = s;
                out = new BufferedOutputStream(s.getOutputStream(), 8192);
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
            closeSocketOnly();
        }
    }

    private void closeSocketOnly() {
        Socket s;
        synchronized (lock) {
            s = sock;
        }
        try {
            if (s != null) s.close();      // the reader thread notices and reports "close"
        } catch (IOException ignored) {
        }
    }

    void close() {
        gen++;
        beaconOn = false;
        Socket s;
        ServerSocket ss;
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

    /* ---------- finding a host on the network (UDP broadcast) ---------- */

    private void startBeacon(final int g, final String name, final int port) {
        beaconOn = true;
        new Thread(() -> {
            DatagramSocket ds = null;
            try {
                ds = new DatagramSocket();
                ds.setBroadcast(true);
                byte[] data = ("CLUCK1|" + name.replace('|', ' ').replace('\n', ' ') + "|" + port).getBytes(StandardCharsets.UTF_8);
                while (g == gen && beaconOn) {
                    List<InetAddress> targets = new ArrayList<>();
                    try {
                        targets.add(InetAddress.getByName("255.255.255.255"));
                        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                            if (!ni.isUp() || ni.isLoopback()) continue;
                            for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                                if (ia.getBroadcast() != null) targets.add(ia.getBroadcast());
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                    for (InetAddress t : targets) {
                        try {
                            ds.send(new DatagramPacket(data, data.length, t, BEACON_PORT));
                        } catch (Throwable ignored) {
                        }
                    }
                    Thread.sleep(1000);
                }
            } catch (Throwable ignored) {
            } finally {
                if (ds != null) ds.close();
            }
        }, "lan-beacon").start();
    }

    /** Listens for host beacons for a few seconds and reports each one as a "room" event: ip|port|name. */
    void scan() {
        final int g = gen;
        new Thread(() -> {
            DatagramSocket ds = null;
            try {
                ds = new DatagramSocket(null);
                ds.setReuseAddress(true);
                ds.bind(new InetSocketAddress(BEACON_PORT));
                ds.setSoTimeout(400);
                long end = System.currentTimeMillis() + 6000;
                byte[] buf = new byte[256];
                while (System.currentTimeMillis() < end && g == gen) {
                    try {
                        DatagramPacket p = new DatagramPacket(buf, buf.length);
                        ds.receive(p);
                        String s = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
                        String[] f = s.split("\\|");
                        if (f.length >= 3 && f[0].equals("CLUCK1")) {
                            emit("room", p.getAddress().getHostAddress() + "|" + f[2] + "|" + f[1]);
                        }
                    } catch (java.net.SocketTimeoutException ignored) {
                    }
                }
            } catch (Throwable e) {
                emit("error", "scan: " + e.getMessage());
            } finally {
                if (ds != null) ds.close();
                emit("scanend", "");
            }
        }, "lan-scan").start();
    }
}
