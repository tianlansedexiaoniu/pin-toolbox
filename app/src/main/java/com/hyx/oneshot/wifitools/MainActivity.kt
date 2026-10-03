package com.hyx.oneshot.wifitools

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import java.io.File
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity(), RunService.Listener {

    private lateinit var logView: TextView
    private lateinit var logScroll: ScrollView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var fileContent: TextView
    private lateinit var filePathLabel: TextView
    private lateinit var fileStatus: TextView
    private lateinit var refreshButton: Button
    private lateinit var saveButton: ImageButton
    private lateinit var deleteButton: ImageButton
    private lateinit var pageHome: LinearLayout
    private lateinit var pageFiles: LinearLayout
    private lateinit var pageAbout: LinearLayout

    private lateinit var plan1Button: Button
    private lateinit var plan2Button: Button
    private lateinit var repeatCheck: CheckBox
    private lateinit var accuracyCheck: CheckBox

    
    private var selectedPlan: ScriptRunner.Mode = ScriptRunner.Mode.FULL

    
    private val homepageUrl = "https://github.com/tianlansedexiaoniu"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US)

    private val running = AtomicBoolean(false)

    
    private var attachedRunId: String? = null

    
    private var tailer: Thread? = null

    @Volatile
    private var tailing = false

    
    companion object {
        private const val REQ_PERMS = 1001

        
        private const val TAIL_INTERVAL_MS = 700L

        
        
        
        private const val COMPACT_BAR_MAX_WIDTH_DP = 400

        
        private const val COLOR_WARN_BG = 0xFFFDECEA.toInt()
        private const val COLOR_WARN_FG = 0xFFC5221F.toInt()

        
        private const val COLOR_INFO_BG = 0xFFE5F4FF.toInt()
        private const val COLOR_INFO_FG = 0xFF005C99.toInt()

        
        private const val COLOR_PLAN_IDLE_BG = 0xFFE8EAED.toInt()
        private const val COLOR_PLAN_IDLE_FG = 0xFF202124.toInt()

        
        private const val PREFS_UI = RunTileService.PREFS_UI
        private const val KEY_SELECTED_PLAN = RunTileService.KEY_SELECTED_PLAN

        
        private const val KEY_REPEAT_MODE = "repeat_mode"

        
        private const val KEY_ACCURACY = "accuracy_mode"

        
        private const val TILE_HINT =
            "提示：控制中心磁贴「连接附近WiFi」需要手动添加 — " +
                "下拉通知栏 → 再下拉一次 → 点「编辑」图标 → 拖入面板"
    }

    
    private val exportPasswordBook = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { target: Uri? ->
        
        
        if (target != null) writePasswordBookTo(target)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logView = findViewById(R.id.logView)
        logScroll = findViewById(R.id.logScroll)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        fileContent = findViewById(R.id.fileContent)
        filePathLabel = findViewById(R.id.filePathLabel)
        fileStatus = findViewById(R.id.fileStatus)
        refreshButton = findViewById(R.id.refreshButton)
        saveButton = findViewById(R.id.saveButton)
        deleteButton = findViewById(R.id.deleteButton)
        pageHome = findViewById(R.id.pageHome)
        pageFiles = findViewById(R.id.pageFiles)
        pageAbout = findViewById(R.id.pageAbout)

        plan1Button = findViewById(R.id.plan1Button)
        plan2Button = findViewById(R.id.plan2Button)
        repeatCheck = findViewById(R.id.repeatCheck)
        accuracyCheck = findViewById(R.id.accuracyCheck)

        
        
        selectedPlan = loadSelectedPlan()
        plan1Button.setOnClickListener { selectPlan(ScriptRunner.Mode.FULL) }
        plan2Button.setOnClickListener { selectPlan(ScriptRunner.Mode.MODE2) }
        renderPlanSelection()

        
        
        
        
        
        
        repeatCheck.isChecked = loadRepeatMode()
        repeatCheck.setOnCheckedChangeListener { _, checked ->
            saveRepeatMode(checked)
        }

        accuracyCheck.isChecked = loadAccuracy()
        accuracyCheck.setOnCheckedChangeListener { _, checked ->
            saveAccuracy(checked)
        }

        val tabs = findViewById<TabLayout>(R.id.tabLayout)
        tabs.addTab(tabs.newTab().setText("主页"))
        tabs.addTab(tabs.newTab().setText("密码本"))
        tabs.addTab(tabs.newTab().setText("关于"))
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                
                
                
                pageHome.visibility = if (tab.position == 0) View.VISIBLE else View.GONE
                if (tab.position == 1) {
                    pageFiles.visibility = View.VISIBLE
                    loadCredentialFile()
                } else {
                    pageFiles.visibility = View.GONE
                }
                pageAbout.visibility = if (tab.position == 2) View.VISIBLE else View.GONE
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        
        
        
        
        for (i in 0 until tabs.tabCount) {
            tabs.getTabAt(i)?.view?.let { tabView ->
                tabView.isLongClickable = false
                tabView.setOnLongClickListener { true }
                tabView.tooltipText = null
            }
        }

        logView.setTextIsSelectable(true)

        startButton.setOnClickListener { startScript() }
        stopButton.setOnClickListener { stopScript() }
        refreshButton.setOnClickListener { loadCredentialFile() }
        saveButton.setOnClickListener { exportPasswordBook() }
        deleteButton.setOnClickListener { confirmDeletePasswordBook() }

        findViewById<TextView>(R.id.aboutLink).setOnClickListener { openHomepage() }

        
        
        
        startButton.isLongClickable = false
        stopButton.isLongClickable = false
        refreshButton.isLongClickable = false
        saveButton.isLongClickable = false
        deleteButton.isLongClickable = false
        plan1Button.isLongClickable = false
        plan2Button.isLongClickable = false

        
        
        
        filePathLabel.visibility = View.GONE

        RunService.listener = this
        
        
        RunService.stateListener = { RunTileService.refresh(this) }

        requestRuntimePermissions()
        prepareRuntimeAsync()
        requestRootAsync()

        
        
        
        
        
    }

    
    override fun onResume() {
        super.onResume()
        resumeTerminal()
    }

    override fun onPause() {
        super.onPause()
        
        
        
        
        if (RunService.listener === this) {
            RunService.listener = null
        }
        stopTailer()
    }

    private fun resumeTerminal() {
        RunService.listener = this

        val snapshot = RunStateStore.snapshot(this)
        val stale = RunStateStore.isStale(attachedRunId, snapshot)

        
        
        
        
        val ownedHere = RunService.isRunning

        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        val liveRunning = ownedHere || snapshot.running

        if (liveRunning && stale) {
            
            
            
            
            
            attachedRunId = snapshot.runId
            clearTerminal()
            renderRunState(true)
            appendLog(TILE_HINT)
            appendLog("[*] 已通过磁贴在后台启动，以下是本次运行的输出：")
            startTailerIfNeeded()
            if (ownedHere) replayTranscriptTail(notice = false)
        } else if (liveRunning) {
            
            
            
            renderRunState(true)
            startTailerIfNeeded()
            if (ownedHere) replayTranscriptTail(notice = false)
        } else {
            stopTailer()
            renderRunState(false)

            
            
            
            
            if (attachedRunId != null) {
                replayTranscriptTail(notice = true)
            }
            attachedRunId = null
        }

        RunTileService.refresh(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (RunService.listener === this) {
            RunService.listener = null
        }
        stopTailer()
        io.shutdown()
    }

    

    
    private fun startTailerIfNeeded() {
        if (RunService.isRunning) {
            stopTailer()
            return
        }
        if (tailing) return

        tailing = true
        
        
        
        val file = RunService.logFile(this)
        var offset = if (file.exists()) file.length() else 0L

        val worker = Thread {
            while (tailing) {
                try {
                    if (file.exists()) {
                        if (file.length() < offset) {
                            
                            offset = 0L
                        }
                        if (file.length() > offset) {
                            
                            
                            
                            
                            val want = (file.length() - offset)
                                .toInt().coerceAtMost(64 * 1024)
                            val bytes = ByteArray(want)
                            RandomAccessFile(file, "r").use { raf ->
                                raf.seek(offset)
                                raf.readFully(bytes)
                                offset = raf.filePointer
                            }
                            for (line in String(bytes, Charsets.UTF_8).split('\n')) {
                                if (line.isNotEmpty()) {
                                    
                                    
                                    
                                    
                                    mainHandler.post { appendLog(line) }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    
                }

                try {
                    Thread.sleep(TAIL_INTERVAL_MS)
                } catch (_: InterruptedException) {
                    return@Thread
                }
            }
        }
        worker.isDaemon = true
        tailer = worker
        worker.start()
    }

    private fun stopTailer() {
        tailing = false
        tailer = null
    }

    
    private fun replayTranscriptTail(notice: Boolean) {
        try {
            val file = RunService.logFile(this)
            if (!file.exists() || file.length() == 0L) return

            val tailBytes = 8 * 1024
            val start = (file.length() - tailBytes).coerceAtLeast(0L)
            val slice = RandomAccessFile(file, "r").use { raf ->
                raf.seek(start)
                ByteArray((file.length() - start).toInt().coerceAtMost(64 * 1024)).also {
                    raf.readFully(it)
                }
            }
            val text = String(slice, Charsets.UTF_8)

            
            
            
            val lines = text.split('\n')
                .let { if (start > 0L) it.drop(1) else it }
                .filter { it.isNotBlank() }
            if (lines.isEmpty()) return

            val shown = logView.text?.toString().orEmpty()
            val shownLines = shown.split('\n').toHashSet()
            val fresh = lines.filter { it !in shownLines }
            if (fresh.isEmpty()) return

            if (notice) appendLog("脚本已停止运行")
            fresh.forEach { appendLog(it) }
        } catch (_: Exception) {
            
        }
    }

    

    
    
    
    
    
    
    

    

    private fun prepareRuntimeAsync() {
        appendLog("[*] 正在准备内置 Python 运行环境…")
        io.execute {
            try {
                PythonInstaller(this).ensureDeployed { line ->
                    mainHandler.post { appendLog(line) }
                }
                mainHandler.post {
                    appendLog("[+] 运行环境就绪。")
                    appendLog("[*] 脚本目录：${PythonInstaller.scriptDir(this).absolutePath}")
                }
            } catch (err: Exception) {
                mainHandler.post { appendLog("[!] 运行环境部署失败：${err.message}") }
            }
        }
    }

    

    
    private fun requestRootAsync() {
        io.execute {
            val status = ScriptRunner.requestRoot()
            mainHandler.post {
                when (status) {
                    ScriptRunner.RootStatus.GRANTED ->
                        appendLog("[+] 已获取 root 权限，脚本将以 uid 0 运行。")

                    ScriptRunner.RootStatus.DENIED ->
                        appendLog("[!] root 请求被拒绝。脚本需要 root 才能运行，请在弹出的授权窗口中允许本应用。")

                    ScriptRunner.RootStatus.NOT_ROOTED ->
                        appendLog("[!] 未检测到 su，设备可能未 root。脚本将尝试以普通权限运行，但很可能失败。")

                    ScriptRunner.RootStatus.ERROR ->
                        appendLog("[!] root 请求过程中出现异常，将尝试以普通权限运行。")
                }
            }
        }
    }

    

    
    private fun loadCredentialFile() {
        val file = ScriptRunner.wifiPasswordFile(this)

        io.execute {
            val (text, error) = try {
                when {
                    !file.exists() -> null to emptyVaultMessage(file)
                    !file.canRead() -> null to
                        "无法读取密码本，请检查文件权限。\n\n密码本路径:\n${file.absolutePath}"
                    else -> {
                        val body = file.readText(Charsets.UTF_8)
                        if (body.isBlank()) "" to null else body to null
                    }
                }
            } catch (err: Exception) {
                null to "读取失败：${err.javaClass.simpleName}: ${err.message}"
            }

            mainHandler.post {
                
                
                filePathLabel.visibility = View.GONE

                when {
                    
                    
                    error != null -> {
                        
                        
                        
                        
                        paintStatus(error, warn = !file.exists())
                        fileContent.text = ""
                    }
                    
                    
                    text.isNullOrEmpty() -> {
                        paintStatus(
                            "密码本还是空的 — 等脚本破解出一个网络后，凭据就会出现在这里。",
                            warn = false
                        )
                        fileContent.text = ""
                    }
                    else -> {
                        fileStatus.visibility = View.GONE
                        fileContent.text = text
                    }
                }
            }
        }
    }

    
    private fun paintStatus(message: String, warn: Boolean) {
        fileStatus.visibility = View.VISIBLE
        fileStatus.text = message
        fileStatus.setBackgroundColor(if (warn) COLOR_WARN_BG else COLOR_INFO_BG)
        fileStatus.setTextColor(if (warn) COLOR_WARN_FG else COLOR_INFO_FG)
    }

    
    private fun emptyVaultMessage(file: File): String =
        "未检测到密码本，点击主页开始你的第一次尝试吧。\n\n" +
            "密码本路径:\n${file.absolutePath}"

    

    
    private fun exportPasswordBook() {
        val file = ScriptRunner.wifiPasswordFile(this)
        val name = file.name

        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(this, "密码本还是空的，暂无可保存内容", Toast.LENGTH_SHORT).show()
            return
        }

        
        
        exportPasswordBook.launch(name)
    }

    
    private fun writePasswordBookTo(target: Uri) {
        val file = ScriptRunner.wifiPasswordFile(this)

        io.execute {
            val outcome = try {
                val body = file.readText(Charsets.UTF_8)
                contentResolver.openOutputStream(target, "wt")?.use { out ->
                    out.write(body.toByteArray(Charsets.UTF_8))
                    out.flush()
                } ?: throw IllegalStateException("无法打开目标文件")
                null
            } catch (err: Exception) {
                "${err.javaClass.simpleName}: ${err.message}"
            }

            mainHandler.post {
                if (outcome == null) {
                    Toast.makeText(this, "密码本已保存到所选位置", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "保存失败：$outcome", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    
    private fun confirmDeletePasswordBook() {
        val dialog = AlertDialog.Builder(this).create()

        
        
        
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padH = if (screenWidthDp() < COMPACT_BAR_MAX_WIDTH_DP) 16 else 22
            setPadding(dp(padH), dp(20), dp(padH), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(COLOR_WARN_BG)
            }
        }

        val message = TextView(this).apply {
            text = "是否删除当前保存的密码？请确认密码已保存。"
            setTextColor(COLOR_WARN_FG)
            textSize = 14f
            setLineSpacing(dp(4).toFloat(), 1f)
        }
        content.addView(message)

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            
            gravity = android.view.Gravity.END
            setPadding(0, dp(14), 0, 0)
        }

        val no = TextView(this).apply {
            text = "否"
            
            
            
            setTextColor(0xFF5F6368.toInt())
            textSize = 15f
            
            
            setPadding(dp(20), dp(10), dp(20), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(Color.TRANSPARENT)
            }
            isClickable = true
            setOnClickListener { dialog.dismiss() }
        }

        val yes = TextView(this).apply {
            text = "是"
            setTextColor(COLOR_WARN_FG)
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(20), dp(10), dp(20), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(Color.TRANSPARENT)
            }
            isClickable = true
            setOnClickListener {
                dialog.dismiss()
                deletePasswordBook()
            }
        }

        
        buttons.addView(no)
        buttons.addView(yes)
        content.addView(buttons)

        dialog.setView(content)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        tuneDialogForScreen(dialog)
    }

    
    private fun deletePasswordBook() {
        val file = ScriptRunner.wifiPasswordFile(this)

        io.execute {
            val error = try {
                if (file.exists() && !file.delete()) {
                    "无法删除文件，请检查文件权限"
                } else {
                    null
                }
            } catch (err: Exception) {
                "${err.javaClass.simpleName}: ${err.message}"
            }

            mainHandler.post {
                if (error != null) {
                    Toast.makeText(this, "删除失败：$error", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "密码本已删除", Toast.LENGTH_SHORT).show()
                }
                
                
                loadCredentialFile()
            }
        }
    }

    
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    
    
    
    
    private fun tuneDialogForScreen(dialog: AlertDialog) {
        val dm = resources.displayMetrics
        val widthDp = (dm.widthPixels / dm.density).toInt()

        
        val contentWidthDp = (widthDp - 64).coerceAtMost(320).coerceAtLeast(220)
        dialog.window?.setLayout(dp(contentWidthDp), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    
    private fun screenWidthDp(): Int {
        val dm = resources.displayMetrics
        val w = if (dm.widthPixels > dm.heightPixels) dm.heightPixels else dm.widthPixels
        return (w / dm.density).toInt()
    }

    

    
    private fun loadSelectedPlan(): ScriptRunner.Mode {
        val stored = getSharedPreferences(PREFS_UI, MODE_PRIVATE)
            .getString(KEY_SELECTED_PLAN, null)

        return runCatching { ScriptRunner.Mode.valueOf(stored ?: "") }
            .getOrDefault(ScriptRunner.Mode.FULL)
    }

    
    private fun loadRepeatMode(): Boolean =
        getSharedPreferences(PREFS_UI, MODE_PRIVATE)
            .getBoolean(KEY_REPEAT_MODE, false)

    private fun saveRepeatMode(enabled: Boolean) {
        getSharedPreferences(PREFS_UI, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_REPEAT_MODE, enabled)
            .apply()
    }

    
    private fun loadAccuracy(): Boolean =
        getSharedPreferences(PREFS_UI, MODE_PRIVATE)
            .getBoolean(KEY_ACCURACY, false)

    private fun saveAccuracy(enabled: Boolean) {
        getSharedPreferences(PREFS_UI, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ACCURACY, enabled)
            .apply()
    }

    
    private fun selectPlan(mode: ScriptRunner.Mode) {
        if (running.get()) {
            Toast.makeText(this, "运行中无法切换方案", Toast.LENGTH_SHORT).show()
            return
        }

        if (mode == selectedPlan) {
            return
        }

        
        
        
        
        
        
        
        
        if (mode == ScriptRunner.Mode.MODE2) {
            confirmPlan2Caveat { commitPlan(mode) }
            return
        }

        commitPlan(mode)
    }

    
    private fun commitPlan(mode: ScriptRunner.Mode) {
        selectedPlan = mode

        getSharedPreferences(PREFS_UI, MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_PLAN, mode.name)
            .apply()

        renderPlanSelection()
    }

    
    private fun confirmPlan2Caveat(onAccept: () -> Unit) {
        val dialog = AlertDialog.Builder(this).create()

        
        var declined = false
        var accepted = false

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padH = if (screenWidthDp() < COMPACT_BAR_MAX_WIDTH_DP) 16 else 22
            setPadding(dp(padH), dp(20), dp(padH), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(android.graphics.Color.WHITE)
            }
        }

        content.addView(TextView(this).apply {
            text = "方案2 提示"
            setTextColor(COLOR_WARN_FG)
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        content.addView(TextView(this).apply {
            text = "部分情况自动断开WiFi功能可能无效，如发现反复出现\n" +
                    "[*] Scanning…\n" +
                    "[*] Associating with AP…\n" +
                    "请尝试自行关闭WiFi再打开以重置。"
            setTextColor(COLOR_WARN_FG)
            textSize = 14f
            setLineSpacing(dp(4).toFloat(), 1f)
            setPadding(0, dp(10), 0, 0)
        })

        
        
        
        
        
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16) }
            layoutParams = lp
        }

        row.addView(Button(this).apply {
            text = "知道了"
            setTextColor(COLOR_WARN_FG)
            isAllCaps = false
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(android.graphics.Color.TRANSPARENT)
                setStroke(dp(1), COLOR_WARN_FG)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(6); marginEnd = dp(6) }
            setOnClickListener {
                accepted = true
                dialog.dismiss()
                onAccept()
            }
        })

        content.addView(row)

        
        
        val scroll = android.widget.ScrollView(this).apply {
            addView(content)
            isFillViewport = true
            setPadding(0, 0, 0, dp(10))
        }

        dialog.setView(scroll)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)

        
        
        
        
        
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK &&
                event.action == android.view.KeyEvent.ACTION_UP) {
                declined = true
                dialog.dismiss()
                true
            } else {
                false
            }
        }

        
        
        
        
        
        dialog.setOnDismissListener {
            if (!declined && !accepted) {
                onAccept()
            }
        }

        dialog.show()
        tuneDialogForScreen(dialog)
    }

    
    private fun renderPlanSelection() {
        val activeBg = ContextCompat.getColor(this, R.color.brand_blue)

        fun paint(button: Button, active: Boolean) {
            button.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (active) activeBg else COLOR_PLAN_IDLE_BG
            )
            button.setTextColor(
                if (active) android.graphics.Color.WHITE else COLOR_PLAN_IDLE_FG
            )
        }

        paint(plan1Button, selectedPlan == ScriptRunner.Mode.FULL)
        paint(plan2Button, selectedPlan == ScriptRunner.Mode.MODE2)
    }

    
    private fun openHomepage() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(homepageUrl)))
        } catch (err: Exception) {
            Toast.makeText(this, homepageUrl, Toast.LENGTH_LONG).show()
        }
    }

    

    private fun startScript() {
        if (running.get()) {
            Toast.makeText(this, "脚本正在运行中", Toast.LENGTH_SHORT).show()
            return
        }
        if (!File(PythonInstaller.pythonRoot(this), "lib/python3.11/os.py").exists()) {
            Toast.makeText(this, "运行环境仍在准备中，请稍候", Toast.LENGTH_SHORT).show()
            return
        }

        
        
        clearTerminal()

        
        
        
        renderRunState(true)
        appendLog(TILE_HINT)
        appendLog("──────── ${timestamp.format(Date())} 启动脚本 ────────")

        
        
        
        
        
        
        
        
        
        
        val intent = Intent(this, RunService::class.java)
            .setAction(RunService.ACTION_START)
            .putExtra(RunService.EXTRA_MODE, selectedPlan.name)
            .putExtra(RunService.EXTRA_REPEAT, repeatCheck.isChecked)
            .putExtra(RunService.EXTRA_LONG_TIMEOUT, accuracyCheck.isChecked)
        ContextCompat.startForegroundService(this, intent)
    }

    
    private fun stopScript() {
        if (!running.get()) {
            Toast.makeText(this, "脚本当前未运行", Toast.LENGTH_SHORT).show()
            return
        }

        renderRunState(false)
        appendLog("[*] 正在停止脚本…")

        val intent = Intent(this, RunService::class.java).setAction(RunService.ACTION_STOP)
        try {
            startService(intent)
        } catch (_: Exception) {
            ContextCompat.startForegroundService(this, intent)
        }
    }

    
    private fun renderRunState(isRunning: Boolean) {
        running.set(isRunning)
        startButton.isEnabled = !isRunning
        stopButton.isEnabled = isRunning

        
        
        
        val accent = ContextCompat.getColor(this, R.color.brand_blue)
        val idleBg = ContextCompat.getColor(this, R.color.button_idle)
        val disabledText = ContextCompat.getColor(this, R.color.button_disabled_text)
        val onAccent = ContextCompat.getColor(this, R.color.button_on_accent)

        startButton.backgroundTintList =
            android.content.res.ColorStateList.valueOf(if (isRunning) idleBg else accent)
        startButton.setTextColor(if (isRunning) disabledText else onAccent)

        stopButton.backgroundTintList =
            android.content.res.ColorStateList.valueOf(if (isRunning) accent else idleBg)
        stopButton.setTextColor(if (isRunning) onAccent else disabledText)
    }

    
    override fun onScriptStopped(exitCode: Int) {
        mainHandler.post {
            if (!running.get()) return@post

            stopTailer()
            renderRunState(false)
            appendLog("脚本已停止运行")

            if (exitCode != 0) {
                appendLog("──────── 进程退出，退出码 $exitCode ────────")
            }

            
            RunTileService.refresh(this)
        }
    }

    

    override fun onLog(line: String) {
        mainHandler.post { appendLog(normalizeForMonospace(line)) }
    }

    
    private fun clearTerminal() {
        logView.text = ""
    }

    private fun appendLog(line: String) {
        val safe = normalizeForMonospace(line)
        val current = logView.text?.toString().orEmpty()
        val next = if (current.isEmpty()) safe else "$current\n$safe"

        
        val trimmed = if (next.length > 240_000) {
            next.substring(next.length - 200_000)
        } else {
            next
        }

        logView.text = trimmed
        logScroll.post { logScroll.fullScroll(View.FOCUS_DOWN) }
    }

    
    
    private fun normalizeForMonospace(line: String): String {
        if (line.isEmpty()) return line

        var touched = false
        val out = StringBuilder(line.length)
        for (ch in line) {
            val replacement = when (ch) {
                
                '\u258F', '\u258E', '\u258D', '\u258C', '\u258B',
                '\u258A', '\u2589', '\u2588' -> {
                    touched = true
                    '|'
                }
                '\u2581', '\u2582', '\u2583', '\u2584' -> {
                    touched = true
                    '_'
                }
                '\u2590' -> {
                    touched = true
                    '|'
                }
                else -> ch
            }
            out.append(replacement)
        }
        return if (touched) out.toString() else line
    }

    

    private fun requestRuntimePermissions() {
        val wanted = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            wanted += Manifest.permission.POST_NOTIFICATIONS
            wanted += Manifest.permission.NEARBY_WIFI_DEVICES
        }
        wanted += Manifest.permission.ACCESS_FINE_LOCATION

        val missing = wanted.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQ_PERMS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !Environment.isExternalStorageManager()
        ) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = android.net.Uri.parse("package:$packageName")
                
                
                
                
                
                startActivity(intent)
            } catch (_: Exception) {
                
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMS) {
            appendLog("[*] 权限请求已处理。")
        }
    }
}
