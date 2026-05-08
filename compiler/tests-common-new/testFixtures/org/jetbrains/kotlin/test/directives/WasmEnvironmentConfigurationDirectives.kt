/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmCompilationMode
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object WasmEnvironmentConfigurationDirectives : SimpleDirectivesContainer() {
    val RUN_UNIT_TESTS by directive(
        description = "Run kotlin.test unit tests (function marked with @Test)",
    )

    val DISABLE_WASM_EXCEPTION_HANDLING by directive(
        description = "Generate wasm without EH proposal and test in runtime with EH turned off",
    )

    val USE_NEW_EXCEPTION_HANDLING_PROPOSAL by directive(
        description = "Generate wasm with the updated EH proposal turned on",
    )

    val USE_OLD_EXCEPTION_HANDLING_PROPOSAL by directive(
        description = "Generate wasm using the old EH proposal",
    )

    /**
     * For each of these, *null means all*, e.g., if `os` is null, it fails regardless of the OS
     * TODO(REVIEW): maybe move to a different file/scope, but I cant think of a good one
     */
    data class WasmIgnoreForConfig(
        val mode: WasmCompilationMode? = null,
        val os: String? = null,
        val vmName: String? = null,
    ) {
        override fun toString(): String {
            val props = listOfNotNull(
                mode?.let { "mode=$it" },
                os?.let { "os=$it" },
                vmName?.let { "vm=$it" }).joinToString(" ")
            return "WASM_IGNORE_FOR: $props"
        }
    }

    val WASM_IGNORE_FOR by valueDirective(
        description = "Ignore test failure in specified (Wasm) environment. " +
                "Multiple conditions in one directive entry are combined with AND, separated by ' ' " +
                "(e.g. 'mode=multi-module os=windows'). Use separate `WASM_IGNORE_FOR` lines for OR semantics.",
        splitValuesOnSpaces = false,
        parser = { raw ->
            // sanity check: no duplicates, neither in keys (no duplicate 'vm='), nor values (luckily, os, mode, and vm don't overlap in their sets of valid values)
            val individualParts = raw.split(' ').flatMap { it.split('=', limit = 2) }
            if (individualParts.distinct().size != individualParts.size) {
                System.err.println("`WASM_IGNORE_FOR` directive arguments '$raw' contain duplicate property assignments, which is not allowed.\nTo ignore based on a logical OR condition, use two separate `WASM_IGNORE_FOR` directives.")
                return@valueDirective null
            }

            val parts = raw.split(' ').associate {
                val splitList = it.split("=", limit = 2)
                // invalid syntax
                if (splitList.size != 2) return@valueDirective null

                val (k, v) = splitList
                k to v
            }

            // sanitizing
            if (parts.isEmpty()) {
                System.err.println("Directive $raw does not specify any properties to base the suppressor on.\nIf this is an intentional catch-all suppression, use IGNORE_BACKEND")
                return@valueDirective null
            }
            if (parts.keys.any { it !in listOf("mode", "os", "vm") }) {
                System.err.println("Invalid key specified in directive $raw, only know keys 'mode', 'os', 'vm'")
                return@valueDirective null
            }
            if (parts["os"]?.lowercase() !in listOf(null, "linux", "windows", "mac")) {
                System.err.println("Invalid OS specified in WASM_IGNORE_FOR directive: os=${parts["os"]}. Only know linux, windows, mac (case insensitive)")
                return@valueDirective null
            }
            // NOTE: mode mismatch will be caught by WasmCompilationMode.valueOf
            // NOTE: vm mismatches will be caught by the test itself, i.e. it will fail, or warn that it should be unmuted,
            //       if the config is wrong.
            //       There's unfortunately no non-hardcoded way to check all WasmVMs, without kotlin-reflections,
            //       and adding a module dependency on the testFixtures module.

            WasmIgnoreForConfig(
                mode = parts["mode"]?.let { WasmCompilationMode.valueOf(it.uppercase().replace('-', '_')) },
                os = parts["os"]?.lowercase(),
                vmName = parts["vm"],
            )
        }
    )

    val WASM_NO_JS_TAG by directive(
        description = "Don't use WebAssembly.JSTag for throwing and catching exceptions",
    )

    val WASM_INTERNAL_LOCAL_VARIABLE_PREFIX by stringDirective(
        description = "Prefix to use for internally generated local variables",
    )

    val WASM_DISABLE_FQNAME_IN_KCLASS by directive(
        description = "Disable 'KClass::qualifiedName' for wasm target",
    )

    // Next directives are used only inside test system and must not be present in test file

    val PATH_TO_TEST_DIR by stringDirective(
        description = "Specify the path to directory with test files. " +
                "This path is used to copy hierarchy from test file to test dir and use the same hierarchy in output dir.",
        applicability = DirectiveApplicability.Global
    )

    val PATH_TO_ROOT_OUTPUT_DIR by stringDirective(
        description = "Specify the path to output directory, where all artifacts will be stored",
        applicability = DirectiveApplicability.Global
    )

    val PATH_TO_NODE_DIR by stringDirective(
        description = "Specify the path to output directory, where all artifacts will be stored",
        applicability = DirectiveApplicability.Global
    )

    val TEST_GROUP_OUTPUT_DIR_PREFIX by stringDirective(
        description = "Specify the prefix directory for output directory that will contains artifacts",
        applicability = DirectiveApplicability.Global
    )

    val GENERATE_SOURCE_MAP by directive(
        description = "Enables generation of source map",
        applicability = DirectiveApplicability.Global
    )

    val GENERATE_DWARF by directive(
        description = "Enables generation of DWARF",
        applicability = DirectiveApplicability.Global
    )

    val FORCE_DEBUG_FRIENDLY_COMPILATION by directive(
        description = "Enable avoiding of the optimizations that can break debugging.",
        applicability = DirectiveApplicability.Global
    )

    val SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_FILES by directive(
        description = "Insert source mappings from libraries even if their sources are unavailable on the end-user machine",
        applicability = DirectiveApplicability.Global
    )

    val RUN_THIRD_PARTY_OPTIMIZER by directive(
        description = "Also run third-party optimizer (for now, only binaryen is supported) after the main compilation",
    )

    val WASM_DISABLE_ARRAY_RANGE_CHECKS by directive(
        description = "Disable array range checks for this test (default is enabled)",
    )

    val WASM_DISABLE_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION by directive(
        description = "Disable bounds check elimination for provably-safe array accesses in for-loops",
    )

    val CHECK_TYPESCRIPT_DECLARATIONS by directive(
        description = "Check typescript declarations generated by the compiler",
    )

    val NO_COMMON_FILES by directive(
        """
            Don't added helper files to prevent linking issues.
        """.trimIndent(),
        applicability = DirectiveApplicability.Global,
    )
}
