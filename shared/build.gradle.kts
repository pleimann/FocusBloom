/*
 * Copyright 2024 Joel Kanyi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinX.serialization.plugin)
    alias(libs.plugins.sqlDelight.plugin)
    alias(libs.plugins.nativeCocoapod)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.joelkanyi.focusbloom.shared"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    // sourceSets["main"].res.srcDirs("src/androidMain/res")
    // sourceSets["main"].resources.srcDirs("src/commonMain/resources")
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

kotlin {
    jvmToolchain(17)

    androidTarget()

    jvm()

    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        version = "1.0"
        summary = "Some description for a Kotlin/Native module"
        homepage = "Link to a Kotlin/Native module homepage"
        ios.deploymentTarget = "14.1"
        podfile = project.file("../ios/Podfile")
        framework {
            baseName = "shared"
            isStatic = false
        }
    }

    targets.withType<KotlinNativeTarget> {
        binaries.withType<Framework> {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            transitiveExport = true
            compilations.all {
                compileTaskProvider.configure {
                    compilerOptions.freeCompilerArgs.add("-linker-options")
                    compilerOptions.freeCompilerArgs.add("-lsqlite3")
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.koin.core)
            implementation(libs.koin.compose)

            implementation(compose.runtime)
            implementation(compose.material3)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)

            implementation(compose.components.resources)

            implementation(libs.kotlinX.serializationJson)

            implementation(libs.material3.window.size.multiplatform)

            implementation(libs.sqlDelight.runtime)
            implementation(libs.coroutines.extensions)
            implementation(libs.primitive.adapters)

            api(libs.multiplatformSettings.noArg)
            api(libs.multiplatformSettings.coroutines)

            api(libs.napier)

            implementation(libs.kotlinX.dateTime)
            implementation(libs.koalaplot.core)

            implementation(libs.stdlib)

            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.navigation.compose)

            // KMPAuth for Google OAuth
            implementation(libs.kmpauth.google)
            implementation(libs.kmpauth.uihelper)

            // Ktor for HTTP client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
        }

        androidMain.dependencies {
            implementation(libs.android.driver)

            implementation(libs.accompanist.systemUIController)
            implementation(libs.core)
            implementation(libs.compose.activity)

            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            // Ktor Android Engine
            implementation(libs.ktor.client.android)
        }

        jvmMain.dependencies {
            implementation(libs.sqlite.driver)

            implementation(libs.kotlinx.coroutines.swing)

            // Toaster for Windows
            implementation(libs.toast4j)

            // JNA for Linux
            implementation("de.jangassen:jfa:1.2.0") {
                // not excluding this leads to a strange error during build:
                // > Could not find jna-5.13.0-jpms.jar (net.java.dev.jna:jna:5.13.0)
                exclude(group = "net.java.dev.jna", module = "jna")
            }

            // JNA for Windows
            implementation(libs.jna)

            // Ktor Java Engine
            implementation(libs.ktor.client.java)
        }

        iosMain.dependencies {
            implementation(libs.native.driver)

            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)

            // Ktor Darwin Engine
            implementation(libs.ktor.client.darwin)
        }
    }
}

sqldelight {
    databases {
        create("BloomDatabase") {
            packageName.set("com.joelkanyi.focusbloom.database")
        }
    }
}

// Workaround for KLIB conflict between SQLDelight and Compose runtime
configurations.all {
    resolutionStrategy {
        force("app.cash.sqldelight:runtime:2.0.2")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}