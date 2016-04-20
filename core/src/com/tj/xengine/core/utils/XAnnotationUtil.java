package com.tj.xengine.core.utils;

import com.tj.xengine.core.toolkit.filter.XFilter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Created by jasontujun on 2016/4/17.
 */
public abstract class XAnnotationUtil {

    public static Field findFirstObjectField(Class<?> clazz,
                                             Class<? extends Annotation> annotationClazz,
                                             XFilter<Field> filter) {
        if (Object.class.equals(clazz))
            return null;
        try {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                int modify = field.getModifiers();
                if (Modifier.isStatic(modify) || Modifier.isTransient(modify)) {
                    continue;
                }
                Annotation ann = field.getAnnotation(annotationClazz);
                if (ann != null) {
                    if (filter != null && filter.doFilter(field) == null) {
                        continue;
                    }
                    // 找到第一个符合的field，结束
                    field.setAccessible(true);
                    return field;
                }
            }
            return findFirstObjectField(clazz.getSuperclass(), annotationClazz, filter);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void findAllObjectField(Class<?> clazz,
                                          Class<? extends Annotation> annotationClazz,
                                          XFilter<Field> filter,
                                          List<Field> result) {
        if (Object.class.equals(clazz))
            return;
        try {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                int modify = field.getModifiers();
                if (Modifier.isStatic(modify) || Modifier.isTransient(modify)) {
                    continue;
                }
                Annotation ann = field.getAnnotation(annotationClazz);
                if (ann != null) {
                    if (filter != null && filter.doFilter(field) == null) {
                        continue;
                    }
                    // 找到符合的field，添加进结果中
                    field.setAccessible(true);
                    result.add(field);
                }
            }
            findAllObjectField(clazz.getSuperclass(), annotationClazz, filter, result);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
