package com.hyx.oneshot.wifitools

import android.content.Context
import java.io.File

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
object ScriptRunner {

    /** File the script writes recovered credentials to, resolved at runtime. */
    fun wifiPasswordFile(context: Context): File =
        File(PythonInstaller.scriptDir(context), "wifipassword.txt")

    fun scriptPath(context: Context): File =
        File(PythonInstaller.scriptDir(context), "ose.py")

    /**
     * How the script should be invoked.
     *
     * The app always runs the full workflow: scan for WPS networks, attack them
     * and save every recovered credential. The script's `-c` connect mode is a
     * manual CLI feature (`ose.py -c <SSID> <password>`) that the app does not
     * expose — the in-app button and the quick-settings tile both scan.
     */
    enum class Mode(val arg: String?) {
        /** Full workflow: scan for targets, attack and save credentials. */
        FULL(null)
    }

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
     */
    fun buildCommand(
        context: Context,
        useRoot: Boolean,
        mode: Mode = Mode.FULL
    ): List<String> {
        val appDir = context.filesDir.absolutePath
        val pythonRoot = "${appDir}/python"
        val libDir = "${appDir}/lib"
        val binDir = "${appDir}/bin"
        val workDir = "${appDir}/script"
        val script = "${workDir}/ose.py"

        // `-u` keeps stdout unbuffered so the log pane fills in real time.
        // The mode argument goes after the script path, which is where the
        // script looks for it ("'-c' in sys.argv[1:]").
        val modeArg = mode.arg?.let { " $it" }.orEmpty()

        // cd into the script's own directory so its relative wifipassword.txt
        // lands next to ose.py, matching os.path.dirname(__file__) in the script.
        val inner = "mkdir -p $appDir $appDir/tmp && cd $workDir && " +
            "$libDir/python3.11 -u $script$modeArg"

        return if (useRoot) {
            listOf("su", "-c", inner)
        } else {
            listOf("/system/bin/sh", "-c", inner)
        }
    }

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
    fun buildEnvironment(context: Context): Map<String, String> {
        val appDir = context.filesDir.absolutePath
        val pythonRoot = "${appDir}/python"
        val libDir = "${appDir}/lib"
        val binDir = "${appDir}/bin"
        val tmpDir = "${appDir}/tmp"

        // TMPDIR too: the script writes session files through
        // tempfile.gettempdir(), and /data/local/tmp is not reliably writable
        // for every app, so it gets a directory it definitely owns.
        return mapOf(
            // The one that fixes the crash.
            "HOME" to appDir,
            "TMPDIR" to tmpDir,

            // Explicit, unambiguous data root for the script's own
            // _resolve_user_home(). Belt and braces: HOME above already covers
            // it, but this one is read directly by name so it cannot be lost
            // even if something resets HOME on the way in.
            "OSE_HOME" to appDir,

            // PYTHONHOME must be the prefix root: CPython appends lib/python3.11
            // to it when locating the stdlib.
            "PYTHONHOME" to pythonRoot,

            // $libDir holds libpython3.11.so, so it must stay here or the
            // interpreter will not even start.
            "LD_LIBRARY_PATH" to "$libDir:$pythonRoot/lib",

            // PATH order matters twice over:
            //   * $binDir first    -> our pixiewps / wpa_supplicant / wpa_cli / iw win
            //   * /system/bin next -> toybox supplies `ip`, and `su` is found
            "PATH" to "$binDir:$libDir:/system/bin:/system/xbin:/sbin",

            "PYTHONUNBUFFERED" to "1",
            "PYTHONUTF8" to "1",
            "PYTHONDONTWRITEBYTECODE" to "1",
            // The script emits UTF-8 box drawing and non-ASCII SSIDs; without a
            // UTF-8 locale Python falls back to ASCII and dies on the first one.
            "LANG" to "C.UTF-8",
            "LC_ALL" to "C.UTF-8",

            // Some root managers wipe the environment entirely; these two give
            // the spawned shell a sane baseline so `mkdir`/`cd` still resolve.
            "SHELL" to "/system/bin/sh",
            "ANDROID_DATA" to "/data",
            "ANDROID_ROOT" to "/system"
        )
    }

    /**
     * Results of the root probe, so the caller can say something meaningful in
     * the log instead of silently switching execution mode.
     */
    enum class RootStatus { GRANTED, DENIED, NOT_ROOTED, ERROR }

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
    fun requestRoot(): RootStatus {
        val su = findSu() ?: return RootStatus.NOT_ROOTED

        return try {
            val process = ProcessBuilder(su, "-c", "id -u")
                .redirectErrorStream(true)
                .start()

            // Read the output before waiting: a large buffered response could
            // otherwise deadlock the child against a full pipe.
            val output = process.inputStream.bufferedReader().readText().trim()

            // 20s is generous enough for a human to tap "Allow" in the root
            // manager dialog, and short enough not to hang the UI flow.
            val finished = process.waitFor(20, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                RootStatus.DENIED
            } else if (process.exitValue() == 0 && output.trim() == "0") {
                RootStatus.GRANTED
            } else {
                RootStatus.DENIED
            }
        } catch (_: Exception) {
            RootStatus.ERROR
        }
    }

    /** Locates a usable `su` binary, preferring the ones on PATH. */
    private fun findSu(): String? {
        val candidates = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/su/bin/su", "/magisk/.core/bin/su", "/debug_ramdisk/su"
        )
        candidates.firstOrNull { File(it).exists() }?.let { return it }

        return try {
            val process = ProcessBuilder("/system/bin/sh", "-c", "command -v su")
                .redirectErrorStream(true)
                .start()
            val out = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            out.lineSequence().firstOrNull { it.isNotBlank() && File(it).exists() }
        } catch (_: Exception) {
            null
        }
    }

    /** True when a working `su` binary is present on the device. */
    fun hasRoot(): Boolean = findSu() != null
}
