# 代码注释提取文档

> 由注释提取工具自动生成。删除前原文照录，行号对应当前版本。

## java/com/hyx/oneshot/wifitools/MainActivity.kt

- /** KDoc */
  ```
  /**
       * Which of the two ways of freeing the interface the next run should use.
       *
       * Persisted, so the choice survives a restart and — more importantly — so
       * the quick-settings tile starts the same kind of run the app last started.
       * Defaults to [ScriptRunner.Mode.FULL] (方案1) on a fresh install.
       */
  ```

- /** KDoc */
  ```
  /** Shown on the 关于 tab. Hard-coded because it is a fixed personal link. */
  ```

- /** KDoc */
  ```
  /**
       * Identity of the run the terminal is currently showing.
       *
       * Null means "not attached to any run". Compared against the id in the
       * shared store to answer the only question that matters when the activity
       * becomes visible again: *is the thing on screen still the thing that is
       * running?* If it is not, the pane is re-attached. See [resumeTerminal].
       */
  ```

- /** KDoc */
  ```
  /**
       * Fallback log tailing, used only when the script runs in another process.
       *
       * The normal path is a direct callback from [RunService]; this exists
       * because that callback cannot cross a process boundary, and a run started
       * from the tile while the app was dead can end up exactly there.
       */
  ```

- /** KDoc */
  ```
  /** How often the out-of-process fallback checks the transcript file. */
  ```

- /** KDoc */
  ```
  /**
           * The warning plate: pale red ground, deep red text.
           *
           * Shared by the "no 密码本" state and the delete-confirmation dialog on
           * purpose — see [confirmDeletePasswordBook]. One pair of constants for
           * both means the two can never drift into looking like different
           * severities.
           */
  ```

- /** KDoc */
  ```
  /** The ordinary (informational) plate, for everything that is not a warning. */
  ```

- /** KDoc */
  ```
  /**
           * Plan selector: active is brand blue on white, idle is the same pale
           * grey as the 关闭 button. Deliberately the same pair of values used by
           * renderRunState(), so the two rows cannot drift apart.
           */
  ```

- /** KDoc */
  ```
  /** SharedPreferences file/keys for the persisted plan choice. */
  ```

- /** KDoc */
  ```
  /**
           * Key for the 重复模式 checkbox.
           *
           * Kept in MainActivity rather than beside KEY_SELECTED_PLAN because the
           * tile has no repeat control of its own — a tile-started run never
           * repeats, so nothing outside the activity needs to read it.
           */
  ```

- /** KDoc */
  ```
  /**
           * The first line of every terminal session.
           *
           * Written on each fresh start rather than only once, because the log
           * buffer is cleared between runs — this is the only reliable place to
           * put a persistent hint, and the user asked for it here specifically.
           */
  ```

- /** KDoc */
  ```
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
  ```

- // 行注释
  ```
  // A null Uri means the user dismissed the picker; that is a normal
  ```

- // 行注释
  ```
  // outcome, not a failure, so it is silent.
  ```

- // 行注释
  ```
  // Restore the remembered plan before the buttons are rendered, so the
  ```

- // 行注释
  ```
  // first frame already shows the right one highlighted.
  ```

- // 行注释
  ```
  // 重复模式：勾选状态跟着偏好走，切换时立刻落盘。
  ```

- // 行注释
  ```
  //
  ```

- // 行注释
  ```
  // 这里刻意 *不* 像方案选择那样禁止运行中切换：方案决定的是本次运行"怎么
  ```

- // 行注释
  ```
  // 腾出网卡"，开始之后就定型了；而重复模式是一个持续生效的开关，运行中允许
  ```

- // 行注释
  ```
  // 取消正是它最需要的用法 —— 用户看到第 N 次循环不对劲，当场取消勾选即可，
  ```

- // 行注释
  ```
  // 服务会在下一次收尾时读到 false 并停下。
  ```

- // 行注释
  ```
  // A three-way swap rather than an else-branch: with only two
  ```

- // 行注释
  ```
  // panes, "not 主页" quietly meant "密码本". A third tab makes
  ```

- // 行注释
  ```
  // that assumption wrong, so every pane is now named explicitly.
  ```

- // 行注释
  ```
  // TabLayout installs its own long-press listener that pops the black
  ```

- // 行注释
  ```
  // rounded tooltip seen over the tab strip. isLongClickable=false on the
  ```

- // 行注释
  ```
  // TabView is not enough because TabLayout re-registers one, so the
  ```

- // 行注释
  ```
  // listener is replaced with a consumer that also swallows the event.
  ```

- // 行注释
  ```
  // Nothing useful is shown on long-press, and the default black tooltip
  ```

- // 行注释
  ```
  // Android pops over a Button is pure noise. Every clickable control in
  ```

- // 行注释
  ```
  // the layout is explicitly made non-long-clickable.
  ```

- // 行注释
  ```
  // The 密码本 tab shows no separate path header: the empty state and the
  ```

- // 行注释
  ```
  // error states each carry the path inline, so a permanent line above
  ```

- // 行注释
  ```
  // the pane would only duplicate it.
  ```

- // 行注释
  ```
  // Mirror every run-state change into the control-centre tile, whichever
  ```

- // 行注释
  ```
  // entry point started the run (in-app button or the tile itself).
  ```

- // 行注释
  ```
  // Deliberately *not* renderRunState(RunService.isRunning) here: onCreate
  ```

- // 行注释
  ```
  // is also the cold-start path when the user taps the notification, and
  ```

- // 行注释
  ```
  // onResume runs immediately afterwards with the authoritative answer.
  ```

- // 行注释
  ```
  // Doing the work twice is what used to make the button and the terminal
  ```

- // 行注释
  ```
  // disagree for a frame.
  ```

- /** KDoc */
  ```
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
  ```

- // 行注释
  ```
  // Detach the callback so the service never holds a reference to a
  ```

- // 行注释
  ```
  // stopped activity, and stop the fallback tailer so it cannot outlive
  ```

- // 行注释
  ```
  // the view it is writing into. The transcript file is untouched, so a
  ```

- // 行注释
  ```
  // later resume replays from it if it needs to.
  ```

- // 行注释
  ```
  // Whether this process's service owns the run. Only meaningful once
  ```

- // 行注释
  ```
  // something has actually been started in it, but that is precisely the
  ```

- // 行注释
  ```
  // case the distinction is for: false + running in the store = the run
  ```

- // 行注释
  ```
  // belongs to another process, and only the file can be read.
  ```

- // 行注释
  ```
  // The run is "live" if EITHER source says so. This is the fix for the
  ```

- // 行注释
  ```
  // tile-started run whose 关闭 button came up greyed out:
  ```

- // 行注释
  ```
  //
  ```

- // 行注释
  ```
  //   * `ownedHere` (RunService.isRunning) is the in-process truth — the
  ```

- // 行注释
  ```
  //     service is the one watching the child, so when we share a process
  ```

- // 行注释
  ```
  //     with it, its word is final.
  ```

- // 行注释
  ```
  //   * `snapshot.running` is the cross-process truth, needed when the
  ```

- // 行注释
  ```
  //     process was recreated (swiped away and relaunched from the
  ```

- // 行注释
  ```
  //     notification) and the in-memory flag is gone.
  ```

- // 行注释
  ```
  //
  ```

- // 行注释
  ```
  // The two can disagree in the tile case: the store validates the run by
  ```

- // 行注释
  ```
  // PID, and the recorded PID belongs to `su`, whose /proc entry can look
  ```

- // 行注释
  ```
  // absent to a non-root reader. Believing only the store then flipped
  ```

- // 行注释
  ```
  // the button to "stopped" while the script was running. OR-ing the two
  ```

- // 行注释
  ```
  // makes the button honest regardless of which side is lying.
  ```

- // 行注释
  ```
  // A run this terminal has never shown is in flight. Clear the pane
  ```

- // 行注释
  ```
  // and re-attach. Both delivery paths are armed: the in-memory
  ```

- // 行注释
  ```
  // callback for the in-process case, the file tail for the other.
  ```

- // 行注释
  ```
  // They cannot double up, because startTailerIfNeeded() declines to
  ```

- // 行注释
  ```
  // run when this process owns the child.
  ```

- // 行注释
  ```
  // Same run, still going — a rotation or a brief trip to another app.
  ```

- // 行注释
  ```
  // Keep the transcript, re-arm delivery, and top up with anything
  ```

- // 行注释
  ```
  // that was logged while the pane was detached.
  ```

- // 行注释
  ```
  // The run this terminal was showing has ended. If the activity was
  ```

- // 行注释
  ```
  // hidden when that happened, the closing lines never reached the
  ```

- // 行注释
  ```
  // pane, so replay the tail rather than leaving a transcript that
  ```

- // 行注释
  ```
  // stops mid-sentence.
  ```

- // 行注释
  ```
  // ---------------------------------------------------------- out-of-process
  ```

- /** KDoc */
  ```
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
  ```

- // 行注释
  ```
  // Seed the position at the current end of file so only new output is
  ```

- // 行注释
  ```
  // appended; the file was truncated when the run started, so anything
  ```

- // 行注释
  ```
  // already in it is history the pane does not need re-showing.
  ```

- // 行注释
  ```
  // Truncated or replaced -> a new run began.
  ```

- // 行注释
  ```
  // Read a bounded chunk as bytes; the file is UTF-8
  ```

- // 行注释
  ```
  // and a multi-byte character can straddle the
  ```

- // 行注释
  ```
  // boundary, so the decoder is allowed to replace
  ```

- // 行注释
  ```
  // rather than throw.
  ```

- // 行注释
  ```
  // The service tail-trims this file, so a
  ```

- // 行注释
  ```
  // partial line can appear at a boundary;
  ```

- // 行注释
  ```
  // showing it is harmless and next tick
  ```

- // 行注释
  ```
  // completes it.
  ```

- // 行注释
  ```
  // Never let a transient read error kill the tailer.
  ```

- /** KDoc */
  ```
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
  ```

- // 行注释
  ```
  // Drop the first line whenever the tail did not begin at the very
  ```

- // 行注释
  ```
  // start of the file: it is almost certainly a fragment of a line
  ```

- // 行注释
  ```
  // that is already on screen.
  ```

- // 行注释
  ```
  // The pane simply keeps what it had.
  ```

- // 行注释
  ```
  // ------------------------------------------------------------------ tiles
  ```

- // 行注释
  ```
  // The "add the tile yourself" reminder (TILE_HINT) is appended directly at
  ```

- // 行注释
  ```
  // the two points that start a fresh log buffer. Android gives apps no way
  ```

- // 行注释
  ```
  // to insert their own quick-settings tile — the user has to drag it in from
  ```

- // 行注释
  ```
  // the control-centre editor — so the information has to live somewhere, and
  ```

- // 行注释
  ```
  // the terminal is where it costs nothing. It is printed on every fresh
  ```

- // 行注释
  ```
  // start, including when the tile has already been added, because the log is
  ```

- // 行注释
  ```
  // wiped between runs and a conditional line would make the layout jump.
  ```

- // 行注释
  ```
  // ---------------------------------------------------------------- runtime
  ```

- // 行注释
  ```
  // ------------------------------------------------------------------ root
  ```

- /** KDoc */
  ```
  /**
       * Actively asks for root on startup.
       *
       * The script refuses to run without uid 0 (`os.getuid() != 0` -> die) and it
       * needs raw nl80211 access, so root is not optional here. Rather than
       * silently probing for a su binary, this triggers the root manager's
       * authorization dialog and reports the outcome in the log area so the user
       * always knows which execution mode the app is in.
       */
  ```

