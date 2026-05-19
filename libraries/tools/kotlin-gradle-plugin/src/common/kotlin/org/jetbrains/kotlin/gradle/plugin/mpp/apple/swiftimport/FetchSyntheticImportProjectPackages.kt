/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject
import org.gradle.api.tasks.Optional
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class FetchSyntheticImportProjectPackages : DefaultTask() {

    /**
     * Refetch when Package manifests of local SwiftPM dependencies change
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val localPackageManifests: ConfigurableFileCollection

    @get:Internal
    val syntheticImportProjectRoot: DirectoryProperty = project.objects.directoryProperty()

    /**
     * These are own manifest and manifests from project/modular dependencies. Refetch when any of these Package manifests changed.
     */
    // For some reason FileTree still invalidates on random directories without this annotation even though directories are not tracked...
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val inputManifests
        get() = syntheticImportProjectRoot
            .asFileTree
            .matching {
                // Don't traverse these for performance reasons
                it.exclude(".swiftpm")
                it.exclude(".build")

                it.include("**/Package.swift")
            }

    @get:Internal
    val swiftPMDependenciesCheckout: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("kotlin/swiftPMCheckout")
    )

    @get:OutputFile
    protected val workspaceStateJson = swiftPMDependenciesCheckout.map { checkoutDir ->
        checkoutDir.file("workspace-state.json")
    }

    @get:Input
    val gitIgnoreCheckoutDir : Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    /**
     * Invalidate fetch when Package.swift or Package.resolved files changed.
     */
    @get:OutputFile
    val syntheticLockFile = syntheticImportProjectRoot.file("Package.resolved")

    @get:Internal
    abstract val additionalSwiftPackageResolveArgs: ListProperty<String>

    @get:Optional
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val xcodebuildExecutionHashFiles: ConfigurableFileCollection

    @get:Internal
    val coordinationDisabled: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    @get:Internal
    abstract val coordinationService: Property<SwiftPMXcodeDumpBuildService>

    @get:Inject
    abstract val fs: FileSystemOperations

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun generateSwiftPMSyntheticImportProjectAndFetchPackages() {
        val hashFiles = xcodebuildExecutionHashFiles.files.sortedBy { it.absolutePath }

        if (coordinationDisabled.get() || hashFiles.isEmpty()) {
            submitSwiftResolveWorkAction(
                ownerSyntheticImportProjectRoot = syntheticImportProjectRoot.get(),
                ownerSwiftPMDependenciesCheckout = swiftPMDependenciesCheckout.get(),
            )
            return
        }

        for (hashFile in hashFiles) {
            val hash = hashFile.readText().trim()

            val existingClaim = coordinationService.get().findExistingSwiftResolve(hash)
            if (existingClaim != null) {
                coordinationService.get().awaitSwiftResolved(existingClaim.bucket)
                copyPasteFromOwner(
                    existingClaim.bucket.ownerPackageResolvedFile,
                    syntheticLockFile.get().asFile,
                )
                copyPasteFromOwner(
                    existingClaim.bucket.ownerWorkspaceStateFile,
                    workspaceStateJson.get().asFile
                )
                return
            }
        }

        val ownerHash = hashFiles.first().readText().trim()
        when (
            val claim = coordinationService.get().claimOrJoinSwiftResolve(
                xcodebuildExecutionHash = ownerHash,
                packageResolvedFile = syntheticLockFile.get().asFile,
                workspaceStateFile = workspaceStateJson.get().asFile,
                swiftPMDependenciesCheckout = swiftPMDependenciesCheckout.get(),
                syntheticImportProjectRoot = syntheticImportProjectRoot.get(),
            )
        ) {
            is SwiftPMXcodeDumpBuildService.SwiftFetchClaim.Existing -> {
                coordinationService.get().awaitSwiftResolved(claim.bucket)
                copyPasteFromOwner(
                    claim.bucket.ownerPackageResolvedFile,
                    syntheticLockFile.get().asFile,
                )
                copyPasteFromOwner(
                    claim.bucket.ownerWorkspaceStateFile,
                    workspaceStateJson.get().asFile
                )
            }

            is SwiftPMXcodeDumpBuildService.SwiftFetchClaim.Owner -> runOwnerSwiftResolve(claim.bucket)
        }
    }

    private fun copyPasteFromOwner(
        source: File,
        destination: File,
    ) {
        require(source.isFile) { "Expected shared SwiftPM resolve output is missing: $source" }
        copySwiftLockFile(fs, source, destination)
    }


    private fun runOwnerSwiftResolve(
        bucket: SwiftPMXcodeDumpBuildService.SwiftResolveBucket,
    ) {
        try {
            // The owner writes directly to the root-build bucket, while still building this task's synthetic package and
            // using this task's SwiftPM checkout.
            submitSwiftResolveWorkAction(
                ownerSyntheticImportProjectRoot = bucket.ownerSyntheticImportProjectRoot,
                ownerSwiftPMDependenciesCheckout = bucket.ownerSwiftPMDependenciesCheckout,
            )
            workerExecutor.await()
            // Completion stamps the shared dump and releases any tasks waiting on the same bucket.
            coordinationService.get().markSwiftResolveCompleted(bucket)
        } catch (failure: Throwable) {
            // Propagate the same failure to every task that joined this bucket.
            coordinationService.get().markSwiftResolveFailed(bucket, failure)
            throw failure
        }
    }

    fun submitSwiftResolveWorkAction(
        ownerSyntheticImportProjectRoot: Directory,
        ownerSwiftPMDependenciesCheckout: Directory,
    ) {
        workerExecutor.noIsolation().submit(SwiftResolveWorkAction::class.java) { params ->
            params.syntheticImportProjectRoot.set(ownerSyntheticImportProjectRoot)
            params.swiftPMDependenciesCheckout.set(ownerSwiftPMDependenciesCheckout)
            params.additionalSwiftPackageResolveArgs.set(additionalSwiftPackageResolveArgs)
            params.gitIgnoreCheckoutDir.set(gitIgnoreCheckoutDir)
        }
    }


    companion object {
        const val TASK_NAME = "fetchSyntheticImportProjectPackages"
        fun fetchUmbrellaPackageTaskName(identifier: String) = lowerCamelCaseName(
            "fetchUmbrellaPackageIdentifierFor",
            identifier
        )

        const val XCODEBUILD_SWIFTPM_CHECKOUT_PATH_PARAMETER = "-clonedSourcePackagesDirPath"
    }
}
