package com.hyx.oneshot.wifitools

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns the lifetime of the target script.
 *
 * Responsibilities, in order of importance:
 *   1. Start the interpreter as a child process and stream stdout+stderr to the UI.
 *   2. Keep a bounded copy of that output, so an activity that was not attached
 *      at the time can still show what happened.
 *   3. Run a watchdog every 5 s that checks whether that process is still alive.
 *   4. As soon as it is gone — clean exit, error exit or crash — publish the
 *      fact so the UI, the tile and any later-launched activity agree.
 *
 * Running this in a foreground service rather than in the activity means the
 * script survives the screen being turned off, and Android will not silently
 * freeze the process group mid-scan.
 *
 * ### Two delivery paths, because there are two processes
 *
 * The activity *usually* lives in this process and receives lines through the
 * in-memory [listener]. But an activity launched by tapping the notification
 * can, on some ROMs, end up in a freshly forked process — in which case there
 * is no listener to call and the terminal would sit empty while the script
 * happily ran in the other process. That is the bug this revision fixes.
 *
 * The fallback is the shared log file: every line is appended to
 * [LOG_FILE_NAME] as well as handed to the listener, and a client that finds
 * itself out-of-process tails that file. Both paths are cheap; the file only
 * ever holds the tail of the run.
 */
class RunService : Service() {

    interface Listener {
        fun onLog(line: String)
        fun onScriptStopped(exitCode: Int)
    }

    companion object {
        const val ACTION_START = "com.hyx.oneshot.wifitools.START"
        const val ACTION_STOP = "com.hyx.oneshot.wifitools.STOP"

        /**
         * Extra carrying a [ScriptRunner.Mode] name. Both entry points — the
         * quick-settings tile and the in-app button — start the full workflow.
         */
        const val EXTRA_MODE = "com.hyx.oneshot.wifitools.MODE"

        /**
         * Extra carrying the run id minted by the caller (the tile) so the
         * activity can recognise the run it is displaying. Absent when the
         * in-app button starts the run; the id is minted here in that case.
         */
        const val EXTRA_RUN_ID = "com.hyx.oneshot.wifitools.RUN_ID"

        private const val CHANNEL_ID = "ose_run_channel"
        private const val NOTIFICATION_ID = 42
        private const val WATCHDOG_INTERVAL_SECONDS = 5L

        /**
         * File the live transcript is mirrored into, for readers that are not
         * in this process. Lives in filesDir so both the app uid and root can
         * read it.
         */
        const val LOG_FILE_NAME = "run_output.log"

        /** Kept deliberately generous: a full scan is chatty. */
        private const val LOG_FILE_MAX_BYTES = 512 * 1024L

        @Volatile
        var listener: Listener? = null

        @Volatile
        var isRunning: Boolean = false
            private set

        /**
         * Mode of the run currently in flight (or the last one to finish).
         * The tile reads this to label itself without having to re-derive it.
         */
        @Volatile
        var currentMode: ScriptRunner.Mode = ScriptRunner.Mode.FULL
            private set

        /** Identity of the run in flight, or the last one to have run. */
        @Volatile
        var currentRunId: String? = null
            private set

        /**
         * Notified whenever `isRunning` flips, so the quick-settings tile can
         * keep its active state in sync with the watchdog's view of the process
         * — including runs started from the in-app button.
         */
        @Volatile
        var stateListener: (() -> Unit)? = null

        /** The shared state file, for callers that only want the answer. */
        fun isRunningNow(context: Context): Boolean = RunStateStore.isRunningNow(context)

        fun logFile(context: Context) =
            java.io.File(context.filesDir, LOG_FILE_NAME)

        private fun setRunning(value: Boolean) {
            if (isRunning != value) {
                isRunning = value
                stateListener?.invoke()
            }
        }
    }

    private var process: Process? = null
    private val stopper = AtomicBoolean(false)
    private var watchdog: ScheduledExecutorService? = null
    private val streamPool = Executors.newCachedThreadPool()

