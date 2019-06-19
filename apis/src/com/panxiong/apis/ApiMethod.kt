package com.panxiong.apis

@Target(AnnotationTarget.FUNCTION)
annotation class ApiMethod(val value: String, val params: Array<ApiParam>)