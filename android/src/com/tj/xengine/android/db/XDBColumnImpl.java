package com.tj.xengine.android.db;

import android.content.ContentValues;
import android.database.Cursor;
import com.tj.xengine.android.db.annotation.XColumn;
import com.tj.xengine.android.utils.XLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;

/**
 * 实现XDBColumn接口的实现类。
 * Created by jason on 2016/4/16.
 */
public final class XDBColumnImpl implements XDBColumn {

    private static final String TAG = "XDBColumnImpl";

    private String name;
    private boolean key;
    private boolean autoKey;
    private boolean unique;
    private boolean notNull;
    private String defaultValue;

    private Field field;
    private XDBDataType dbType;
    private Method getMethod;
    private Method setMethod;

    private XDBColumnImpl() {}

    public XDBColumnImpl(Class clazz, Field field, XColumn ann) {
        field.setAccessible(true);

        this.field = field;
        this.name = ann.name();
        this.key = ann.key();
        this.unique = ann.unique();
        this.notNull = ann.notNull();
        this.defaultValue = ann.defaultValue();

        Class<?> fieldType = field.getType();
        this.autoKey = ann.key() && ann.auto() && (isInt(fieldType) || isLong(fieldType));
        this.dbType = getDbType(fieldType);

        this.getMethod = findGetMethod(clazz, field);
        if (this.getMethod != null && !this.getMethod.isAccessible()) {
            this.getMethod.setAccessible(true);
        }
        this.setMethod = findSetMethod(clazz, field);
        if (this.setMethod != null && !this.setMethod.isAccessible()) {
            this.setMethod.setAccessible(true);
        }
    }

    public static XDBColumn createAutoId(String name) {
        XDBColumnImpl column = new XDBColumnImpl();
        column.name = name;
        column.key = true;
        column.autoKey = true;
        column.dbType = XDBDataType.INTEGER;
        return column;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getObjectFieldName() {
        return field == null ? null : field.getName();
    }

    @Override
    public XDBDataType getType() {
        return dbType;
    }

    @Override
    public boolean isKey() {
        return key;
    }

    @Override
    public boolean isAutoKey() {
        return autoKey;
    }

    @Override
    public boolean isNotNull() {
        return notNull;
    }

    @Override
    public boolean isUnique() {
        return unique;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean setFieldValueFromDb(Object obj, Cursor cursor, int index) {
        if (field == null || obj == null || cursor == null)
            return false;
        Object value = toFieldValue(field.getType(), cursor, index);
        if (value == null)
            return false;
        if (setMethod != null) {
            try {
                setMethod.invoke(obj, value);
                return true;
            } catch (Throwable e) {
                XLog.e(TAG, "setFieldValueFromDb() call setMethod error." + e.getMessage());
                return false;
            }
        } else {
            try {
                field.set(obj, value);
                return true;
            } catch (Throwable e) {
                XLog.e(TAG, "setFieldValueFromDb() set field error." + e.getMessage());
                return false;
            }
        }
    }

    @Override
    public boolean setDbValueFormField(Object obj, ContentValues dbValues) {
        if (obj == null || dbValues == null)
            return false;
        Object fieldValue = null;
        if (getMethod != null) {
            try {
                fieldValue = getMethod.invoke(obj);
            } catch (Throwable e) {
                XLog.e(TAG, "setDbValueFormField() call getMethod error." + e.getMessage());
            }
        } else if (field != null) {
            try {
                fieldValue = field.get(obj);
            } catch (Throwable e) {
                XLog.e(TAG, "setDbValueFormField() get field error." + e.getMessage());
            }
        }
        return toDbValue(fieldValue, name, dbValues);
    }

    private static boolean isInt(Class<?> fieldType) {
        return fieldType.equals(int.class) || fieldType.equals(Integer.class);
    }

    private static boolean isLong(Class<?> fieldType) {
        return fieldType.equals(long.class) || fieldType.equals(Long.class);
    }

    private static boolean isBoolean(Class<?> fieldType) {
        return fieldType.equals(boolean.class) || fieldType.equals(Boolean.class);
    }

    private static Method findGetMethod(Class<?> clazz, Field field) {
        if (Object.class.equals(clazz))
            return null;

        String fieldName = field.getName();
        Method getMethod = null;
        if (isBoolean(field.getType())) {
            getMethod = findBooleanGetMethod(clazz, fieldName);
        }
        if (getMethod == null) {
            String methodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            try {
                getMethod = clazz.getDeclaredMethod(methodName);
            } catch (NoSuchMethodException e) {
                XLog.d(TAG, clazz.getName() + "#" + methodName + " not exist");
            }
        }

        if (getMethod == null) {
            return findGetMethod(clazz.getSuperclass(), field);
        }
        return getMethod;
    }

    private static Method findSetMethod(Class<?> clazz, Field field) {
        if (Object.class.equals(clazz))
            return null;

        String fieldName = field.getName();
        Class<?> fieldType = field.getType();
        Method setMethod = null;
        if (isBoolean(fieldType)) {
            setMethod = findBooleanSetMethod(clazz, fieldName, fieldType);
        }
        if (setMethod == null) {
            String methodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            try {
                setMethod = clazz.getDeclaredMethod(methodName, fieldType);
            } catch (NoSuchMethodException e) {
                XLog.d(TAG, clazz.getName() + "#" + methodName + " not exist");
            }
        }

        if (setMethod == null) {
            return findSetMethod(clazz.getSuperclass(), field);
        }
        return setMethod;
    }

    private static Method findBooleanGetMethod(Class<?> clazz,
                                               String fieldName) {
        String methodName = fieldName.startsWith("is") ? fieldName :
                "is" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        try {
            return clazz.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            XLog.d(TAG, clazz.getName() + "#" + methodName + " not exist");
            return null;
        }
    }

    private static Method findBooleanSetMethod(Class<?> clazz,
                                               String fieldName,
                                               Class<?> fieldType) {
        String methodName = fieldName.startsWith("is") ?
                "set" + fieldName.substring(2, 3).toUpperCase() + fieldName.substring(3) :
                "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        try {
            return clazz.getDeclaredMethod(methodName, fieldType);
        } catch (NoSuchMethodException e) {
            XLog.d(TAG, clazz.getName() + "#" + methodName + " not exist");
            return null;
        }
    }

    protected static XDBDataType getDbType(Class<?> fieldType) {
        if (isBoolean(fieldType)) {
            return XDBDataType.INTEGER;
        }
        if (fieldType.equals(byte.class) || fieldType.equals(Byte.class)) {
            return XDBDataType.INTEGER;
        }
        if (fieldType.equals(char.class) || fieldType.equals(Character.class)) {
            return XDBDataType.INTEGER;
        }
        if (fieldType.equals(short.class) || fieldType.equals(Short.class)) {
            return XDBDataType.INTEGER;
        }
        if (isInt(fieldType)) {
            return XDBDataType.INTEGER;
        }
        if (isLong(fieldType)) {
            return XDBDataType.INTEGER;
        }
        if (fieldType.equals(Date.class)) {
            return XDBDataType.INTEGER;
        }
        if (fieldType.equals(float.class) || fieldType.equals(Float.class)) {
            return XDBDataType.REAL;
        }
        if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
            return XDBDataType.REAL;
        }
        if (fieldType.equals(String.class)) {
            return XDBDataType.TEXT;
        }
        if (fieldType.equals(byte[].class)) {
            return XDBDataType.BLOB;
        }
        return null;
    }

