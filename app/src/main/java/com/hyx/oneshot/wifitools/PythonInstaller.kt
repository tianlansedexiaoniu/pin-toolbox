package com.hyx.oneshot.wifitools

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

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
class PythonInstaller(private val context: Context) {

    companion object {
        /** Bumped whenever the asset payload changes shape. */
        private const val BUILD_STAMP = "ose-python-build-2"

        /** Directory the bundled script lives in; also its CWD at runtime. */
        fun scriptDir(context: Context): File = File(context.filesDir, "script")

        /**
         * CPython prefix root. The interpreter resolves its stdlib relative to
         * this, expecting `<prefix>/lib/python3.11/`, so it must be a directory
         * that *contains* lib/ rather than the stdlib directory itself.
         */
        fun pythonRoot(context: Context): File = File(context.filesDir, "python")

        /** Where the interpreter binary + its .so files are copied. */
        fun nativeDir(context: Context): File = File(context.filesDir, "lib")

        /** Where pixiewps / wpa_supplicant / wpa_cli / iw are copied. */
        fun binDir(context: Context): File = File(context.filesDir, "bin")

        fun stampFile(context: Context): File = File(context.filesDir, ".build_stamp")

        /**
         * The primary ABI this device should use, restricted to the two we ship.
         * Falls back to arm64-v8a (our baseline) if the platform reports
         * something unexpected.
         */
        fun deviceAbi(): String {
            val supported = Build.SUPPORTED_ABIS
            for (abi in supported) {
                if (abi == "arm64-v8a" || abi == "armeabi-v7a") return abi
            }
            return "arm64-v8a"
        }
    }

    /**
     * Returns true when the runtime was (re)deployed, false when the existing
     * copy was already up to date.
     */
    fun ensureDeployed(onProgress: (String) -> Unit): Boolean {
        val stamp = stampFile(context)
        val pythonRoot = pythonRoot(context)
        val scriptDir = scriptDir(context)

        val marker = File(pythonRoot, "lib/python3.11/os.py")
        if (stamp.exists() && marker.exists() && scriptDir.exists()) {
            val recorded = runCatching { stamp.readText().trim() }.getOrDefault("")
            if (recorded == BUILD_STAMP) {
                onProgress("[*] Python runtime already deployed.")
                return false
            }
        }

        val abi = deviceAbi()
        onProgress("[*] First launch: deploying embedded Python runtime for $abi…")

        // Start from a clean slate so a partially-extracted older build cannot
        // poison the new one.
        pythonRoot.deleteRecursively()
        scriptDir.deleteRecursively()
        binDir(context).deleteRecursively()
        pythonRoot.mkdirs()
        scriptDir.mkdirs()
        nativeDir(context).mkdirs()
        binDir(context).mkdirs()

        // assets/python holds the *contents* of lib/python3.11, so it is deployed
        // one level deeper to satisfy CPython's <prefix>/lib/python3.11 layout.
        val stdlibDest = File(pythonRoot, "lib/python3.11")
        stdlibDest.mkdirs()
        copyAssetTree("python", stdlibDest, onProgress)
        copyAssetTree("script", scriptDir, onProgress)

        deployNativeLibs(abi, stdlibDest, onProgress)
        deployTools(abi, onProgress)

        stamp.writeText(BUILD_STAMP)
        onProgress("[+] Runtime and tools ready ($abi).")

        return true
    }

    /** Recursively copies one asset subtree into a destination directory. */
    private fun copyAssetTree(assetRoot: String, destRoot: File, onProgress: (String) -> Unit) {
        val children = context.assets.list(assetRoot) ?: emptyArray()

        if (children.isEmpty()) {
            // A leaf: it is a file, not a directory.
            copyAssetFile(assetRoot, File(destRoot.parentFile, destRoot.name))
            return
        }

        destRoot.mkdirs()
        for (child in children) {
            val assetPath = "$assetRoot/$child"
            val childDest = File(destRoot, child)
            val grandChildren = context.assets.list(assetPath) ?: emptyArray()

            if (grandChildren.isEmpty()) {
                copyAssetFile(assetPath, childDest)
            } else {
                copyAssetTree(assetPath, childDest, onProgress)
            }
        }
    }

    private fun copyAssetFile(assetPath: String, dest: File) {
        dest.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output, 64 * 1024)
            }
        }
    }

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
    private fun deployNativeLibs(abi: String, stdlibDest: File, onProgress: (String) -> Unit) {
        val libDest = nativeDir(context)
        libDest.mkdirs()
        val dynloadDest = File(stdlibDest, "lib-dynload")
        dynloadDest.mkdirs()

        val assetPath = "py/$abi/python-nativelibs.zip"
        try {
            context.assets.open(assetPath).use { raw ->
                ZipInputStream(raw).use { zip ->
                    var entry = zip.nextEntry
                    var fileCount = 0
                    while (entry != null) {
                        val name = File(entry.name).name
                        if (name.isNotEmpty() && !entry.isDirectory) {
                            val isDynload = entry.name.contains("lib-dynload/")
                            val out = if (isDynload) File(dynloadDest, name) else File(libDest, name)
                            FileOutputStream(out).use { fos -> zip.copyTo(fos, 64 * 1024) }
                            out.setReadable(true, false)
                            // The interpreter itself must be executable; the .so
                            // files only need to be readable.
                            if (name == "python3.11") {
                                out.setExecutable(true, false)
                            }
                            fileCount++
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                    onProgress("[*] Unpacked $fileCount runtime files ($abi).")
                }
            }
        } catch (err: Exception) {
            onProgress("[!] Failed to unpack Python runtime: ${err.message}")
        }
    }

    /**
     * Extracts the command-line tools for [abi] into filesDir/bin and marks them
     * executable.
     *
     * These are what the script's `which('pixiewps' | 'wpa_supplicant' | 'iw')`
     * requirement check looks for, so the directory is prepended to PATH by
     * ScriptRunner.
     */
    private fun deployTools(abi: String, onProgress: (String) -> Unit) {
        val dest = binDir(context)
        dest.mkdirs()

        val names = listOf("pixiewps", "wpa_supplicant", "wpa_cli", "iw")
        var deployed = 0

        for (name in names) {
            try {
                context.assets.open("bin/$abi/$name").use { input ->
                    val out = File(dest, name)
                    FileOutputStream(out).use { output -> input.copyTo(output, 64 * 1024) }
                    out.setReadable(true, false)
                    out.setExecutable(true, false)
                    deployed++
                }
            } catch (err: Exception) {
                onProgress("[!] Tool $name ($abi) unavailable: ${err.message}")
            }
        }

        onProgress("[*] Deployed $deployed tool binaries ($abi).")
    }
}
