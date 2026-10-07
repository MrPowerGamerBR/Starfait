package com.mrpowergamerbr.starfait.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import java.io.OutputStream

class StarfaitAnnotationProcessor(val codeGenerator: CodeGenerator) : SymbolProcessor {
    val namedConstants = mutableListOf<KSPropertyDeclaration>()
    val events = mutableListOf<KSPropertyDeclaration>()
    val subevents = mutableListOf<KSPropertyDeclaration>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        fun runVisitorForAnnotationProperty(annotationName: String, visitor: KSVisitorVoid) {
            resolver
                .getSymbolsWithAnnotation(annotationName)
                .filter { it.validate() }
                .filterIsInstance<KSPropertyDeclaration>()
                .forEach { it.accept(visitor, Unit) }
        }

        runVisitorForAnnotationProperty("com.mrpowergamerbr.starfait.NamedConstant", AddPropertyToListVisitor(namedConstants))
        runVisitorForAnnotationProperty("com.mrpowergamerbr.starfait.Event", AddPropertyToListVisitor(events))
        runVisitorForAnnotationProperty("com.mrpowergamerbr.starfait.Subevent", AddPropertyToListVisitor(subevents))

        return emptyList()
    }

    inner class AddPropertyToListVisitor(val list: MutableList<KSPropertyDeclaration>) : KSVisitorVoid() {
        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
            list.add(property)
        }
    }

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
    }
}