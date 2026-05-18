/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@OptIn(EnvironmentalVariablesOverride::class)
@DisplayName("integrateEmbedAndSign task tests")
@SwiftPMImportGradlePluginTests
class IntegrateEmbedAndSignIntoXcodeProjectIT : KGPBaseTest() {

    @GradleTest
    fun `integrateEmbedAndSign uses root project task path without duplicate separators`(version: GradleVersion) {
        Assumptions.assumeTrue(version >= GradleVersion.version("8.14.4"))

        project("emptyxcode-no-embedandsign", version) {
            initDefaultKmpWithLocalSPM()

            val pbxFile = projectPath.resolve("iosApp/iosApp.xcodeproj/project.pbxproj")

            build(
                "integrateEmbedAndSign",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                    "GRADLEW_PATH" to projectPath.resolve("gradlew").absolutePathString(),
                    "GRADLE_PROJECT_PATH" to ":",
                )
            ) {
                val pbxFileContent = pbxFile.readText()

                assertContains(
                    pbxFileContent,
                    "./gradlew :embedAndSignAppleFrameworkForXcode -i",
                    message = "Generated embed-and-sign phase should target the root project task path",
                )
                assertFalse(
                    pbxFileContent.contains("./gradlew ::embedAndSignAppleFrameworkForXcode -i"),
                    "Generated embed-and-sign phase should not contain duplicate separators in the task path",
                )
            }
        }
    }

    @GradleTest
    fun `integrateEmbedAndSign injects gradle invocation into pbxproj when absent`(version: GradleVersion) {
        project("emptyxcode-no-embedandsign", version) {
            initDefaultKmpWithLocalSPM()

            val pbxFile = projectPath.resolve("iosApp/iosApp.xcodeproj/project.pbxproj")
            assertFileDoesNotContain(pbxFile, "./gradlew ")

            build(
                "integrateEmbedAndSign",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                    "GRADLEW_PATH" to projectPath.resolve("gradlew").absolutePathString(),
                    "GRADLE_PROJECT_PATH" to ":",
                )
            ) {
                val pbxFileContent = pbxFile.readText()
                assertContains(
                    pbxFileContent,
                    "./gradlew :embedAndSignAppleFrameworkForXcode",
                    message = "Generated shell script phase should invoke the embedAndSign Gradle task",
                )
            }
        }
    }

    @GradleTest
    fun `integrateEmbedAndSign is idempotent when a gradle script phase already exists`(version: GradleVersion) {
        project("emptyxcode", version) {
            initDefaultKmpWithLocalSPM()

            val pbxFile = projectPath.resolve("iosApp/iosApp.xcodeproj/project.pbxproj")
            val pbxBefore = pbxFile.readText()

            build(
                "integrateEmbedAndSign",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                    "GRADLEW_PATH" to projectPath.resolve("gradlew").absolutePathString(),
                    "GRADLE_PROJECT_PATH" to ":",
                )
            ) {
                assertOutputContains("Found embedAndSign integration. Nothing to do")
                assertEquals(
                    pbxBefore,
                    pbxFile.readText(),
                    "pbxproj must not be modified when an embedAndSign integration already exists",
                )
            }
        }
    }

    @GradleTest
    fun `integrateEmbedAndSign fails when pbxproj has no native targets`(version: GradleVersion) {
        project("emptyxcode-no-embedandsign", version) {
            initDefaultKmpWithLocalSPM()

            // Drop the only PBXNativeTarget by renaming its isa so the native-targets list ends up empty.
            val pbxFile = projectPath.resolve("iosApp/iosApp.xcodeproj/project.pbxproj")
            pbxFile.toFile().writeText(
                pbxFile.readText().replace("isa = PBXNativeTarget;", "isa = PBXOpaqueTargetForTest;")
            )

            buildAndFail(
                "integrateEmbedAndSign",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                    "GRADLEW_PATH" to projectPath.resolve("gradlew").absolutePathString(),
                    "GRADLE_PROJECT_PATH" to ":",
                )
            ) {
                assertOutputContains("Couldn't find targets to insert embedAndSign integration")
            }
        }
    }

    @GradleTest
    fun `integrateEmbedAndSign fails when GRADLE_PROJECT_PATH env var is missing`(version: GradleVersion) {
        project("emptyxcode-no-embedandsign", version) {
            initDefaultKmpWithLocalSPM()

            buildAndFail(
                "integrateEmbedAndSign",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                    "GRADLEW_PATH" to projectPath.resolve("gradlew").absolutePathString(),
                )
            ) {
                assertOutputContains("Please specify path to gradle project in GRADLE_PROJECT_PATH environment variable")
            }
        }
    }
}

private fun TestProject.initDefaultKmpWithLocalSPM() {
    val localSwiftPackageRelativePath = "../localSwiftPackage"
    createLocalSwiftPackage(projectPath.resolve(localSwiftPackageRelativePath))

    initDefaultKmp {
        swiftPMDependencies {
            localSwiftPackage(
                directory = project.layout.projectDirectory.dir(localSwiftPackageRelativePath),
                products = listOf("LocalSwiftPackage"),
            )
        }
    }
}
