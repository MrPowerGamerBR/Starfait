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

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation("com.mrpowergamerbr.starfait.NamedConstant")
            .filter { it.validate() }
            .filterIsInstance<KSPropertyDeclaration>()
            .forEach { it.accept(NamedConstantVisitor(), Unit) }

        return emptyList()
    }

    inner class NamedConstantVisitor : KSVisitorVoid() {
        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
            namedConstants.add(property)
        }
    }

    override fun finish() {
        println("Done!")

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
    }
}