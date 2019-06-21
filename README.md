# spring-boot-apis
    + 通过注解与反射的方式,自动生成接口文档数据
    + 为了导出jar方便,使用model的方式建立了项目, apis 模块为源代码目录
    + 项目需要依赖 spring boot (2.1.5.RELEASE) 环境

### 使用方式
######    1. 把apis-utils.jar引入到你的项目环境.
######    2. 在接口控制器(controller)上添加注解.
                @ApiClass(value = "控制器说明")
                @Controller
                @RequestMapping(value = ["/test"])
######    3. 在方法(接口)上添加
                @ApiMethod(value = "测试接口一", params = [
                    ApiParam(value = "p1", depict = "参数1", type = ApiType.String, example = "1"),
                    ApiParam(value = "p2", depict = "参数2", type = ApiType.String, example = "2")
                ])
                @ApiResult(value = "JSON", params = [ApiParam(value = "p1", depict = "参数1", type = ApiType.String, example = "1")])
                @RequestMapping(value = ["/index"])
######    4. 在任何地方调用获取接口数据
                val data = ApiUtils().getApiData("你的控制器所在的包路径", false);
                或者或者前端可以直接展示的HTML文档,文档依赖layui且已经引入.
                    + httpServletResponse.contentType = "text/html; charset=utf-8"  // 要浏览器能显示html文档
                    + val html = ApiUtils().getApiDataWithHtml("你的控制器所在的包路径", false, "文档标题", "文档描述");
######    5. 接口数据,如果需要漂亮的显示方式可以编写一个HTML页面来接收展示数据
                [
                    {
                        "controller": "com.fqchildren.course.controller.TestController",
                        "depict": "这是一个测试使用的控制器,演示API注解生成",
                        "apis": [
                            {
                                "path": "/test/index",
                                "depict": "测试接口一",
                                "method": [],
                                "params": [
                                    {
                                        "name": "p1",
                                        "depict": "参数1",
                                        "required": false,
                                        "type": "String",
                                        "example": "1"
                                    },
                                    {
                                        "name": "p2",
                                        "depict": "参数2",
                                        "required": false,
                                        "type": "String",
                                        "example": "2"
                                    }
                                ]
                            },
                            {
                                "path": "/test/index3",
                                "depict": "测试接口三",
                                "method": [],
                                "params": [
                                    {
                                        "name": "p1",
                                        "depict": "参数1",
                                        "required": false,
                                        "type": "String",
                                        "example": "1"
                                    },
                                    {
                                        "name": "p2",
                                        "depict": "参数2",
                                        "required": false,
                                        "type": "String",
                                        "example": "2"
                                    }
                                ]
                            },
                            {
                                "path": "/test/index2",
                                "depict": "测试接口二",
                                "method": [],
                                "params": [
                                    {
                                        "name": "p1",
                                        "depict": "参数1",
                                        "required": false,
                                        "type": "String",
                                        "example": "1"
                                    },
                                    {
                                        "name": "p2",
                                        "depict": "参数2",
                                        "required": false,
                                        "type": "String",
                                        "example": "2"
                                    }
                                ],
                                "resultExample": "JSON",
                                "resultParams": [
                                    {
                                        "name": "p1",
                                        "depict": "参数1",
                                        "required": false,
                                        "type": "String",
                                        "example": "1"
                                    },
                                    {
                                        "name": "",
                                        "depict": "",
                                        "required": false,
                                        "type": "Other",
                                        "example": ""
                                    }
                                ]
                            }
                        ]
                    }
                ]
