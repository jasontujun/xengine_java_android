package com.tj.xengine.android.db;

import android.content.ContentValues;
import android.database.Cursor;
import com.tj.xengine.android.db.annotation.XColumn;
import com.tj.xengine.android.db.annotation.XTable;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.toolkit.filter.XBaseFilter;
import com.tj.xengine.core.utils.XAnnotationUtil;
import com.tj.xengine.core.utils.XStringUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 实现XDBTable接口的实现类。
 * 注意：模板实例化的数据类，必须含有XTable的Annotation，以及有一个无参数的构造函数。
 * Created by jasontujun on 2016/4/16.
 */
public final class XDBTableImpl<T> implements XDBTable<T> {

    private static final String TAG = "XDBTableImpl";

    private String name;
    private List<XDBColumn> columns;// 数据类对应的所有列(可能不含主键)
    private XDBColumn id;// 表的主键

    private Constructor<T> constructor;// 数据类的无参数构造函数

    public XDBTableImpl(Class<T> clazz) throws IllegalArgumentException {
        this.columns = new ArrayList<XDBColumn>();
        try {
            this.constructor = clazz.getConstructor();
            this.constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            XLog.e(TAG, "No non-param constructor." + e.getMessage());
            throw new IllegalArgumentException("No non-param constructor.");
        }
        XTable ann = clazz.getAnnotation(XTable.class);
        if (ann != null) {
            this.name = ann.name();
            List<Field> colFields = new ArrayList<Field>();
            XAnnotationUtil.findAllObjectField(clazz, XColumn.class,
                    new XBaseFilter<Field>() {
                        @Override
                        public Field doFilter(Field source) {
                            return XDBColumnImpl.getDbType(source.getType()) != null ? source : null;
                        }
                    }, colFields);
            for (Field colField : colFields) {
                XColumn colAnn = colField.getAnnotation(XColumn.class);
                XDBColumn column = new XDBColumnImpl(clazz, colField, colAnn);
                columns.add(column);
                if (id == null && colAnn.key()) {
                    id = column;
                }
            }
            // 如果递归遍历完还没有找到定义的主键，则创建一个名为_id的自增主键
            if (id == null) {
                id = XDBColumnImpl.createAutoId("_id");
            }
        } else {
            throw new IllegalArgumentException("No XTable Annotation.");
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String createTableString() {
        if (id == null)
            return null;

        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE IF NOT EXISTS ");
        builder.append("\"").append(getName()).append("\"");
        builder.append(" ( ");
        if (id.isAutoKey()) {
            builder.append("\"").append(id.getName()).append("\"").append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        } else {
            builder.append("\"").append(id.getName()).append("\" ").append(id.getType()).append(" PRIMARY KEY, ");
        }

        for (XDBColumn column : columns) {
            if (column.getName().equals(id.getName()))
                continue;
            builder.append("\"").append(column.getName()).append("\"");
            builder.append(" ").append(column.getType());
            if (column.isNotNull()) {
                builder.append(" NOT NULL");
            }
            if (column.isUnique()) {
                builder.append(" UNIQUE");
            }
            if (!XStringUtil.isEmpty(column.getDefaultValue())) {
                builder.append(" DEFAULT ").append(column.getDefaultValue());
            }
            builder.append(",");
        }

        builder.deleteCharAt(builder.length() - 1);
        builder.append(" )");
        return builder.toString();
    }

    @Override
    public T createInstance() {
        try {
            return this.constructor.newInstance();
        } catch (Exception e) {
            XLog.e(TAG, "createInstance() error." + e.getMessage());
        }
        return null;
    }

    @Override
    public List<XDBColumn> getColumns() {
        return columns;
    }

    @Override
    public XDBColumn getPrimaryKey() {
        return id;
    }

    @Override
    public ContentValues getContentValues(T instance) {
        ContentValues values = new ContentValues();
        for (XDBColumn column : columns) {
            column.setDbValueFormField(instance, values);
        }
        return values;
    }

    @Override
    public T getFilledInstance(Cursor cursor) {
        T instance = createInstance();
        if (instance == null)
            return null;
        for (XDBColumn column : columns) {
            column.setFieldValueFromDb(instance, cursor, cursor.getColumnIndex(column.getName()));
        }
        return instance;
    }
}
