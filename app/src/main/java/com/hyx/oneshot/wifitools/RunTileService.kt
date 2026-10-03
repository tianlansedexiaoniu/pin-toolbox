package com.hyx.oneshot.wifitools

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.File

@RequiresApi(Build.VERSION_CODES.N)
class RunTileService : TileService() {

    companion object {
        
        const val PREFS_UI = "wifitools_ui"
        const val KEY_SELECTED_PLAN = "selected_plan"

        
        fun refresh(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
            try {
                requestListeningState(
                    context,
                    ComponentName(context, RunTileService::class.java)
                )
            } catch (_: Exception) {
                
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        
        
        updateTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    
    override fun onStopListening() {
        super.onStopListening()
        if (!RunStateStore.isRunningNow(this)) {
            setTileVisual(active = false)
        }
    }

    
    override fun onDestroy() {
        super.onDestroy()
        try {
            if (!RunStateStore.isRunningNow(this)) {
                setTileVisual(active = false)
            }
        } catch (_: Exception) {
            
        }
    }

    
    private fun updateTile() {
        val tile = qsTile ?: return
        val snapshot = RunStateStore.snapshot(this)
        val running = snapshot.running

        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "连接附近WiFi"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (running) "脚本运行中" else "开始连接附近WiFi"
        }
        tile.updateTile()
    }

    

    
    override fun onClick() {
        super.onClick()

        if (RunStateStore.isRunningNow(this)) {
            
            setTileVisual(active = false)
            val stop = Intent(this, RunService::class.java).setAction(RunService.ACTION_STOP)
            try {
                startService(stop)
            } catch (_: Exception) {
                
            }
            return
        }

        
        
        
        
        
        if (!File(PythonInstaller.pythonRoot(this), "lib/python3.11/os.py").exists()) {
            setTileVisual(active = false)
            openApp()
            return
        }

        
        
        
        setTileVisual(active = true)

        
        
        
        
        
        val plan = rememberedPlan()
        val newRunId = RunStateStore.markStarting(this, plan)

        if (!startRunFromTile(newRunId)) {
            
            
            setTileVisual(active = false)
            RunStateStore.clear(this)
            openApp()
            return
        }

        
        
        
        
        
        verifyStartAfterDelay()
    }

    
    private fun verifyStartAfterDelay() {
        val appContext = applicationContext
        Thread {
            try {
                Thread.sleep(2000)
            } catch (_: InterruptedException) {
                return@Thread
            }

            val snapshot = RunStateStore.snapshot(appContext)

            
            
            
            if (snapshot.running) {
                return@Thread
            }

            
            
            
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                setTileVisual(active = false)
                openApp()
            }
        }.also { it.isDaemon = true }.start()
    }

    
    private fun startRunFromTile(runId: String): Boolean {
        val intent = Intent(this, RunService::class.java)
            .setAction(RunService.ACTION_START)
            .putExtra(RunService.EXTRA_MODE, rememberedPlan().name)
            .putExtra(RunService.EXTRA_RUN_ID, runId)
            .putExtra(RunService.EXTRA_CONNECT, true)
        return try {
            ContextCompat.startForegroundService(this, intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    
    private fun rememberedPlan(): ScriptRunner.Mode {
        val stored = getSharedPreferences(PREFS_UI, MODE_PRIVATE)
            .getString(KEY_SELECTED_PLAN, null)

        return runCatching { ScriptRunner.Mode.valueOf(stored ?: "") }
            .getOrDefault(ScriptRunner.Mode.FULL)
    }

    
    private fun openApp() {
        try {
            val intent = Intent(this, MainActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
            startActivity(intent)
        } catch (_: Exception) {
            
        }
    }

    
    private fun setTileVisual(active: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "连接附近WiFi"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (active) "脚本运行中" else "开始连接附近WiFi"
        }
        tile.updateTile()
    }
}