- // 行注释
  ```
  // ------------------------------------------------------------------ tabs
  ```

- /** KDoc */
  ```
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
  ```

- // 行注释
  ```
  // The permanent path header is unused; the empty state and the
  ```

- // 行注释
  ```
  // error states each carry the path inline.
  ```

- // 行注释
  ```
  // Nothing generated yet, or nothing readable: show the one
  ```

- // 行注释
  ```
  // prompt and leave the pane below empty.
  ```

- // 行注释
  ```
  // Red only for "no 密码本 at all" — the one state that
  ```

- // 行注释
  ```
  // needs the user to act. The other messages reuse the
  ```

- // 行注释
  ```
  // same plate in the ordinary card colours, so the red is
  ```

- // 行注释
  ```
  // never diluted into meaning nothing.
  ```

- // 行注释
  ```
  // The file exists but is blank — the run happened, no
  ```

- // 行注释
  ```
  // network cracked. Different message, same pane.
  ```

- /** KDoc */
  ```
  /**
       * Shows [message] in the status plate, red only when it is a warning.
       *
       * The red-on-pale-red pair is reserved for "no 密码本 exists", the one case
       * where the user has to do something. Everything else keeps the ordinary
       * card look, so a glance at the colour alone tells you whether there is a
       * problem.
       */
  ```

- /** KDoc */
  ```
  /** The one line shown when no 密码本 exists yet. */
  ```

- // 行注释
  ```
  // ----------------------------------------------------- 密码本: export/delete
  ```

- /** KDoc */
  ```
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
  ```

- // 行注释
  ```
  // SAF, not a raw path: no storage permission is needed, and the user
  ```

- // 行注释
  ```
  // decides where their own credentials end up.
  ```

- /** KDoc */
  ```
  /**
       * Writes the book to the [target] the user chose in the picker.
       *
       * Done entirely off the UI thread, and any failure surfaces as a toast
       * rather than an exception — a read-only provider or a revoked URI should
       * leave the app usable, and the original file is never modified either way.
       */
  ```

- /** KDoc */
  ```
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
  ```

- // 行注释
  ```
  // The dialog's own window is transparent-wrapped by the framework; the
  ```

- // 行注释
  ```
  // rounded red card is drawn by the custom view below so the shape and
  ```

- // 行注释
  ```
  // colour survive on every API level.
  ```

- // 行注释
  ```
  // Right-aligned, like every other dialog footer on the platform.
  ```

- // 行注释
  ```
  // Back to the ordinary grey. It has to be visibly *quieter* than
  ```

- // 行注释
  ```
  // 「是」 so the destructive choice is the one that stands out — a
  ```

- // 行注释
  ```
  // white label was too loud for the dismissive option.
  ```

- // 行注释
  ```
  // Generous touch target, and a flat hit area so the two words do
  ```

- // 行注释
  ```
  // not look like a pair of stock buttons.
  ```

- // 行注释
  ```
  // 否 on the left, 是 on the right.
  ```

- /** KDoc */
  ```
  /** Actually removes the password book, then repaints the tab. */
  ```

- // 行注释
  ```
  // Either way, re-read: on success this shows the empty-state
  ```

- // 行注释
  ```
  // prompt, on failure it shows the file that is still there.
  ```

- /** KDoc */
  ```
  /** dp -> px, for the hand-built dialog above. */
  ```

- // 行注释
  ```
  // ------------------------------------------------------------ plan chooser
  ```

- /** KDoc */
  ```
  /**
       * Reads the remembered plan, falling back to 方案1.
       *
       * Any unrecognised value (an older build's, or a corrupted one) also lands
       * on 方案1 rather than throwing — `Mode.valueOf` is the thing that would
       * throw, so it is wrapped.
       */
  ```

- /** KDoc */
  ```
  /**
       * 重复模式的勾选状态。
       *
       * 默认关闭：这是一个会持续重跑脚本的开关，默认打开会在用户没注意时反复
       * 折腾 WiFi。想看结果的人自己勾一次，之后再打开 App 也会记得。
       */
  ```

- /** KDoc */
  ```
  /**
       * Switches the plan, remembers it, and repaints.
       *
       * Refused while a run is in flight: the plan decides *how the interface is
       * freed*, which happens once at the start of a run. Letting it change
       * mid-run would show a highlighted button that no longer describes what the
       * running process is actually doing.
       */
  ```

- // 行注释
  ```
  // 方案2 与方案1 的区别值得在切换的那一刻说清楚：两者都会让手机在这个
  ```

- // 行注释
  ```
  // 过程里上不了网，但代价完全不同 —— 方案1 是把 WiFi 整块关掉，方案2 只是
  ```

- // 行注释
  ```
  // 断开当前这条连接，WiFi 开关保持开启。
  ```

- // 行注释
  ```
  //
  ```

- // 行注释
  ```
  // 这里不再有"断开可能无效，请自行关闭 WiFi 再打开"那段话：断开改由 App 在
  ```

- // 行注释
  ```
  // 框架层完成（WifiManager.disconnect），而且有四条兜底链，所以那条自救
  ```

- // 行注释
  ```
  // 说明对应的失败模式已经不存在了。
  ```

- // 行注释
  ```
  // 只在 *进入* 方案2 时弹；从方案2 切回方案1 不需要任何提醒。
  ```

- /** KDoc */
  ```
  /** Persists [mode] as the active plan and repaints the two buttons. */
  ```

- /** KDoc */
  ```
  /**
       * 切换到方案2 时的提醒弹窗。
       *
       * 用与 [confirmDeletePasswordBook] 相同的手绘卡片，因为这些是同一类
       * 信息：都是"接下来可能不会完全按你预期走"的警示。底色用普通白色，
       * **只有文字和按钮是红色** —— 与删除密码本那个整块红底的框区分开：
       * 那个是不可逆的删除，这个是即将发生的、可恢复的中断，轻重不一样，不该
       * 长得一模一样。
       *
       * 文案是一句**事前提醒**：部分情况下自动断开可能无效，如果看到扫描/关联
       * 反复重来，就自己把 WiFi 关一下再打开重置。断开本身由 App 在框架层用 root
       * 完成、且有四级兜底，多数情况下是确定成功的；这句提示是为那些兜底也没救回来
       * 的场景留的手动出口。
       *
       * ### 出口
       *
       * 弹窗底部只有一个「知道了」，这不是"要不要切"的询问 —— 方案已经被选中了，
       * 这里只是告知，所以不给「取消」这种看似能反悔、其实只是把框关掉的按钮。
       *
       *   * 点「知道了」 → 切换
       *   * 点弹窗**外面** → 切换（`OnDismissListener` 兜底，见下）
       *   * 返回键 → **不切换**，这是唯一明确的拒绝出口
       *
       * 之所以要 `setCanceledOnTouchOutside(true)` + 在 `OnDismissListener` 里
       * 补一次 `onAccept`：在小屏设备上弹窗内容可能高于可视区，底部那个按钮会
       * 被挤出屏幕，用户点不到。早先的版本在这种情况下点外面只会把框关掉、
       * 方案根本没切过去，用户看到的是"点了没反应"。现在点外面和点按钮等价，
       * 小屏也能切换。
       *
       * 用一个标志位区分"是被 dismiss 掉的"和"是走了返回键"，否则返回键也会被
       * `OnDismissListener` 当成同意。
       */
  ```

- // 行注释
  ```
  // 明确拒绝时置位，让 OnDismissListener 知道不该补 onAccept。
  ```

- // 行注释
  ```
  // 按钮行：只有一个「知道了」。
  ```

- // 行注释
  ```
  //
  ```

- // 行注释
  ```
  // 这不是"要不要切"的询问 —— 方案已经被选中了，这里只是告知。所以不给
  ```

- // 行注释
  ```
  // 「取消」这个看似能反悔、其实只是把框关掉的按钮，避免用户误以为切换本身
  ```

- // 行注释
  ```
  // 需要二选一。按钮占满整行，与删除密码本那对保持同一种描边样式。
  ```

- // 行注释
  ```
  // 小屏兜底：内容高于可视区时允许滚动，否则底部的按钮永远点不到。
  ```

- // 行注释
  ```
  // 底部留一点内边距，让按钮不贴在滚动区边缘上。
  ```

- // 行注释
  ```
  // 返回键 = 明确拒绝。
  ```

- // 行注释
  ```
  //
  ```

- // 行注释
  ```
  // 没有这一层的话，返回键会走 OnDismissListener 那条路，被当成"点外面"
  ```

- // 行注释
  ```
  // 而切过去 —— 但按返回键在用户心里一直是"算了，别切了"，和点弹窗外面的
  ```

- // 行注释
  ```
  // "我看过了别烦我"不是一回事。用一个 OnKeyListener 把两者分开。
  ```

- // 行注释
  ```
  // 点弹窗外面 = 同意（"我知道了，别烦我"）。
  ```

- // 行注释
  ```
  //
  ```

- // 行注释
  ```
  // 小屏设备上弹窗内容可能高于可视区，底部按钮被挤出屏幕点不到 —— 这时
  ```

- // 行注释
  ```
  // 点外面是唯一能表达的"知道了"。早先的版本在这种情况下只是把框关掉、
  ```

- // 行注释
  ```
  // 方案根本没切过去，用户看到的是"点了没反应"。
  ```

- /** KDoc */
  ```
  /**
       * Paints the two plan buttons in the start/stop colour language: the active
       * one is solid brand blue, the inactive one the idle grey.
       *
       * Both are always enabled — turning the inactive one grey-but-disabled
       * would make the pair look like a status readout rather than something you
       * can click, and the only time switching is actually refused is mid-run,
       * which [selectPlan] reports with a Toast instead of a dead-looking button.
       */
  ```

- /** KDoc */
  ```
  /**
       * Opens the author's homepage in whatever browser the user has.
       *
       * Wrapped in a try/catch on purpose: a device with no browser (or with
       * every browser disabled) has nothing to resolve ACTION_VIEW, and
       * `startActivity` would throw ActivityNotFoundException — crashing the
       * app because someone tapped a link. Falling back to a Toast at least
       * tells them the address.
       */
  ```

- // 行注释
  ```
  // -------------------------------------------------------------- start/stop
  ```

- // 行注释
  ```
  // A new run gets a clean pane; see clearTerminal() for why this happens
  ```

- // 行注释
  ```
  // here rather than when the previous run ended.
  ```

- // 行注释
  ```
  // Flip the buttons optimistically so the UI reacts on the same frame as
  ```

- // 行注释
  ```
  // the tap; onScriptStopped / the service will correct it if the start
  ```

- // 行注释
  ```
  // actually fails.
  ```

- // 行注释
  ```
  // The plan the user picked in the terminal header decides how the
  ```

- // 行注释
  ```
  // script frees the interface; 方案2 adds `-m` on the command line.
  ```

- // 行注释
  ```
  // The interface cannot be chosen from the UI any more: the script
  ```

- // 行注释
  ```
  // auto-detects it, which is strictly better than a text field could be
  ```

- // 行注释
  ```
  // (see ScriptRunner.DEFAULT_IFACE).
  ```

- // 行注释
  ```
  //
  ```

- // 行注释
  ```
  // The repeat flag rides along so the service knows whether to relaunch
  ```

- // 行注释
  ```
  // itself at the end. It is sent on every start, not only when ticked,
  ```

- // 行注释
  ```
  // so unticking mid-run is honoured by the next teardown instead of
  ```

- // 行注释
  ```
  // leaving the old value in place.
  ```

