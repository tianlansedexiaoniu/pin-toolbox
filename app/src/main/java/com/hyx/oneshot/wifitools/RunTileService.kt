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

/**
 * Quick-settings tile that starts a fresh scan of the surrounding Wi-Fi.
 *
 * Tapping the tile runs the full workflow — scan for WPS networks, attack them
 * and save every recovered credential — exactly like the in-app 开始 button.
 * It does *not* connect to anything and it does not read a previously saved
 * password file: every press starts a brand-new scan.
 *
 * The tap starts the run silently in the background; there is no confirmation
 * dialog. The tile itself doubles as the status indicator.
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
 */
@RequiresApi(Build.VERSION_CODES.N)
class RunTileService : TileService() {

    companion object {
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
        fun refresh(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
            try {
                requestListeningState(
                    context,
                    ComponentName(context, RunTileService::class.java)
                )
            } catch (_: Exception) {
                // The tile simply will not refresh; never crash the caller.
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        // Re-reads RunService, which reconstructs persisted state if this
        // process was just started by SystemUI. See the class comment.
        updateTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    /** Reflect the live run state into the tile's visual state. */
    private fun updateTile() {
        val tile = qsTile ?: return
        val snapshot = RunStateStore.snapshot(this)
        val running = snapshot.running

        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "扫描周围WiFi"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (running) "脚本运行中" else "开始扫描周围WiFi"
        }
        tile.updateTile()
    }

    /**
     * Toggles the run, flipping the tile's own appearance first.
     *
     * The order matters for how the interaction feels. The visual is repainted
     * immediately at the top, so the shade animates to the new state on the same
     * frame as the tap; the actual service call happens after. Waiting for the
     * service round-trip before repainting would leave a visible lag between the
     * tap and the tile changing.
     */
    override fun onClick() {
        super.onClick()

        if (RunStateStore.isRunningNow(this)) {
            // Paint "off" first, then actually stop.
            setTileVisual(active = false)
            val stop = Intent(this, RunService::class.java).setAction(RunService.ACTION_STOP)
            try {
                startService(stop)
            } catch (_: Exception) {
                // Nothing to show here; the next onStartListening corrects it.
            }
            return
        }

        // No runtime yet -> nothing sensible to connect to. Open the app so the
        // user sees why instead of getting a silent no-op.
        if (!File(PythonInstaller.pythonRoot(this), "lib/python3.11/os.py").exists()) {
            openApp()
            return
        }

        // Paint "on" first, then start. If the start fails, the next
        // onStartListening (or the watchdog) pulls it back to off — a brief
        // optimistic state is worth the responsiveness.
        setTileVisual(active = true)

        // The run's identity is minted *here*, before the service is asked to
        // start, and carried through to it. That is what lets the activity
        // recognise "the run I am about to show" and refresh its terminal for
        // it — the activity and the tile can live in different processes, so an
        // in-memory handshake is not available.
        val newRunId = RunStateStore.markStarting(this, ScriptRunner.Mode.FULL)

        if (!startRunFromTile(newRunId)) {
            // The direct route was refused. Roll the optimistic paint back so
            // the tile does not lie about a run that never began.
            setTileVisual(active = false)
            RunStateStore.clear(this)
            openApp()
        }
    }

    /**
     * Starts a fresh scan straight from the tile.
     *
     * A TileService is bound by SystemUI, so the background-start restriction
     * does not apply and this is a legal, direct foreground-service start — no
     * relay activity involved. See the class comment for why that matters.
     *
     * @param runId the identity minted by [RunStateStore.markStarting], passed
     *        through so the service records the same run the tile announced.
     * @return true when the service accepted the start.
     */
    private fun startRunFromTile(runId: String): Boolean {
        val intent = Intent(this, RunService::class.java)
            .setAction(RunService.ACTION_START)
            .putExtra(RunService.EXTRA_MODE, ScriptRunner.Mode.FULL.name)
            .putExtra(RunService.EXTRA_RUN_ID, runId)
        return try {
            ContextCompat.startForegroundService(this, intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Brings the app to the foreground so the user can see what is wrong. */
    private fun openApp() {
        try {
            val intent = Intent(this, MainActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
            startActivity(intent)
        } catch (_: Exception) {
            // Even this can be refused; nothing further we can do from here.
        }
    }

    /**
     * Repaints the tile without consulting RunService.
     *
     * Used by [onClick] for the optimistic flip, where the authoritative state
     * has intentionally not changed yet.
     */
    private fun setTileVisual(active: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "扫描周围WiFi"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (active) "脚本运行中" else "开始扫描周围WiFi"
        }
        tile.updateTile()
    }
}
