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

    /** Shown on the 关于 tab. Hard-coded because it is a fixed personal link. */
    private val homepageUrl = "https://github.com/tianlansedexiaoniu"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US)

    private val running = AtomicBoolean(false)

    /**
     * Identity of the run the terminal is currently showing.
     *
     * Null means "not attached to any run". Compared against the id in the
     * shared store to answer the only question that matters when the activity
     * becomes visible again: *is the thing on screen still the thing that is
     * running?* If it is not, the pane is re-attached. See [resumeTerminal].
     */
    private var attachedRunId: String? = null

    /**
     * Fallback log tailing, used only when the script runs in another process.
     *
     * The normal path is a direct callback from [RunService]; this exists
     * because that callback cannot cross a process boundary, and a run started
     * from the tile while the app was dead can end up exactly there.
     */
    private var tailer: Thread? = null

    @Volatile
    private var tailing = false

    companion object {
        private const val REQ_PERMS = 1001

        /** How often the out-of-process fallback checks the transcript file. */
        private const val TAIL_INTERVAL_MS = 700L

        /**
         * The warning plate: pale red ground, deep red text.
         *
         * Shared by the "no 密码本" state and the delete-confirmation dialog on
         * purpose — see [confirmDeletePasswordBook]. One pair of constants for
         * both means the two can never drift into looking like different
         * severities.
         */
        private const val COLOR_WARN_BG = 0xFFFDECEA.toInt()
        private const val COLOR_WARN_FG = 0xFFC5221F.toInt()

        /** The ordinary (informational) plate, for everything that is not a warning. */
        private const val COLOR_INFO_BG = 0xFFE5F4FF.toInt()
        private const val COLOR_INFO_FG = 0xFF005C99.toInt()

        /**
         * The first line of every terminal session.
         *
         * Written on each fresh start rather than only once, because the log
         * buffer is cleared between runs — this is the only reliable place to
         * put a persistent hint, and the user asked for it here specifically.
         */
        private const val TILE_HINT =
            "提示：控制中心磁贴「扫描周围WiFi」需要手动添加 — " +
                "下拉通知栏 → 再下拉一次 → 点「编辑」图标 → 拖入面板"
    }

    /**
     * The system "save file" surface, for the 密码本 export button.
     *
     * `CreateDocument` is used rather than a plain `ACTION_CREATE_DOCUMENT`
     * intent so the contract does the plumbing: it takes the bare file name as
     * input, drives the whole picker, and hands back the resulting [Uri] (or
     * null if the user backs out). Everything about *where* the file lands is
     * left to the user and to the storage provider they pick — which is the
     * point of going through SAF at all: the app never needs a broad storage
     * permission just to let someone take their own credentials out.
     *
     * Registered as a field initialiser, i.e. before `onCreate` runs, because
     * `registerForActivityResult` must happen before the activity is STARTED,
     * and `onCreate` is the only sane place whose ordering we control.
     */
    private val exportPasswordBook = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { target: Uri? ->
        // A null Uri means the user dismissed the picker; that is a normal
        // outcome, not a failure, so it is silent.
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

        val tabs = findViewById<TabLayout>(R.id.tabLayout)
        tabs.addTab(tabs.newTab().setText("主页"))
        tabs.addTab(tabs.newTab().setText("密码本"))
        tabs.addTab(tabs.newTab().setText("关于"))
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // A three-way swap rather than an else-branch: with only two
                // panes, "not 主页" quietly meant "密码本". A third tab makes
                // that assumption wrong, so every pane is now named explicitly.
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

        // TabLayout installs its own long-press listener that pops the black
        // rounded tooltip seen over the tab strip. isLongClickable=false on the
        // TabView is not enough because TabLayout re-registers one, so the
        // listener is replaced with a consumer that also swallows the event.
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

        // Nothing useful is shown on long-press, and the default black tooltip
        // Android pops over a Button is pure noise. Every clickable control in
        // the layout is explicitly made non-long-clickable.
        startButton.isLongClickable = false
        stopButton.isLongClickable = false
        refreshButton.isLongClickable = false
        saveButton.isLongClickable = false
        deleteButton.isLongClickable = false

        // The 密码本 tab shows no separate path header: the empty state and the
        // error states each carry the path inline, so a permanent line above
        // the pane would only duplicate it.
        filePathLabel.visibility = View.GONE

        RunService.listener = this
        // Mirror every run-state change into the control-centre tile, whichever
        // entry point started the run (in-app button or the tile itself).
        RunService.stateListener = { RunTileService.refresh(this) }

        requestRuntimePermissions()
        prepareRuntimeAsync()
        requestRootAsync()

        // Deliberately *not* renderRunState(RunService.isRunning) here: onCreate
        // is also the cold-start path when the user taps the notification, and
        // onResume runs immediately afterwards with the authoritative answer.
        // Doing the work twice is what used to make the button and the terminal
        // disagree for a frame.
    }

    /**
     * Re-attaches the terminal to the live run every time the activity becomes
     * visible.
     *
     * This is the fix for "I started it from the tile, came back through the
     * notification, and the terminal never refreshed". The activity's own state
     * is stale by definition after a pause — the tile can start or stop a run
     * while the app is hidden, and the notification can relaunch an activity
     * that still describes the world from before.
     *
     * The decision is made on the run's *identity*, not on a boolean:
     *
     *   * a live run whose id differs from [attachedRunId] is one this terminal
     *     has never shown -> clear the pane and re-attach;
     *   * a live run with the same id is the one already on screen -> leave the
     *     text alone, so a rotation or a trip to another app does not wipe the
     *     transcript the user is reading;
     *   * no live run -> stop tailing, but keep whatever was displayed, because
     *     the output of a finished run is still worth reading.
     */
    override fun onResume() {
        super.onResume()
        resumeTerminal()
    }

    override fun onPause() {
        super.onPause()
        // Detach the callback so the service never holds a reference to a
        // stopped activity, and stop the fallback tailer so it cannot outlive
        // the view it is writing into. The transcript file is untouched, so a
        // later resume replays from it if it needs to.
        if (RunService.listener === this) {
            RunService.listener = null
        }
        stopTailer()
    }

    private fun resumeTerminal() {
        RunService.listener = this

        val snapshot = RunStateStore.snapshot(this)
        val stale = RunStateStore.isStale(attachedRunId, snapshot)

        // Whether this process's service owns the run. Only meaningful once
        // something has actually been started in it, but that is precisely the
        // case the distinction is for: false + running in the store = the run
        // belongs to another process, and only the file can be read.
        val ownedHere = RunService.isRunning

        if (snapshot.running && stale) {
            // A run this terminal has never shown is in flight. Clear the pane
            // and re-attach. Both delivery paths are armed: the in-memory
            // callback for the in-process case, the file tail for the other.
            // They cannot double up, because startTailerIfNeeded() declines to
            // run when this process owns the child.
            attachedRunId = snapshot.runId
            clearTerminal()
            renderRunState(true)
            appendLog(TILE_HINT)
            appendLog("[*] 已通过磁贴在后台启动，以下是本次运行的输出：")
            startTailerIfNeeded()
            if (ownedHere) replayTranscriptTail(notice = false)
        } else if (snapshot.running) {
            // Same run, still going — a rotation or a brief trip to another app.
            // Keep the transcript, re-arm delivery, and top up with anything
            // that was logged while the pane was detached.
            renderRunState(true)
            startTailerIfNeeded()
            if (ownedHere) replayTranscriptTail(notice = false)
        } else {
            stopTailer()
            renderRunState(false)

            // The run this terminal was showing has ended. If the activity was
            // hidden when that happened, the closing lines never reached the
            // pane, so replay the tail rather than leaving a transcript that
            // stops mid-sentence.
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

    // ---------------------------------------------------------- out-of-process

    /**
     * Starts the transcript tailer, unless this process is the one running the
     * script.
     *
     * The check is the point: [RunService.isRunning] is only true in the process
     * that owns the child, so an activity that finds it false while the shared
     * store says a run is live is by definition in the other process. Starting
     * the tailer in the owning process would duplicate every line, because it
     * already receives them through [onLog].
     */
    private fun startTailerIfNeeded() {
        if (RunService.isRunning) {
            stopTailer()
            return
        }
        if (tailing) return

        tailing = true
        // Seed the position at the current end of file so only new output is
        // appended; the file was truncated when the run started, so anything
        // already in it is history the pane does not need re-showing.
        val file = RunService.logFile(this)
        var offset = if (file.exists()) file.length() else 0L

        val worker = Thread {
            while (tailing) {
                try {
                    if (file.exists()) {
                        if (file.length() < offset) {
                            // Truncated or replaced -> a new run began.
                            offset = 0L
                        }
                        if (file.length() > offset) {
                            // Read a bounded chunk as bytes; the file is UTF-8
                            // and a multi-byte character can straddle the
                            // boundary, so the decoder is allowed to replace
                            // rather than throw.
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
                                    // The service tail-trims this file, so a
                                    // partial line can appear at a boundary;
                                    // showing it is harmless and next tick
                                    // completes it.
                                    mainHandler.post { appendLog(line) }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Never let a transient read error kill the tailer.
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

    /**
     * Shows the closing lines of a run that ended while the activity was away,
     * or tops up a live one after the pane was detached.
     *
     * Only used when this process owns the child. When it does not, the tailer
     * is already covering the same ground and running both would duplicate
     * every line.
     *
     * Deduplication is by content rather than by byte offset: the pane holds a
     * trimmed copy of what it has already printed, so a line that is already
     * there is simply skipped. That is far more robust than trying to track a
     * file offset across a rotation, and the cost is one `contains` per line of
     * a few kilobytes.
     *
     * @param notice whether to open with the "script stopped" line. True when
     *        the run is over; false when this is a live top-up, where that line
     *        would be a lie.
     */
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

            // Drop the first line whenever the tail did not begin at the very
            // start of the file: it is almost certainly a fragment of a line
            // that is already on screen.
            val lines = text.split('\n')
                .let { if (start > 0L) it.drop(1) else it }
                .filter { it.isNotBlank() }
            if (lines.isEmpty()) return

            val shown = logView.text?.toString().orEmpty()
            val fresh = lines.filter { !shown.contains(it) }
            if (fresh.isEmpty()) return

            if (notice) appendLog("脚本已停止运行")
            fresh.forEach { appendLog(it) }
        } catch (_: Exception) {
            // The pane simply keeps what it had.
        }
    }

    // ------------------------------------------------------------------ tiles

    /**
     * Puts the "add the tile yourself" reminder in the terminal, as its first
     * line.
     *
     * Android gives apps no way to insert their own quick-settings tile — the
     * user has to drag it in from the control-centre editor — so the
     * information still has to live somewhere; the terminal is where it costs
     * nothing.
     *
     * Shown on every fresh start (see [TILE_HINT]), including when the tile has
     * already been added, because the log buffer is wiped between runs and a
     * conditional line would make the layout jump around.
     */
    private fun showTileHint() {
        appendLog(TILE_HINT)
    }

    // ---------------------------------------------------------------- runtime

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

    // ------------------------------------------------------------------ root

    /**
     * Actively asks for root on startup.
     *
     * The script refuses to run without uid 0 (`os.getuid() != 0` -> die) and it
     * needs raw nl80211 access, so root is not optional here. Rather than
     * silently probing for a su binary, this triggers the root manager's
     * authorization dialog and reports the outcome in the log area so the user
     * always knows which execution mode the app is in.
     */
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

    // ------------------------------------------------------------------ tabs

    /**
     * Renders the 密码本 tab.
     *
     * The tab shows exactly one of two things:
     *
     *   * the contents of wifipassword.txt, when it exists and has something in
     *     it, or
     *   * a single prompt telling the user there is nothing yet and what to do
     *     about it.
     *
     * Earlier versions printed a separate "路径: …" line above the content and
     * repeated the path again inside the empty-state text. Both are gone: the
     * header line was redundant once the empty state carries the path, and
     * showing it over real credentials just added noise.
     */
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
                // The permanent path header is unused; the empty state and the
                // error states each carry the path inline.
                filePathLabel.visibility = View.GONE

                when {
                    // Nothing generated yet, or nothing readable: show the one
                    // prompt and leave the pane below empty.
                    error != null -> {
                        // Red only for "no 密码本 at all" — the one state that
                        // needs the user to act. The other messages reuse the
                        // same plate in the ordinary card colours, so the red is
                        // never diluted into meaning nothing.
                        paintStatus(error, warn = !file.exists())
                        fileContent.text = ""
                    }
                    // The file exists but is blank — the run happened, no
                    // network cracked. Different message, same pane.
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

    /**
     * Shows [message] in the status plate, red only when it is a warning.
     *
     * The red-on-pale-red pair is reserved for "no 密码本 exists", the one case
     * where the user has to do something. Everything else keeps the ordinary
     * card look, so a glance at the colour alone tells you whether there is a
     * problem.
     */
    private fun paintStatus(message: String, warn: Boolean) {
        fileStatus.visibility = View.VISIBLE
        fileStatus.text = message
        fileStatus.setBackgroundColor(if (warn) COLOR_WARN_BG else COLOR_INFO_BG)
        fileStatus.setTextColor(if (warn) COLOR_WARN_FG else COLOR_INFO_FG)
    }

    /** The one line shown when no 密码本 exists yet. */
    private fun emptyVaultMessage(file: File): String =
        "未检测到密码本，点击主页开始你的第一次尝试吧。\n\n" +
            "密码本路径:\n${file.absolutePath}"

    // ----------------------------------------------------- 密码本: export/delete

    /**
     * The 保存 icon: hands the password book to the system file picker.
     *
     * Two things are deliberate here:
     *
     *   1. **The suggested name is copied verbatim from the real file**, so the
     *      export lands as `wifipassword.txt` unless the user renames it — the
     *      user asked for the saved file to carry the same name as the password
     *      book, and deriving it from [ScriptRunner.wifiPasswordFile] means the
     *      two can never drift apart if the file is ever renamed.
     *   2. **A missing or empty book still opens the picker**, but says so
     *      first. Silently exporting nothing would look like the button is
     *      broken.
     */
    private fun exportPasswordBook() {
        val file = ScriptRunner.wifiPasswordFile(this)
        val name = file.name

        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(this, "密码本还是空的，暂无可保存内容", Toast.LENGTH_SHORT).show()
            return
        }

        // SAF, not a raw path: no storage permission is needed, and the user
        // decides where their own credentials end up.
        exportPasswordBook.launch(name)
    }

    /**
     * Writes the book to the [target] the user chose in the picker.
     *
     * Done entirely off the UI thread, and any failure surfaces as a toast
     * rather than an exception — a read-only provider or a revoked URI should
     * leave the app usable, and the original file is never modified either way.
     */
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

    /**
     * The 垃圾桶 icon: asks before deleting, in the same red the "no 密码本"
     * warning uses.
     *
     * The red plate is the point. Deleting the password book throws away every
     * credential collected so far and there is no undo, so this dialog is
     * styled to look *exactly like the warning state on the page behind it*
     * (`#FDECEA` on `#C5221F` — the same pair as [paintStatus]) rather than
     * with the stock Material dialog colours. A user who has seen the red
     * warning once already reads this as "the same kind of thing".
     *
     * The affirmative button is on the **right** and the dismissive one on the
     * **left**, which is both what was asked for ("把是和否按钮反过来") and what
     * the platform itself does — Android's own dialogs put the cancel-ish
     * choice on the left. AlertDialog's convenience API cannot express this
     * order, so the two labels are placed explicitly rather than through
     * setPositiveButton / setNegativeButton.
     *
     * Colour on the two labels carries the weight: 「是」 keeps the deep red of
     * the card (this is the dangerous one), while 「否」 drops back to the
     * ordinary body grey — the quieter of the two, which is the right signal
     * for the choice that does nothing.
     */
    private fun confirmDeletePasswordBook() {
        val dialog = AlertDialog.Builder(this).create()

        // The dialog's own window is transparent-wrapped by the framework; the
        // rounded red card is drawn by the custom view below so the shape and
        // colour survive on every API level.
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(8))
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
            // Right-aligned, like every other dialog footer on the platform.
            gravity = android.view.Gravity.END
            setPadding(0, dp(14), 0, 0)
        }

        val no = TextView(this).apply {
            text = "否"
            // Back to the ordinary grey. It has to be visibly *quieter* than
            // 「是」 so the destructive choice is the one that stands out — a
            // white label was too loud for the dismissive option.
            setTextColor(0xFF5F6368.toInt())
            textSize = 15f
            // Generous touch target, and a flat hit area so the two words do
            // not look like a pair of stock buttons.
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

        // 否 on the left, 是 on the right.
        buttons.addView(no)
        buttons.addView(yes)
        content.addView(buttons)

        dialog.setView(content)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
    }

    /** Actually removes the password book, then repaints the tab. */
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
                // Either way, re-read: on success this shows the empty-state
                // prompt, on failure it shows the file that is still there.
                loadCredentialFile()
            }
        }
    }

    /** dp -> px, for the hand-built dialog above. */
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    /**
     * Opens the author's homepage in whatever browser the user has.
     *
     * Wrapped in a try/catch on purpose: a device with no browser (or with
     * every browser disabled) has nothing to resolve ACTION_VIEW, and
     * `startActivity` would throw ActivityNotFoundException — crashing the
     * app because someone tapped a link. Falling back to a Toast at least
     * tells them the address.
     */
    private fun openHomepage() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(homepageUrl)))
        } catch (err: Exception) {
            Toast.makeText(this, homepageUrl, Toast.LENGTH_LONG).show()
        }
    }

    // -------------------------------------------------------------- start/stop

    private fun startScript() {
        if (running.get()) {
            Toast.makeText(this, "脚本正在运行中", Toast.LENGTH_SHORT).show()
            return
        }
        if (!File(PythonInstaller.pythonRoot(this), "lib/python3.11/os.py").exists()) {
            Toast.makeText(this, "运行环境仍在准备中，请稍候", Toast.LENGTH_SHORT).show()
            return
        }

        // A new run gets a clean pane; see clearTerminal() for why this happens
        // here rather than when the previous run ended.
        clearTerminal()

        // Flip the buttons optimistically so the UI reacts on the same frame as
        // the tap; onScriptStopped / the service will correct it if the start
        // actually fails.
        renderRunState(true)
        appendLog(TILE_HINT)
        appendLog("──────── ${timestamp.format(Date())} 启动脚本 ────────")

        // The button always runs the full workflow; only the quick-settings tile
        // asks for the connect-only variant.
        val intent = Intent(this, RunService::class.java)
            .setAction(RunService.ACTION_START)
            .putExtra(RunService.EXTRA_MODE, ScriptRunner.Mode.FULL.name)
        ContextCompat.startForegroundService(this, intent)
    }

    /**
     * The 关闭 button.
     *
     * The UI is flipped to "stopped" *before* the stop is requested, so the
     * button state and the tile both change on the same frame as the tap. The
     * service confirms via onScriptStopped shortly after; doing it in that order
     * is what makes the interaction feel instant rather than laggy.
     */
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

    /**
     * Single place that decides what the two buttons look like.
     *
     * Keeping start and stop mutually exclusive here means the user can never
     * fire the wrong one, and there is exactly one code path to audit when the
     * enabled/disabled state ever looks wrong.
     */
    private fun renderRunState(isRunning: Boolean) {
        running.set(isRunning)
        startButton.isEnabled = !isRunning
        stopButton.isEnabled = isRunning

        // Colours come from resources rather than literals so the palette stays
        // in one place — see res/values/colors.xml. The actionable button is
        // always the brand-coloured one; the other goes flat grey.
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

    /**
     * Called by the service when a run it owned has actually finished.
     *
     * Guarded on two counts, because this callback used to fire even when
     * nothing was running and flood the terminal with a "进程退出，退出码 0"
     * block every time the activity was created:
     *
     *   1. **Only while a run is believed live.** The activity is *created*
     *      whenever the user opens the app, and the service posts a stopped
     *      notification during its own teardown; without this check the block
     *      was printed on every cold start.
     *   2. **Not after the user already stopped it.** [stopScript] flips the
     *      state up front, so `running` is already false by the time the
     *      confirmation arrives — the block must not be repeated for a stop the
     *      user performed deliberately.
     *
     * The exit-code line is only meaningful on an abnormal exit, so it is shown
     * for non-zero codes and omitted for the ordinary clean return.
     */
    override fun onScriptStopped(exitCode: Int) {
        mainHandler.post {
            if (!running.get()) return@post

            stopTailer()
            renderRunState(false)
            appendLog("脚本已停止运行")

            if (exitCode != 0) {
                appendLog("──────── 进程退出，退出码 $exitCode ────────")
            }

            // Keep the quick-settings tile from showing a stale "running" state.
            RunTileService.refresh(this)
        }
    }

    // -------------------------------------------------------------------- log

    override fun onLog(line: String) {
        mainHandler.post { appendLog(normalizeForMonospace(line)) }
    }

    /**
     * Wipes the terminal so a new run starts on a clean pane.
     *
     * Called when a run is launched, not when one ends: keeping the previous
     * run's transcript on screen until the next one genuinely starts means the
     * user can still read the result of a finished run.
     *
     * Not applied in [onCreate], so a rotation (which recreates the activity)
     * preserves whatever was on screen.
     */
    private fun clearTerminal() {
        logView.text = ""
    }

    private fun appendLog(line: String) {
        val safe = normalizeForMonospace(line)
        val current = logView.text?.toString().orEmpty()
        val next = if (current.isEmpty()) safe else "$current\n$safe"

        // Keep the buffer bounded so a long run cannot kill the UI thread.
        val trimmed = if (next.length > 240_000) {
            next.substring(next.length - 200_000)
        } else {
            next
        }

        logView.text = trimmed
        logScroll.post { logScroll.fullScroll(View.FOCUS_DOWN) }
    }

    /**
     * Replaces characters that break column alignment in the terminal pane.
     *
     * The script's ASCII banner uses U+258F (▏ LEFT ONE EIGHTH BLOCK) as a
     * stand-in for a vertical bar. Android's `monospace` maps to a font that
     * does not contain the Block Elements range, so the renderer falls back to
     * a *proportional* font for that one glyph — it then occupies more than one
     * character cell and shoves the rest of its line to the right. The result
     * is exactly the staircase effect visible in the banner.
     *
     * Swapping it for an ASCII `|` keeps every glyph inside the monospace font,
     * so all lines stay on the same grid. The substitution is display-only:
     * the bytes the script wrote are untouched.
     *
     * A few related block glyphs are mapped too, so a future banner or a
     * progress-bar style output does not reintroduce the problem.
     */
    private fun normalizeForMonospace(line: String): String {
        if (line.isEmpty()) return line

        var touched = false
        val out = StringBuilder(line.length)
        for (ch in line) {
            val replacement = when (ch) {
                // The one actually used by the banner, plus its siblings.
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

    // ------------------------------------------------------------ permissions

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
                // startActivity, not startActivityForResult: there is no
                // onActivityResult handler for REQ_MANAGE_STORAGE, so the
                // result code was always discarded. Nothing needs to run when
                // the user comes back — the permission state is re-read on
                // the next file operation.
                startActivity(intent)
            } catch (_: Exception) {
                // Not fatal: the app only needs this for its optional file browser.
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
