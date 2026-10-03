package com.hyx.oneshot.wifitools

import android.content.Context

object RunStateStore {

    private const val PREFS = "wifitools_run_state"

    private const val KEY_RUNNING = "running"
    private const val KEY_MODE = "mode"
    private const val KEY_PID = "pid"
    private const val KEY_STARTED_AT = "started_at"
    private const val KEY_RUN_ID = "run_id"
    private const val KEY_DETAIL = "detail"

    
    data class Snapshot(
        val running: Boolean,
        val mode: ScriptRunner.Mode,
        val runId: String?,
        val pid: Int,
        val startedAt: Long,
        
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

        
        
        
        if (snapshot.pid <= 0) return snapshot

        if (!isPidAlive(snapshot.pid)) {
            
            
            
            clear(context)
            return Snapshot.EMPTY.copy(mode = mode, runId = snapshot.runId)
        }

        return snapshot
    }

    
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

        
        
        
        val id = runId ?: existingId ?: newRunId()
        editor.putString(KEY_RUN_ID, id)
        editor.apply()
    }

    
    fun setDetail(context: Context, detail: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_DETAIL, detail)
            .apply()
    }

    
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_RUNNING, false)
            .putInt(KEY_PID, -1)
            .putString(KEY_DETAIL, null)
            
            
            
            .apply()
    }

    
    fun isRunningNow(context: Context): Boolean = snapshot(context).running

    fun currentMode(context: Context): ScriptRunner.Mode = snapshot(context).mode

    
    private fun isPidAlive(pid: Int): Boolean {
        if (pid <= 0) return false

        val proc = java.io.File("/proc/$pid")
        if (!proc.exists()) return false

        return runCatching {
            val stat = java.io.File(proc, "stat").readText()
            
            
            
            val after = stat.substringAfterLast(") ")
            after.isNotEmpty() && after[0] != 'Z'
        }.getOrDefault(true)
    }

    
    private val idCounter = java.util.concurrent.atomic.AtomicLong(0)

    private fun newRunId(): String =
        "${System.currentTimeMillis()}-${idCounter.incrementAndGet()}"
}
