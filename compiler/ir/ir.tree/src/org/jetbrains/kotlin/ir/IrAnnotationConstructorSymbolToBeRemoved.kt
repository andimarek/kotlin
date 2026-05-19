/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

@RequiresOptIn("This API will be removed in future versions. Please migrate to `classSymbol`.", level = RequiresOptIn.Level.ERROR)
@Target(AnnotationTarget.PROPERTY)
annotation class IrAnnotationConstructorSymbolToBeRemoved
