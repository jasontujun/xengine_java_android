package com.tj.xengine.core.toolkit.taskmgr.serial;

import com.tj.xengine.core.toolkit.task.XTaskBean;
import com.tj.xengine.core.toolkit.taskmgr.XMgrTaskExecutor;
import com.tj.xengine.core.toolkit.taskmgr.XTaskMgrListener;
import com.tj.xengine.core.toolkit.taskmgr.speed.XBaseSpeedMonitor;

import java.util.LinkedList;
import java.util.List;

/**
 * <pre>
 * 用于监测当前SerialTask的速度。
 * User: jasontujun
 * Date: 13-12-21
 * Time: 下午5:03
 * </pre>
 */
public class XSerialSpeedMonitor<B extends XTaskBean>
        extends XBaseSpeedMonitor<XMgrTaskExecutor<B>> {

    private XSerialMgr<B> mSerialMgr;
    private List<XMgrTaskExecutor<B>> mRunningTasks;

    public XSerialSpeedMonitor(XSerialMgr<B> serialMgr) {
        super();
        mSerialMgr = serialMgr;
        mRunningTasks = new LinkedList<XMgrTaskExecutor<B>>();
    }

    public XSerialSpeedMonitor(XSerialMgr<B> serialMgr, int interval) {
        super(interval);
        mSerialMgr = serialMgr;
        mRunningTasks = new LinkedList<XMgrTaskExecutor<B>>();
    }

    @Override
    public List<XMgrTaskExecutor<B>> getRunningTasks() {
        mRunningTasks.clear();
        XMgrTaskExecutor<B> task = mSerialMgr.getRunningTask();
        if (task != null)
            mRunningTasks.add(task);
        return mRunningTasks;
    }

    @Override
    public void notifyUpdateSpeed(XMgrTaskExecutor<B> task, long speed) {
        List<XTaskMgrListener<B>> listeners =  task.getTaskMgr().getListeners();
        for (XTaskMgrListener<B> listener : listeners)
            listener.onSpeedUpdate(task.getBean(), speed);
    }
}