- /** KDoc */
  ```
  /**
       * The 关闭 button.
       *
       * The UI is flipped to "stopped" *before* the stop is requested, so the
       * button state and the tile both change on the same frame as the tap. The
       * service confirms via onScriptStopped shortly after; doing it in that order
       * is what makes the interaction feel instant rather than laggy.
       */
  ```

- /** KDoc */
  ```
  /**
       * Single place that decides what the two buttons look like.
       *
       * Keeping start and stop mutually exclusive here means the user can never
       * fire the wrong one, and there is exactly one code path to audit when the
       * enabled/disabled state ever looks wrong.
       */
  ```

- // 行注释
  ```
  // Colours come from resources rather than literals so the palette stays
  ```

- // 行注释
  ```
  // in one place — see res/values/colors.xml. The actionable button is
  ```

- // 行注释
  ```
  // always the brand-coloured one; the other goes flat grey.
  ```

- /** KDoc */
  ```
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
  ```

- // 行注释
  ```
  // Keep the quick-settings tile from showing a stale "running" state.
  ```

- // 行注释
  ```
  // -------------------------------------------------------------------- log
  ```

- /** KDoc */
  ```
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
  ```

- // 行注释
  ```
  // Keep the buffer bounded so a long run cannot kill the UI thread.
  ```

- /** KDoc */
  ```
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
  ```

- // 行注释
  ```
  // The one actually used by the banner, plus its siblings.
  ```

- // 行注释
  ```
  // ------------------------------------------------------------ permissions
  ```

- // 行注释
  ```
  // startActivity, not startActivityForResult: there is no
  ```

- // 行注释
  ```
  // onActivityResult handler for REQ_MANAGE_STORAGE, so the
  ```

- // 行注释
  ```
  // result code was always discarded. Nothing needs to run when
  ```

- // 行注释
  ```
  // the user comes back — the permission state is re-read on
  ```

- // 行注释
  ```
  // the next file operation.
  ```

- // 行注释
  ```
  // Not fatal: the app only needs this for its optional file browser.
  ```

## java/com/hyx/oneshot/wifitools/PythonInstaller.kt

- /** KDoc */
  ```
  /**
   * Handles the "assets -> app private storage" deployment of the Python runtime
   * and the bundled command-line tools.
   *
   * A CPython build cannot run straight out of an APK: the interpreter needs a real
   * filesystem for its stdlib, its extension modules and the executable itself.
   * The same is true of pixiewps / wpa_supplicant / iw — a binary stored under
   * assets/ cannot be exec'd in place on modern Android, so everything is copied
   * into filesDir with the executable bit set.
   *
   * Everything is ABI-aware. The APK carries both arm64-v8a and armeabi-v7a builds
   * of the interpreter and of every tool, and only the set matching the device is
   * unpacked.
   */
  ```

- /** KDoc */
  ```
  /** Bumped whenever the asset payload changes shape. */
  ```

- /** KDoc */
  ```
  /** Directory the bundled script lives in; also its CWD at runtime. */
  ```

- /** KDoc */
  ```
  /**
           * CPython prefix root. The interpreter resolves its stdlib relative to
           * this, expecting `<prefix>/lib/python3.11/`, so it must be a directory
           * that *contains* lib/ rather than the stdlib directory itself.
           */
  ```

- /** KDoc */
  ```
  /** Where the interpreter binary + its .so files are copied. */
  ```

- /** KDoc */
  ```
  /** Where pixiewps / wpa_supplicant / wpa_cli / iw are copied. */
  ```

- /** KDoc */
  ```
  /**
           * The primary ABI this device should use, restricted to the two we ship.
           * Falls back to arm64-v8a (our baseline) if the platform reports
           * something unexpected.
           */
  ```

- /** KDoc */
  ```
  /**
       * Returns true when the runtime was (re)deployed, false when the existing
       * copy was already up to date.
       */
  ```

- // 行注释
  ```
  // Start from a clean slate so a partially-extracted older build cannot
  ```

- // 行注释
  ```
  // poison the new one.
  ```

- // 行注释
  ```
  // assets/python holds the *contents* of lib/python3.11, so it is deployed
  ```

- // 行注释
  ```
  // one level deeper to satisfy CPython's <prefix>/lib/python3.11 layout.
  ```

- /** KDoc */
  ```
  /** Recursively copies one asset subtree into a destination directory. */
  ```

- // 行注释
  ```
  // A leaf: it is a file, not a directory.
  ```

- /** KDoc */
  ```
  /**
       * Extracts the interpreter, libpython and the extension modules for [abi].
       *
       * These ship inside assets/py/<abi>/python-nativelibs.zip rather than as
       * plain asset files because a .so stored under assets/ cannot be dlopen()ed
       * or exec'd in place on modern Android — it has to become a real file with
       * the executable bit set inside the app's private storage first.
       *
       * lib-dynload entries land in [stdlibDest] so CPython finds them where the
       * stdlib expects them; the interpreter and libpython go to filesDir/lib.
       */
  ```

- // 行注释
  ```
  // The interpreter itself must be executable; the .so
  ```

- // 行注释
  ```
  // files only need to be readable.
  ```

- /** KDoc */
  ```
  /**
       * Extracts the command-line tools for [abi] into filesDir/bin and marks them
       * executable.
       *
       * These are what the script's `which('pixiewps' | 'wpa_supplicant' | 'iw')`
       * requirement check looks for, so the directory is prepended to PATH by
       * ScriptRunner.
       */
  ```

## java/com/hyx/oneshot/wifitools/RunService.kt

- /** KDoc */
  ```
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
  ```

- /** KDoc */
  ```
  /**
           * Extra carrying a [ScriptRunner.Mode] name. Both entry points — the
           * quick-settings tile and the in-app button — start the full workflow.
           */
  ```

- /** KDoc */
  ```
  /**
           * Extra carrying the run id minted by the caller (the tile) so the
           * activity can recognise the run it is displaying. Absent when the
           * in-app button starts the run; the id is minted here in that case.
           */
  ```

- /** KDoc */
  ```
  /**
           * Extra asking for a *connect-only* run: the script is started with
           * `-c` and joins the network named by its own `CONNECT_SSID` /
           * `CONNECT_PSK` variables, then exits.
           *
           * Only the tile's "破解 + 连接" tap sets this. It is an extra rather
           * than a third [ScriptRunner.Mode] because it is orthogonal: you can
           * connect-only under either plan, so it composes with `-m` instead of
           * replacing it.
           */
  ```

- /** KDoc */
  ```
  /**
           * Extra asking the run to restart itself every time it stops.
           *
           * "Every time" is literal: a clean exit, a crash, a watchdog kill, a
           * signal — all of them restart. Only the user pressing 关闭 (or stopping
           * from the tile) ends the cycle, because that is the one stop that is
           * deliberate rather than incidental.
           *
           * The restart is driven from *this service*, not from the activity:
           * the activity can be swiped away while the loop keeps going, and a
           * repeat mode that died with the UI would be worse than none.
           */
  ```

- /** KDoc */
  ```
  /** Grace period before a repeat restart, so a hard-failing script
           *  cannot spin the CPU at full tilt. Long enough to be a real pause,
           *  short enough not to feel like a stall. */
  ```

- /** KDoc */
  ```
  /**
           * File the live transcript is mirrored into, for readers that are not
           * in this process. Lives in filesDir so both the app uid and root can
           * read it.
           */
  ```

- /** KDoc */
  ```
  /** Kept deliberately generous: a full scan is chatty. */
  ```

- /** KDoc */
  ```
  /**
           * Mode of the run currently in flight (or the last one to finish).
           * The tile reads this to label itself without having to re-derive it.
           */
  ```

- /** KDoc */
  ```
  /** Identity of the run in flight, or the last one to have run. */
  ```

- /** KDoc */
  ```
  /**
           * Notified whenever `isRunning` flips, so the quick-settings tile can
           * keep its active state in sync with the watchdog's view of the process
           * — including runs started from the in-app button.
           */
  ```

- /** KDoc */
  ```
  /** The shared state file, for callers that only want the answer. */
  ```

- /** KDoc */
  ```
  /**
       * Pool that pumps the child's output.
       *
       * Recreated rather than kept: [shutDownHelpers] shuts it down at the end of
       * every run, and a shut-down executor rejects every later `execute`. Under
       * 重复模式 that would kill the second iteration's transcript with a
       * `RejectedExecutionException`, so it is built fresh on each run.
       */
  ```

- /** KDoc */
  ```
  /**
       * Whether this run restarts itself when it ends. See [EXTRA_REPEAT].
       *
       * Held on the service instance rather than in the store: it describes this
       * service's behaviour, and a service that has been torn down has no loop
       * left to continue.
       */
  ```

- /** KDoc */
  ```
  /** The plan and connect flag the current run was started with, kept so a
       *  repeat restart reproduces the same run rather than falling back to
       *  defaults. */
  ```

- /** KDoc */
  ```
  /** The run id this service instance owns; see [RunStateStore]. */
  ```

- // 行注释
  ```
  // A deliberate stop ends the repeat loop: the flag is cleared
  ```

- // 行注释
  ```
  // *before* the kill so the teardown cannot be mistaken for an
  ```

- // 行注释
  ```
  // incidental end and restart itself.
  ```

- // 行注释
  ```
  // Only a start that carries the extra sets the flag. A restart
  ```

- // 行注释
  ```
  // issued by this service goes straight to startScript() and
  ```

- // 行注释
  ```
  // must not clear what it already set.
  ```

- // 行注释
  ```
  // Remembered so a repeat restart reproduces this exact run.
  ```

- // 行注释
  ```
  // A fresh transcript: the file is truncated here rather than after the
  ```

- // 行注释
  ```
  // run, so a reader that starts tailing it at any moment only ever sees
  ```

- // 行注释
  ```
  // this run's output.
  ```

- // 行注释
  ```
  // 方案2 的前置动作：先把当前 WiFi 连接断开，再起脚本。
  ```

- // 行注释
  ```
  //
  ```

- // 行注释
  ```
  // 这一步必须在 App 进程里做，而不是交给脚本 —— 断开是框架层的操作
  ```

- // 行注释
  ```
  // （WifiManager.disconnect），它同时会暂停自动重连；脚本从底层做同一个
  ```

- // 行注释
  ```
  // 动作只会被框架的自动重连顶回来，前几版就是这么失败的。脚本侧只负责
  ```

- // 行注释
  ```
  // 校验网卡确实空闲，占用则直接退出，见 ose.py 的 _interfaceIsFree()。
  ```

- // 行注释
  ```
  //
  ```

- // 行注释
  ```
  // 断开本身走一条四级兜底链（见 WifiDisconnector），因为真机上单一机制
  ```

- // 行注释
  ```
  // 靠不住：各级依赖的东西厂家都可能改。
  ```

- // 行注释
  ```
  // Authoritative environment. Setting these on the builder (rather
  ```

- // 行注释
  ```
  // than relying on `VAR=x` prefixes inside the su command string)
  ```

- // 行注释
  ```
  // is what makes them survive a root manager that rewrites the
  ```

- // 行注释
  ```
  // command it is handed. See ScriptRunner.buildEnvironment().
  ```

- // 行注释
  ```
  // Logged so that if a path problem shows up again on a device we can
  ```

- // 行注释
  ```
  // see immediately which values actually reached the child, instead
  ```

- // 行注释
  ```
  // of inferring it from the Python traceback.
  ```

- // 行注释
  ```
  // Recorded so a tile bound in a *new* process (after this one is
  ```

- // 行注释
  ```
  // killed) still knows a run is in flight, and so the activity can
  ```

- // 行注释
  ```
  // tell *which* run it is. See RunStateStore.
  ```

