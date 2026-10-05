package com.mrpowergamerbr.starfait.vm

import kotlin.reflect.full.hasAnnotation

object CmpOp {
    @ComparisonFunc const val LESS_THAN = 1
    @ComparisonFunc const val LESS_THAN_OR_EQUAL = 2
    @ComparisonFunc const val EQUAL = 3
    @ComparisonFunc const val NOT_EQUAL = 4
    @ComparisonFunc const val GREATER_THAN_OR_EQUAL = 5
    @ComparisonFunc const val GREATER_THAN = 6

    private val comparisonFunctionToName = CmpOp::class.members.filter { it.hasAnnotation<ComparisonFunc>() } .associateBy({ it.call() }) { it.name }

    fun getComparisonFunctionName(comparisonFunction: Int) = comparisonFunctionToName[comparisonFunction] ?: "UNKNOWN"

    private annotation class ComparisonFunc
}