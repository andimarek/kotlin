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

@State(Scope.Thread)
open class KClassMembersHierarchyBenchmark {
    private val targetClass: KClass<out JavaFinalLayer> = JavaFinalLayer::class

    @TearDown
    fun after() {
        println("DEBUG Own (finalOwn1): " + targetClass.members.find { it.name == "finalOwn1" }!!::class)
        println("DEBUG Static (finalOwnStatic0):" + targetClass.members.find { it.name == "finalOwnStatic0" }!!::class)
        println("DEBUG Base (abstractBase0): " + targetClass.members.find { it.name == "abstractBase0" }!!::class)
        println("DEBUG Gen (equals):" + targetClass.members.find { it.name == "equals" }!!::class)
    }

    @Benchmark
    open fun membersToString0(): String {
        return targetClass.members.joinToString { it.toString() }
    }

    @Benchmark
    open fun membersToString1(): String {
        return targetClass.members.joinToString { it.toString() }
    }
    @Benchmark
    open fun membersToString2(): String {
        return targetClass.members.joinToString { it.toString() }
    }
    @Benchmark
    open fun membersToString3(): String {
        return targetClass.members.joinToString { it.toString() }
    }
    @Benchmark
    open fun membersToString4(): String {
        return targetClass.members.joinToString { it.toString() }
    }

    @Benchmark
    open fun membersToString5(): String {
        return targetClass.members.joinToString { it.toString() }
    }

    @Benchmark
    open fun membersToString6(): String {
        return targetClass.members.joinToString { it.toString() }
    }

    @Benchmark
    open fun membersToString7(): String {
        return targetClass.members.joinToString { it.toString() }
    }

    @Benchmark
    open fun membersToString8(): String {
        return targetClass.members.joinToString { it.toString() }
    }

    @Benchmark
    open fun membersToString9(): String {
        return targetClass.members.joinToString { it.toString() }
    }

}