- // 行注释
  ```
  // Read back: markRunning mints an id when none was supplied, and
  ```

- // 行注释
  ```
  // this instance has to report the same one the store now holds.
  ```

- /** KDoc */
  ```
  /**
       * Tears the run down when the *pre-flight* step failed and the script was
       * never started.
       *
       * [finishRun] cannot be used here: it branches on [isRunning], which is
       * still false at this point, and that early branch deliberately skips
       * [Listener.onScriptStopped] — the assumption being "we were never
       * running, so no UI ever asked to be told". That assumption does not hold
       * for a failed pre-flight: the activity has already flipped its buttons to
       * "running" on the tap (see MainActivity.startScript), so *somebody* has to
       * flip them back. This is that somebody.
       */
  ```

- /** KDoc */
  ```
  /**
       * `Process.pid()` is API 26+ and this app's minSdk is 24, so on older
       * devices the PID is simply not available. That degrades gracefully:
       * [RunStateStore] treats "no PID" as "cannot verify", which keeps the run
       * marked live rather than clearing it.
       */
  ```

- /** KDoc */
  ```
  /** Streams the merged stdout/stderr of the child into the log view. */
  ```

- // 行注释
  ```
  // ANSI colour codes are meaningless in a TextView and would
  ```

- // 行注释
  ```
  // otherwise show up as literal escape noise.
  ```

- /** KDoc */
  ```
  /**
       * The 5-second liveness watch. `Process.isAlive` is authoritative and also
       * catches the case where the shell died but the pipe has not flushed yet.
       */
  ```

- // 行注释
  ```
  // A watchdog must never crash the service.
  ```

- /** KDoc */
  ```
  /**
       * Stops the run for real.
       *
       * `Process.destroy()` only signals the immediate child — which is `su`, not
       * the script. The interpreter (`python3.11`) is a grandchild and is *not*
       * killed by it: signal `su` and the Python process is orphaned and keeps
       * running, still holding the Wi-Fi interface. The UI would then say
       * "stopped" while the radio was still being driven underneath.
       *
       * So the kill is done twice over, and both are needed:
       *
       *   1. **By PID** through root, targeting the process group. `su -c` runs
       *      the command in its own group, so a negative PID reaches the whole
       *      tree. This is the one that actually ends the script.
       *   2. **[Process.destroy]** on the handle itself, as the fallback for a
       *      device where the first step could not run (no root, or the PID was
       *      never recorded). It is also what reaps the child so the watchdog
       *      stops seeing it.
       *
       * Killing an already-dead PID is harmless — `kill` reports ESRCH and the
       * exit status is ignored.
       */
  ```

- // 行注释
  ```
  // 1) The reliable route: signal the whole process group as root.
  ```

- // 行注释
  ```
  // Low chance the group id differs, but the negative form is
  ```

- // 行注释
  ```
  // the one that reaches the script's children. `|| true`
  ```

- // 行注释
  ```
  // keeps a missing process from looking like a failure.
  ```

- /* 块注释 */
  ```
  /* no root: step 2 still runs */
  ```

- // 行注释
  ```
  // 2) The handle we own. Reaped explicitly so `isAlive` and the watchdog
  ```

- // 行注释
  ```
  //    agree immediately rather than after the next poll.
  ```

- /** KDoc */
  ```
  /** Idempotent: only the first caller actually flips the UI back. */
  ```

- // 行注释
  ```
  // Nothing was running, so there is no cycle to continue — but the
  ```

- // 行注释
  ```
  // repeat flag is deliberately left alone here: this branch is
  ```

- // 行注释
  ```
  // reached during teardown after the loop already decided to stop.
  ```

- // 行注释
  ```
  // Read before the state is cleared: the loop decision comes first, so
  ```

- // 行注释
  ```
  // an iteration that is about to restart never publishes a "stopped"
  ```

- // 行注释
  ```
  // snapshot. That matters for the terminal — a stop notification
  ```

- // 行注释
  ```
  // followed 1.5 s later by a fresh run makes the pane flicker between
  ```

- // 行注释
  ```
  // running and stopped — and for the tile, which would dim and relight
  ```

- // 行注释
  ```
  // on every cycle.
  ```

- // 行注释
  ```
  // The old PID is stale the moment the child is reaped, so the
  ```

- // 行注释
  ```
  // record is cleared either way. What is *not* cleared is the
  ```

- // 行注释
  ```
  // running flag the UI follows: it is rewritten by the restart
  ```

- // 行注释
  ```
  // below, and clearing it here would flip every listener to
  ```

- // 行注释
  ```
  // "stopped" for a run that is still coming back.
  ```

- // 行注释
  ```
  // The watchdog belongs to the process that just died; leaving it
  ```

- // 行注释
  ```
  // running would pile one more poller onto the pile every cycle.
  ```

- // 行注释
  ```
  // The pool is swapped for a live one at the same time, so no
  ```

- // 行注释
  ```
  // `onScriptStopped` is published — see the note above.
  ```

- // 行注释
  ```
  // Cleared here rather than in onDestroy: this is the one moment we know
  ```

- // 行注释
  ```
  // for certain the script is gone, so the tile will not stay lit for it.
  ```

- /** KDoc */
  ```
  /**
       * Queues the next iteration of a repeat run.
       *
       * Three things have to be true for the loop to be *deliberately* wrong to
       * skip, and all three are checked at the call site above or here:
       *
       *   * `repeatMode` — the user asked for it.
       *   * `!stopper` — this end was not a 关闭 press. `stopScript()` sets the
       *     flag before killing, so a deliberate stop can never re-enter.
       *   * the service is not already being destroyed — belt and braces, since
       *     a teardown concurrent with this would make the restart a no-op that
       *     still leaves a notification behind.
       *
       * The delay is not cosmetic: a script that fails instantly (no root, no
       * interface) would otherwise restart in a tight loop and pin a core.
       *
       * The restart goes through [startScript] directly rather than through an
       * Intent, so it cannot be confused with a fresh user-initiated start — in
       * particular it must not re-read EXTRA_REPEAT from a stale intent.
       */
  ```

- // 行注释
  ```
  // Published straight away, before the delay, so the gap between two
  ```

- // 行注释
  ```
  // iterations does not read as "stopped" to the tile or the activity.
  ```

- // 行注释
  ```
  // A fresh id is minted each cycle — the old one belongs to a process
  ```

- // 行注释
  ```
  // that no longer exists, and reusing it would make a terminal that is
  ```

- // 行注释
  ```
  // already showing this run treat the restart as "the same run" and
  ```

- // 行注释
  ```
  // skip the header it needs.
  ```

- // 行注释
  ```
  // Re-checked at fire time: the user may have pressed 关闭 during
  ```

- // 行注释
  ```
  // the delay, and a restart after that would be exactly the
  ```

- // 行注释
  ```
  // "it will not stay stopped" complaint this mode must not cause.
  ```

- /** KDoc */
  ```
  /**
       * Releases the per-run executors.
       *
       * The pool is replaced rather than only shut down: a shut-down executor
       * cannot be reused, and under 重复模式 the next iteration needs a working
       * one. Replacing it here (rather than at the start of a run) means the
       * service is never left holding a dead executor between iterations.
       */
  ```

- // 行注释
  ```
  // --------------------------------------------------------------- log file
  ```

- // 行注释
  ```
  // A missing mirror costs us the out-of-process view, nothing else.
  ```

- /** KDoc */
  ```
  /**
       * Appends one line to the transcript mirror.
       *
       * The file is the *only* way an activity in another process can see the
       * run, so a write failure is worth reporting once — but not worth aborting
       * the run over, hence no exception escapes.
       */
  ```

- // 行注释
  ```
  // Trim the head rather than dropping everything: the tail is
  ```

- // 行注释
  ```
  // what a late-attaching reader actually wants.
  ```

- // 行注释
  ```
  // The service only goes away when the run is over or the user stopped
  ```

- // 行注释
  ```
  // it, so any persisted "running" state is now a lie. RunStateStore
  ```

- // 行注释
  ```
  // would catch it via the PID check anyway, but clearing here means the
  ```

- // 行注释
  ```
  // tile goes grey immediately instead of on the next shade pull.
  ```

- // 行注释
  ```
  // ----------------------------------------------------------- notification
  ```

## java/com/hyx/oneshot/wifitools/RunStateStore.kt

- /** KDoc */
  ```
  /**
   * Cross-process record of "a run is in flight, and here is how to prove it".
   *
   * ### Why this exists
   *
   * A quick-settings tile is bound by SystemUI, and SystemUI lives in its own
   * process. In practice that means the app's service can be talking to a
   * `RunService` instance that belongs to a *different* `Process` object than the
   * one that is actually running the script. Nothing in the in-memory
   * `@Volatile` fields crosses that boundary, so every question of the form "is a
   * run happening?" has to be answerable from a file.
   *
   * Two questions are asked, and they are not the same question:
   *
   *   * **is a run live right now?** — needs a liveness proof, because a stale
   *     record left behind by a killed process must not keep reporting "running".
   *   * **which run is it?** — needs a *stable identity*, because the activity
   *     has to know whether the log it is showing belongs to the run that is
   *     still going, or to one that finished five minutes ago.
   *
   * The previous revision only answered the first one, with a boolean and a PID.
   * That was enough to light the tile correctly but not enough to decide whether
   * the terminal needed refreshing, which is the bug this class fixes.
   *
   * ### The identity
   *
   * [RUN_ID] is minted fresh on every start and is deliberately *not* derived
   * from the PID: Android recycles PIDs aggressively, so "same PID" is not the
   * same run. It is written together with everything else in a single
   * `apply()` — one editor, one commit — so a reader can never observe a
   * half-written record.
   *
   * ### Liveness
   *
   * `kill -0 <pid>` (signal 0) is the portable existence probe. It works on
   * another app's process on Android when run by the app itself, whereas
   * `File("/proc/<pid>").exists()` does not — the sandbox hides `/proc` entries
   * that the caller does not own.
   *
   * Note that this only *proves death*; it cannot prove life, because the PID may
   * have been recycled onto an unrelated process. For the run id that is fine:
   * the id is only ever compared against the run the activity is displaying.
   */
  ```

- /** KDoc */
  ```
  /** Everything a reader needs to know about the current (or last) run. */
  ```

- /** KDoc */
  ```
  /** Free-form progress note, e.g. which SSID is being attacked. */
  ```

- /** KDoc */
  ```
  /**
       * True only when the run this activity is showing has been superseded.
       *
       * A different non-null id means a *newer* run started while the activity was
       * hidden, which is exactly when the terminal has to be re-attached. A null
       * [currentId] means "we have not attached to any run yet" and is treated as
       * stale so a cold start always picks the live run up.
       */
  ```

- // 行注释
  ```
  // A record with no PID is a run that was marked before the process
  ```

- // 行注释
  ```
  // existed — believe it, because the alternative is a tile that flickers
  ```

- // 行注释
  ```
  // off in the gap between the tap and the fork.
  ```

- // 行注释
  ```
  // The script died while nobody was watching (force-stop, OOM kill,
  ```

- // 行注释
  ```
  // reboot). Drop the record so the tile goes grey and the activity
  ```

- // 行注释
  ```
  // stops waiting for output that will never arrive.
  ```

- /** KDoc */
  ```
  /**
       * Records a run as started, with a brand-new identity.
       *
       * Called by the tile before the service has even been asked to start: the
       * process may be killed in the gap between the tap and the fork, and the
       * record has to survive that.
       */
  ```

