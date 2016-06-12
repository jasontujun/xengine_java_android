package com.tj.xengine.android.data.listener;

import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import com.tj.xengine.android.db.XDBColumn;
import com.tj.xengine.android.db.XDBTable;
import com.tj.xengine.android.db.XDatabase;
import com.tj.xengine.core.data.XListDataSource;
import com.tj.xengine.core.data.XWithId;
import com.tj.xengine.core.data.annotation.XId;
import com.tj.xengine.core.toolkit.filter.XBaseFilter;
import com.tj.xengine.core.utils.XAnnotationUtil;
import com.tj.xengine.core.utils.XStringUtil;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 用于对实现了XWithId接口的数据源，让数据源和数据库实时同步(在数据源发生增删操作时)。
 * 使用时只需对数据源注册XDataChangeListener即可。
 * Created by jasontujun on 2016/4/17.
 */
public class XAsyncDatabaseListener<T> implements XWithId.Listener<T> {

    private XWithId<T> mDataSource;
    private XDBTable<T> table;
    private String idColumnName;

    public XAsyncDatabaseListener(Class<T> clazz, XWithId<T> dataSource) {
        mDataSource = dataSource;
        // 标注为@XId的字段，不一定同时标注为@Column(key = true)，
        // 因此不能直接通过标注为@Column(key = true)的字段作为查询条件来操作数据库中的记录。
        // 所以要找到同时标注为@XId和标注为@Column的字段，作为查询条件来操作数据库记录
        Field idField = XAnnotationUtil.findFirstObjectField(clazz, XId.class,
                new XBaseFilter<Field>() {
                    @Override
                    public Field doFilter(Field source) {
                        // @XId标注的属性，类型必须是String，否则不符合id需求失败
                        return source.getType().equals(String.class) ? source : null;
                    }
                });
        if (idField != null) {
            String idFieldName = idField.getName();
            table = XDatabase.getInstance().getTable(clazz);
            if (table == null)
                return;
            List<XDBColumn> columns = table.getColumns();
            for (XDBColumn column : columns) {
                if (column.getObjectFieldName().equals(idFieldName)) {
                    idColumnName = column.getName();
                    break;
                }
            }
        }
    }

    @Override
    public void onChange() {
    }

    @Override
    public void onAdd(final T item) {
        if (item == null)
            return;
        if (XStringUtil.isEmpty(table.getName()))
            return;
        new AsyncTask<Object, Void, Void>() {
            @Override
            protected Void doInBackground(Object[] objects) {
                XDatabase dbHelper = XDatabase.getInstance();
                SQLiteDatabase db = dbHelper.openDatabase();
                db.insert(table.getName(), null, table.getContentValues(item));
                dbHelper.closeDatabase();
                return null;
            }
        }.execute();
    }

    @Override
    public void onAddAll(final List<T> items) {
        if (items == null || items.size() == 0)
            return;
        if (XStringUtil.isEmpty(table.getName()))
            return;
        new AsyncTask<Object, Void, Void>() {
            @Override
            protected Void doInBackground(Object[] objects) {
                XDatabase dbHelper = XDatabase.getInstance();
                SQLiteDatabase db = dbHelper.openDatabase();
                for (T item : items) {
                    db.insert(table.getName(), null, table.getContentValues(item));
                }
                dbHelper.closeDatabase();
                return null;
            }
        }.execute();
    }

    @Override
    public void onDelete(final T item) {
        if (item == null)
            return;
        if (XStringUtil.isEmpty(table.getName()) || XStringUtil.isEmpty(idColumnName))
            return;
        new AsyncTask<Object, Void, Void>() {
            @Override
            protected Void doInBackground(Object[] objects) {
                XDatabase dbHelper = XDatabase.getInstance();
                SQLiteDatabase db = dbHelper.openDatabase();
                db.delete(table.getName(), idColumnName + "=?",
                        new String[]{mDataSource.getId(item)});
                dbHelper.closeDatabase();
                return null;
            }
        }.execute();
    }

    @Override
    public void onDeleteAll(final List<T> items) {
        if (items == null || items.size() == 0)
            return;
        if (XStringUtil.isEmpty(table.getName()) || XStringUtil.isEmpty(idColumnName))
            return;
        new AsyncTask<Object, Void, Void>() {
            @Override
            protected Void doInBackground(Object[] objects) {
                XDatabase dbHelper = XDatabase.getInstance();
                SQLiteDatabase db = dbHelper.openDatabase();
                for (T item : items) {
                    db.delete(table.getName(), idColumnName + "=?",
                            new String[]{mDataSource.getId(item)});
                }
                dbHelper.closeDatabase();
                return null;
            }
        }.execute();
    }

    @Override
    public void onReplace(final List<T> newItems, final List<T> oldItems) {
        if (newItems == null || newItems.size() == 0)
            return;
        if (XStringUtil.isEmpty(table.getName()) || XStringUtil.isEmpty(idColumnName))
            return;
        new AsyncTask<Object, Void, Void>() {
            @Override
            protected Void doInBackground(Object[] objects) {
                XDatabase dbHelper = XDatabase.getInstance();
                SQLiteDatabase db = dbHelper.openDatabase();
                String[] idValue = new String[1];
                for (T item : newItems) {
                    idValue[0] = mDataSource.getId(item);
                    db.update(table.getName(), table.getContentValues(item),
                            idColumnName + "=?", idValue);
                }
                dbHelper.closeDatabase();
                return null;
            }
        }.execute();
    }
}
