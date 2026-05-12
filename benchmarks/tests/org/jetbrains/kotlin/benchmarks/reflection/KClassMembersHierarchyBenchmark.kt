/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.reflection

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import kotlin.reflect.KClass

@State(Scope.Benchmark)
open class KClassMembersHierarchyBenchmark {
    private val targetClass: KClass<out JavaFinalLayer> = JavaFinalLayer::class

    @TearDown
    fun after() {
        println("DEBUG Own: " + targetClass.members.find { it.name == "finalOwn1" }!!::class)
        println("DEBUG Base: " + targetClass.members.find { it.name == "abstractBase0" }!!::class)
        println("DEBUG Gen:" + targetClass.members.find { it.name == "equals" }!!::class)
        println("DEBUG Static:" + targetClass.members.find { it.name == "finalOwnStatic0" }!!::class)
    }
    @Benchmark
    open fun membersToString(): String {
        return targetClass.members.joinToString { it.toString() }
    }
}