- /** KDoc */
  ```
  /**
       * Fills in the real PID once the child process exists.
       *
       * The run id is only applied when one was passed in; a start that went
       * straight through the service (the in-app button does this) has no prior
       * optimistic record and mints its identity here instead.
       */
  ```

- // 行注释
  ```
  // Keep the id the tile minted if there is one; the service must not
  ```

- // 行注释
  ```
  // overwrite it, or the activity would see the id change mid-run and
  ```

- // 行注释
  ```
  // re-attach (and clear the terminal) for a run it is already showing.
  ```

- /** KDoc */
  ```
  /**
       * Publishes a human-readable progress note.
       *
       * Polled by the activity when the activity and the script are in different
       * processes, so it is the only way for the terminal to show *what* the run
       * is doing rather than just *that* it is doing something.
       */
  ```

- /** KDoc */
  ```
  /**
       * Undoes [markStarting] when the start was refused, so the tile does not
       * stay lit for a run that never began.
       */
  ```

- // 行注释
  ```
  // KEY_RUN_ID is deliberately left in place: it is the id of the last
  ```

- // 行注释
  ```
  // run and the activity still needs it to decide whether what it is
  ```

- // 行注释
  ```
  // showing is current.
  ```

- /** KDoc */
  ```
  /** True when a run is live, by the same rules [snapshot] applies. */
  ```

- /** KDoc */
  ```
  /**
       * Whether [pid] still names a live process.
       *
       * This used to be `kill -0 <pid>`, which is wrong for the one case that
       * matters here. The script runs as **root** (`su -c …`), and the app does
       * not: `kill -0` against a process owned by another uid fails with EPERM,
       * not ESRCH. Treating that non-zero exit as "dead" cleared the record while
       * the script was very much alive — which is what made the 关闭 button
       * arrive greyed out and unclickable after starting a run from the tile.
       *
       * `/proc/<pid>` has no such ambiguity: the directory is readable for any
       * process on Android, its *absence* is ESRCH and means genuinely gone, and
       * a process we lack rights over simply presents fewer files rather than
       * disappearing. A zombie is excluded by reading its state, because a
       * wait()ed-for corpse still holds a /proc entry at the moment the run is
       * being torn down.
       *
       * Failure to read at all is treated as alive: a run wrongly believed live
       * is self-correcting (the watchdog and the tailer both notice), whereas a
       * run wrongly declared dead leaves a terminal that cannot be stopped.
       */
  ```

- // 行注释
  ```
  // Format: "<pid> (<comm>) <state> …" — comm may itself contain
  ```

- // 行注释
  ```
  // spaces and parentheses, so the state is read from after the
  ```

- // 行注释
  ```
  // *last* ')'.
  ```

- /** KDoc */
  ```
  /**
       * A run identity that is unique per start.
       *
       * Uniqueness only has to hold within one device's lifetime — the value is
       * never persisted anywhere but here and never compared across reboots — so
       * a millisecond clock plus a counter is sufficient and avoids depending on
       * `java.util.UUID` string parsing.
       */
  ```

## java/com/hyx/oneshot/wifitools/RunTileService.kt

- /** KDoc */
  ```
  /**
   * Quick-settings tile: scan for WPS networks, then connect to the configured one.
   *
   * ### The two gestures
   *
   * | Gesture | Action |
   * |---|---|
   * | **Tap** | Scan + attack (the full workflow), then connect to the network named by `ose.py`'s `CONNECT_SSID` / `CONNECT_PSK`. Tap again to stop. |
   * | **Long-press** | Open the app. |
   *
   * Long-press is Android's built-in gesture for "more options", so it is exactly
   * where a user looks for the app itself; using it for anything else would make
   * the tile feel like it swallowed the launcher. The tap keeps working silently
   * in the background with no confirmation dialog, and the tile doubles as the
   * status indicator.
   *
   * ### Why the tile starts a *foreground* service directly
   *
   * An earlier version routed the tap through a zero-UI relay Activity, on the
   * theory that a background tile is not allowed to call
   * `startForegroundService`. That turned out to be both wrong and the cause of a
   * real bug: the relay was a task-less, `noHistory` activity with an empty
   * `taskAffinity`, and Android 14 refuses to start an activity from a background
   * app without a visible task to attach it to. The call did not throw — it was
   * silently dropped — so nothing ran and the tile appeared to "react but do
   * nothing".
   *
   * The correct mechanism is the foreground-service *exemption*: a
   * `TileService` is bound by SystemUI, which is a visible, system-owned
   * component, so the "app is in the background" restriction does not apply to
   * it in the first place. `ContextCompat.startForegroundService` from a tile is
   * permitted, and this is the documented way tiles are meant to launch work.
   *
   * ### Staying usable while the app is not running
   *
   * Android will eventually kill any app's process once it is backgrounded, so
   * there is no such thing as a permanently resident daemon here. What this
   * service does instead is rely on the system's own wake-up contract:
   *
   *   * `android:metadata TOGGLEABLE_TILE` (declared in the manifest) tells
   *     SystemUI that this tile's active state is a meaningful on/off rather than
   *     a mere launcher. That makes the tile "live": SystemUI binds the service
   *     and calls [onStartListening] every time the shade is opened, even if the
   *     app has been killed in the meantime.
   *   * [onStartListening] therefore never trusts a cached flag. It asks
   *     [RunStateStore] for the truth, and that store validates the persisted
   *     record against the recorded PID — i.e. exactly when the process had been
   *     dead and was just recreated.
   *
   * The upshot: pulling down the notification shade is enough to bring the tile
   * back to a correct, tappable state. No background polling, no battery cost.
   *
   * ### Dimming when the app goes away
   *
   * The tile must never stay lit for a run that is not there. Three places keep
   * it honest: [onStopListening] reconciles whenever the shade closes, the
   * 2-second check in [verifyStartAfterDelay] catches a start that was accepted
   * and then died, and [onDestroy] covers the app's own process going away
   * mid-run — the last one is what makes the tile dim "no matter what".
   */
  ```

- /** KDoc */
  ```
  /**
           * Name of the SharedPreferences file holding the remembered plan, and
           * the key inside it.
           *
           * These mirror the constants in MainActivity. Normally that would be a
           * smell — two places spelling the same string — but the tile and the
           * activity can genuinely run in different processes, so the value has
           * to travel through a real store either way.
           */
  ```

- /** KDoc */
  ```
  /**
           * Pushes the current run state into the tile.
           *
           * Called from MainActivity when a run finishes, and from the service's
           * state listener when the watchdog observes the process exit — so a run
           * started from the in-app button also updates the tile.
           *
           * Safe to call at any time: requestListeningState is a no-op when the
           * tile is not currently added to the shade.
           */
  ```

- // 行注释
  ```
  // The tile simply will not refresh; never crash the caller.
  ```

- // 行注释
  ```
  // Re-reads RunService, which reconstructs persisted state if this
  ```

- // 行注释
  ```
  // process was just started by SystemUI. See the class comment.
  ```

- /** KDoc */
  ```
  /**
       * Runs when the shade is closed and the tile stops being shown.
       *
       * Whatever the optimistic paint in [onClick] claimed, the run state the
       * store holds is the truth — so reconcile on the way out. This is the
       * second half of the fix for "the tile is lit but nothing is running": a
       * refused `startForegroundService` rolls back in [onClick], but a call that
       * was *accepted* and then died early (no root, script missing, pre-flight
       * failed) would otherwise leave the tile lit until the next shade pull.
       */
  ```

- /** KDoc */
  ```
  /**
       * Dims the tile when this service goes away — i.e. when the app's process is
       * torn down.
       *
       * This is the "no matter what" case. SystemUI unbinds the tile when the
       * app's process dies, so [onDestroy] is the one hook guaranteed to run on
       * the way out — whether the app was swiped away, killed by the system for
       * memory, or crashed. `RunStateStore` has already been cleared by
       * [RunService.onDestroy] on the normal path, so this mainly covers the
       * abrupt cases where the service never got that far.
       *
       * `qsTile` can legitimately be null here (the shade may already be gone, and
       * the system may have dropped the binder). [setTileVisual] checks for that
       * and returns quietly, so a null tile is not treated as an error.
       */
  ```

- // 行注释
  ```
  // Nothing more the tile can do about it at this point.
  ```

- /** KDoc */
  ```
  /** Reflect the live run state into the tile's visual state. */
  ```

- /** KDoc */
  ```
  /**
       * Long-press opens the app.
       *
       * `TileService` has no `getLongClickIntent` — that is an `Activity` API.
       * The system's own long-press behaviour is to open the app's *tile settings
       * activity*, which is declared in the manifest with the
       * `android.service.quicksettings.action.QS_TILE_PREFERENCES` action. So the
       * long-press target is chosen there, not here; [TilePreferencesActivity]
       * is that target and immediately forwards to [MainActivity].
       *
       * There is deliberately no override in this class: adding one would compile
       * against nothing, and the manifest route is the only one the platform
       * actually honours for quick-settings tiles.
       */
  ```

- /** KDoc */
  ```
  /**
       * Tap = scan + break the strongest crackable AP + join it; tap again while
       * running = stop.
       *
       * `-c` used to mean "skip the scan and join whatever is in the variables",
       * which never matched what the tile was for: pressing it in a place you had
       * never visited joined nothing at all. It now means "break one, then join
       * that one", so the connect uses the PSK the attack just recovered and the
       * variables are only a fallback for a scan that came up empty.
       *
       * It is one script invocation rather than two runs. Doing it in a single
       * process means the connect happens while the interface is still ours — a
       * second run would have to be handed the interface back by the framework
       * first, and that hand-off is where a race would live.
       *
       * The order matters for how the interaction feels. The visual is repainted
       * immediately at the top, so the shade animates to the new state on the same
       * frame as the tap; the actual service call happens after. Waiting for the
       * service round-trip before repainting would leave a visible lag between the
       * tap and the tile changing.
       */
  ```

- // 行注释
  ```
  // Paint "off" first, then actually stop.
  ```

- // 行注释
  ```
  // Nothing to show here; the next onStartListening corrects it.
  ```

- // 行注释
  ```
  // No runtime yet -> nothing sensible to run. Open the app so the user
  ```

- // 行注释
  ```
  // sees why instead of getting a silent no-op, and make sure the tile is
  ```

- // 行注释
  ```
  // painted *off* first: this branch returns before the optimistic paint
  ```

- // 行注释
  ```
  // below, and on a cold process the tile may still be showing the state
  ```

- // 行注释
  ```
  // it had when the shade was opened.
  ```

- // 行注释
  ```
  // Paint "on" first, then start. If the start fails, the next
  ```

- // 行注释
  ```
  // onStartListening (or the watchdog) pulls it back to off — a brief
  ```

- // 行注释
  ```
  // optimistic state is worth the responsiveness.
  ```

- // 行注释
  ```
  // The run's identity is minted *here*, before the service is asked to
  ```

- // 行注释
  ```
  // start, and carried through to it. That is what lets the activity
  ```

- // 行注释
  ```
  // recognise "the run I am about to show" and refresh its terminal for
  ```

- // 行注释
  ```
  // it — the activity and the tile can live in different processes, so an
  ```

- // 行注释
  ```
  // in-memory handshake is not available.
  ```

- // 行注释
  ```
  // The direct route was refused. Roll the optimistic paint back so
  ```

- // 行注释
  ```
  // the tile does not lie about a run that never began.
  ```

- // 行注释
  ```
  // A start that was *accepted* can still die immediately (root refused,
  ```

- // 行注释
  ```
  // interpreter missing, a 方案2 pre-flight that could not disconnect).
  ```

- // 行注释
  ```
  // Give the service a moment and then confirm — otherwise the tile stays
  ```

