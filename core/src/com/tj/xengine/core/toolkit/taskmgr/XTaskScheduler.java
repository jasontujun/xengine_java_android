package com.tj.xengine.core.toolkit.taskmgr;

/**
 * <pre>
 * 任务调度器接口。
 * 用于计算各个任务的优先级。
 * User: jasontujun
 * Date: 13-10-15
 * Time: 下午5:28
 * </pre>
 */
public interface XTaskScheduler<T> {
    /**
     * 比较两个任务的先后执行顺序。
     * @param task1 第一个任务(比较对象)
     * @param task2 第二个任务(比较对象)
     * @param curTask 当前任务(参考对象)
     * @return 比较结果，-1表示第一个task1优先，1表示task2优先，0表示相同优先级。
     */
    int compare(T task1, T task2, T curTask);
}
