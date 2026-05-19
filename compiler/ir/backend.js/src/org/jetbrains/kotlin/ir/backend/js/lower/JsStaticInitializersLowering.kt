/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.InitializersLowering
import org.jetbrains.kotlin.backend.common.lower.InitializersLoweringBase
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.PRIVATE
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lower.JsStaticInitializersLowering.Companion.STATIC_FIELD_INITIALIZER
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.backend.js.staticInitializer
import org.jetbrains.kotlin.ir.backend.js.utils.primaryConstructorReplacement
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObjectValue
import org.jetbrains.kotlin.ir.builders.irReturnUnit
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name

/**
 * Moves initializers of static members of the class coming from companion blocks into a static initializer function.
 */
@PhasePrerequisites(
    ObjectDeclarationLowering::class,
    JsInitializersLowering::class,
    JsInitializersCleanupLowering::class
)
internal class JsStaticInitializersLowering(private val context: JsIrBackendContext) : FileLoweringPass {
    companion object {
        val STATIC_FIELD_INITIALIZER by IrStatementOriginImpl
        val STATIC_CLASS_INITIALIZER by IrDeclarationOriginImpl.Synthetic

        const val STATIC_INIT_FUNCTION_NAME = "static_init"
        const val STATIC_INIT_CALLED_PROPERTY_NAME = "static_init_called"
    }

    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                processDeclarationContainer(declaration)
                declaration.acceptChildrenVoid(this)
            }
        })
    }

    private fun IrStatement.isConst(): Boolean = when (this) {
        is IrConst, is IrConstantValue -> true
        is IrBlock -> {
            if (statements.isEmpty())
                true
            else {
                // This might happen after the local declarations lowering where local declarations are replaced with an empty composite.
                statements.take(statements.size - 1).all { it is IrComposite && it.statements.isEmpty() }
                        && statements.last().isConst()
            }
        }
        else -> false
    }

    private fun processDeclarationContainer(container: IrClass) {
        val builder = context.irBuiltIns.createIrBuilder(container.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
        val staticDeclarationsByFields = buildMap {
            for (declaration in container.declarations) {
                val field = declaration as? IrField ?: (declaration as? IrProperty)?.backingField ?: continue
                if (!field.isStatic) continue
                val initializer = field.initializer?.expression ?: continue
                /**
                 * Optimization: initialize in place constantly to avoid runtime checks if possible.
                 * To do it, we need to know the initializer in compile time
                 */
                if (initializer.isConst()) continue
                put(declaration, field to initializer)
            }
        }

        val initializers = buildList {
            for (declaration in container.declarations) {
                when (declaration) {
                    in staticDeclarationsByFields -> {
                        staticDeclarationsByFields[declaration]?.let { (field, initializer) ->
                            add(
                                builder.irSetField(
                                    receiver = null,
                                    field = field,
                                    value = initializer,
                                    origin = STATIC_FIELD_INITIALIZER
                                )
                            )
                            field.initializer = null
                        }
                    }
                    is IrClass if declaration.isCompanion && staticDeclarationsByFields.isNotEmpty() -> {
                        add(builder.irGetObjectValue(declaration.defaultType, declaration.symbol))
                    }
                }
            }
        }

        if (initializers.isEmpty()) return

        val staticInitCalledField = createStaticInitCalledField(container)
        val staticInitFunction = createInitFunction(
            container = container,
            origin = STATIC_CLASS_INITIALIZER,
            initCalledVar = staticInitCalledField,
            initializers = initializers
        )
        container.staticInitializer = staticInitFunction
        container.declarations.add(0, staticInitFunction)
        container.declarations.add(0, staticInitCalledField)

        fun IrFunction.addStaticInitCall() {
            val body = body ?: return
            val statements = (body as IrBlockBody).statements
            staticInitFunction.let { statements.add(0, builder.irCall(it.symbol)) }
        }

        for (function in container.simpleFunctions()) {
            if (function.dispatchReceiverParameter != null) continue // already initialized when instance was created
            if (function.origin == STATIC_CLASS_INITIALIZER) continue // don't initialize recursively
            function.addStaticInitCall()
        }

        for (constructor in container.constructors) {
            constructor.addStaticInitCall()
        }

        container.companionObject()?.constructors?.forEach {
            it.addStaticInitCall()
        }
    }

    private fun createStaticInitCalledField(irClass: IrClass): IrField = context.irFactory.buildField {
        name = Name.identifier("${irClass.name.identifier}$$STATIC_INIT_CALLED_PROPERTY_NAME")
        origin = STATIC_CLASS_INITIALIZER
        type = context.irBuiltIns.booleanType
        visibility = PRIVATE
        isStatic = true
    }.apply {
        parent = irClass
        initializer = context.irFactory.createExpressionBody(
            SYNTHETIC_OFFSET,
            SYNTHETIC_OFFSET,
            JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, false)
        )
    }

    private fun createInitFunction(
        container: IrClass,
        origin: IrDeclarationOrigin,
        initCalledVar: IrField,
        initializers: List<IrStatement>
    ): IrSimpleFunction {
        val initFunction = context.irFactory.buildFun {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            this.origin = origin
            name = Name.identifier(container.name.identifier + '$' + STATIC_INIT_FUNCTION_NAME)
            visibility = PRIVATE
            returnType = context.irBuiltIns.unitType
        }
        return initFunction.apply {
            parent = container
            body = context.irFactory.createBlockBody(startOffset, endOffset) {
                statements += context.createIrBuilder(symbol, SYNTHETIC_OFFSET).irBlockBody(this) {
                    +irIfThen(irGetField(null, initCalledVar), irReturnUnit())
                    +irSetField(null, initCalledVar, irBoolean(true))
                }.statements
                statements += initializers.map { it.setDeclarationsParent(initFunction) }
            }
        }
    }
}

/**
 * Inserts parents static initializer calls into the child static initializers body
 */
@PhasePrerequisites(JsStaticInitializersLowering::class)
internal class JsStaticInitializersInheritanceLowering(val context: JsIrBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                val parentClass = declaration.parentClassOrNull ?: return
                if (parentClass.staticInitializer != declaration) return
                val body = declaration.body as? IrBlockBody ?: return
                val parentInitializerCalls =
                    context.createIrBuilder(declaration.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).let { builder ->
                        parentClass.parentStaticInitializers.map { builder.irCall(it) }
                    }

                // Prepend the first initializer of this class with call to parent ones
                val initStartIndex = body.statements.indexOfFirst {
                    it is IrSetField && it.origin == STATIC_FIELD_INITIALIZER
                }

                when (initStartIndex) {
                    -1 -> body.statements += parentInitializerCalls
                    else -> body.statements.addAll(initStartIndex, parentInitializerCalls)
                }

                declaration.acceptChildrenVoid(this)
            }

            private val IrClass.parentStaticInitializers: List<IrSimpleFunctionSymbol>
                get() = getAllSuperclasses().mapNotNull { it.staticInitializer?.symbol }
        })
    }
}