- // 行注释
  ```
  // lit for a run that never really began, which is the "tile is dead
  ```

- // 行注释
  ```
  // while the app is in the background" symptom.
  ```

- /** KDoc */
  ```
  /**
       * Confirms, a couple of seconds after the tap, that the run really is live.
       *
       * [startForegroundService] returning without throwing only means the call
       * was accepted — not that a process came up. Every early-exit path inside
       * the service (no root, missing runtime, failed 方案2 pre-flight) clears the
       * shared state; so if the state is gone, the run is gone, and the tile has
       * to be repainted off and the reason shown.
       *
       * The delay is deliberately on a background thread: `onClick` runs on the
       * main thread and blocking it (never mind sleeping in it) would freeze the
       * shade. 2 s is enough for the service to have reached its start-or-abort
       * decision — it does no network work before that point.
       */
  ```

- // 行注释
  ```
  // Healthy: the store still describes a live run. That is true both
  ```

- // 行注释
  ```
  // when it is *our* run and when a newer tap has superseded us —
  ```

- // 行注释
  ```
  // either way something is running and the tile is right to be lit.
  ```

- // 行注释
  ```
  // The run is gone. Repaint off on the main thread, then surface the
  ```

- // 行注释
  ```
  // terminal so the user can read the reason instead of wondering why
  ```

- // 行注释
  ```
  // the tile did nothing.
  ```

- /** KDoc */
  ```
  /**
       * Starts the tile's run: full workflow, then connect.
       *
       * A TileService is bound by SystemUI, so the background-start restriction
       * does not apply and this is a legal, direct foreground-service start — no
       * relay activity involved. See the class comment for why that matters.
       *
       * `-c` is always set: the tile's job is "破解 + 连接", so it scans, breaks
       * the strongest AP it can, and joins that one. Passed as a separate extra
       * rather than folded into the mode, because it composes with the plan
       * (`-m -c` = connect without taking the radio down).
       *
       * @param runId the identity minted by [RunStateStore.markStarting], passed
       *        through so the service records the same run the tile announced.
       * @return true when the service accepted the start.
       */
  ```

- /** KDoc */
  ```
  /**
       * The plan the user last picked in the app, so a tile-started run behaves
       * the same way an in-app run would.
       *
       * The tile has no UI of its own to offer a choice (a quick-settings tile
       * cannot host two buttons), so it inherits the app's selection. Unknown or
       * missing values fall back to 方案1 — the same default the app uses.
       */
  ```

- /** KDoc */
  ```
  /** Brings the app to the foreground so the user can see what is wrong. */
  ```

- // 行注释
  ```
  // Even this can be refused; nothing further we can do from here.
  ```

- /** KDoc */
  ```
  /**
       * Repaints the tile without consulting RunService.
       *
       * Used by [onClick] for the optimistic flip, where the authoritative state
       * has intentionally not changed yet.
       */
  ```

## java/com/hyx/oneshot/wifitools/ScriptRunner.kt

- /** KDoc */
  ```
  /**
   * Describes how to launch the bundled script, and gives the log reader a stable
   * place to find its output.
   *
   * Two execution paths are supported and the class figures out which one applies:
   *
   *  1. Root available (`su -c`): run the interpreter as uid 0. This is what the
   *     script actually needs — it drives nl80211 through wpa_supplicant.
   *  2. No root: run under the app's own uid. The UI still works end-to-end and
   *     the script will report its own permission errors, which is the honest
   *     outcome rather than a fake success.
   */
  ```

- /** KDoc */
  ```
  /**
       * Interface the script is told to use.
       *
       * Sent as `OSE_IFACE`, but only as a *last resort*: the script checks that
       * the name really exists before honouring it and otherwise auto-detects, so
       * this is a hint about which name is most likely — not a decision the app
       * gets to force. The old "指定网卡" text field was removed because typing a
       * name can only ever make things worse than detection: a typo silently
       * degrades to the same detection, and a correct entry does not reveal
       * anything detection was not already going to find. Phones name their
       * wireless device `wlan0` unless the kernel was patched, which is why
       * auto-detection is the better default and this stays a fallback.
       */
  ```

- /** KDoc */
  ```
  /** File the script writes recovered credentials to, resolved at runtime. */
  ```

- /** KDoc */
  ```
  /**
       * How the script should be invoked.
       *
       * The app always runs the full workflow: scan for WPS networks, attack them
       * and save every recovered credential. The script's `-c` connect mode is a
       * manual CLI feature (`ose.py -c <SSID> <password>`) that the app does not
       * expose — the in-app button and the quick-settings tile both scan.
       *
       * The two entries here are the two *ways of freeing the interface*, which
       * the script selects with `-m`:
       *
       *   * [FULL] — "方案1", the original behaviour: `svc wifi disable` takes the
       *     whole radio down for the run and `svc wifi enable` brings it back.
       *   * [MODE2] — "方案2": the radio is left alone. Before the script starts,
       *     the app disconnects the current association through the framework
       *     (see [WifiDisconnector]); the script then only verifies the interface
       *     is free (`-m`) and refuses to run if it is not.
       */
  ```

- /** KDoc */
  ```
  /** 方案1: svc wifi disable/enable — takes the radio down. */
  ```

- /** KDoc */
  ```
  /** 方案2: leave the radio up, disconnect through the framework. */
  ```

- /** KDoc */
  ```
  /**
       * Builds the command line used to start the script.
       *
       * The interpreter is launched through the shim in lib/ so PYTHONHOME and
       * PYTHONPATH are always correct, and `cd` is explicit so the script's
       * relative paths (wifipassword.txt) land next to it, not in "/".
       *
       * PATH is the important part: the script calls `which('pixiewps')`,
       * `which('wpa_supplicant')`, `which('iw')` and `which('ip')` in its
       * requirement check, so the bundled binaries in filesDir/bin must come
       * first. /system/bin is appended so toybox's `ip` still resolves.
       *
       * @param connectOnly append `-c`: connect-only, no scanning. The script
       *        then joins the network named by its own `CONNECT_SSID` /
       *        `CONNECT_PSK` variables and exits. Used by the tile's long-press
       *        alternative and by the "破解 + 连接" tap.
       */
  ```

- // 行注释
  ```
  // `-u` keeps stdout unbuffered so the log pane fills in real time.
  ```

- // 行注释
  ```
  // Mode arguments go after the script path, which is where the script
  ```

- // 行注释
  ```
  // looks for them ("'-c' in sys.argv[1:]").
  ```

- // 行注释
  ```
  //
  ```

- // 行注释
  ```
  // `-m -c` reads as "connect without taking the radio down". The script
  ```

- // 行注释
  ```
  // parses each flag independently, so the order is cosmetic — this one
  ```

- // 行注释
  ```
  // just keeps the log line readable.
  ```

- // 行注释
  ```
  // cd into the script's own directory so its relative wifipassword.txt
  ```

- // 行注释
  ```
  // lands next to ose.py, matching os.path.dirname(__file__) in the script.
  ```

- /** KDoc */
  ```
  /**
       * The environment the interpreter must be started with.
       *
       * This is set on the [ProcessBuilder] itself rather than as `VAR=value`
       * prefixes inside the shell command, and that distinction is the whole
       * point:
       *
       *   * A `su -c "VAR=x cmd"` prefix is only honoured if the root manager
       *     passes the string through verbatim. Several of them rewrite or
       *     re-quote the argument, and anything after the first `&&` can end up
       *     in a different shell context — silently dropping the assignments.
       *   * [ProcessBuilder.environment] is inherited by the child directly and
       *     cannot be re-interpreted by any intermediate shell.
       *
       * Both are applied (the prefixes are kept in [buildCommand] as a harmless
       * belt-and-braces measure); this map is the authoritative one.
       *
       * Why HOME in particular matters: the script derives its data directories
       * from it —
       *
       *     USER_HOME    = str(Path.home())
       *     SESSIONS_DIR = f'{USER_HOME}/.OneShot-Extended/sessions/'
       *     PIXIEWPS_DIR = f'{USER_HOME}/.OneShot-Extended/pixiewps/'
       *
       * — and Android hands every app a HOME of `/data`, which is not writable
       * by the app itself. Under root the pwd lookup also fails, so `Path.home()`
       * degrades all the way to `/` and the paths collapse to the literal
       * `"//.OneShot-Extended"` seen in the crash:
       *
       *     OSError: [Errno 30] Read-only file system: '//.OneShot-Extended'
       *
       * Pointing HOME at filesDir puts everything under
       * `<filesDir>/.OneShot-Extended/{sessions,pixiewps}`, which is writable by
       * both the app uid and root.
       */
  ```

- // 行注释
  ```
  // TMPDIR too: the script writes session files through
  ```

- // 行注释
  ```
  // tempfile.gettempdir(), and /data/local/tmp is not reliably writable
  ```

- // 行注释
  ```
  // for every app, so it gets a directory it definitely owns.
  ```

- // 行注释
  ```
  // The one that fixes the crash.
  ```

- // 行注释
  ```
  // Explicit, unambiguous data root for the script's own
  ```

- // 行注释
  ```
  // _resolve_user_home(). Belt and braces: HOME above already covers
  ```

- // 行注释
  ```
  // it, but this one is read directly by name so it cannot be lost
  ```

- // 行注释
  ```
  // even if something resets HOME on the way in.
  ```

- // 行注释
  ```
  // Which wireless device to drive. Always DEFAULT_IFACE now that the
  ```

- // 行注释
  ```
  // 指定网卡 field is gone; the script treats it as a fallback and
  ```

- // 行注释
  ```
  // auto-detects when the name is not present on the device. Kept as
  ```

- // 行注释
  ```
  // an explicit environment entry rather than dropped entirely so the
  ```

- // 行注释
  ```
  // two sides keep the same contract and the script's own override
  ```

- // 行注释
  ```
  // path stays exercised.
  ```

- // 行注释
  ```
  // PYTHONHOME must be the prefix root: CPython appends lib/python3.11
  ```

- // 行注释
  ```
  // to it when locating the stdlib.
  ```

- // 行注释
  ```
  // $libDir holds libpython3.11.so, so it must stay here or the
  ```

- // 行注释
  ```
  // interpreter will not even start.
  ```

- // 行注释
  ```
  // PATH order matters twice over:
  ```

- // 行注释
  ```
  //   * $binDir first    -> our pixiewps / wpa_supplicant / wpa_cli / iw win
  ```

- // 行注释
  ```
  //   * /system/bin next -> toybox supplies `ip`, and `su` is found
  ```

- // 行注释
  ```
  // The script emits UTF-8 box drawing and non-ASCII SSIDs; without a
  ```

- // 行注释
  ```
  // UTF-8 locale Python falls back to ASCII and dies on the first one.
  ```

- // 行注释
  ```
  // Some root managers wipe the environment entirely; these two give
  ```

- // 行注释
  ```
  // the spawned shell a sane baseline so `mkdir`/`cd` still resolve.
  ```

- /** KDoc */
  ```
  /**
       * Results of the root probe, so the caller can say something meaningful in
       * the log instead of silently switching execution mode.
       */
  ```

- /** KDoc */
  ```
  /**
       * Actively asks for root by running `su` and reading back the uid.
       *
       * This is deliberately *not* a passive "is a su binary present?" check. The
       * point is to make the root manager (Magisk / KernelSU / SuperSU) show its
       * authorization prompt, and to confirm the grant by verifying that the
       * process really ends up as uid 0. A su binary existing on PATH proves
       * nothing — the user may deny the request, or the binary may be a decoy.
       *
       * @return GRANTED when the resulting uid is 0, DENIED when su exists but
       *         refused (or the user declined), NOT_ROOTED when no su is present,
       *         ERROR for unexpected failures.
       */
  ```

