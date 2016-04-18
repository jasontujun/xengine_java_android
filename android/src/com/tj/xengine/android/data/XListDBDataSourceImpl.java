package com.tj.xengine.android.data;

import android.os.AsyncTask;
import com.tj.xengine.core.data.XListDataSourceImpl;
import com.tj.xengine.core.data.XWithDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 继承自XListDataSourceImpl的带数据库支持的数据源。
 * Created by jason on 2016/4/16.
 */
public class XListDBDataSourceImpl<T>
        extends XListDataSourceImpl<T> implements XWithDatabase<T> {

    private Class<T> mClazz;
    private List<XWithDatabase.Listener<T>> mDbListeners;

    public XListDBDataSourceImpl(Class<T> clazz, String sourceName) {
        super(sourceName);
        mClazz = clazz;
        mDbListeners = new CopyOnWriteArrayList<XWithDatabase.Listener<T>>();
    }

    @Override
    public void registerDbListener(XWithDatabase.Listener<T> listener) {
        if (!mDbListeners.contains(listener))
            mDbListeners.add(listener);
    }

    @Override
    public void unregisterDbListener(XWithDatabase.Listener<T> listener) {
        mDbListeners.remove(listener);
    }

    @Override
    public void saveToDatabase() {
        new AsyncTask<Object, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Object[] objects) {
                return XWithDBUtil.saveToDb(mClazz, mItemList);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                for (XWithDatabase.Listener<T> listener: mDbListeners) {
                    listener.onSaveFinish(result);
                }
            }
        }.execute();
    }

    @Override
    public void loadFromDatabase() {
        new AsyncTask<Object, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Object[] objects) {
                return XWithDBUtil.loadFromDb(mClazz, mItemList);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                for (XWithDatabase.Listener<T> listener: mDbListeners) {
                    listener.onLoadFinish(result, result ? new ArrayList<T>(mItemList) : null);
                }
            }
        }.execute();
    }

}
