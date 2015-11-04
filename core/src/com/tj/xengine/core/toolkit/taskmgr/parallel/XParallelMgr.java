package com.tj.xengine.core.toolkit.taskmgr.parallel;

import com.tj.xengine.core.toolkit.task.XTaskBean;
import com.tj.xengine.core.toolkit.taskmgr.XMgrTaskExecutor;
import com.tj.xengine.core.toolkit.taskmgr.XTaskMgr;

import java.util.List;

/**
 * <pre>
 * 并行任务执行器接口。
 * T表示任务的类型
 * B表示数据的类型
 * Created by jasontujun.
 * Date: 12-10-30
 * Time: 下午8:48
 * </pre>
 */
public interface XParallelMgr<B extends XTaskBean>
        extends XTaskMgr<XMgrTaskExecutor<B>, B> {

    /**
     * 运行队列是否已空
     * @return 已空则返回true；否则返回false
     */
    boolean isEmptyParallel();

    /**
     * 运行队列是否已满
     * @return 已满则返回true；否则返回false
     */
    boolean isFullParallel();

    /**
     * 是否所有任务都停止
     * @return
     */
    boolean isAllStop();

    /**
     * 获取运行队列的所有任务
     * @return 返回当前正在运行的任务，如果没有，则返回null
     */
    List<XMgrTaskExecutor<B>> getRunningTask();

    /**
     * 获取当前等待队列的所有任务
     * @return 返回等待执行的任务列表
     */
    List<XMgrTaskExecutor<B>> getWaitingTask();
}
