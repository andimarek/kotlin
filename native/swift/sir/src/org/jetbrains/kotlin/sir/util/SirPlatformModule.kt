/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirImport
import org.jetbrains.kotlin.sir.SirModule

/**
 * Common base for SIR modules that represent externally-defined modules — Swift Export does not generate
 * any code for them, instead emitting `import <name>` and qualifying types as `<name>.Type`. Two flavours:
 *  - [SirPlatformModule] — a module bundled with the Kotlin/Native distribution (Foundation, UIKit, …).
 *  - [SirCinteropModule] — a user-provided cinterop klib re-exported through an existing ObjC module.
 */
sealed class SirPlatformLikeModule(override val name: String) : SirModule() {
    override val declarations: MutableList<SirDeclaration> = mutableListOf()
    override val imports: MutableList<SirImport> = mutableListOf()
}

class SirPlatformModule(name: String) : SirPlatformLikeModule(name)

/**
 * Represents a user cinterop klib whose types originate from an ObjC module with the given [name].
 * Unlike [SirPlatformModule], there is no blacklist of excluded module names — the caller opts in explicitly.
 */
class SirCinteropModule(name: String) : SirPlatformLikeModule(name)
