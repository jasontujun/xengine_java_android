package com.tj.xengine.android.data.listener;

import android.os.Message;
import android.util.Pair;
import com.tj.xengine.core.data.XWithId;

import java.util.List;

/**
 * Created by jasontujun on 2016/4/18.
 */
public abstract class XHandlerIdDataSourceListener<T>
        extends  XHandlerDataSourceListener<T> implements XWithId.Listener<T> {

    private static final int MSG_ON_REPLACE = 11;

    @Override
    public void onReplace(List<T> newItems, List<T> oldItems) {
        handler.obtainMessage(MSG_ON_REPLACE, new Pair<List<T>, List<T>>(newItems, oldItems)).sendToTarget();
    }

    @Override
    protected boolean handleMsg(Message msg) {
        if (super.handleMsg(msg)) {
            return true;
        }
        if (msg.what == MSG_ON_REPLACE) {
            Pair<List<T>, List<T>> pair = (Pair<List<T>, List<T>>) msg.obj;
            onReplaceInUI(pair.first, pair.second);
            return true;
        }
        return false;
    }

    public abstract void onReplaceInUI(List<T> newItems, List<T> oldItems);
}
