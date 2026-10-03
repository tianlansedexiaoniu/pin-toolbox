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

class RunService : Service() {

    interface Listener {
        fun onLog(line: String)
        fun onScriptStopped(exitCode: Int)
    }

    companion object {
        const val ACTION_START = "com.hyx.oneshot.wifitools.START"
        const val ACTION_STOP = "com.hyx.oneshot.wifitools.STOP"

        
        const val EXTRA_MODE = "com.hyx.oneshot.wifitools.MODE"

        
        const val EXTRA_RUN_ID = "com.hyx.oneshot.wifitools.RUN_ID"

        
        const val EXTRA_CONNECT = "com.hyx.oneshot.wifitools.CONNECT"

        
        const val EXTRA_REPEAT = "com.hyx.oneshot.wifitools.REPEAT"

        
        const val EXTRA_LONG_TIMEOUT = "com.hyx.oneshot.wifitools.LONG_TIMEOUT"

        
        private const val REPEAT_DELAY_MS = 1500L

        private const val CHANNEL_ID = "ose_run_channel"
        private const val NOTIFICATION_ID = 42
        private const val WATCHDOG_INTERVAL_SECONDS = 5L

        
        const val LOG_FILE_NAME = "run_output.log"

        
        private const val LOG_FILE_MAX_BYTES = 512 * 1024L

        @Volatile
        var listener: Listener? = null

        @Volatile
        var isRunning: Boolean = false
            private set

        
        @Volatile
        var currentMode: ScriptRunner.Mode = ScriptRunner.Mode.FULL
            private set

        
        @Volatile
        var currentRunId: String? = null
            private set

        
        @Volatile
        var stateListener: (() -> Unit)? = null

        
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

    
    @Volatile
    private var streamPool: java.util.concurrent.ExecutorService =
        Executors.newCachedThreadPool()

    
    @Volatile
    private var repeatMode: Boolean = false

    
    @Volatile
    private var lastMode: ScriptRunner.Mode = ScriptRunner.Mode.FULL

    @Volatile
    private var lastConnectOnly: Boolean = false

    @Volatile
    private var longTimeout: Boolean = false

    
    @Volatile
    private var runId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                
                
                
                repeatMode = false
                stopScript()
                return START_NOT_STICKY
            }
            else -> {
                val mode = intent?.getStringExtra(EXTRA_MODE)
                    ?.let { name ->
                        runCatching { ScriptRunner.Mode.valueOf(name) }.getOrNull()
                    }
                    ?: ScriptRunner.Mode.FULL

                val connectOnly =
                    intent?.getBooleanExtra(EXTRA_CONNECT, false) == true

                
                
                
                if (intent?.hasExtra(EXTRA_REPEAT) == true) {
                    repeatMode = intent.getBooleanExtra(EXTRA_REPEAT, false)
                }

                if (intent?.hasExtra(EXTRA_LONG_TIMEOUT) == true) {
                    longTimeout = intent.getBooleanExtra(EXTRA_LONG_TIMEOUT, false)
                }

                runId = intent?.getStringExtra(EXTRA_RUN_ID)
                startScript(mode, connectOnly = connectOnly, longTimeout = longTimeout)
            }
        }
        return START_NOT_STICKY
    }

    private fun startScript(mode: ScriptRunner.Mode, connectOnly: Boolean = false, longTimeout: Boolean = false) {
        if (isRunning) {
            emit("[*] 脚本已在运行。")
            return
        }

        startForegroundCompat()
        currentMode = mode

        
        lastMode = mode
        lastConnectOnly = connectOnly
        this.longTimeout = longTimeout

        val useRoot = ScriptRunner.hasRoot()
        val command = ScriptRunner.buildCommand(this, useRoot, mode, connectOnly, longTimeout)

        
        
        
        resetLogFile()

        emit("[*] root 权限：" + if (useRoot) "可用（以 root 身份运行）" else "不可用（以应用身份运行）")
        if (connectOnly) {
            emit("[*] 模式：破解并连接（扫描 → 破解信号最强的可破解 WiFi → 立即连接，读变量为兜底）")
        }

        if (repeatMode) {
            emit("[*] 重复模式：已开启 — 脚本每次结束都会自动重新运行（点「关闭」停止）")
        }

        if (longTimeout) {
            emit("[*] 确保准确：WPS 超时已从 10s 延长至 30s")
        }

        
        
        
        
        
        
        
        
        
        if (mode == ScriptRunner.Mode.MODE2) {
            when (val result = WifiDisconnector.disconnect(this)) {
                is WifiDisconnector.Result.Disconnected -> {
                    emit("[*] 已断开当前 WiFi 连接（方式：${result.level}，WiFi 开关保持开启）")
                    if (result.blipped) {
                        emit("[*] 注：本次为断开连接重启过一次射频，WiFi 开关已自动重新打开")
                    }
                }
                is WifiDisconnector.Result.AlreadyFree ->
                    emit("[*] 当前未连接任何 WiFi，无需断开")
                is WifiDisconnector.Result.NoRoot -> {
                    emit("[!] 方案2 需要 root 权限才能断开 WiFi 连接。")
                    abortBeforeStart()
                    return
                }
                is WifiDisconnector.Result.Failed -> {
                    emit("[!] 无法断开当前 WiFi 连接：${result.detail}")
                    emit("[!] 请在系统设置里手动断开当前 WiFi（不必关闭 WiFi 开关），然后重试。")
                    abortBeforeStart()
                    return
                }
            }
        }

        try {
            val builder = ProcessBuilder(command)
            builder.directory(PythonInstaller.scriptDir(this))
            builder.redirectErrorStream(true)

            
            
            
            
            builder.environment().putAll(ScriptRunner.buildEnvironment(this))

            
            
            
            emit("[*] HOME=${builder.environment()["HOME"]}")
            emit("[*] TMPDIR=${builder.environment()["TMPDIR"]}")

            val proc = builder.start()
            process = proc
            setRunning(true)
            currentRunId = runId
            stopper.set(false)

            
            
            
            RunStateStore.markRunning(
                this, mode, processPid(proc), runId
            )
            
            
            runId = RunStateStore.snapshot(this).runId
            currentRunId = runId

            pumpOutput(proc)
            startWatchdog(proc)
        } catch (err: Exception) {
            emit("[!] 无法启动脚本：${err.javaClass.simpleName}: ${err.message}")
            finishRun(-1)
        }
    }

    
    private fun abortBeforeStart() {
        RunStateStore.clear(this)
        shutDownHelpers()

        listener?.onScriptStopped(-1)

        stopSelfSafely()
    }

    
    private fun processPid(proc: Process): Int = runCatching {
        val m = proc.javaClass.getMethod("pid")
        (m.invoke(proc) as? Int) ?: -1
    }.getOrDefault(-1)

    
    private fun pumpOutput(proc: Process) {
        streamPool.execute {
            try {
                BufferedReader(InputStreamReader(proc.inputStream, Charsets.UTF_8)).use { reader ->
                    
                    
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
                
            }
        }, WATCHDOG_INTERVAL_SECONDS, WATCHDOG_INTERVAL_SECONDS, TimeUnit.SECONDS)
    }

    
    private fun stopScript() {
        stopper.set(true)

        
        val pid = RunStateStore.snapshot(this).pid
        if (pid > 0) {
            runCatching {
                ProcessBuilder(
                    "su", "-c",
                    
                    
                    
                    "kill -TERM -$pid 2>/dev/null; kill -TERM $pid 2>/dev/null; " +
                        "sleep 1; kill -KILL -$pid 2>/dev/null; kill -KILL $pid 2>/dev/null; true"
                ).redirectErrorStream(true).start().waitFor()
            }.onFailure {  }
        }

        
        
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

        process = null
        finishRun(-9)
    }

    
    private fun finishRun(exitCode: Int) {
        if (!isRunning) {
            RunStateStore.clear(this)
            shutDownHelpers()

            
            
            
            stopSelfSafely()
            return
        }

        setRunning(false)
        process = null

        
        
        
        
        
        
        val willRepeat = repeatMode && !stopper.get()

        if (willRepeat) {
            
            
            
            
            
            RunStateStore.clear(this)

            
            
            
            
            shutDownHelpers()

            scheduleRepeat(exitCode)
            return
        }

        
        
        RunStateStore.clear(this)
        shutDownHelpers()

        listener?.onScriptStopped(exitCode)
        stopSelfSafely()
    }

    
    private fun scheduleRepeat(exitCode: Int) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val seconds = REPEAT_DELAY_MS / 1000.0
        emit("[*] 重复模式：脚本已结束（退出码 $exitCode），${"%.1f".format(seconds)}s 后重新启动…")

        
        
        
        
        
        
        val nextId = RunStateStore.markStarting(this, lastMode)

        handler.postDelayed({
            
            
            
            if (!repeatMode || stopper.get()) {
                RunStateStore.clear(this)
                stopSelfSafely()
                return@postDelayed
            }
            runId = nextId
            startScript(lastMode, connectOnly = lastConnectOnly, longTimeout = longTimeout)
        }, REPEAT_DELAY_MS)
    }

    
    private fun shutDownHelpers() {
        watchdog?.shutdownNow()
        watchdog = null
        try {
            streamPool.shutdownNow()
        } catch (_: Exception) {
        }
        streamPool = Executors.newCachedThreadPool()
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

    

    private fun resetLogFile() {
        try {
            logFile(this).writeText("")
        } catch (_: Exception) {
            
        }
    }

    
    private fun appendToLogFile(line: String) {
        try {
            val file = logFile(this)
            if (file.length() > LOG_FILE_MAX_BYTES) {
                
                
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
        
        
        
        
        RunStateStore.clear(this)
    }

    

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
