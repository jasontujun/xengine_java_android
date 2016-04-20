package com.tj.xengine.android.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据库管理帮助类。
 * Created by jasontujun on 2016/4/16.
 */
public class XDatabase {

    private static class SingletonHolder {
        final static XDatabase INSTANCE = new XDatabase();
    }

    public static XDatabase getInstance() {
        return SingletonHolder.INSTANCE;
    }
    /**
     * SQLite系统维护的表，记录数据库相关属性信息。
     */
    private static final String SYSTEM_TABLE = "sqlite_master";

    /**
     * 系统表中记录数据表名称的字段名称。
     */
    private static final String TABLE_NAME = "name";

    /**
     * 缓存系统中已经创建的表的名称
     */
    private Map<Class, XDBTable> tables;

    private SQLiteOpenHelper mSql;
    private SQLiteDatabase mDatabase;
    private int mOpenCounter;

    private XDatabase() {
        tables = new HashMap<Class, XDBTable>();
        mOpenCounter = 0;
    }

    public synchronized void init(Context context, String dbName, int dbVersion) {
        if (mSql == null) {
            mSql = new XSQLiteHelper(context, dbName, dbVersion);
        }
    }

    public synchronized SQLiteDatabase openDatabase() {
        if (mSql == null)
            throw new IllegalStateException("SQLiteOpenHelper is null!");

        mOpenCounter++;
        if(mOpenCounter == 1) {
            // Opening new database
            mDatabase = mSql.getWritableDatabase();
            mDatabase.enableWriteAheadLogging();
        }
        return mDatabase;
    }

    public synchronized void closeDatabase() {
        if (mSql == null)
            throw new IllegalStateException("SQLiteOpenHelper is null!");

        mOpenCounter--;
        if(mOpenCounter == 0) {
            // Closing database
            mDatabase.close();
        }
    }

    public <T> XDBTable<T> getTable(Class<T> clazz) {
        XDBTable<T> table = tables.get(clazz);
        if (table == null) {
            try {
                table = new XDBTableImpl<T>(clazz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return table;
    }

    /**
     * 查询数据库中是否已经存在name所对应的数据表，否则创建。
     * @param clazz 数据表对象
     */
    public <T> boolean isTableExist(Class<T> clazz) {
        // 直接查询内部缓存的名称，如果已经存在就没必要去查询数据库了。
        XDBTable<T> table = tables.get(clazz);
        if(table != null) {
            return true;
        }
        try {
            table = new XDBTableImpl<T>(clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (table == null) {
            return false;
        }
        SQLiteDatabase db = openDatabase();
        // TIP sqlite_master数据表是sqlite数据库维护的系统数据表
        Cursor cur = db.query(SYSTEM_TABLE, new String[] {TABLE_NAME},
                        TABLE_NAME + " = " + "'" + table.getName() + "'",
                        null, null, null, null);
        if(cur.moveToFirst()) {
            cur.close();
            closeDatabase();
            tables.put(clazz, table);// 如果table确实存在，但不在tables缓存中，则添加
            return true;
        }else {
            cur.close();
            closeDatabase();
            return false;
        }
    }

    /**
     * 在数据库中创建一张新表
     * @param clazz 要创建的数据表
     */
    public <T> XDBTable<T> createTable(Class<T> clazz) {
        XDBTable<T> table = getTable(clazz);
        if (table != null) {
            if (!tables.containsKey(clazz)) {
                tables.put(clazz, table);// 创建表时，该表不在tables缓存中，则添加
            }
            SQLiteDatabase db = openDatabase();
            db.execSQL(table.createTableString());
            closeDatabase();
        }
        return table;
    }

    /**
     * 如果数据表在数据库中不存在，则创建这个数据表
     */
    public <T> XDBTable<T> createIfNotExist(Class<T> clazz) {
        if (!isTableExist(clazz)) {
            return createTable(clazz);
        } else {
            return getTable(clazz);
        }
    }

    /**
     * 删除数据表
     * @param clazz 要删除的数据表对象
     */
    public <T> void dropTable(Class<T> clazz) {
        XDBTable table = getTable(clazz);
        if (table == null)
            return;
        SQLiteDatabase db = openDatabase();
        db.execSQL("DROP TABLE " + table.getName());
        closeDatabase();
    }


    private class XSQLiteHelper extends SQLiteOpenHelper {

        public XSQLiteHelper(Context context, String name, int version) {
            super(context, name, null, version);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {

        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        }
    }
}
