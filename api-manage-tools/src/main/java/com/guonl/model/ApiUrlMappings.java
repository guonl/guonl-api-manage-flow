package com.guonl.model;

import lombok.Data;

import java.util.Map;

/**
 * Created by guonl
 * Date 2020/7/19 6:23 PM
 * Description: 解析对象类
 */
@Data
public class ApiUrlMappings {

    /**
     * 类名
     */
    public String controllerName;

    /**
     * 请求url
     */
    public String requestUrl;

    /**
     * 方法名
     */
    public String methodName;


    /**
     * 方法类型 GET POST等
     */
    public String methodType;

    /**
     * body体类型
     * 当前台界面使用GET或POST方式提交数据时，数据编码格式由请求头的ContentType指定。分为以下几种情况：
     * 1. application/x-www-form-urlencoded，这种情况的数据@RequestParam、@ModelAttribute可以处理，@RequestBody也可以处理。
     * 2. multipart/form-data，@RequestBody不能处理这种格式的数据。（form表单里面有文件上传时，必须要指定enctype属性值为multipart/form-data，意思是以二进制流的形式传输文件。）
     * 3. application/json、application/xml等格式的数据，必须使用@RequestBody来处理
     */
    public String methodContentType;

    /**
     * 请求参数类
     */
    public Class<?>[] requestParamClass;

    /**
     * 请求参数
     */
    public Map<String,Object> requestParamMap;

    /**
     * 请求参数类
     */
    public Class<?> returnParamClass;

    /**
     * 返回参数
     */
    public Map<String,Object> returnParamMap;


}
