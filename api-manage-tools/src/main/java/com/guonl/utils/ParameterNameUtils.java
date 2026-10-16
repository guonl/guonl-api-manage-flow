package com.guonl.utils;

import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by guonl
 * Date 2020/7/19 7:11 PM
 * Description: 获取方法的参数名与结构信息
 * Spring 6.1+ 已移除 LocalVariableTableParameterNameDiscoverer（ASM 方案），
 * 统一使用 -parameters 编译参数 + 反射读取参数名。
 */
public class ParameterNameUtils {

    private static final List<String> filterList;

    static {
        filterList = new ArrayList<>();
        filterList.add("jakarta.servlet.http.HttpServletRequest");
        filterList.add("jakarta.servlet.http.HttpServletResponse");
        filterList.add("jakarta.servlet.http.HttpSession");
        filterList.add("jakarta.servlet.ServletRequest");
        filterList.add("jakarta.servlet.ServletResponse");
        filterList.add("org.springframework.ui.ModelMap");
        filterList.add("org.springframework.ui.Model");
        filterList.add("org.springframework.validation.BindingResult");
    }

    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new StandardReflectionParameterNameDiscoverer();

    /**
     * 获取指定方法的参数名（依据 @RequestParam 注解）
     *
     * @param method 要获取参数名的方法
     * @return 按参数顺序排列的参数名列表
     */
    public static String[] getMethodParameterNamesByAnnotation(Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        if (parameterAnnotations == null || parameterAnnotations.length == 0) {
            return null;
        }
        String[] parameterNames = new String[parameterAnnotations.length];
        int i = 0;
        for (Annotation[] parameterAnnotation : parameterAnnotations) {
            for (Annotation annotation : parameterAnnotation) {
                if (annotation instanceof RequestParam param) {
                    parameterNames[i++] = param.value();
                }
            }
        }
        return parameterNames;
    }

    /**
     * 获取请求的ContentType
     * application/x-www-form-urlencoded是默认类型  application/json
     *
     * @param method
     * @return
     */
    public static String getMethodContentType(Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        if (parameterAnnotations == null || parameterAnnotations.length == 0) {
            return "application/x-www-form-urlencoded";
        }
        for (Annotation[] parameterAnnotation : parameterAnnotations) {
            for (Annotation annotation : parameterAnnotation) {
                if (annotation instanceof RequestParam) {
                    return "application/x-www-form-urlencoded";
                } else if (annotation instanceof RequestBody) {
                    return "application/json";
                }
            }
        }
        return "application/x-www-form-urlencoded";
    }


    /**
     * 获取方法的参数名与参数结构。
     * 依赖宿主应用以 -parameters 编译（Spring Boot 官方 parent 默认开启）。
     *
     * @param method
     * @return
     */
    public static Map<String, Object> getMethodParameter(Method method) {
        Map<String, Object> map = new HashMap<>();
        String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
        if (parameterNames == null) {
            return map;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterNames.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            boolean baseType = isBaseType(parameterType);
            if (baseType) {
                map.put(parameterNames[i], parameterType.getSimpleName());
            } else {
                if (!filterList.contains(parameterType.getName())) {
                    Map<String, Object> classFields = getClassFields(parameterType);
                    map.putAll(classFields);
                }
            }
        }
        return map;
    }


    /**
     * 获取类的所有成员变量
     *
     * @param clazz
     * @return
     */
    public static Map<String, Object> getClassFields(Class<?> clazz) {
        Map<String, Object> map = new HashMap<>();
        if (isBaseType(clazz)) {
            map.put(clazz.getSimpleName(), clazz.getSimpleName());
        } else if (clazz.isArray()) {
            map.put(clazz.getSimpleName(), clazz.getSimpleName());
        } else {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field field : declaredFields) {
                map.put(field.getName(), field.getType().getSimpleName());
            }
        }
        return map;
    }

    /**
     * 判断变量是否为基础类型
     *
     * @param clazz
     * @return
     */
    public static boolean isBaseType(Class<?> clazz) {
        if (clazz.equals(java.lang.Integer.class) ||
                clazz.equals(java.lang.Byte.class) ||
                clazz.equals(java.lang.Long.class) ||
                clazz.equals(java.lang.Double.class) ||
                clazz.equals(java.lang.Float.class) ||
                clazz.equals(java.lang.Character.class) ||
                clazz.equals(java.lang.Short.class) ||
                clazz.equals(java.lang.Boolean.class) ||
                clazz.equals(java.lang.String.class) ||
                clazz.isPrimitive()) {
            return true;
        }
        return false;
    }


}
