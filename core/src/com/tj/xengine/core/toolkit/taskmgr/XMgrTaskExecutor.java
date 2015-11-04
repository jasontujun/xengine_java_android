package com.tj.xengine.core.toolkit.taskmgr;

import com.tj.xengine.core.toolkit.task.XTaskBean;
import com.tj.xengine.core.toolkit.task.XTaskExecutor;
import com.tj.xengine.core.toolkit.taskmgr.speed.calc.XSpeedCalculable;

/**
 * <pre>
 * 表示可以放进XTaskMgr管理的任务执行器接口。
 * 继承XTaskExecutor和XSpeedCalculable。
 * User: jasontujun
 * Date: 14-6-12
 * Time: 下午4:08
 * </pre>
 */
public interface XMgrTaskExecutor<B extends XTaskBean>
        extends XTaskExecutor<B>, XSpeedCalculable {

    /**
     * 设置任务所属的任务管理器。
     * 当任务结束时，调用TaskMgr的notifyTaskFinished()
     * @param taskMgr
     */
    void setTaskMgr(XTaskMgr<XMgrTaskExecutor<B>, B> taskMgr);

    /**
     * 获取任务所属的任务管理器。
     * @return 返回任务所诉的任务管理器
     */
    XTaskMgr<XMgrTaskExecutor<B>, B> getTaskMgr();
}
