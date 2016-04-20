package com.tj.xengine.android.db;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * 表示数据表的列的接口。
 * 定义了列的相关属性，以及数据库列和对象字段的转换方法。
 * Created by jasontujun on 2016/4/16.
 */
public interface XDBColumn {

    String getName();

    /**
     * 获取列对应的对象字段的名字。
     * @return
     */
    String getObjectFieldName();

    XDBDataType getType();

    boolean isKey();

    boolean isAutoKey();

    boolean isNotNull();

    boolean isUnique();

    String getDefaultValue();

    /**
     * 将数据库游标对应的列的值转化成对象字段的值。
     * @param obj
     * @param cursor
     * @param index
     * @return 转换成功返回true,否则返回false
     */
    boolean setFieldValueFromDb(Object obj, Cursor cursor, int index);

    /**
     * 将对象字段的值转化为数据库对应列的类型的值。
     * @param obj
     * @return
     */
    boolean setDbValueFormField(Object obj, ContentValues dbValues);
}
