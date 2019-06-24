package com.panxiong.apis

data class ApiPub(
        var name: String = "",
        var place: ApiPlace = ApiPlace.Other,
        var type: ApiType = ApiType.Other,
        var required: Boolean = false,
        var pio: ApiIo = ApiIo.Other,
        var example: String = "",
        var depict: String = ""
)