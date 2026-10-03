package com.hyx.oneshot.wifitools

import android.content.Context
import java.io.File

object ScriptRunner {

    
    const val DEFAULT_IFACE = "wlan0"

    
    fun wifiPasswordFile(context: Context): File =
        File(PythonInstaller.scriptDir(context), "wifipassword.txt")

    fun scriptPath(context: Context): File =
        File(PythonInstaller.scriptDir(context), "ose.py")

    
    enum class Mode(val arg: String?) {
        
        FULL(null),

        
        MODE2("-m")
    }

    
    fun buildCommand(
        context: Context,
        useRoot: Boolean,
        mode: Mode = Mode.FULL,
        connectOnly: Boolean = false,
        longTimeout: Boolean = false
    ): List<String> {
        val appDir = context.filesDir.absolutePath
        val pythonRoot = "${appDir}/python"
        val libDir = "${appDir}/lib"
        val binDir = "${appDir}/bin"
        val workDir = "${appDir}/script"
        val script = "${workDir}/ose.py"

        
        
        
        
        
        
        
        val modeArg = buildString {
            if (mode.arg != null) append(" ").append(mode.arg)
            if (connectOnly) append(" -c")
            if (longTimeout) append(" --long-timeout")
        }

        
        
        val inner = "mkdir -p $appDir $appDir/tmp && cd $workDir && " +
            "$libDir/python3.11 -u $script$modeArg"

        return if (useRoot) {
            listOf("su", "-c", inner)
        } else {
            listOf("/system/bin/sh", "-c", inner)
        }
    }

    
    fun buildEnvironment(context: Context): Map<String, String> {
        val appDir = context.filesDir.absolutePath
        val pythonRoot = "${appDir}/python"
        val libDir = "${appDir}/lib"
        val binDir = "${appDir}/bin"
        val tmpDir = "${appDir}/tmp"

        
        
        
        return mapOf(
            
            "HOME" to appDir,
            "TMPDIR" to tmpDir,

            
            
            
            
            "OSE_HOME" to appDir,

            
            
            
            
            
            
            "OSE_IFACE" to DEFAULT_IFACE,

            
            
            "PYTHONHOME" to pythonRoot,

            
            
            "LD_LIBRARY_PATH" to "$libDir:$pythonRoot/lib",

            
            
            
            "PATH" to "$binDir:$libDir:/system/bin:/system/xbin:/sbin",

            "PYTHONUNBUFFERED" to "1",
            "PYTHONUTF8" to "1",
            "PYTHONDONTWRITEBYTECODE" to "1",
            
            
            "LANG" to "C.UTF-8",
            "LC_ALL" to "C.UTF-8",

            
            
            "SHELL" to "/system/bin/sh",
            "ANDROID_DATA" to "/data",
            "ANDROID_ROOT" to "/system"
        )
    }

    
    enum class RootStatus { GRANTED, DENIED, NOT_ROOTED, ERROR }

    
    fun requestRoot(): RootStatus {
        val su = findSu() ?: return RootStatus.NOT_ROOTED

        return try {
            val process = ProcessBuilder(su, "-c", "id -u")
                .redirectErrorStream(true)
                .start()

            
            
            val output = process.inputStream.bufferedReader().readText().trim()

            
            
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

    
    internal fun findSuPath(): String? = findSu()

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

    
    fun hasRoot(): Boolean = findSu() != null
}
