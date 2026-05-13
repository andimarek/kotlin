/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.TestSuiteName
import org.gradle.api.attributes.VerificationType
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Build-side support for KGP JaCoCo test coverage. Companion to the `kgp-coverage-producer`
 * precompiled script plugin, which owns the per-project JaCoCo wiring (jacoco plugin apply,
 * tool-version pin, `Test`-task toggle, `mainSourceElements` augmentation).
 *
 * This file is intentionally consolidated:
 * - `registerKgpCoverageDataVariant` is the function consumer build scripts call to expose a
 *   `.exec` file via `jacoco-report-aggregation`'s attribute schema.
 * - `InstrumentKgpJarsForCoverage` is the integration-tests-specific task that rewrites the
 *   installed KGP JARs in Maven Local with offline JaCoCo probes.
 *
 * `registerKgpCoverageDataVariant` lives here (rather than inside the `.gradle.kts` script
 * plugin) because top-level functions declared in a precompiled script plugin are not reachable
 * from consumer build scripts.
 */

/**
 * Registers a consumable outgoing configuration that exposes a JaCoCo `.exec` file to
 * `jacoco-report-aggregation` consumers via the standard attribute schema
 * (`Category.VERIFICATION` + `VerificationType.JACOCO_RESULTS` + `TestSuiteName=<suiteName>`).
 *
 * The artifact is wired with `builtBy(testTask)` so consumers (e.g.,
 * `:kotlin-gradle-plugin-test-coverage:functionalCoverageReport`) auto-trigger the producing
 * test task through Gradle's dependency graph and gain correct up-to-date checks across projects.
 */
fun Project.registerKgpCoverageDataVariant(
    configurationName: String,
    suiteName: String,
    execFile: Provider<RegularFile>,
    testTask: TaskProvider<*>,
) {
    configurations.consumable(configurationName) {
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.VERIFICATION))
            attribute(VerificationType.VERIFICATION_TYPE_ATTRIBUTE, objects.named(VerificationType.JACOCO_RESULTS))
            attribute(TestSuiteName.TEST_SUITE_NAME_ATTRIBUTE, objects.named(suiteName))
        }
        outgoing.artifact(execFile) {
            type = ArtifactTypeDefinition.BINARY_DATA_TYPE
            builtBy(testTask)
        }
    }
}

/**
 * Offline-instruments KGP JARs in Maven Local with JaCoCo probes.
 *
 * This embeds probes into the bytecode before Gradle TestKit applies its own transforms,
 * avoiding conflicts between Gradle's instrumentation in TestKit and JaCoCo's on-the-fly agent.
 */
@DisableCachingByDefault(because = "Modifies external files in Maven Local")
abstract class InstrumentKgpJarsForCoverage : org.gradle.api.tasks.JavaExec() {

    @get:Input
    abstract val kotlinVersion: Property<String>

    @get:Input
    abstract val mavenLocalDir: Property<String>

    @get:Input
    abstract val artifactIds: ListProperty<String>

    init {
        mainClass.set("org.jacoco.cli.internal.Main")

        argumentProviders.add(CommandLineArgumentProvider {
            val mavenLocal = File(mavenLocalDir.get())
            val version = kotlinVersion.get()
            buildList {
                add("instrument")
                for (artifactId in artifactIds.get()) {
                    val jarFile = mavenLocal.resolve("org/jetbrains/kotlin/$artifactId/$version/$artifactId-$version.jar")
                    if (jarFile.exists()) add(jarFile.absolutePath)
                }
                add("--dest")
                add(temporaryDir.absolutePath)
            }
        })

        doLast {
            val mavenLocal = File(mavenLocalDir.get())
            val version = kotlinVersion.get()
            for (artifactId in artifactIds.get()) {
                val jarFile = mavenLocal.resolve("org/jetbrains/kotlin/$artifactId/$version/$artifactId-$version.jar")
                val instrumentedJar = temporaryDir.resolve(jarFile.name)
                if (instrumentedJar.exists()) {
                    instrumentedJar.copyTo(jarFile, overwrite = true)
                    logger.lifecycle("Instrumented $artifactId JAR for JaCoCo offline coverage: ${jarFile.absolutePath}")
                } else {
                    logger.warn("KGP JAR not found for instrumentation: $jarFile")
                }
            }
        }
    }
}