- // 行注释
  ```
  // Read the output before waiting: a large buffered response could
  ```

- // 行注释
  ```
  // otherwise deadlock the child against a full pipe.
  ```

- // 行注释
  ```
  // 20s is generous enough for a human to tap "Allow" in the root
  ```

- // 行注释
  ```
  // manager dialog, and short enough not to hang the UI flow.
  ```

- /** KDoc */
  ```
  /** Locates a usable `su` binary, preferring the ones on PATH. */
  ```

- /** KDoc */
  ```
  /** True when a working `su` binary is present on the device. */
  ```

## java/com/hyx/oneshot/wifitools/TilePreferencesActivity.kt

- /** KDoc */
  ```
  /**
   * Long-press target for the quick-settings tile.
   *
   * ### Why this exists
   *
   * Android gives quick-settings tiles exactly one long-press behaviour: open the
   * activity the app declared with the `QS_TILE_PREFERENCES` action. There is no
   * `TileService.getLongClickIntent()` to override — that is an `Activity` API,
   * and overriding it in a `TileService` compiles against nothing. So the only
   * way to make long-press do something is to declare this activity and let the
   * system route to it.
   *
   * ### What it does
   *
   * Immediately forwards to [MainActivity] and finishes itself, so the user
   * experience is simply "long-press opens the app". This activity exists purely
   * because the platform insists on a *separate* component for the tile's
   * preferences entry point; it is a one-line redirect, not a screen.
   *
   * `noHistory` + `excludeFromRecents` + a transparent theme keep it from ever
   * showing up as a step you can go back to, or as a stray card in the recents
   * list.
   */
  ```

- // 行注释
  ```
  // Task-less and invisible: this is a redirect, not a destination.
  ```

## java/com/hyx/oneshot/wifitools/WifiDisconnector.kt

- /** KDoc */
  ```
  /**
   * Drops the current Wi-Fi *connection* without turning the Wi-Fi *switch* off.
   *
   * ### Why this lives in Kotlin and not in the script
   *
   * Earlier revisions tried to do this from `ose.py`, with `wpa_cli disconnect`,
   * `iw disconnect`, `disable_network` on every saved profile and finally by
   * killing the framework's supplicant. All of it lost the same race: Android's
   * Wi-Fi state machine owns the auto-join decision, notices the link drop, and
   * reassociates within about a second. Fighting it from below is a losing
   * position — the supplicant we are racing is the one the framework keeps
   * restarting.
   *
   * The framework has a first-class call for exactly this: `WifiManager
   * .disconnect()`, documented as "disassociate from the currently active access
   * point". It suspends auto-join for the current network instead of merely
   * tearing the link down, which is why it sticks where `iw disconnect` does
   * not.
   *
   * ### Why root is required
   *
   * `WifiManager.disconnect()` is a public API, but since Android Q the Wi-Fi
   * service answers it with `false` for any ordinary third-party caller — the
   * framework only acts on it for system callers. There is no manifest
   * permission that changes that; the check is on the caller's identity, not on
   * a permission the app could request.
   *
   * With root we can become a system caller. `su -c "cmd wifi ..."` does *not*
   * get us there: the `wifi` shell command has no disconnect verb at all (only
   * `set-wifi-enabled`, which is the wrong operation, and `forget-network`,
   * which deletes the saved network). So instead we run a small Java program
   * under `app_process` — the same mechanism tools like `am`/`svc` use — after
   * telling the runtime to impersonate the `android` uid and package. Inside
   * that process `WifiManager.disconnect()` is answered normally.
   *
   * ### The fallback chain
   *
   * A single mechanism is not enough in practice. Every one of these depends on
   * something a vendor ROM is free to change — whether `app_process` can attach
   * to `system_server`, whether the shell command exists, whether the driver
   * honours a raw nl80211 disconnect, whether the framework notices. Rather than
   * pick one and hope, four attempts are made in order of *how faithful* they
   * are to "disconnect but leave the switch alone", and the first one that
   * leaves the interface genuinely unassociated wins:
   *
   * | # | Mechanism | Cost when it is the one that works |
   * |---|---|---|
   * | 1 | `WifiManager.disconnect()` via `app_process` | none — the ideal call |
   * | 2 | `cmd wifi connect-network` on a throwaway network | none (adds a saved network, removed after) |
   * | 3 | raw `iw dev <if> disconnect` + autojoin suppression | none |
   * | 4 | `svc wifi disable` → wait → `svc wifi enable` | **the switch blinks** — last resort |
   *
   * Level 4 is the honest admission that "the user wants working Wi-Fi at the
   * end, not a specific syscall in the middle". Toggling the radio does drop the
   * association and does leave the switch on by the time we return. It is the
   * one outcome the earlier revisions refused to consider, and refusing it is
   * why they kept failing outright. It is tried last, and only when the cheaper
   * mechanisms have all been defeated.
   *
   * ### Scope of the change
   *
   * Levels 1–3 change nothing that persists: the radio stays on, the saved
   * network is not removed, and auto-join resumes as soon as the framework
   * decides to reconnect. Level 4 temporarily takes the radio down and puts it
   * straight back. Nothing here survives the run.
   */
  ```

- /** KDoc */
  ```
  /** How long to wait for a root shell before giving up on it. */
  ```

- /** KDoc */
  ```
  /**
       * Outcome of a disconnect attempt, so the caller can say something useful
       * in the terminal instead of a bare pass/fail.
       */
  ```

- /** KDoc */
  ```
  /**
           * The interface is no longer associated.
           *
           * @param level which mechanism ended up doing it, for the log.
           * @param blipped true when the radio had to be cycled (level 4), so the
           *        UI can mention the switch flicker rather than pretend nothing
           *        happened.
           */
  ```

- /** KDoc */
  ```
  /** Wi-Fi was already off or already disconnected — nothing to do. */
  ```

- /** KDoc */
  ```
  /** Root was not available, so no call could have been made. */
  ```

- /** KDoc */
  ```
  /** Every mechanism ran and none of them detached the interface. */
  ```

- // 行注释
  ```
  // ------------------------------------------------------------------ tools
  ```

- /** KDoc */
  ```
  /**
       * Builds the shell driver for one attempt.
       *
       * Everything runs inside a single `su -c` so the root grant is asked for
       * exactly once per attempt and the helper's own `iw` check happens in the
       * same context that did the disconnecting — asking from the app process
       * instead would be a different uid with different netlink permissions.
       */
  ```

- // 行注释
  ```
  // `iw` is not on PATH on every ROM; the app ships its own copy under
  ```

- // 行注释
  ```
  // bin/, but that is an app-private path that root can read. Both are
  ```

- // 行注释
  ```
  // tried so the helper works whether or not the ROM provides one.
  ```

- /** KDoc */
  ```
  /** The association check every level ends with, straight from the kernel. */
  ```

- /** KDoc */
  ```
  /**
       * Runs [script] as root and returns (exit code, output).
       *
       * A null return means the shell itself could not be started, which is a
       * different failure from "the command ran and did not work" — the caller
       * distinguishes them so the log says which.
       */
  ```

- /** KDoc */
  ```
  /**
       * True when the last line of [output] says the interface is genuinely free.
       *
       * The check is deliberately on the *kernel's* answer (`iw ... link`), not
       * on the exit status of whatever disconnected it. A ROM that reports
       * failure and disconnects anyway, and a ROM that reports success and lets
       * auto-join undo it, are both real; only the driver's view is trustworthy.
       */
  ```

- // 行注释
  ```
  // ------------------------------------------------------------------ levels
  ```

- /** KDoc */
  ```
  /**
       * Level 1 — the framework's own disconnect.
       *
       * This is the only mechanism that also tells the framework to *stop
       * reconnecting*, which is why it is tried first. The helper is a Java
       * program run by `app_process`; see [javaProgram] for why that indirection
       * is necessary.
       *
       * `am` is not used because the app has no exported component that could
       * accept the request, and adding one would mean a broadcast any other app
       * could send.
       */
  ```

- // 行注释
  ```
  // `--nice-name` is cosmetic but shows up in `ps`, which is the
  ```

- // 行注释
  ```
  // difference between "what is this mystery process" and a name that
  ```

- // 行注释
  ```
  // explains itself in a bug report.
  ```

- // 行注释
  ```
  //
  ```

- // 行注释
  ```
  // BOOTCLASSPATH is inherited from the shell; on some ROMs `su` starts
  ```

- // 行注释
  ```
  // with it unset, so it is filled in from the framework jars when
  ```

- // 行注释
  ```
  // missing. Without it `app_process` cannot boot a VM at all.
  ```

- // 行注释
  ```
  // Two attempts: on a cold run `dex2oat` may not have an image for the
  ```

- // 行注释
  ```
  // class yet and dalvikvm reports "Class not found" once.
  ```

- /** KDoc */
  ```
  /**
       * Level 2 — make the framework move itself.
       *
       * `cmd wifi connect-network` asks the Wi-Fi service to join a network. It
       * is a public, documented shell verb (unlike disconnect), and the service
       * performs a real handover: the current association is torn down as part
       * of switching to the requested one.
       *
       * A throwaway open network with a random name is used as the destination,
       * so a real AP is never involved and the SSID cannot collide. The entry it
       * creates is removed again immediately, which is why this costs nothing
       * that outlives the call.
       *
       * The `-b` BSSID pin points at a MAC that does not exist, so the framework
       * drops the current link and then fails to find the target — ending up
       * associated to nothing, which is exactly the state we want.
       */
  ```

- // 行注释
  ```
  // Ask the service to switch to something unreachable: it
  ```

- // 行注释
  ```
  // disassociates from the current AP first.
  ```

- // 行注释
  ```
  // Clean the entry up so no trace of the attempt is left behind.
  ```

- /** KDoc */
  ```
  /**
       * Level 3 — raw nl80211 disconnect with auto-join suppressed.
       *
       * This is the mechanism the earliest revisions used and it is here only
       * because it can still win on a ROM that leaves us a window: the link is
       * dropped and the supplicant is told to stop managing networks for a
       * moment, all in one shell so no scheduler gap opens between them.
       *
       * It is ranked below the two framework calls precisely because it *is*
       * racy — but a racy mechanism that works is better than a principled one
       * that does not, and the caller verifies the result either way.
       */
  ```

- // 行注释
  ```
  // The app's bundled wpa_cli, for ROMs with no system copy.
  ```

- /** KDoc */
  ```
  /**
       * Level 4 — cycle the radio.
       *
       * The last resort, and the one earlier revisions refused to consider. The
       * switch is turned off and then back on, which drops the association
       * unconditionally: no auto-join survives a radio that was off while it
       * decided. The delay matters — bringing it straight back up can race the
       * off transition and leave Wi-Fi in a stuck state, so the down state is
       * held for a moment and then the interface is polled until it comes back.
       *
       * What the user asked for is *working Wi-Fi with no connection*, not a
       * particular syscall. This delivers that whenever nothing else can, at the
       * cost of one visible flicker of the switch.
       */
  ```

- // 行注释
  ```
  // Wi-Fi needs a moment to come back before its state is meaningful.
  ```

- // 行注释
  ```
  // ------------------------------------------------------------------- api
  ```

- /** KDoc */
  ```
  /**
       * Asks the system to drop the current association, trying each mechanism
       * in [the chain][WifiDisconnector] until one of them leaves the interface
       * genuinely free.
       */
  ```

- // 行注释
  ```
  // Already free? Then there is nothing to do, and saying so is better
  ```

