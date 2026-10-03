package com.hyx.oneshot.wifitools

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class PythonInstaller(private val context: Context) {

    companion object {
        
        private const val BUILD_STAMP = "ose-python-build-3"

        
        fun scriptDir(context: Context): File = File(context.filesDir, "script")

        
        fun pythonRoot(context: Context): File = File(context.filesDir, "python")

        
        fun nativeDir(context: Context): File = File(context.filesDir, "lib")

        
        fun binDir(context: Context): File = File(context.filesDir, "bin")

        fun stampFile(context: Context): File = File(context.filesDir, ".build_stamp")

        
        fun deviceAbi(): String {
            val supported = Build.SUPPORTED_ABIS
            for (abi in supported) {
                if (abi == "arm64-v8a" || abi == "armeabi-v7a") return abi
            }
            return "arm64-v8a"
        }
    }

    
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

        
        
        pythonRoot.deleteRecursively()
        scriptDir.deleteRecursively()
        binDir(context).deleteRecursively()
        pythonRoot.mkdirs()
        scriptDir.mkdirs()
        nativeDir(context).mkdirs()
        binDir(context).mkdirs()

        
        
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

    
    private fun copyAssetTree(assetRoot: String, destRoot: File, onProgress: (String) -> Unit) {
        val children = context.assets.list(assetRoot) ?: emptyArray()

        if (children.isEmpty()) {
            
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
