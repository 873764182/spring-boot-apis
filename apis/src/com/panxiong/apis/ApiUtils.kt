package com.panxiong.apis

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.bind.annotation.RequestMapping
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.net.URLEncoder
import java.util.*
import java.util.jar.JarFile
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet


class ApiUtils {

    companion object {
        private val mCacheMap = mutableMapOf<String, Any>();    // 数据缓存
    }

    /**
     * 获取某包下所有类
     *
     * @param packageName 包名
     * @param childPackage 是否遍历子包
     *
     * @return 类的完整名称
     */
    fun getClassName(packageName: String, childPackage: Boolean = true): List<String>? {
        var fileNames: List<String>? = null
        val loader = Thread.currentThread().contextClassLoader
        val packagePath = packageName.replace(".", "/")
        val url = loader.getResource(packagePath)
        if (url != null) {
            val type = url.protocol
            if (type == "file") {
                fileNames = getClassNameByFile(url.path, null, childPackage)
            } else if (type == "jar") {
                fileNames = getClassNameByJar(url.path, childPackage)
            }
        } else {
            fileNames = getClassNameByJars((loader as URLClassLoader).urLs, packagePath, childPackage)
        }
        return fileNames
    }

    /**
     * 从项目文件获取某包下所有类
     *
     * @param filePath
     * 文件路径
     * @param className
     * 类名集合
     * @param childPackage
     * 是否遍历子包
     * @return 类的完整名称
     */
    private fun getClassNameByFile(filePath: String, className: List<String>?, childPackage: Boolean): List<String> {
        val myClassName = ArrayList<String>()
        val file = File(filePath)
        val childFiles = file.listFiles()
        for (childFile in childFiles) {
            if (childFile.isDirectory) {
                if (childPackage) {
                    myClassName.addAll(getClassNameByFile(childFile.path, myClassName as List<String>?, childPackage))
                }
            } else {
                var childFilePath = childFile.path
                if (childFilePath.endsWith(".class")) {
                    childFilePath = childFilePath.substring(childFilePath.indexOf("\\classes") + 9,
                            childFilePath.lastIndexOf("."))
                    childFilePath = childFilePath.replace("\\", ".")
                    myClassName.add(childFilePath)
                }
            }
        }

        return myClassName
    }

