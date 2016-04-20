package com.tj.xengine.android.data.listener;

import android.os.Handler;
import android.os.Message;
import com.tj.xengine.core.data.XListDataSource;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * 通过Handler调度回ui主线程的数据源监听器抽象类。
 * 待实现的抽象方法都运行在ui线程。
 * Created by jasontujun on 2016/4/18.
 */
public abstract class XHandlerDataSourceListener<T> implements XListDataSource.Listener<T> {

    private static final int MSG_ON_CHANGE = 1;
    private static final int MSG_ON_ADD = 2;
    private static final int MSG_ON_ADD_ALL = 3;
    private static final int MSG_ON_DELETE = 4;
    private static final int MSG_ON_DELETE_ALL = 5;

    private static class InnerHandler<T> extends Handler {
        private WeakReference<XHandlerDataSourceListener<T>> outer;

        public InnerHandler(XHandlerDataSourceListener<T> outer) {
            super();
            this.outer = new WeakReference<XHandlerDataSourceListener<T>>(outer);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ON_CHANGE:
                    if (outer.get() != null) {
                        outer.get().onChangeInUI();
                    }
                    break;
                case MSG_ON_ADD:
                    if (outer.get() != null) {
                        T item = (T) msg.obj;
                        outer.get().onAddInUI(item);
                    }
                    break;
                case MSG_ON_ADD_ALL:
                    if (outer.get() != null) {
                        List<T> items = (List<T>) msg.obj;
                        outer.get().onAddAllInUI(items);
                    }
                    break;
                case MSG_ON_DELETE:
                    if (outer.get() != null) {
                        T item = (T) msg.obj;
                        outer.get().onDeleteInUI(item);
                    }
                    break;
                case MSG_ON_DELETE_ALL:
                    if (outer.get() != null) {
                        List<T> items = (List<T>) msg.obj;
                        outer.get().onDeleteInUI(items);
                    }
                    break;
                default:
                    if (outer.get() != null) {
                        outer.get().handleMsg(msg);
                    }
            }
        }
    }
    protected Handler handler = new InnerHandler<T>(this);

    protected boolean handleMsg(Message msg) {
        // 用于继承的子类，来扩展handler定义的事件。
        // 该方法返回false，表示没有处理该message，返回true表示已经处理该message，子类不应该处理了。
        return false;
    }

    @Override
    public void onChange() {
        handler.sendEmptyMessage(MSG_ON_CHANGE);
    }

    @Override
    public void onAdd(T item) {
        handler.obtainMessage(MSG_ON_ADD, item).sendToTarget();
    }

    @Override
    public void onAddAll(List<T> items) {
        handler.obtainMessage(MSG_ON_ADD_ALL, items).sendToTarget();
    }

    @Override
    public void onDelete(T item) {
        handler.obtainMessage(MSG_ON_DELETE, item).sendToTarget();
    }

    @Override
    public void onDeleteAll(List<T> items) {
        handler.obtainMessage(MSG_ON_DELETE_ALL, items).sendToTarget();
    }

    public abstract void onChangeInUI();
    public abstract void onAddInUI(T item);
    public abstract void onAddAllInUI(List<T> items);
    public abstract void onDeleteInUI(T item);
    public abstract void onDeleteInUI(List<T> items);
}
