package com.tj.xengine.android.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.tj.xengine.android.db.XDBTable;
import com.tj.xengine.android.db.XDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasontujun on 2016/4/17.
 */
public abstract class XWithDBUtil {

    public static <T> boolean saveToDb(Class<T> clazz, List<T> itemList) {
        XDatabase dbHelper = XDatabase.getInstance();
        XDBTable<T> table;
        if (dbHelper.isTableExist(clazz)) {
            table = dbHelper.getTable(clazz);
            if (table == null)
                return false;
            SQLiteDatabase db = dbHelper.openDatabase();
            db.delete(table.getName(), null, null);// 清空表的内容，重新写入
            dbHelper.closeDatabase();
        } else {
            table = dbHelper.createTable(clazz);
        }
        if (table == null)
            return false;

        // 拷贝一份本地列表，防止在插入过程中列表数据发生变化
        ArrayList<T> copyList = new ArrayList<T>(itemList);
        SQLiteDatabase db = dbHelper.openDatabase();
        for (int i = 0; i < itemList.size(); i++) {
            ContentValues cv = table.getContentValues(copyList.get(i));
            db.insert(table.getName(), null, cv);
        }
        dbHelper.closeDatabase();
        return true;
    }

    public static <T> boolean loadFromDb(Class<T> clazz, List<T> itemList) {
        XDatabase dbHelper = XDatabase.getInstance();
        XDBTable<T> table = dbHelper.createIfNotExist(clazz);
        if (table == null) {
            return false;
        }

        SQLiteDatabase db = dbHelper.openDatabase();
        if (db == null) {
            return false;
        }

        itemList.clear();
        Cursor cur = db.rawQuery("SELECT * FROM " + table.getName(), null);
        if (cur.moveToFirst()) {
            while (!cur.isAfterLast()) {
                T item = table.getFilledInstance(cur);
                itemList.add(item);
                cur.moveToNext();
            }
        }
        cur.close();
        dbHelper.closeDatabase();
        return true;
    }
}