    /**
     * 从jar获取某包下所有类
     *
     * @param jarPath
     * jar文件路径
     * @param childPackage
     * 是否遍历子包
     * @return 类的完整名称
     */
    private fun getClassNameByJar(jarPath: String, childPackage: Boolean): List<String> {
        val myClassName = ArrayList<String>()
        val jarInfo = jarPath.split("!".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val jarFilePath = jarInfo[0].substring(jarInfo[0].indexOf("/"))
        val packagePath = jarInfo[1].substring(1)
        try {
            val jarFile = JarFile(jarFilePath)
            val entrys = jarFile.entries()
            while (entrys.hasMoreElements()) {
                val jarEntry = entrys.nextElement()
                var entryName = jarEntry.name
                if (entryName.endsWith(".class")) {
                    if (childPackage) {
                        if (entryName.startsWith(packagePath)) {
                            entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."))
                            myClassName.add(entryName)
                        }
                    } else {
                        val index = entryName.lastIndexOf("/")
                        val myPackagePath = if (index != -1) {
                            entryName.substring(0, index)
                        } else {
                            entryName
                        }
                        if (myPackagePath == packagePath) {
                            entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."))
                            myClassName.add(entryName)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return myClassName
    }

    /**
     * 从所有jar中搜索该包，并获取该包下所有类
     *
     * @param urls
     * URL集合
     * @param packagePath
     * 包路径
     * @param childPackage
     * 是否遍历子包
     * @return 类的完整名称
     */
    private fun getClassNameByJars(urls: Array<URL>?, packagePath: String, childPackage: Boolean): List<String> {
        val myClassName = ArrayList<String>()
        if (urls != null) {
            for (i in urls.indices) {
                val url = urls[i]
                val urlPath = url.path
                // 不必搜索classes文件夹
                if (urlPath.endsWith("classes/")) {
                    continue
                }
                val jarPath = "$urlPath!/$packagePath"
                myClassName.addAll(getClassNameByJar(jarPath, childPackage))
            }
        }
        return myClassName
    }

    // =================================================================================================================

    private fun getClassApi(className: String): Any? {
        val cls = Class.forName(className)
        val isApi = cls.isAnnotationPresent(ApiClass::class.java)
        if (!isApi) {
            return null;
        }
        val apiClass = cls.getAnnotation(ApiClass::class.java) ?: return null;
        val clsMapper = cls.getAnnotation(RequestMapping::class.java) ?: return null;

        val data = LinkedHashMap<String, Any>();
        data["controller"] = className;
        data["depict"] = apiClass.value;
        // data["mappers"] = clsMapper.value;
        // data["methods"] = clsMapper.method;

        val apis = LinkedHashSet<Any>();

        val methodList = cls.declaredMethods;
        for (method in methodList) {
            val isMethod = method.isAnnotationPresent(ApiMethod::class.java);
            val isMapper = method.isAnnotationPresent(RequestMapping::class.java);
            if (!isMethod || !isMapper) {
                continue;
            }
            val am = method.getAnnotation(ApiMethod::class.java);
            val rm = method.getAnnotation(RequestMapping::class.java);

            val pathList = mutableListOf<String>();
            if (clsMapper.value.isNotEmpty() && rm.value.isNotEmpty()) {
                clsMapper.value.forEach { c ->
                    rm.value.forEach { m ->
                        pathList.add(c + m);    // 多个路径组合
                    }
                }
            }

            val api = mutableMapOf<String, Any>();
            api["path"] = pathList; // clsMapper.value[0] + rm.value[0]; // 只读第一个路径
            api["depict"] = am.value;
            api["remark"] = am.depict;
            api["method"] = rm.method;

            if (am.params.isNotEmpty()) {
                val params = LinkedHashSet<Any>();
                am.params.forEach {
                    val pm = mutableMapOf<String, Any>();
                    pm["name"] = it.value;
                    pm["depict"] = it.depict;
                    pm["required"] = it.required;
                    pm["type"] = it.type;
                    pm["example"] = it.example;
                    params.add(pm);
                }
                api["params"] = params;
            }

            // 处理响应结果
            if (am.result.isNotEmpty()) {
                val resultParams = LinkedHashSet<Any>();
                am.result.forEach {
                    val pm = mutableMapOf<String, Any>();
                    pm["name"] = it.value;
                    pm["depict"] = it.depict;
                    pm["required"] = it.required;
                    pm["type"] = it.type;
                    pm["example"] = it.example;
                    resultParams.add(pm);
                }
                api["resultParams"] = resultParams;
            }
            if (am.example.isNotEmpty()) {
                api["resultExample"] = am.example;
            }

            apis.add(api);
        }

        data["apis"] = apis;

        return data;
    }

    /**
    [
    {
    "controller": "com.fqchildren.course.controller.TestController",
    "depict": "控制器描述说明",
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
    ],
    "resultExample": "JSON"
    }
    ]
    }
    ]
     */
    fun getApiData(packName: String, isChild: Boolean = false): Set<Any> {
        val cacheData = mCacheMap[packName + isChild];
        if (cacheData != null && cacheData is Set<*>) {
            return cacheData as Set<Any>;
        }

        val packageList = getClassName(packName, isChild)
        if (packageList.isNullOrEmpty()) {
            return setOf();
        }
        val dataMap = LinkedHashSet<Any>();
        packageList.forEach {
            val apiData = getClassApi(it);
            if (apiData != null) {
                dataMap.add(apiData);
            }
        }

        mCacheMap[packName + isChild] = dataMap;

        return dataMap;
    }

    // TODO 这个文档应该放在单独的html中方便修改样式.
    private val htmlData = "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head>" +
            "    <meta charset=\"utf-8\">" +
            "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
            "    <title>Title</title>" +
            "    <link rel=\"stylesheet\" type=\"text/css\" href=\"http://layui.hcwl520.com.cn/layui/css/layui.css\"/>" +
            "    <style type=\"text/css\">" +
            "        * {" +
            "            font-family: PingFang SC, Lantinghei SC, Helvetica Neue, Helvetica, Arial, Microsoft YaHei, \\\\5FAE\\8F6F\\96C5\\9ED1, STHeitiSC-Light, simsun, \\\\5B8B\\4F53, WenQuanYi Zen Hei, WenQuanYi Micro Hei, \"sans-serif\";" +
            "            margin: 0;" +
            "            padding: 0;" +
            "        }" +
            "    </style>" +
            "</head>" +
            "<body>" +
            "" +
            "<div style=\"margin: 16px\">" +
            "    <p style=\"font-size: 24px; font-weight: bold; color: #3385FF;\" id=\"titleView\">@DOC-TITLE</p>" +
            "    <span style=\"color: burlywood; font-size: 14px;\">@DOC-DEPICT</span>" +
            "</div>" +
            "" +
            "<div id=\"publicView\" style=\"margin: 16px; display: none;\" class=\"layui-collapse\">" +
            "    <div class=\"layui-colla-item\">" +
            "       <h2 class=\"layui-colla-title\" style='font-weight: bold; font-size: 16px; color: #3385FF;'>公共参数</h2>" +
            "       <div class=\"layui-colla-content\"  id=\"publicViewContent\">内容区域</div>" +
            "    </div>" +
            "</div>" +
            "" +
            "<div id=\"apisView\" style=\"margin: 16px;\"></div>" +
            "" +
            "<script src=\"http://layui.hcwl520.com.cn/layui/layui.all.js\"></script>" +
            "<script type=\"text/javascript\">" +
            "    (function () {" +
            "        document.title = layui.\$(\"#titleView\").text();" +
            "    })();" +
            "" +
            "    var basePackName = '@DOC-PACK-NAME'; " +
            "" +
            "    var publicParamData = '@DOC-PUBLIC-PARAM';" +
            "    if (publicParamData !== ('@DOC-PUBLIC' + '-PARAM')) {" +
            "        var publicView = createPublicParamView(publicParamData);" +
            "        layui.\$(\"#publicViewContent\").html(publicView); " +
            "        layui.\$(\"#publicView\").show();" +
            "    } " +
            "" +
            "    var apiJsonData = '@DOC-APIS-DATA';" +
            "" +
            "    var jsonApisObject = JSON.parse(apiJsonData);" +
            "" +
            "    var controllerListView = \"\";" +
            "    jsonApisObject.forEach(function (object) {" +
            "        var methodListView = \"<p style='color: burlywood'>控制器没有标注接口</p>\";" +
            "        var apisData = object.apis;" +
            "        if (apisData !== undefined && apisData.length > 0) {" +
            "            methodListView = \"\";" +
            "            apisData.forEach(function (method) {" +
            "                var paramsView = getParamView(method.params);" +
            "                var resultExampleView = getResultExample(method.resultExample);" +
            "                var resultParamsView = getResultParams(method.resultParams);" +
            "" +
            "                var requestMethod = \"<p style='font-weight: bold; font-size: 16px;'>\" +" +
            "                    \"请求方式: <span style='color: burlywood; font-weight: normal'>\" + method.method +" +
            "                    \"</span></p><br>\";" +
            "                if (method.method === undefined || method.method.length <= 0) {" +
            "                    requestMethod = \"<p style='font-weight: bold; font-size: 16px;'>\" +" +
            "                        \"请求方式: <span style='color: burlywood; font-weight: normal'>默认</span>\" +" +
            "                        \"</p><br>\";" +
            "                }" +
            "                var pathString = \"<p style='font-weight: bold; font-size: 16px;'>接口地址: <span style='color: burlywood; font-weight: normal'>\" + method.path + \"</span></p><br>\";" +
            "                var remarkString = \"<p style='font-weight: bold; font-size: 16px;'>备注信息:<br><span style='color: burlywood; font-weight: normal; font-size: 14px;'>\" + method.remark + \"</span></p><br>\";" +
            "                var depictString = \"<span style='color: #3385FF;'>\" + method.depict + \"</span>\";" +
            "                var methodView =" +
            "                    \"  <div class='layui-colla-item' title='\" + method.depict + \"'>\" +" +
            "                    \"    <h2 class='layui-colla-title'>\" + depictString + \"</h2>\" +" +
            "                    \"    <div class='layui-colla-content'>\" +" +
            "                    \"       \" + pathString + requestMethod + paramsView + resultExampleView + resultParamsView + remarkString +" +
            "                    \"    </div>\" +" +
            "                    \"  </div>\";" +
            "                methodListView += methodView;" +
            "            });" +
            "        }" +
            "        methodListView = \"<div class='layui-collapse' lay-accordion>\" + methodListView + \"</div>\";" +
            "" +
            "        var depictString = \"<span style='font-weight: bold; font-size: 16px;'>\" + object.depict + \"</span>\";" +
            "        var controllerName = \" (<span style='color: #999999;'>\" + object.controller + \"</span>) \";" +
            "        controllerName = controllerName.replace(basePackName, ''); " +
            "        var controllerView =" +
            "            \"<div class='layui-colla-item' title='\" + object.depict + \"'>\" +" +
            "            \"  <h2 class='layui-colla-title'>\" + depictString + controllerName + \"</h2>\" +" +
            "            \"  <div class='layui-colla-content'>\" + methodListView + \"</div>\" +" +
            "            \"</div>\";" +
            "" +
            "        controllerListView += controllerView;" +
            "    });" +
            "    var view = \"<div class='layui-collapse' lay-accordion>\" + controllerListView + \"</div>\";" +
            "" +
            "    layui.\$(\"#apisView\").html(view);" +
            "    layui.element.init();" +
            "" +
            "    function getParamView(params) {" +
            "        if (params === undefined || params.length <= 0) {" +
            "            return \"<p style='color: burlywood'>没有参数描述</p>\";" +
            "        }" +
            "        var paramView = \"\";" +
            "        params.forEach(function (item) {" +
            "            var itemView =" +
            "                \"<tr>\" +" +
            "                \"  <td>\" + item.name + \"</td>\" +" +
            "                \"  <td>\" + item.depict + \"</td>\" +" +
            "                \"  <td>\" + item.type + \"</td>\" +" +
            "                \"  <td>\" + item.required + \"</td>\" +" +
            "                \"  <td>\" + item.example + \"</td>\" +" +
            "                \"</tr>\";" +
            "            paramView += itemView;" +
            "        });" +
            "" +
            "        var tableView =" +
            "            \"<table class='layui-table' lay-size='sm'>\" +" +
            "            \"  <thead>\" +" +
            "            \"    <tr>\" +" +
            "            \"      <th>参数名称</th>\" +" +
            "            \"      <th>参数说明</th>\" +" +
            "            \"      <th>数据类型</th>\" +" +
            "            \"      <th>是否必传</th>\" +" +
            "            \"      <th>示例数据</th>\" +" +
            "            \"    </tr> \" +" +
            "            \"  </thead>\" +" +
            "            \"  <tbody>\" +" +
            "            \"   \" + paramView +" +
            "            \"  </tbody>\" +" +
            "            \"</table>\";" +
            "" +
            "        return \"<span style='font-size: 16px; font-weight: bold;'>参数说明</span><br>\" + tableView + \"<br>\";" +
            "    }" +
            "" +
            "    function getResultExample(example) {" +
            "        if (example === undefined || example.length <= 0) {" +
            "            return \"<p style='color: burlywood'>没有返回值数据示例</p>\";" +
            "        }" +
            "        return \"<span style='font-size: 16px; font-weight: bold;'>返回值示例</span><br><span style='color: burlywood;'>\" + example + \"</span><br><br>\";" +
            "    }" +
            "" +
            "    function getResultParams(resultParams) {" +
            "        if (resultParams === undefined || resultParams.length <= 0) {" +
            "            return \"<p style='color: burlywood'>没有返回值字段说明</p>\";" +
            "        }" +
            "        var paramView = \"\";" +
            "        resultParams.forEach(function (result) {" +
            "            var itemView =" +
            "                \"<tr>\" +" +
            "                \"  <td>\" + result.name + \"</td>\" +" +
            "                \"  <td>\" + result.depict + \"</td>\" +" +
            "                \"  <td>\" + result.type + \"</td>\" +" +
            "                \"  <td>\" + result.required + \"</td>\" +" +
            "                \"</tr>\";" +
            "            paramView += itemView;" +
            "        });" +
            "" +
            "        var tableView =" +
            "            \"<table class='layui-table' lay-size='sm'>\" +" +
            "            \"  <thead>\" +" +
            "            \"    <tr>\" +" +
            "            \"      <th>字段名称</th>\" +" +
            "            \"      <th>字段说明</th>\" +" +
            "            \"      <th>数据类型</th>\" +" +
            "            \"      <th>是否必有</th>\" +" +
            "            \"    </tr> \" +" +
            "            \"  </thead>\" +" +
            "            \"  <tbody>\" +" +
            "            \"   \" + paramView +" +
            "            \"  </tbody>\" +" +
            "            \"</table>\";" +
            "" +
            "        return \"<span style='font-size: 16px; font-weight: bold;'>返回值说明</span><br>\" + tableView + \"<br>\";" +
            "    } " +
            "" +
            "    function createPublicParamView(publicParams) {" +
            "        if (publicParams === undefined || publicParams.length <= 0) {" +
            "            return \"<p style='color: burlywood'>没有返回值字段说明</p>\";" +
            "        }" +
            "        var paramView = \"\";" +
            "        JSON.parse(publicParams).forEach(function (result) {" +
            "            var itemView =" +
            "                \"<tr>\" +" +
            "                \"  <td>\" + result.name + \"</td>\" +" +
            "                \"  <td>\" + result.type + \"</td>\" +" +
            "                \"  <td>\" + result.required + \"</td>\" +" +
            "                \"  <td>\" + result.place + \"</td>\" +" +
            "                \"  <td>\" + result.pio + \"</td>\" +" +
            "                \"  <td>\" + result.example + \"</td>\" +" +
            "                \"  <td>\" + result.depict + \"</td>\" +" +
            "                \"</tr>\";" +
            "            paramView += itemView;" +
            "        });" +
            "" +
            "        var tableView =" +
            "            \"<table class='layui-table' lay-size='sm'>\" +" +
            "            \"  <thead>\" +" +
            "            \"    <tr>\" +" +
            "            \"      <th>参数名称</th>\" +" +
            "            \"      <th>数据类型</th>\" +" +
            "            \"      <th>是否必有</th>\" +" +
            "            \"      <th>提交位置</th>\" +" +
            "            \"      <th>出参入参</th>\" +" +
            "            \"      <th>参数示例</th>\" +" +
            "            \"      <th>参数说明</th>\" +" +
            "            \"    </tr> \" +" +
            "            \"  </thead>\" +" +
            "            \"  <tbody>\" +" +
            "            \"   \" + paramView +" +
            "            \"  </tbody>\" +" +
            "            \"</table>\";" +
            "" +
            "        return tableView;" +
            "    }  " +
            "" +
            "    var passwordValue = '@DOC-PASS';" +
            "    var pass = localStorage.getItem('doc-pass-data');" +
            "    if (pass !== passwordValue) {" +
            "        layer.open({" +
            "            title: \"输入密码\"," +
            "            content: \"<div style='padding: 8px 16px 8px 16px'> <input type='password' id='passwordView' style='width: 100%; height: 32px;'></div>\"," +
            "            yes: function(index, layero){ " +
            "                var value = layui.\$('#passwordView').val();" +
            "                var ePass = encodeURIComponent(value);" +
            "                if (ePass === passwordValue) {" +
            "                    localStorage.setItem('doc-pass-data', ePass);" +
            "                    layer.close(index);" +
            "                }else {" +
            "                    layer.msg('密码错误');" +
            "                }" +
            "            }," +
            "            end:function () {" +
            "                window.location.reload();" +
            "            }" +
            "        });" +
            "    }" +
            "</script>" +
            "</body>" +
            "</html>";

    fun getApiDataWithHtml(
            packName: String, isChild: Boolean = false,
            docTitle: String = "", docDepict: String = "",
            publicParams: List<ApiPub> = listOf(), defPass: String = "123456",
            isZip: Boolean = true): String {
        var apiJsonData = ObjectMapper().writeValueAsString(getApiData(packName, isChild));
        if (apiJsonData.isNotEmpty()) {
            apiJsonData = apiJsonData.replace("\"", "\\\"");    // JSON引号问题
        }
        var htmlDoc = htmlData
                .replace("@DOC-PASS", URLEncoder.encode(defPass, "UTF-8"))
                .replace("@DOC-PACK-NAME", packName)
                .replace("@DOC-TITLE", docTitle)
                .replace("@DOC-DEPICT", docDepict)
                .replace("@DOC-APIS-DATA", apiJsonData);
        if (publicParams.isNotEmpty()) {
            var publicJsonData = ObjectMapper().writeValueAsString(publicParams);
            if (publicJsonData.isNotEmpty()) {
                publicJsonData = publicJsonData.replace("\"", "\\\"");    // JSON引号问题
            }
            htmlDoc = htmlDoc.replace("@DOC-PUBLIC-PARAM", publicJsonData);
        }
        return if (isZip) {
            htmlDoc.replace("  ", "");
        } else {
            htmlDoc;
        }
    }
}
