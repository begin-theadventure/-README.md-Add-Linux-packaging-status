val mockk_version: String by extra
val moko_resources_version: String by extra
val ktor_version: String by extra
val json_version: String by extra
val atomicfu_version: String by extra
val ktoml_version: String by extra

plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.android.library)
	id("kotlin-parcelize")
	//id("parcelize-darwin")
	id("org.jetbrains.kotlinx.kover")
	id("dev.icerock.mobile.multiplatform-resources")
}

group = "com.darkrockstudios.apps.hammer"
version = libs.versions.app.get()

kotlin {
	android()
	jvm("desktop") {
		compilations.all {
			kotlinOptions.jvmTarget = libs.versions.jvm.get()
		}
	}
	ios {
		binaries {
			framework {
				baseName = "Hammer"
				//transitiveExport = true
				export(libs.decompose)
				// This isn't working for some reason, once it is remove transitiveExport
				export(libs.essenty)
				export(libs.coroutines.core)
				export("dev.icerock.moko:resources:$moko_resources_version")
				export(libs.napier)
			}
		}
	}

	sourceSets {
		val commonMain by getting {
			resources.srcDirs("resources")

			dependencies {
				api(project(":base"))

				api(libs.decompose)
				api(libs.napier)
				api(libs.coroutines.core)
				api(libs.koin.core)
				api(libs.okio)

				api("io.ktor:ktor-client-core:$ktor_version")
				implementation("io.ktor:ktor-client-auth:$ktor_version")
				implementation("io.ktor:ktor-client-logging:$ktor_version")
				implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
				implementation("io.ktor:ktor-client-encoding:$ktor_version")
				implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")

				api(libs.serialization.core)
				api(libs.serialization.json)
				api(libs.datetime)
				implementation("com.akuleshov7:ktoml-core:$ktoml_version")
				api(libs.essenty)
				implementation("io.github.reactivecircus.cache4k:cache4k:0.9.0")
				api("dev.icerock.moko:resources:$moko_resources_version")
				implementation("org.jetbrains.kotlinx:atomicfu:$atomicfu_version")
			}
		}
		val commonTest by getting {
			dependencies {
				implementation(kotlin("test"))
				//implementation("io.insert-koin:koin-test:$koin_version")
				implementation(libs.okio.fakefilesystem)
				implementation(libs.kotlin.reflect)
				implementation("dev.icerock.moko:resources-test:$moko_resources_version")
			}
		}
		val androidMain by getting {
			dependencies {
				//api("androidx.appcompat:appcompat:1.5.1")
				api("androidx.core:core-ktx:1.10.0")
				api(libs.coroutines.android)
				implementation(libs.koin.android)
				implementation("io.ktor:ktor-client-okhttp:$ktor_version")
			}
		}
		val iosMain by getting {
			dependencies {
				api(libs.decompose)
				api(libs.essenty)
				api("dev.icerock.moko:resources:$moko_resources_version")
				api("io.ktor:ktor-client-darwin:$ktor_version")
			}
		}
		val iosTest by getting
		val androidUnitTest by getting {
			dependencies {
			}
		}
		val desktopMain by getting {
			dependencies {
				implementation("org.slf4j:slf4j-simple:2.0.6")
				api(libs.serialization.jvm)
				api(libs.coroutines.swing)
				implementation("net.harawata:appdirs:1.2.1")
				api("dev.icerock.moko:resources-compose:$moko_resources_version")
				//implementation("io.ktor:ktor-client-curl:$ktor_version")
				implementation("io.ktor:ktor-client-java:$ktor_version")
			}
		}
		val desktopTest by getting {
			dependencies {
				implementation(libs.coroutines.test)
				implementation("io.mockk:mockk:$mockk_version")
				implementation(libs.koin.test)
			}
		}
	}
}

multiplatformResources {
	multiplatformResourcesPackage = "com.darkrockstudios.apps.hammer"
}

android {
	namespace = "com.darkrockstudios.apps.hammer.common"
	compileSdk = libs.versions.android.sdk.compile.get().toInt()
	sourceSets {
		named("main") {
			manifest.srcFile("src/androidMain/AndroidManifest.xml")
			res.srcDirs(
				"resources",
				"src/androidMain/res",
				"src/commonMain/resources",
				// https://github.com/icerockdev/moko-resources/issues/353#issuecomment-1179713713
				File(buildDir, "generated/moko/androidMain/res")
			)
		}
	}
	defaultConfig {
		minSdk = libs.versions.android.sdk.min.get().toInt()
		targetSdk = libs.versions.android.sdk.target.get().toInt()
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
}

kover {
	filters {
		classes {
			includes += "com.darkrockstudios.apps.hammer.*"
			excludes += listOf(
				"com.darkrockstudios.apps.hammer.util.*",
				"com.darkrockstudios.apps.hammer.parcelize.*",
				"com.darkrockstudios.apps.hammer.fileio.*",
			)
		}
	}
}