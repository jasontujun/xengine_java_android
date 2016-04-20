package com.tj.xengine.android.db;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.List;

/**
 * 表示数据表的接口。
 * 定义了表的名字和列，以及数据库记录和对象的转换方法。
 * Created by jasontujun on 2016/4/16.
 */
public interface XDBTable<T> {

    /**
     * 返回数据表的表名
     */
    String getName();

    /**
     * 返回表中所有的字段名称
     */
    List<XDBColumn> getColumns();

    /**
     * 返回表中的主键
     */
    XDBColumn getPrimaryKey();

    /**
     * 返回创建表达额SQL CREATE TABLE语句
     */
    String createTableString();

    /**
     * 调用T类的无参数构造函数，创建一个空对象。
     */
    T createInstance();

    /**
     * 根据丢过来的实例，返回一个包含所有属性内容的ContentValues。
     * @param instance 模型的实例
     */
    ContentValues getContentValues(T instance);

    /**
     * 从cursor中取出数据，并填到一个实例中去，然后返回这个实例。
     */
    T getFilledInstance(Cursor cursor);
}
