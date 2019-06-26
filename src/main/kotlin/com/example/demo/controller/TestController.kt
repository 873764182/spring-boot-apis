package com.example.demo.controller

import com.panxiong.apis.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

@ApiClass(value = "测试控制器")
@RestController
@RequestMapping(value = ["/test"])
class TestController {

    @ApiMethod(
            value = "测试接口,运行项目,直接访问这个接口即可查看接口数据",
            depict = "备注信息",
            params = [
                ApiParam(value = "p1", depict = "参数1", type = ApiType.String, example = "1"),
                ApiParam(value = "p2", depict = "参数2", type = ApiType.String, example = "2")
            ],
            result = [
                ApiParam(value = "r1", depict = "字段1", type = ApiType.String, example = "1"), ApiParam()
            ],
            example = "{\"key\":\"value\"}"
    )
    @RequestMapping(value = ["/index"])
    fun index(httpServletResponse: HttpServletResponse): Any {
        httpServletResponse.contentType = "text/html; charset=utf-8"

        val apiPubs = listOf(
                ApiPub(name = "a", place = ApiPlace.Header, type = ApiType.String, required = true, pio = ApiIo.In, example = "1", depict = "测试1"),
                ApiPub(name = "b", place = ApiPlace.Param, type = ApiType.Int, required = true, pio = ApiIo.Out, example = "2", depict = "测试2"),
                ApiPub(name = "c", place = ApiPlace.Other, type = ApiType.Other, required = false, pio = ApiIo.Other, example = "3", depict = "测试3")
        )

        return ApiUtils().getApiDataWithHtml(
                "com.example.demo", true, "标题", "描述", apiPubs)
    }

    @ApiMethod(
            value = "接口2,获取JSON类型的原始数据",
            depict = "这是接口备注说明信息",
            params = [
                ApiParam(value = "p1", depict = "参数1", type = ApiType.String, example = "1"),
                ApiParam(value = "p2", depict = "参数2", type = ApiType.String, example = "2")
            ],
            result = [
                ApiParam(value = "r1", depict = "返回字段1", type = ApiType.String, example = "1")
            ],
            example = "{\"r1\":\"xxx\"}"
    )
    @RequestMapping(value = ["/index2"], method = [RequestMethod.GET, RequestMethod.POST])
    fun index2(httpServletResponse: HttpServletResponse): Any {
        return ApiUtils().getApiData("com.example.demo", true)
    }

}