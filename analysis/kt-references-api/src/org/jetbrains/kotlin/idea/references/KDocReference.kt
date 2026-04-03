/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.ResolveResult
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.psi.KtImplementationDetail

@OptIn(KtImplementationDetail::class)
interface KDocReference : KtReference, PsiPolyVariantReference {
    abstract override fun getElement(): KDocName

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult>
}
