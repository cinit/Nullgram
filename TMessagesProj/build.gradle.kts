@file:Suppress("UnstableApiUsage")

import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.FilterConfiguration
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.google.services)
    alias(libs.plugins.triplet.play)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.rust)
}

configurations {
    all {
        exclude(group = "com.google.firebase", module = "firebase-core")
        exclude(group = "androidx.recyclerview", module = "recyclerview")
    }
}

var serviceAccountCredentialsFile = File(rootProject.projectDir, "service_account_credentials.json")
val abiName = mapOf("armeabi-v7a" to "arm32", "arm64-v8a" to "arm64", "x86" to "x86", "x86_64" to "x86_64")

if (serviceAccountCredentialsFile.isFile) {
    setupPlay(Version.isStable)
    play.serviceAccountCredentials.set(serviceAccountCredentialsFile)
} else if (System.getenv().containsKey("ANDROID_PUBLISHER_CREDENTIALS")) {
    setupPlay(Version.isStable)
}

fun setupPlay(stable: Boolean) {
    val targetTrace = if (stable) "production" else "beta"
    play {
        track.set(targetTrace)
        defaultToAppBundles.set(true)
    }
}

cargo {
    module = "../libs/rust"
    libname = "rust"
    targets = listOf("arm64", "x86_64")

    prebuiltToolchains = true
    profile = "release"
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics.ndk)

    implementation(libs.core.ktx)
    implementation(libs.palette.ktx)
    implementation(libs.exifinterface)
    implementation(libs.dynamicanimation)
    implementation(libs.interpolator)
    implementation(libs.fragment)
    implementation(libs.sharetarget)
    implementation(libs.biometric)

    compileOnly(libs.checker.compat.qual)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.config)
    implementation(libs.play.services.vision)
    implementation(libs.play.services.location)
    implementation(libs.play.services.wallet)
    implementation(libs.play.services.mlkit.vision)
    implementation(libs.play.services.mlkit.imageLabeling)
    implementation(libs.isoparser)
    implementation(files("libs/stripe.aar"))
    implementation(libs.language.id)
    implementation(files("libs/libgsaverification-client.aar"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.gson)
    implementation(libs.process.phoenix)
    implementation(libs.hiddenapibypass)

    implementation(libs.kotlin.stdlib.common)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.osmdroid.android)
    implementation(libs.guava)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.json)

    implementation(project(":libs:tcp2ws"))
    implementation(project(":libs:pangu"))
    ksp(project(":libs:ksp"))
}

android {
    defaultConfig.applicationId = "top.qwq2333.nullgram"
    namespace = "org.telegram.messenger"

    sourceSets.getByName("main") {
        java.srcDir("src/main/java")
        jniLibs.srcDirs("./jni/")
    }

    externalNativeBuild {
        cmake {
            path = File(projectDir, "jni/CMakeLists.txt")
        }
    }

    lint {
        checkReleaseBuilds = true
        disable += listOf(
            "MissingTranslation", "ExtraTranslation", "BlockedPrivateApi"
        )
    }

    packaging {
        resources.excludes += "**"
    }

    kotlin {
        jvmToolchain(Version.java.toString().toInt())
    }

    var keystorePwd: String? = null
    var alias: String? = null
    var pwd: String? = null
    if (project.rootProject.file("local.properties").exists()) {
        keystorePwd = getLocalProperty(rootDir, "RELEASE_STORE_PASSWORD")
        alias = getLocalProperty(rootDir, "RELEASE_KEY_ALIAS")
        pwd = getLocalProperty(rootDir, "RELEASE_KEY_PASSWORD")
    }

    signingConfigs {
        val keystoreFile = File(projectDir, "config/release.keystore")
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = keystoreFile
                storePassword = (keystorePwd ?: System.getenv("KEYSTORE_PASS"))
                keyAlias = (alias ?: System.getenv("ALIAS_NAME"))
                keyPassword = (pwd ?: System.getenv("ALIAS_PASS"))
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(File(projectDir, "proguard-rules.pro"))

            the<CrashlyticsExtension>().nativeSymbolUploadEnabled = true
        }

        getByName("debug") {
            // If we have release signing config, use it for debug as well
            if (signingConfigs.findByName("release")?.keyAlias != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isDefault = true
            isDebuggable = true
            isJniDebuggable = false
        }

        create("play") {
            initWith(getByName("release"))
        }
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        externalNativeBuild {
            cmake {
                version = "3.22.1"
                arguments += listOf(
                    "-DANDROID_STL=c++_static",
                    "-DANDROID_PLATFORM=android-27",
                    "-DCMAKE_C_COMPILER_LAUNCHER=ccache",
                    "-DCMAKE_CXX_COMPILER_LAUNCHER=ccache",
                    "-DNDK_CCACHE=ccache",
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
                )
            }
        }
        buildConfigField("String", "BUILD_TIME", "\"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}\"")
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
        }
    }

    androidComponents {
        onVariants { variant ->
            variant.buildConfigFields.put("isPlay", BuildConfigField("boolean", variant.name.lowercase() == "play", null))
        }
    }

    applicationVariants.all {
        outputs.all {
            val abi = this.filters.find { it.filterType == FilterConfiguration.FilterType.ABI.name }?.identifier
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val outputFileName = "Nullgram-${defaultConfig.versionName}-${abiName[abi]}.apk"
            output?.outputFileName = outputFileName
        }
    }


}

kotlin {
    sourceSets.configureEach {
        kotlin.srcDir("${layout.buildDirectory.asFile.get().absolutePath}/generated/ksp/$name/kotlin/")
    }
}

private fun getLocalProperty(dir: File, propertyName: String): String? {
    val localProp = File(dir, "local.properties")
    if (!localProp.exists()) {
        return null
    }
    val localProperties = Properties()
    localProp.inputStream().use {
        localProperties.load(it)
    }
    return localProperties.getProperty(propertyName, null)
}

tasks.withType<JavaCompile> {
    // lager heap for javac
    options.forkOptions.memoryMaximumSize = "4g"
}
