package com.panxiong.apis

@Target(AnnotationTarget.FUNCTION)
annotation class ApiResult(val value: String = "", val params: Array<ApiParam> = [])