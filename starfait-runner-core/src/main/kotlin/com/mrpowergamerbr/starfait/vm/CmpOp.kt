package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.NamedConstant
import kotlin.reflect.full.hasAnnotation

object CmpOp {
    @NamedConstant const val LESS_THAN = 1
    @NamedConstant const val LESS_THAN_OR_EQUAL = 2
    @NamedConstant const val EQUAL = 3
    @NamedConstant const val NOT_EQUAL = 4
    @NamedConstant const val GREATER_THAN_OR_EQUAL = 5
    @NamedConstant const val GREATER_THAN = 6

    fun getComparisonFunctionName(comparisonFunction: Int) = CmpOpNames[comparisonFunction] ?: "UNKNOWN"
}