import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.hyx.oneshot.wifitools"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hyx.oneshot.wifitools"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        ndk {
            // 同时支持 64 位与 32 位 ARM 设备。
            // 注意：不存在 "arm64v7" 这一 ABI —— ARM 只有互斥的两套：
            //   arm64-v8a   = 64 位 (AArch64, armv8)
            //   armeabi-v7a = 32 位 (armv7)
            // Python 解释器与全部 C 工具（pixiewps / wpa_supplicant / iw）
            // 均已按这两个 ABI 分别交叉编译。
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../release.keystore")
            storePassword = "oneshot123"
            keyAlias = "oneshot"
            keyPassword = "oneshot123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        // extractNativeLibs=true in the manifest requires legacy packaging;
        // the Python interpreter must exist as a real file to be exec'd.
        jniLibs {
            useLegacyPackaging = true
        }
    }

    // Do NOT compress the payload; python needs to mmap/exec from disk
    androidResources {
        noCompress += listOf("zip", "so", "py", "pyc", "txt", "dat")
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core:1.13.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.documentfile:documentfile:1.0.1")
}
