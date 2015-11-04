package com.tj.xengine.core.toolkit.taskmgr.parallel;

import com.tj.xengine.core.toolkit.task.XTaskBean;
import com.tj.xengine.core.toolkit.taskmgr.XMgrTaskExecutor;
import com.tj.xengine.core.toolkit.taskmgr.XTaskMgrListener;
import com.tj.xengine.core.toolkit.taskmgr.speed.XBaseSpeedMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * 用于监测当前SerialTask的速度。
 * User: jasontujun
 * Date: 13-12-21
 * Time: 下午5:03
 * </pre>
 */
public class XParallelSpeedMonitor<B extends XTaskBean>
        extends XBaseSpeedMonitor<XMgrTaskExecutor<B>> {

    private XParallelMgr<B> mParallelMgr;
    private List<XMgrTaskExecutor<B>> mRunningTasks;

    public XParallelSpeedMonitor(XParallelMgr<B> parallelMgr) {
        super();
        mParallelMgr = parallelMgr;
        mRunningTasks = new ArrayList<XMgrTaskExecutor<B>>();
    }

    public XParallelSpeedMonitor(XParallelMgr<B> parallelMgr, int interval) {
        super(interval);
        mParallelMgr = parallelMgr;
        mRunningTasks = new ArrayList<XMgrTaskExecutor<B>>();
    }

    @Override
    public List<XMgrTaskExecutor<B>> getRunningTasks() {
        mRunningTasks.clear();
        mRunningTasks.addAll(mParallelMgr.getRunningTask());
        return mRunningTasks;
    }

    @Override
    public void notifyUpdateSpeed(XMgrTaskExecutor<B> task, long speed) {
        List<XTaskMgrListener<B>> listeners =  task.getTaskMgr().getListeners();
        for (int i = 0; i < listeners.size(); i++)
            listeners.get(i).onSpeedUpdate(task.getBean(), speed);
    }
}
