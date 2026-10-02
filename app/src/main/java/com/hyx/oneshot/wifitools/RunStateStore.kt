package com.hyx.oneshot.wifitools

import android.content.Context

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
object RunStateStore {

    private const val PREFS = "wifitools_run_state"

    private const val KEY_RUNNING = "running"
    private const val KEY_MODE = "mode"
    private const val KEY_PID = "pid"
    private const val KEY_STARTED_AT = "started_at"
    private const val KEY_RUN_ID = "run_id"
    private const val KEY_DETAIL = "detail"

    /** Everything a reader needs to know about the current (or last) run. */
    data class Snapshot(
        val running: Boolean,
        val mode: ScriptRunner.Mode,
        val runId: String?,
        val pid: Int,
        val startedAt: Long,
        /** Free-form progress note, e.g. which SSID is being attacked. */
        val detail: String?
    ) {
        companion object {
            val EMPTY = Snapshot(
                running = false,
                mode = ScriptRunner.Mode.FULL,
                runId = null,
                pid = -1,
                startedAt = 0L,
                detail = null
            )
        }
    }

    /**
     * True only when the run this activity is showing has been superseded.
     *
     * A different non-null id means a *newer* run started while the activity was
     * hidden, which is exactly when the terminal has to be re-attached. A null
     * [currentId] means "we have not attached to any run yet" and is treated as
     * stale so a cold start always picks the live run up.
     */
    fun isStale(currentId: String?, snapshot: Snapshot): Boolean {
        val liveId = snapshot.runId ?: return false
        return currentId == null || currentId != liveId
    }

    fun snapshot(context: Context): Snapshot {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val running = prefs.getBoolean(KEY_RUNNING, false)
        val mode = runCatching {
            ScriptRunner.Mode.valueOf(prefs.getString(KEY_MODE, null) ?: "")
        }.getOrDefault(ScriptRunner.Mode.FULL)

        val snapshot = Snapshot(
            running = running,
            mode = mode,
            runId = prefs.getString(KEY_RUN_ID, null),
            pid = prefs.getInt(KEY_PID, -1),
            startedAt = prefs.getLong(KEY_STARTED_AT, 0L),
            detail = prefs.getString(KEY_DETAIL, null)
        )

        if (!running) return snapshot

        // A record with no PID is a run that was marked before the process
        // existed — believe it, because the alternative is a tile that flickers
        // off in the gap between the tap and the fork.
        if (snapshot.pid <= 0) return snapshot

        if (!isPidAlive(snapshot.pid)) {
            // The script died while nobody was watching (force-stop, OOM kill,
            // reboot). Drop the record so the tile goes grey and the activity
            // stops waiting for output that will never arrive.
            clear(context)
            return Snapshot.EMPTY.copy(mode = mode, runId = snapshot.runId)
        }

        return snapshot
    }

    /**
     * Records a run as started, with a brand-new identity.
     *
     * Called by the tile before the service has even been asked to start: the
     * process may be killed in the gap between the tap and the fork, and the
     * record has to survive that.
     */
    fun markStarting(context: Context, mode: ScriptRunner.Mode): String {
        val runId = newRunId()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_RUNNING, true)
            .putString(KEY_MODE, mode.name)
            .putString(KEY_RUN_ID, runId)
            .putInt(KEY_PID, -1)
            .putLong(KEY_STARTED_AT, System.currentTimeMillis())
            .putString(KEY_DETAIL, null)
            .apply()
        return runId
    }

    /**
     * Fills in the real PID once the child process exists.
     *
     * The run id is only applied when one was passed in; a start that went
     * straight through the service (the in-app button does this) has no prior
     * optimistic record and mints its identity here instead.
     */
    fun markRunning(
        context: Context,
        mode: ScriptRunner.Mode,
        pid: Int,
        runId: String?
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existingId = prefs.getString(KEY_RUN_ID, null)

        val editor = prefs.edit()
            .putBoolean(KEY_RUNNING, true)
            .putString(KEY_MODE, mode.name)
            .putInt(KEY_PID, pid)
            .putLong(KEY_STARTED_AT, prefs.getLong(KEY_STARTED_AT, System.currentTimeMillis()))

        // Keep the id the tile minted if there is one; the service must not
        // overwrite it, or the activity would see the id change mid-run and
        // re-attach (and clear the terminal) for a run it is already showing.
        val id = runId ?: existingId ?: newRunId()
        editor.putString(KEY_RUN_ID, id)
        editor.apply()
    }

    /**
     * Publishes a human-readable progress note.
     *
     * Polled by the activity when the activity and the script are in different
     * processes, so it is the only way for the terminal to show *what* the run
     * is doing rather than just *that* it is doing something.
     */
    fun setDetail(context: Context, detail: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_DETAIL, detail)
            .apply()
    }

    /**
     * Undoes [markStarting] when the start was refused, so the tile does not
     * stay lit for a run that never began.
     */
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_RUNNING, false)
            .putInt(KEY_PID, -1)
            .putString(KEY_DETAIL, null)
            // KEY_RUN_ID is deliberately left in place: it is the id of the last
            // run and the activity still needs it to decide whether what it is
            // showing is current.
            .apply()
    }

    /** True when a run is live, by the same rules [snapshot] applies. */
    fun isRunningNow(context: Context): Boolean = snapshot(context).running

    fun currentMode(context: Context): ScriptRunner.Mode = snapshot(context).mode

    private fun isPidAlive(pid: Int): Boolean = runCatching {
        ProcessBuilder("/system/bin/sh", "-c", "kill -0 $pid")
            .redirectErrorStream(true)
            .start()
            .waitFor() == 0
    }.getOrDefault(false)

    /**
     * A run identity that is unique per start.
     *
     * Uniqueness only has to hold within one device's lifetime — the value is
     * never persisted anywhere but here and never compared across reboots — so
     * a millisecond clock plus a counter is sufficient and avoids depending on
     * `java.util.UUID` string parsing.
     */
    private val idCounter = java.util.concurrent.atomic.AtomicLong(0)

    private fun newRunId(): String =
        "${System.currentTimeMillis()}-${idCounter.incrementAndGet()}"
}