    private static Object toFieldValue(Class<?> fieldType, Cursor cursor, int index) {
        if (isBoolean(fieldType)) {
            return cursor.isNull(index) ? null : cursor.getInt(index) == 1;
        }
        if (fieldType.equals(byte.class) || fieldType.equals(Byte.class)) {
            return cursor.isNull(index) ? null : (byte) cursor.getInt(index);
        }
        if (fieldType.equals(char.class) || fieldType.equals(Character.class)) {
            return cursor.isNull(index) ? null : (char) cursor.getInt(index);
        }
        if (fieldType.equals(short.class) || fieldType.equals(Short.class)) {
            return cursor.isNull(index) ? null : cursor.getShort(index);
        }
        if (isInt(fieldType)) {
            return cursor.isNull(index) ? null : cursor.getInt(index);
        }
        if (isLong(fieldType)) {
            return cursor.isNull(index) ? null : cursor.getLong(index);
        }
        if (fieldType.equals(Date.class)) {
            return cursor.isNull(index) ? null : new Date(cursor.getLong(index));
        }
        if (fieldType.equals(float.class) || fieldType.equals(Float.class)) {
            return cursor.isNull(index) ? null : cursor.getFloat(index);
        }
        if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
            return cursor.isNull(index) ? null : cursor.getDouble(index);
        }
        if (fieldType.equals(String.class)) {
            return cursor.isNull(index) ? null : cursor.getString(index);
        }
        if (fieldType.equals(byte[].class)) {
            return cursor.isNull(index) ? null : cursor.getBlob(index);
        }
        return null;
    }

    private static boolean toDbValue(Object fieldValue, String key, ContentValues dbValues) {
        if (fieldValue == null)
            return false;
        Class<?> fieldType = fieldValue.getClass();
        if (isBoolean(fieldType)) {
            dbValues.put(key, (Boolean) fieldValue ? 1 : 0);
            return true;
        }
        if (fieldType.equals(byte.class) || fieldType.equals(Byte.class)) {
            dbValues.put(key, (Byte) fieldValue);
            return true;
        }
        if (fieldType.equals(char.class) || fieldType.equals(Character.class)) {
            dbValues.put(key, (Integer) fieldValue);
            return true;
        }
        if (fieldType.equals(short.class) || fieldType.equals(Short.class)) {
            dbValues.put(key, (Short) fieldValue);
            return true;
        }
        if (isInt(fieldType)) {
            dbValues.put(key, (Integer) fieldValue);
            return true;
        }
        if (isLong(fieldType)) {
            dbValues.put(key, (Long) fieldValue);
            return true;
        }
        if (fieldType.equals(Date.class)) {
            Date date = (Date) fieldValue;
            dbValues.put(key, date.getTime());
            return true;
        }
        if (fieldType.equals(float.class) || fieldType.equals(Float.class)) {
            dbValues.put(key, (Float) fieldValue);
            return true;
        }
        if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
            dbValues.put(key, (Double) fieldValue);
            return true;
        }
        if (fieldType.equals(String.class)) {
            dbValues.put(key, (String) fieldValue);
            return true;
        }
        if (fieldType.equals(byte[].class)) {
            dbValues.put(key, (byte[]) fieldValue);
            return true;
        }
        return false;
    }
}
