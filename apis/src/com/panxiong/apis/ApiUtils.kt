package com.panxiong.apis

import org.springframework.web.bind.annotation.RequestMapping
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.jar.JarFile
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet


class ApiUtils {

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
                    myClassName.addAll(getClassNameByFile(childFile.path, myClassName, childPackage))
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

            val api = mutableMapOf<String, Any>();
            api["path"] = clsMapper.value[0] + rm.value[0];
            api["depict"] = am.value;
            api["method"] = rm.method;

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
            apis.add(api);

            // 处理响应结果
            val methodResult = method.getAnnotation(ApiResult::class.java);
            if (methodResult != null) {
                val resultParams = LinkedHashSet<Any>();
                methodResult.params.forEach {
                    val pm = mutableMapOf<String, Any>();
                    pm["name"] = it.value;
                    pm["depict"] = it.depict;
                    pm["required"] = it.required;
                    pm["type"] = it.type;
                    pm["example"] = it.example;
                    resultParams.add(pm);
                }
                api["resultExample"] = methodResult.value;
                api["resultParams"] = resultParams;
            }
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
        return dataMap;
    }
}