- // 行注释
  ```
  // than reporting a success for work that never happened.
  ```

- /** KDoc */
  ```
  /**
       * Reads the kernel's view of the interface before anything is attempted.
       *
       * Uses `iw` from the system if present, otherwise the bundled copy — the
       * same choice the levels make, so the "already free" answer cannot
       * disagree with the answer that follows an attempt.
       */
  ```

- // 行注释
  ```
  // cannot tell; assume busy and let a level try
  ```

- // 行注释
  ```
  // -------------------------------------------------------- level 1 helper
  ```

- /** KDoc */
  ```
  /**
       * The Java program handed to `app_process`.
       *
       * Kept as a single string rather than a compiled-in class because it has
       * to run under a *different* uid than ours: a class from this APK would
       * need this APK's classloader, and by the time we are uid 1000 that
       * classloader is exactly what we do not have. `app_process` boots a bare
       * VM for us instead, and inside it these lines are self-contained.
       *
       * ### What changed after it did not work on a real device
       *
       * The first version called `ActivityThread.systemMain()`, which attaches
       * to the *system server* — a heavy, privileged operation that fails on
       * most ROMs when attempted from a shell-spawned process. `systemMain()` is
       * for forking a second system server, not for borrowing a system context.
       *
       * Two lighter routes are used instead, and the cheap one is tried first:
       *
       *   * `ActivityThread.currentActivityThread()` — when `app_process`
       *     actually runs as `--nice-name` under an app-like uid this returns a
       *     usable thread with a real context.
       *   * `ActivityThread.systemMain()` — kept as the second route, because on
       *     the ROMs where it does work it is the most complete context.
       *
       * ### The other bug fixed here
       *
       * The old program treated `getConnectionInfo().getNetworkId() == -1` as
       * "not associated". That is true *by definition* on Android 10+, where
       * `getNetworkId()` is deprecated and documented to return -1 — so the
       * check silently reported FREE on exactly the devices that needed the
       * disconnect, skipping the call entirely. Association is now read from
       * `getSupplicantState()` and `getBSSID()`, neither of which is stubbed.
       */
  ```

- /** KDoc */
  ```
  /** Single-quotes [value] for `sh`, escaping any embedded quote. */
  ```

## res/drawable/ic_delete.xml

- `<!-- -->` XML 注释
  ```
  <!--
    Trash icon for the 密码本 header.
  
    Standard Material "delete" glyph — lid, body and the two vertical seams that
    make it read as a waste bin rather than a plain rectangle. Paired with the
    save icon to its left, the two together are the usual "export / purge" pair,
    which is why neither needs a text label.
  
    Same brand_blue as ic_save: the destructive meaning is carried by the
    confirmation dialog (red plate, "是 / 否"), not by the icon colour, so the
    header row stays visually calm and the red is reserved for the moment it
    actually matters.
  -->
  ```

## res/drawable/ic_save.xml

- `<!-- -->` XML 注释
  ```
  <!--
    Save icon for the 密码本 header.
  
    The user asked for a "save icon" on the left of the two header buttons, so
    the glyph is the standard Material *floppy disk* (the canonical "save"
    symbol), not the download-to-tray arrow — the floppy is what reads as
    "save" without a label.
  
    Colour comes from brand_blue rather than a hard-coded hex so the two header
    icons stay in step with the rest of the palette (and a re-brand stays a
    one-line change in colors.xml). The vector is used directly as an
    ImageButton's src, so no separate tint list is needed.
  -->
  ```

## res/drawable/ic_tile_oneshot.xml

- `<!-- -->` XML 注释
  ```
  <!-- Quick-settings tile icon: Wi-Fi glyph + open padlock.
  
       Converted from the supplied PSD. The PSD holds a single flat pixel layer
       (no shape layers, no vector masks), so the artwork was traced from its
       alpha channel with potrace and then converted to VectorDrawable — every
       curve, arc and rounded stroke of the original is preserved as Bezier
       geometry, and nothing was redrawn by hand. -->
  ```

## res/layout/activity_main.xml

- `<!-- -->` XML 注释
  ```
  <!--
    Root layout.
  
    Plain white ground. Earlier revisions set a photographic background here and
    layered translucent white panels on top so the text stayed readable; that was
    removed along with the image, so the panes below are now the only surfaces and
    need no translucency of their own.
  -->
  ```

- `<!-- -->` XML 注释
  ```
  <!--
        Tab strip: white background, brand-coloured *text*.
  
        The colour goes on the label and the indicator, never on the bar itself —
        a solid blue strip reads as a toolbar and competes with the content,
        whereas blue-on-white keeps the accent where the user is actually looking
        and leaves the page looking light.
      -->
  ```

- `<!-- -->` XML 注释
  ```
  <!-- ============================ Tab 1: 主页 ============================ -->
  ```

- `<!-- -->` XML 注释
  ```
  <!--
                Terminal pane.
  
                Layout notes, all of which exist to keep the ASCII banner on a
                single grid:
  
                1. `includeFontPadding=false` plus a fixed `lineSpacingExtra=0`
                   and `lineSpacingMultiplier=1.0` removes the extra leading
                   Android adds around each line by default. That padding differs
                   per line depending on the tallest glyph in it, so lines with
                   tall characters drifted apart from lines without.
                2. `textSize` is 9sp and the pane scrolls horizontally. The banner
                   is 53 columns and the longest log line is 83 characters, both
                   wider than a phone in portrait; without horizontal scrolling
                   the right edge would simply be clipped.
                3. width is `wrap_content` inside the HorizontalScrollView so the
                   text lays out at its natural width instead of being wrapped.
                4. `fontFamily="monospace"` with no letterSpacing keeps every
                   glyph in one cell. Characters the monospace font does not
                   cover are substituted before they reach the view — see
                   MainActivity.normalizeForMonospace().
              -->
  ```

- `<!-- -->` XML 注释
  ```
  <!--
                Title row: the pane's own label on the left, then the plan
                selector on the right.
  
                The two plan buttons are the *same control* the script's `-m`
                flag exposes on the command line, surfaced in the UI: 方案1 is
                the original `svc wifi disable/enable` behaviour, 方案2 leaves the
                radio up and frees the interface by disconnecting instead.
  
                They reuse the start/stop colour language — the active plan is
                solid brand blue with white text, the inactive one is the same
                pale grey as the idle 关闭 button — so "which one is on" is read
                the same way in both rows. A pair of radio buttons would also
                work, but this keeps the whole screen speaking one visual
                language.
  
                「解除占用」 is a plain label, not a control: it names what the
                selector does, so the buttons do not need to explain themselves.
  
                To its left sits 「重复模式」, the one control in this row that is
                not part of the plan selection. Ticking it makes a run restart
                itself whenever it stops — see MainActivity.repeatCheck. It is
                deliberately a checkbox rather than another start/stop button:
                it is a *property of the next run*, not an action.
              -->
  ```

- `<!-- -->` XML 注释
  ```
  <!--
                    重复模式：文字在左、勾选框在右。文字是纯展示的 TextView，
                    勾选框是只有按钮、没有文字的 CheckBox —— 这样可点击区域
                    就只剩那个小方框，点「重复模式」这四个字不会误触发。
  
                    `buttonTint` 用品牌蓝，让勾选状态跟 方案1/方案2、开始按钮
                    用同一套颜色语言，而不是默认的青色。
                  -->
  ```

- `<!-- -->` XML 注释
  ```
  <!-- Two buttons side by side: start on the left, stop on the
                   right. Both are enabled/disabled in lockstep with the script
                   state so the wrong one can never be fired. -->
  ```

- `<!-- -->` XML 注释
  ```
  <!-- ======================= Tab 2: 密码本 ======================= -->
  ```

- `<!-- -->` XML 注释
  ```
  <!--
                Header row: title on the left, the two icon actions pinned to the
                right edge.
  
                The actions live here rather than beside the 刷新 button at the
                bottom because they act on the *password book as a whole* —
                export it elsewhere, or wipe it — whereas 刷新 only re-reads it.
                Putting the pair up top also keeps them out of thumb-reach of
                the refresh button, so a mis-tap cannot turn "reload" into
                "delete".
  
                Both are borderless ImageButtons (`?attr/selectableItemBackgroundBorderless`)
                so they read as icons, not as chrome, and each carries a
                contentDescription because an icon-only control has no visible
                label for a screen reader.
              -->
  ```

- `<!-- -->` XML 注释
  ```
  <!-- Save: opens the system file picker, pre-named after the
                       password book itself. -->
  ```

- `<!-- -->` XML 注释
  ```
  <!-- Delete: confirms first, because it is irreversible. -->
  ```

- `<!-- -->` XML 注释
  ```
  <!--
                No permanent path header here on purpose. The empty state and the
                error states each print the path themselves, so a standing
                "路径: …" line above the pane would just repeat it — and it was
                noise once real credentials were showing. The view is kept (set
                to GONE in code) so the layout id stays stable.
              -->
  ```

- `<!-- -->` XML 注释
  ```
  <!-- ======================= Tab 3: 关于 ======================= -->
  ```

- `<!-- -->` XML 注释
  ```
  <!--
                The card is a plain Surface-like panel, not a MaterialCardView:
                this screen is static, so a card would only add elevation and
                rounded corners without earning either.
              -->
  ```

- `<!-- -->` XML 注释
  ```
  <!-- The avatar sits squarely in the middle of the screen width.
                   It is a circular PNG on a transparent canvas, so no extra
                   clipping is needed — the corners are already empty. -->
  ```

- `<!-- -->` XML 注释
  ```
  <!-- Name, centred under the avatar. -->
  ```

- `<!-- -->` XML 注释
  ```
  <!--
                Homepage link, left-aligned like the rest of the body copy.
  
                The label and the link are *separate* TextViews inside one
                left-aligned row. That is what makes "我的个人主页:" inert and
                "github.com/tianlansedexiaoniu" the only tappable thing — put
                them in a single TextView and the whole line, colon included,
                would become the click target.
              -->
  ```

## res/mipmap-anydpi-v26/ic_launcher.xml

## res/values/colors.xml

- `<!-- -->` XML 注释
  ```
  <!--
        Single source of truth for the app's colour palette.
  
        The brand colour is an **accent**, not a surface: it belongs on the
        selected tab label, the tab indicator, the primary button and the tile —
        not on the status bar, the tab strip background or any large area. Large
        blue blocks fight with the content; blue-on-white keeps the accent where
        the eye is already going.
  
        A re-brand is a one-line change to brand_blue below.
      -->
  ```

- `<!-- -->` XML 注释
  ```
  <!-- Pressed / darker step and a very light tint for提示 backgrounds. -->
  ```

- `<!-- -->` XML 注释
  ```
  <!-- Button pair colours. See MainActivity.renderRunState(). -->
  ```

## res/values/strings.xml

## res/values/themes.xml

- `<!-- -->` XML 注释
  ```
  <!--
            The brand colour is an *accent*, not a surface.
  
            It is deliberately kept off the system bars: a blue status bar turns
            the top of the screen into a coloured block that fights with the
            content underneath. White bars with dark icons leave the blue to do
            the one job it is good at — drawing the eye to the selected tab and
            the primary button.
          -->
  ```

- `<!-- -->` XML 注释
  ```
  <!--
            Long-press tooltips.
  
            Android pops a black rounded tooltip whenever a view that carries
            text or a contentDescription is long-pressed. Nothing useful lives in
            it here, and it was appearing over the buttons. Setting the tooltip
            text to null suppresses it at the theme level, so no individual view
            needs touching.
          -->
  ```

