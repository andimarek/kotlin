/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    jacoco
}

val jacocoToolVersion: String = extensions
    .getByType(VersionCatalogsExtension::class.java)
    .named("libs")
    .findVersion("jacoco")
    .get()
    .requiredVersion

jacoco {
    toolVersion = jacocoToolVersion
}

val kgpTestCoverageEnabled: Boolean =
    providers.gradleProperty("kgp.jacoco.enabled").orNull?.toBoolean() ?: false

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isEnabled = kgpTestCoverageEnabled
    }
    // Don't fail the build on test failures when collecting coverage: JaCoCo writes the .exec on
    // shutdown regardless of test outcome, and the dependent coverage report task is more useful
    // than the failure signal in this mode.
    if (kgpTestCoverageEnabled) ignoreFailures = true
}

// jacoco-report-aggregation's source-dir discovery picks up `mainSourceElements`. The `common`
// source set isn't part of `mainSourceElements` by default, so augment it here when present so
// it's not dropped from coverage reports. Class outputs from `common` are already exposed via
// the `classes` secondary variant of `runtimeElements` by the gradle-plugin convention plugin.
plugins.withId("java") {
    configurations.findByName("mainSourceElements")?.let { mainSourceElements ->
        sourceSets.findByName("common")?.allSource?.srcDirs?.forEach { srcDir ->
            mainSourceElements.outgoing.artifact(srcDir) {
                type = ArtifactTypeDefinition.DIRECTORY_TYPE
            }
        }
    }
}
