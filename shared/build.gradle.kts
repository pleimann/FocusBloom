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
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinX.serialization.plugin)
    id("app.cash.sqldelight") version "2.0.2"
    alias(libs.plugins.nativeCocoapod)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.joelkanyi.focusbloom.shared"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    // sourceSets["main"].res.srcDirs("src/androidMain/res")
    // sourceSets["main"].resources.srcDirs("src/commonMain/resources")
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

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

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Framework> {
            transitiveExport = true
            compilerOptions {
                freeCompilerArgs.add("-linker-options")
                freeCompilerArgs.add("-lsqlite3")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.koin.core)
            api(libs.koin.compose)

            implementation(compose.material3)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)

            implementation(compose.components.resources)

            implementation(libs.voyager.navigator)
            implementation(libs.voyager.bottomSheetNavigator)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.tabNavigator)
            implementation(libs.voyager.koin)

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
        }

        androidMain.dependencies {
            implementation(libs.android.driver)

            implementation(libs.accompanist.systemUIController)
            implementation(libs.core)
            implementation(libs.compose.activity)

            implementation(libs.stately.common)
        }

        jvmMain.dependencies {
            implementation(libs.sqlite.driver)

            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.stately.common)

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
        }

        iosMain.dependencies {
            implementation(libs.native.driver)

            implementation(libs.stately.isolate)
            implementation(libs.stately.isolate.collections)

            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)        }
    }
}

sqldelight {
    databases {
        create("BloomDatabase") {
            packageName.set("com.joelkanyi.focusbloom.database")
        }
    }
}