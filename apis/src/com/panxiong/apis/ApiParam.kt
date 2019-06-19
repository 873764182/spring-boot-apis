package com.panxiong.apis

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ApiParam(
        val value: String = "",
        val depict: String = "",
        val required: Boolean = false,
        val type: ApiType = ApiType.Other,
        val example: String = ""
)