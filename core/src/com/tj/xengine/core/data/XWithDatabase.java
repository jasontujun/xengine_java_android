package com.tj.xengine.core.data;


import java.util.List;

/**
 * Created by 赵之韵.
 * Email: ttxzmorln@163.com
 * Date: 12-3-8
 * Time: 下午7:12
 */
public interface XWithDatabase<T> {

    interface Listener<T> {
        void onSaveFinish(boolean result);

        void onLoadFinish(boolean result, List<T> items);
    }

    void registerDbListener(Listener<T> listener);

    void unregisterDbListener(Listener<T> listener);

    void saveToDatabase();

    void loadFromDatabase();
}
