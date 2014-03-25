package org.telegram.android.util;

import java.lang.reflect.Field;

/**
 * Created by ex3ndr on 25.03.14.
 */
public class ReflectionUtiles {

    public static Field findField(Object src, String fieldName) throws Exception {
        return findField(src.getClass(), fieldName);
    }

    public static Field findField(Class src, String fieldName) throws Exception {
        try {
            return src.getDeclaredField(fieldName);
        } catch (Exception e) {
            if (src.getSuperclass() != null) {
                return findField(src.getSuperclass(), fieldName);
            } else {
                throw e;
            }
        }
    }

    public static <T> T reflectField(Object src, String fieldName, Class<T> clazz) throws Exception {
        Field field = findField(src, fieldName);
        field.setAccessible(true);
        return (T) field.get(src);
    }

    public static Object reflectField(Object src, String fieldName) throws Exception {
        return reflectField(src, fieldName, Object.class);
    }

    public static void writeValue(Object src, String fieldName, Object value) throws Exception {
        Field field = findField(src, fieldName);
        field.setAccessible(true);
        field.set(src, value);
    }
}
