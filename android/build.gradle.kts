val RELEASE_STORE_FILE = System.getenv("RELEASE_STORE_FILE") ?: "/"
val RELEASE_STORE_PASSWORD = System.getenv("RELEASE_STORE_PASSWORD") ?: ""
val RELEASE_KEY_ALIAS = System.getenv("RELEASE_KEY_ALIAS") ?: ""
val RELEASE_KEY_PASSWORD = System.getenv("RELEASE_KEY_PASSWORD") ?: ""

plugins {
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.android.application)
	alias(libs.plugins.jetbrains.compose)
	alias(libs.plugins.jetbrains.kover)
}

group = "com.darkrockstudios.apps.hammer"
version = libs.versions.app.get()

repositories {
	mavenCentral()
}

dependencies {
	api(project(":composeUi"))
	implementation(libs.activity.compose)
	implementation(libs.koin.android)
	implementation(libs.glance)
	implementation(libs.glance.appwidget)
	implementation(libs.glance.material3)

	implementation(libs.work.runtime.ktx)
	implementation(libs.material)
	implementation(libs.appcompat)

	androidTestImplementation(libs.junit)
	androidTestImplementation(libs.junit.ktx)
	androidTestImplementation(libs.core)
	androidTestImplementation(libs.core.ktx)
	androidTestImplementation(libs.androidx.runner)
	androidTestUtil(libs.orchestrator)
}

android {
	namespace = "com.darkrockstudios.apps.hammer.android"
	compileSdk = libs.versions.android.sdk.compile.get().toInt()
	defaultConfig {
		applicationId = "com.darkrockstudios.apps.hammer.android"
		minSdk = libs.versions.android.sdk.min.get().toInt()
		targetSdk = libs.versions.android.sdk.target.get().toInt()
		versionCode = libs.versions.android.version.code.get().toInt()
		versionName = libs.versions.app.get()

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}
	buildFeatures {
		compose = true
	}
	composeOptions {
		kotlinCompilerExtensionVersion = libs.versions.jetpack.compose.compiler.get()
	}
	compileOptions {
		sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
		targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
	}
	signingConfigs {
		create("release") {
			keyAlias = RELEASE_KEY_ALIAS
			keyPassword = RELEASE_KEY_PASSWORD
			storeFile = file(RELEASE_STORE_FILE)
			storePassword = RELEASE_STORE_PASSWORD
		}
	}

	buildTypes {
		debug {
			applicationIdSuffix = ".dev"
			versionNameSuffix = "-dev"
		}

		release {
			isMinifyEnabled = false
			isShrinkResources = false

			signingConfig = signingConfigs.getByName("release")

			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				File("proguard-rules.pro")
			)
		}
	}
}