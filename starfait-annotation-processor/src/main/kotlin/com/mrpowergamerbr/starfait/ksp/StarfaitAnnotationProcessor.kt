package com.mrpowergamerbr.starfait.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import kotlin.sequences.forEach

class StarfaitAnnotationProcessor(val codeGenerator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    val gmlFunctions = mutableListOf<KSFunctionDeclaration>()
    val namedConstants = mutableListOf<KSPropertyDeclaration>()
    val events = mutableListOf<KSPropertyDeclaration>()
    val subevents = mutableListOf<KSPropertyDeclaration>()

    inline fun <reified T : KSDeclaration> runVisitorForAnnotation(resolver: Resolver, annotationName: String, visitor: KSVisitorVoid) {
        resolver
            .getSymbolsWithAnnotation(annotationName)
            .filter { it.validate() }
            .filterIsInstance<T>()
            .forEach { it.accept(visitor, Unit) }
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        runVisitorForAnnotation<KSPropertyDeclaration>(resolver, "com.mrpowergamerbr.starfait.NamedConstant", AddPropertyToListVisitor(namedConstants))
        runVisitorForAnnotation<KSPropertyDeclaration>(resolver, "com.mrpowergamerbr.starfait.Event", AddPropertyToListVisitor(events))
        runVisitorForAnnotation<KSPropertyDeclaration>(resolver, "com.mrpowergamerbr.starfait.Subevent", AddPropertyToListVisitor(subevents))
        runVisitorForAnnotation<KSFunctionDeclaration>(resolver, "com.mrpowergamerbr.starfait.GMLFunction", AddFunctionToListVisitor(gmlFunctions))

        return emptyList()
    }

    class AddFunctionToListVisitor(val list: MutableList<KSFunctionDeclaration>) : KSVisitorVoid() {
        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            list.add(function)
        }
    }

    class AddPropertyToListVisitor(val list: MutableList<KSPropertyDeclaration>) : KSVisitorVoid() {
        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
            list.add(property)
        }
    }

    // NEVER EVER THROW HERE, if you do, KSP will crash with "[ksp] java.lang.IllegalStateException: Storage for [...] is already registered" on later runs
    override fun finish() {
        val namedConstantsByParentDeclaration = namedConstants.groupBy { it.parentDeclaration!! }

        for ((parentDeclaration, constants) in namedConstantsByParentDeclaration) {
            val parent = parentDeclaration.containingFile!!
            val parentName = parentDeclaration.simpleName.asString()

            val outputStream = codeGenerator.createNewFile(
                dependencies = Dependencies(aggregating = false, parent),
                packageName = parentDeclaration.packageName.asString(),
                fileName = "Generated${parentDeclaration.simpleName.asString()}"
            )

            outputStream.write(
                buildString {
                    appendLine("package ${parent.packageName.asString()}")
                    appendLine("val ${parentName}Names = mapOf(")
                    for (constant in constants) {
                        appendLine("    $parentName.${constant.simpleName.asString()} to \"${constant.simpleName.asString()}\",")
                    }
                    appendLine(")")
                }.toByteArray(Charsets.UTF_8)
            )

            outputStream.close()
        }

        val eventByParentDeclaration = events.groupBy { it.parentDeclaration!! }

        for ((parentDeclaration, events) in eventByParentDeclaration) {
            val parent = parentDeclaration.containingFile!!
            val parentName = parentDeclaration.simpleName.asString()

            val outputStream = codeGenerator.createNewFile(
                dependencies = Dependencies(aggregating = false, parent),
                packageName = parentDeclaration.packageName.asString(),
                fileName = "Generated${parentDeclaration.simpleName.asString()}"
            )

            outputStream.write(
                buildString {
                    appendLine("package ${parent.packageName.asString()}")
                    appendLine("val MaxEventId = maxOf(${events.joinToString { "$parentName.${it.simpleName.asString()}" }})")
                    appendLine("val EventTypesToName = mapOf(")
                    for (constant in events) {
                        appendLine("    $parentName.${constant.simpleName.asString()} to \"${constant.simpleName.asString()}\",")
                    }
                    appendLine(")")

                    appendLine("val EventSubtypesToName = mapOf(")
                    for (constant in subevents) {
                        val parentAnnotation = constant.parentDeclaration!!.annotations.first { it.shortName.asString() == "SubtypesOf" }
                        val parentId = parentAnnotation.arguments.first().value
                        appendLine("    EventNameKey($parentId, EventType.${constant.parentDeclaration!!.simpleName.asString()}.${constant.simpleName.asString()}) to \"${constant.simpleName.asString()}\",")
                    }
                    appendLine(")")
                }.toByteArray(Charsets.UTF_8)
            )

            outputStream.close()
        }

        val outputStream = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true, *gmlFunctions.map { it.containingFile!! }.toTypedArray()),
            packageName = "com.mrpowergamerbr.starfait.vm.builtins",
            fileName = "GMLFunctions"
        )

        val generatedFunctionNames = mutableListOf<String>()

        outputStream.write(
            buildString {
                appendLine("package com.mrpowergamerbr.starfait.vm.builtins")
                appendLine("import com.mrpowergamerbr.starfait.vm.*")
                appendLine("import com.mrpowergamerbr.starfait.runner.*")
                appendLine("import com.mrpowergamerbr.starfait.*")

                for (func in gmlFunctions) {
                    val parentDeclaration = func.parentDeclaration!!

                    val parent = func.containingFile!!
                    val parentName = func.simpleName.asString()

                    // println("Function is ${func.simpleName.asString()} in ${parent.packageName.asString()}")

                    val allParameters = func.parameters
                    val parameters = allParameters.drop(2)

                    var parameterRange: IntRange
                    var hasRValueList = false

                    var minParameters = 0
                    var maxParameters = 0

                    for (argument in allParameters) {
                        if (argument.type.resolve().toString() == "StarfaitRunner" || argument.type.resolve().toString() == "StarfaitVM")
                            continue

                        if (argument.type.resolve().toString() == "List<RValue>") {
                            hasRValueList = true
                            break
                        }

                        if (!argument.type.resolve().isMarkedNullable) {
                            minParameters++
                        }

                        maxParameters++
                    }

                    if (hasRValueList) {
                        parameterRange = IntRange(0, Int.MAX_VALUE)
                    } else {
                        parameterRange = minParameters..maxParameters
                    }

                    val returnType = func.returnType!!.resolve().declaration.simpleName.asString()
                    val annotation = func.annotations.first { it.shortName.asString() == "GMLFunction" }
                    val names = (annotation.arguments.first { it.name!!.asString() == "names" }.value as List<String>).toMutableList()
                    names.addFirst(func.simpleName.asString())
                    val stub = annotation.arguments.first { it.name!!.asString() == "stub" }.value as Boolean

                    val wrapperName = "GMLFunction_${func.simpleName.asString()}_Wrapper"
                    generatedFunctionNames.add(wrapperName)

                    appendLine("object $wrapperName : BuiltinFunction(listOf(${names.joinToString(", ") { "\"$it\"" }}), $parameterRange) {")
                    appendLine("    override fun execute(runner: StarfaitRunner, vm: StarfaitVM, args: List<RValue>): RValue {")
                    if (stub) {
                        appendLine("        VMBuiltins.warnFunctionStub(vm, \"${names.first()}\")")
                    }
                    append("    val result = ${parentDeclaration.simpleName.asString()}.${func.simpleName.asString()}(")
                    var consumedArgs = 0
                    for ((index, arg) in allParameters.withIndex()) {
                        if (index != 0) append(", ")
                        val argumentType = arg.type.resolve().toString()

                        append(
                            when (argumentType) {
                                "StarfaitRunner" -> "runner"
                                "StarfaitVM" -> "vm"
                                "RValue" -> "args[${consumedArgs++}]"
                                "String" -> "args[${consumedArgs++}].convertToGMLString()"
                                "String?" -> "args.getOrNull(${consumedArgs++})?.convertToGMLString()"
                                "Double" -> "args[${consumedArgs++}].convertToReal()"
                                "Double?" -> "args.getOrNull(${consumedArgs++})?.convertToReal()"
                                "Float" -> "args[${consumedArgs++}].convertToReal().toFloat()"
                                "Float?" -> "args.getOrNull(${consumedArgs++})?.convertToReal()?.toFloat()"
                                "Int" -> "args[${consumedArgs++}].convertToInt32()"
                                "Int?" -> "args.getOrNull(${consumedArgs++})?.convertToInt32()"
                                "GMLColor" -> "GMLColor(args[${consumedArgs++}].convertToInt32())"
                                "GMLColor?" -> "args.getOrNull(${consumedArgs++})?.convertToInt32()?.let { GMLColor(it) }"
                                "Boolean" -> "args[${consumedArgs++}].convertToBoolean()"
                                "List<RValue>" -> "args"
                                else -> logger.error("Unsupported parameter type ${arg.type.resolve()}!")
                            }
                        )
                    }
                    append(")")
                    appendLine()
                    when (returnType) {
                        "Unit" -> appendLine("    return RValue.Undefined")
                        "Double" -> appendLine("    return RValue.Real(result)")
                        "Int" -> appendLine("    return RValue.Real(result.toDouble())")
                        "Float" -> appendLine("    return RValue.Real(result.toDouble())")
                        "String" -> appendLine("    return RValue.String(result)")
                        "Boolean" -> appendLine("    return RValue.Boolean(result)")
                        "TextFile" -> appendLine("    return RValue.Real(result.textFileId.toDouble())")
                        "RValue" -> appendLine("    return result")
                        else -> logger.error("Unsupported return type $returnType!")
                    }
                    appendLine("    }")
                    appendLine("}")
                }

                appendLine("fun registerBuiltinFunctions(vm: VMBuiltins) {")
                for (genName in generatedFunctionNames) {
                    appendLine("    vm.registerFunction($genName)")
                }
                appendLine("}")
            }.toByteArray(Charsets.UTF_8)
        )
    }
}