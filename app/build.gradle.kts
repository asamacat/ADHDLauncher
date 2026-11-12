import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.example.adhdlauncher"
  compileSdk = 36
  defaultConfig {
    applicationId = "com.example.adhdlauncher"
    minSdk = 26
    targetSdk = 36
    versionCode = 4
    versionName = "0.3.4"
  }

  val keystorePropsFile = rootProject.file("keystore.properties").takeIf { it.exists() }
    ?: project.file("keystore.properties").takeIf { it.exists() }

  val keystoreProps = Properties().apply {
    keystorePropsFile?.inputStream()?.use { load(it) }
  }

  val releaseKeystoreFile = when {
    keystoreProps.getProperty("storeFile") != null -> {
      val path = keystoreProps.getProperty("storeFile")
      val f = file(path)
      if (f.exists()) f else rootProject.file(path)
    }
    project.file("release.jks").exists() -> project.file("release.jks")
    rootProject.file("release.jks").exists() -> rootProject.file("release.jks")
    else -> null
  }

  signingConfigs {
    create("release") {
      if (releaseKeystoreFile != null && releaseKeystoreFile.exists()) {
        storeFile = releaseKeystoreFile
        storePassword = keystoreProps.getProperty("storePassword")
          ?: System.getenv("KEYSTORE_PASSWORD")
          ?: "android"
        keyAlias = keystoreProps.getProperty("keyAlias")
          ?: System.getenv("KEY_ALIAS")
          ?: "androiddebugkey"
        keyPassword = keystoreProps.getProperty("keyPassword")
          ?: System.getenv("KEY_PASSWORD")
          ?: "android"
      }
    }
  }

  buildTypes {
    val releaseConfig = signingConfigs.getByName("release")
    val debugConfig = signingConfigs.getByName("debug")
    val hasValidReleaseKey = releaseKeystoreFile != null && releaseKeystoreFile.exists()
    val activeSigningConfig = if (hasValidReleaseKey) releaseConfig else debugConfig

    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = activeSigningConfig
    }
    debug {
      signingConfig = activeSigningConfig
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = true
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation("org.json:json:20240303")

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