    /** The run id this service instance owns; see [RunStateStore]. */
    @Volatile
    private var runId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopScript()
                return START_NOT_STICKY
            }
            else -> {
                val mode = intent?.getStringExtra(EXTRA_MODE)
                    ?.let { name ->
                        runCatching { ScriptRunner.Mode.valueOf(name) }.getOrNull()
                    }
                    ?: ScriptRunner.Mode.FULL
                runId = intent?.getStringExtra(EXTRA_RUN_ID)
                startScript(mode)
            }
        }
        return START_NOT_STICKY
    }

    private fun startScript(mode: ScriptRunner.Mode) {
        if (isRunning) {
            emit("[*] 脚本已在运行。")
            return
        }

        startForegroundCompat()
        currentMode = mode

        val useRoot = ScriptRunner.hasRoot()
        val command = ScriptRunner.buildCommand(this, useRoot, mode)

        // A fresh transcript: the file is truncated here rather than after the
        // run, so a reader that starts tailing it at any moment only ever sees
        // this run's output.
        resetLogFile()

        emit("[*] root 权限：" + if (useRoot) "可用（以 root 身份运行）" else "不可用（以应用身份运行）")
        emit("[*] 模式：完整流程（扫描 + 攻击，不自动连接）")

        try {
            val builder = ProcessBuilder(command)
            builder.directory(PythonInstaller.scriptDir(this))
            builder.redirectErrorStream(true)

            // Authoritative environment. Setting these on the builder (rather
            // than relying on `VAR=x` prefixes inside the su command string)
            // is what makes them survive a root manager that rewrites the
            // command it is handed. See ScriptRunner.buildEnvironment().
            builder.environment().putAll(ScriptRunner.buildEnvironment(this))

            // Logged so that if a path problem shows up again on a device we can
            // see immediately which values actually reached the child, instead
            // of inferring it from the Python traceback.
            emit("[*] HOME=${builder.environment()["HOME"]}")
            emit("[*] TMPDIR=${builder.environment()["TMPDIR"]}")

            val proc = builder.start()
            process = proc
            setRunning(true)
            currentRunId = runId
            stopper.set(false)

            // Recorded so a tile bound in a *new* process (after this one is
            // killed) still knows a run is in flight, and so the activity can
            // tell *which* run it is. See RunStateStore.
            RunStateStore.markRunning(
                this, mode, processPid(proc), runId
            )
            // Read back: markRunning mints an id when none was supplied, and
            // this instance has to report the same one the store now holds.
            runId = RunStateStore.snapshot(this).runId
            currentRunId = runId

            pumpOutput(proc)
            startWatchdog(proc)
        } catch (err: Exception) {
            emit("[!] 无法启动脚本：${err.javaClass.simpleName}: ${err.message}")
            finishRun(-1)
        }
    }

    /**
     * `Process.pid()` is API 26+ and this app's minSdk is 24, so on older
     * devices the PID is simply not available. That degrades gracefully:
     * [RunStateStore] treats "no PID" as "cannot verify", which keeps the run
     * marked live rather than clearing it.
     */
    private fun processPid(proc: Process): Int = runCatching {
        val m = proc.javaClass.getMethod("pid")
        (m.invoke(proc) as? Int) ?: -1
    }.getOrDefault(-1)

    /** Streams the merged stdout/stderr of the child into the log view. */
    private fun pumpOutput(proc: Process) {
        streamPool.execute {
            try {
                BufferedReader(InputStreamReader(proc.inputStream, Charsets.UTF_8)).use { reader ->
                    // ANSI colour codes are meaningless in a TextView and would
                    // otherwise show up as literal escape noise.
                    val ansi = Regex("\u001B\\[[0-9;]*[A-Za-z]")
                    while (true) {
                        val line = reader.readLine() ?: break
                        val clean = ansi.replace(line, "")
                        appendToLogFile(clean)
                        emitToListener(clean)
                    }
                }
            } catch (err: Exception) {
                if (!stopper.get()) {
                    emit("[!] 读取输出失败：${err.message}")
                }
            }
        }
    }

    /**
     * The 5-second liveness watch. `Process.isAlive` is authoritative and also
     * catches the case where the shell died but the pipe has not flushed yet.
     */
    private fun startWatchdog(proc: Process) {
        val executor = Executors.newSingleThreadScheduledExecutor()
        watchdog = executor

        executor.scheduleWithFixedDelay({
            try {
                if (stopper.get()) return@scheduleWithFixedDelay

                if (!proc.isAlive) {
                    val code = try {
                        proc.exitValue()
                    } catch (_: IllegalThreadStateException) {
                        -1
                    }
                    finishRun(code)
                }
            } catch (_: Exception) {
                // A watchdog must never crash the service.
            }
        }, WATCHDOG_INTERVAL_SECONDS, WATCHDOG_INTERVAL_SECONDS, TimeUnit.SECONDS)
    }

    private fun stopScript() {
        stopper.set(true)
        try {
            process?.let { proc ->
                if (proc.isAlive) {
                    proc.destroy()
                    if (!proc.waitFor(3, TimeUnit.SECONDS)) {
                        proc.destroyForcibly()
                    }
                }
            }
        } catch (_: Exception) {
        }
        finishRun(-9)
    }

    /** Idempotent: only the first caller actually flips the UI back. */
    private fun finishRun(exitCode: Int) {
        if (!isRunning) {
            RunStateStore.clear(this)
            shutDownHelpers()
            stopSelfSafely()
            return
        }

        setRunning(false)
        process = null
        // Cleared here rather than in onDestroy: this is the one moment we know
        // for certain the script is gone, so the tile will not stay lit for it.
        RunStateStore.clear(this)
        shutDownHelpers()

        listener?.onScriptStopped(exitCode)

        stopSelfSafely()
    }

    private fun shutDownHelpers() {
        watchdog?.shutdownNow()
        watchdog = null
        try {
            streamPool.shutdownNow()
        } catch (_: Exception) {
        }
    }

    private fun stopSelfSafely() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Exception) {
        }
        stopSelf()
    }

    private fun emit(line: String) {
        emitToListener(line)
    }

    private fun emitToListener(line: String) {
        listener?.onLog(line)
    }

    // --------------------------------------------------------------- log file

    private fun resetLogFile() {
        try {
            logFile(this).writeText("")
        } catch (_: Exception) {
            // A missing mirror costs us the out-of-process view, nothing else.
        }
    }

    /**
     * Appends one line to the transcript mirror.
     *
     * The file is the *only* way an activity in another process can see the
     * run, so a write failure is worth reporting once — but not worth aborting
     * the run over, hence no exception escapes.
     */
    private fun appendToLogFile(line: String) {
        try {
            val file = logFile(this)
            if (file.length() > LOG_FILE_MAX_BYTES) {
                // Trim the head rather than dropping everything: the tail is
                // what a late-attaching reader actually wants.
                val keep = file.readText(Charsets.UTF_8)
                file.writeText(keep.substring(keep.length / 2))
            }
            file.appendText(line + "\n", Charsets.UTF_8)
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopper.set(true)
        try {
            process?.destroy()
        } catch (_: Exception) {
        }
        shutDownHelpers()
        setRunning(false)
        // The service only goes away when the run is over or the user stopped
        // it, so any persisted "running" state is now a lie. RunStateStore
        // would catch it via the PID check anyway, but clearing here means the
        // tile goes grey immediately instead of on the next shade pull.
        RunStateStore.clear(this)
    }

    // ----------------------------------------------------------- notification

    private fun startForegroundCompat() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "脚本运行状态", NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "显示 pin工具箱 脚本的运行状态"
            manager.createNotificationChannel(channel)
        }

        val tap = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtitle = "脚本已在后台启动，点击返回查看实时日志"

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("pin工具箱 正在运行")
            .setContentText(subtitle)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setOngoing(true)
            .setContentIntent(tap)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }
}
