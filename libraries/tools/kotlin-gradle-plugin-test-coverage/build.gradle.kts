import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    `jacoco-report-aggregation`
}

description = "Test Coverage report generation for KGP tests"

val KGP_TEST_TASKS_GROUP = "Kotlin Gradle Plugin Verification"

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

// Variant-based wiring: the producer projects expose `.exec` data, classes, and sources via
// outgoing configurations matched by `jacoco-report-aggregation`'s resolvable configurations.
// This replaces the previous approach that read `project(":kgp").layout.buildDirectory` and
// `project(":kgp").projectDir` directly, which is incompatible with Gradle Project Isolation and
// broke up-to-date / task-ordering wiring across project boundaries.
dependencies {
    jacocoAggregation(project(":kotlin-gradle-plugin"))
    jacocoAggregation(project(":kotlin-gradle-plugin-api"))
    jacocoAggregation(project(":kotlin-gradle-plugin-integration-tests"))
}

reporting {
    reports {
        register<JacocoCoverageReport>("functionalCoverageReport") {
            testSuiteName = "functionalTest"
        }
        register<JacocoCoverageReport>("integrationCoverageReport") {
            testSuiteName = "integrationTest"
        }
    }
}

// Combined report: there's no plugin-level "match all test suites" option, so we wire one manually
// by composing inputs from the two per-suite report tasks. The data is still pulled through the
// `jacocoAggregation` dependency graph — no direct access to other projects' build dirs.
val functionalReport = tasks.named<JacocoReport>("functionalCoverageReport")
val integrationReport = tasks.named<JacocoReport>("integrationCoverageReport")

val combinedCoverageReport by tasks.registering(JacocoReport::class) {
    group = KGP_TEST_TASKS_GROUP
    description = "Aggregated HTML/XML coverage report for KGP functional + integration tests"

    executionData.from(functionalReport.map { it.executionData })
    executionData.from(integrationReport.map { it.executionData })
    classDirectories.from(functionalReport.map { it.classDirectories })
    sourceDirectories.from(functionalReport.map { it.sourceDirectories })

    reports {
        html.required = true
        xml.required = true
        csv.required = false
    }

    // JacocoReport fails if no .exec files exist. Skip cleanly when neither suite has been run.
    onlyIf { executionData.files.any { it.exists() } }
}

// Enable XML report on the per-suite tasks (default is HTML only).
listOf(functionalReport, integrationReport).forEach { reportTask ->
    reportTask.configure {
        group = KGP_TEST_TASKS_GROUP
        reports {
            html.required = true
            xml.required = true
            csv.required = false
        }
        onlyIf { executionData.files.any { it.exists() } }
    }
}
