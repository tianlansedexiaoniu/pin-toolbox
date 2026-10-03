package com.hyx.oneshot.wifitools

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit

object WifiDisconnector {

    
    private const val TIMEOUT_SECONDS = 25L

    
    sealed class Result {
        
        data class Disconnected(val level: String, val blipped: Boolean = false) : Result()

        
        object AlreadyFree : Result()

        
        object NoRoot : Result()

        
        data class Failed(val detail: String) : Result()
    }

    

    
    private fun scriptFor(body: String): String =
        
        
        
        "IFACE=\$(getprop wifi.interface 2>/dev/null); " +
        "[ -z \"\$IFACE\" ] && IFACE=wlan0; " +
        "IW=\$(command -v iw 2>/dev/null); " +
        "[ -z \"\$IW\" ] && IW=/system/bin/iw; " +
        "$body"

    
    private const val LINK_CHECK =
        "link=\$(\"\$IW\" dev \"\$IFACE\" link 2>/dev/null); " +
        "case \"\$link\" in *'Connected to'*) echo 'RESULT=BUSY' ;; " +
        "*) echo 'RESULT=OK' ;; esac"

    
    private fun runShell(script: String): Pair<Int, String>? {
        val su = ScriptRunner.findSuPath() ?: return null

        return try {
            val process = ProcessBuilder(su, "-c", script)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()

            val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return null
            }
            process.exitValue() to output
        } catch (_: Exception) {
            null
        }
    }

    
    private fun detached(output: String): Boolean =
        output.lineSequence().any { it.trim() == "RESULT=OK" }

    

    
    private fun level1(context: Context): Boolean {
        val dir = File(context.filesDir, "disconnect")
        if (!dir.exists() && !dir.mkdirs()) {
            return false
        }

        val source = File(dir, "OseDisconnect.java")
        try {
            source.writeText(javaProgram(), Charsets.UTF_8)
        } catch (_: Exception) {
            return false
        }

        
        
        
        
        
        
        
        val shell = scriptFor(
            "cd ${shellQuote(dir.absolutePath)} || exit 1; " +
            "[ -z \"\$BOOTCLASSPATH\" ] && " +
            "BOOTCLASSPATH=/system/framework/core-oj.jar:/system/framework/core-libart.jar:" +
            "/system/framework/conscrypt.jar:/system/framework/okhttp.jar:" +
            "/system/framework/core-junit.jar:/system/framework/bouncycastle.jar:" +
            "/system/framework/ext.jar:/system/framework/framework.jar:" +
            "/system/framework/telephony-common.jar:/system/framework/voip-common.jar:" +
            "/system/framework/ims-common.jar:/system/framework/apache-xml.jar:" +
            "/system/framework/mediatek-common.jar; " +
            "export BOOTCLASSPATH; " +
            "CLASSPATH=/system/framework/framework.jar " +
            "app_process /system/bin --nice-name=ose-disconnect OseDisconnect 2>&1; " +
            LINK_CHECK
        )

        
        
        repeat(2) {
            val run = runShell(shell) ?: return false
            if (detached(run.second)) {
                return true
            }
        }
        return false
    }

    
    private fun level2(): Boolean {
        val tag = "ose" + System.currentTimeMillis().toString(16).takeLast(6)

        val shell = scriptFor(
            "CMD=\$(command -v cmd 2>/dev/null); " +
            "[ -z \"\$CMD\" ] && CMD=/system/bin/cmd; " +
            
            
            "\"\$CMD\" wifi connect-network '$tag' open -b 02:00:00:00:00:01 " +
            ">/dev/null 2>&1; " +
            "sleep 1; " +
            
            "id=\$(\"\$CMD\" wifi list-networks 2>/dev/null | " +
            "awk -v n='$tag' '\$2==n {print \$1}'); " +
            "[ -n \"\$id\" ] && \"\$CMD\" wifi forget-network \"\$id\" >/dev/null 2>&1; " +
            LINK_CHECK
        )

        return runShell(shell)?.let { detached(it.second) } ?: false
    }

    
    private fun level3(context: Context): Boolean {
        
        val bundled = File(context.filesDir, "bin/wpa_cli")

        val shell = scriptFor(
            "\"\$IW\" dev \"\$IFACE\" disconnect >/dev/null 2>&1; " +
            "W=\$(command -v wpa_cli 2>/dev/null); " +
            "[ -z \"\$W\" ] && [ -x ${shellQuote(bundled.absolutePath)} ] && " +
            "W=${shellQuote(bundled.absolutePath)}; " +
            "if [ -n \"\$W\" ]; then " +
            "  \"\$W\" -i \"\$IFACE\" disconnect >/dev/null 2>&1; " +
            "  \"\$W\" -i \"\$IFACE\" set autoscan 0 >/dev/null 2>&1; " +
            "fi; " +
            "sleep 1; " +
            LINK_CHECK
        )

        return runShell(shell)?.let { detached(it.second) } ?: false
    }

    
    private fun level4(): Boolean {
        val shell = scriptFor(
            "SVC=\$(command -v svc 2>/dev/null); " +
            "[ -z \"\$SVC\" ] && SVC=/system/bin/svc; " +
            "\"\$SVC\" wifi disable >/dev/null 2>&1; " +
            "sleep 2; " +
            "\"\$SVC\" wifi enable >/dev/null 2>&1; " +
            
            "sleep 3; " +
            LINK_CHECK
        )

        return runShell(shell)?.let { detached(it.second) } ?: false
    }

    

    
    fun disconnect(context: Context): Result {
        if (!ScriptRunner.hasRoot()) {
            return Result.NoRoot
        }

        
        
        if (currentlyFree(context)) {
            return Result.AlreadyFree
        }

        if (level1(context)) {
            return Result.Disconnected("框架断开")
        }
        if (level2()) {
            return Result.Disconnected("网络切换")
        }
        if (level3(context)) {
            return Result.Disconnected("底层断开")
        }
        if (level4()) {
            return Result.Disconnected("射频重启", blipped = true)
        }

        return Result.Failed(
            "四种方式均未能断开（框架断开 / 网络切换 / 底层断开 / 射频重启）"
        )
    }

    
    private fun currentlyFree(context: Context): Boolean {
        val systemIw = "/system/bin/iw"
        val bundled = File(context.filesDir, "bin/iw")

        val iw = if (File(systemIw).exists()) systemIw
        else if (bundled.exists()) bundled.absolutePath
        else return false      

        val shell = scriptFor(
            "IW=${shellQuote(iw)}; " + LINK_CHECK
        )

        return runShell(shell)?.let { detached(it.second) } ?: false
    }

    

    
    private fun javaProgram(): String = """
        import android.app.ActivityThread;
        import android.content.Context;
        import android.net.wifi.SupplicantState;
        import android.net.wifi.WifiInfo;
        import android.net.wifi.WifiManager;

        public class OseDisconnect {
            public static void main(String[] args) {
                Context ctx = null;
                String how = "none";

                // Route A: an ordinary activity thread, if app_process gave us
                // one. Cheap and side-effect free.
                try {
                    ActivityThread at = ActivityThread.currentActivityThread();
                    if (at != null) {
                        Context c = at.getSystemContext();
                        if (c == null) c = at.getApplication();
                        if (c != null) { ctx = c; how = "current"; }
                    }
                } catch (Throwable ignored) { }

                // Route B: the system-server context. Heavier, works on some
                // ROMs where route A gives nothing.
                if (ctx == null) {
                    try {
                        ActivityThread at = ActivityThread.systemMain();
                        Context c = at.getSystemContext();
                        if (c != null) { ctx = c; how = "systemMain"; }
                    } catch (Throwable ignored) { }
                }

                if (ctx == null) {
                    System.out.println("NOTE=no context");
                    return;
                }

                WifiManager wm = null;
                try {
                    wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
                } catch (Throwable ignored) { }
                if (wm == null) {
                    System.out.println("NOTE=no WifiManager (" + how + ")");
                    return;
                }

                System.out.println("NOTE=ctx=" + how);

                if (!wm.isWifiEnabled()) {
                    System.out.println("NOTE=wifi already off");
                    return;
                }

                // Association is judged by the supplicant state and the BSSID,
                // never by getNetworkId() — see the doc comment above.
                if (!isAssociated(wm)) {
                    System.out.println("NOTE=not associated");
                    return;
                }

                boolean asked = false;
                try { asked = wm.disconnect(); } catch (Throwable t) {
                    System.out.println("NOTE=disconnect threw " + t);
                }

                // Give the state machine time to act. The caller re-checks with
                // `iw`, so this loop is a courtesy, not the verdict.
                for (int i = 0; i < 8; i++) {
                    try { Thread.sleep(400); } catch (Throwable ignored) { }
                    if (!isAssociated(wm)) break;
                }
                System.out.println("NOTE=disconnect() returned " + asked);
            }

            /** True when the STA thinks it is on an AP. */
            private static boolean isAssociated(WifiManager wm) {
                try {
                    WifiInfo info = wm.getConnectionInfo();
                    if (info == null) return false;
                    SupplicantState st = info.getSupplicantState();
                    if (st == SupplicantState.COMPLETED) return true;
                    String bssid = info.getBSSID();
                    return bssid != null && !"02:00:00:00:00:00".equals(bssid);
                } catch (Throwable t) {
                    return false;
                }
            }
        }
    """.trimIndent()

    
    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"
}
