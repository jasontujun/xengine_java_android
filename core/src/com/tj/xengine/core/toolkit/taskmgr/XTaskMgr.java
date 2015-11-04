package com.tj.xengine.core.toolkit.taskmgr;

import com.tj.xengine.core.toolkit.filter.XFilter;
import com.tj.xengine.core.toolkit.task.XTaskBean;
import com.tj.xengine.core.toolkit.taskmgr.speed.XSpeedMonitor;

import java.util.List;

/**
 * <pre>
 * 任务管理器接口。
 * T 表示任务执行类的类型
 * B 表示任务数据类的类型
 * User: jasontujun
 * Date: 14-2-10
 * Time: 下午3:37
 * </pre>
 */
public interface XTaskMgr<T extends XMgrTaskExecutor<B>, B extends XTaskBean> {

    /**
     * 获取task的唯一Id。
     * @param task 任务
     * @return 如果返回的id为null，则认为此task唯一。
     */
    String getTaskId(T task);

    /**
     * 根据唯一Id获取等待队列和运行队列中的对应任务。
     * @param taskId  任务的唯一Id
     * @return 返回指定Id的任务；如果不存在，则返回null
     */
    T getTaskById(String taskId);

    /**
     * 添加任务(不重复，根据taskId判断唯一性)。
     * @param task 任务
     * @return 如果任务已存在，则返回false，添加失败；否则返回true。
     */
    boolean addTask(T task);

    /**
     * 批量添加任务(不重复，根据taskId判断唯一性)。
     * @param tasks 任务
     */
    void addTasks(List<T> tasks);

    /**
     * 删除任务(传入任务的对象)。
     * 如果任务是当前正在执行的任务，则终止并执行下一个任务
     * @param task 要删除的任务
     */
    void removeTask(T task);

    /**
     * 删除任务(传入任务的id)。
     * 如果任务是当前正在执行的任务，则终止并整体暂停
     * @param taskId 任务的唯一Id
     */
    void removeTaskById(String taskId);

    /**
     * 批量删除任务(传入任务对象列表)
     * @param tasks  待删除的任务对象列表
     */
    void removeTasks(List<T> tasks);

    /**
     * 批量删除任务(传入任务id列表)
     * @param taskIds 待删除的任务Id列表
     */
    void removeTasksById(List<String> taskIds);

    /**
     * 启动运行队列的所有任务；
     * 如果运行队列未满，则启动多个等待队列中的任务直到满
     * @return 如果当前有任务启动成功，则返回true；否则返回false
     */
    boolean start();

    /**
     * 启动指定Id的任务。
     * 如果指定Id的任务不存在，则什么都不做，返回false；
     * 如果指定Id的任务存在，且在运行队列中，恢复该任务；
     * 如果指定Id的任务存在，且在等待队列中，启动该任务(如果运行队列已满，则替换一个任务)。
     * @param taskId 任务的唯一Id
     * @return 如果启动成功，返回true；否则返回false
     */
    boolean start(String taskId);

    /**
     * 恢复运行队列的所有任务；
     * @return 如果当前有任务，且被恢复成功，则返回true；否则返回false
     */
    boolean resume();

    /**
     * 恢复指定Id的任务。
     * 如果指定Id的任务不存在，则什么都不做，返回false；
     * 如果指定Id的任务存在，且在运行队列中，恢复该任务；
     * 如果指定Id的任务存在，且在等待队列中，且运行队列已满，什么都不做；
     * 如果指定Id的任务存在，且在等待队列中，且运行队列未满，启动该任务。
     * @param taskId 任务的唯一Id
     * @return 如果恢复成功，则返回true；否则返回false
     */
    boolean resume(String taskId);

    /**
     * 暂停运行队列的所有任务(不会将任务从运行队列移回等待队列)。
     * @return 如果当前有任务，且被暂停成功，则返回true；否则返回false
     */
    boolean pause();

    /**
     * 暂停指定Id的任务。
     * 如果指定Id的任务不存在，或在等待队列中，则什么都不做，返回false
     * 如果指定Id的任务存在，且在运行队列中，暂停该任务(不会将任务从运行队列移回等待队列)。
     * @param taskId 任务的唯一Id
     * @return 如果暂停成功，则返回true；否则返回false
     */
    boolean pause(String taskId);

    /**
     * 暂停运行队列中被filter过滤掉的任务(不会将任务从运行队列移回等待队列)；
     * 无论是否暂停成功，都调用setTaskFilter(XFilter)将filter设置为当前的任务过滤器
     * @param filter 任务过滤器
     * @return 如果有任务暂停成功，则返回true；否则返回false
     * @see #setTaskFilter(XFilter)
     */
    boolean pauseByFilter(XFilter<B> filter);

    /**
     * 停止运行队列的所有任务(将任务从运行队列移回等待队列)
     * @return 如果当前有任务，且被暂停成功，则返回true；否则返回false
     */
    boolean stop();

    /**
     * 停止指定Id的任务。
     * 如果指定Id的任务不存在，或在等待队列中，则什么都不做，返回false
     * 如果指定Id的任务存在，且在运行队列中，停止该任务(将任务从运行队列移回等待队列)
     * @param taskId 任务的唯一Id
     * @return 如果暂停成功，则返回true；否则返回false
     */
    boolean stop(String taskId);

    /**
     * 停止运行队列中被filter过滤掉的任务(将任务从运行队列移回等待队列)。
     * 无论是否暂停成功，都调用setTaskFilter(XFilter)将filter设置为当前的任务过滤器
     * @param filter 任务过滤器
     * @return 如果有任务暂停成功，则返回true；否则返回false
     * @see #setTaskFilter(XFilter)
     */
    boolean stopByFilter(XFilter<B> filter);

    /**
     * 停止并清空所有任务。
     */
    void stopAndReset();

    /**
     * 如果运行队列未满，则将指定Id的任务
     * 从等待队列添加进运行队列，且不启动执行。
     * 如果运行队列已满，则什么都不做。
     * @param taskId 任务的唯一Id
     */
    void setRunningTask(String taskId);

    /**
     * 设置速度监控器
     * @param speedMonitor
     */
    void setSpeedMonitor(XSpeedMonitor<T> speedMonitor);

    /**
     * 设置任务过滤器，过滤掉的任务不会被执行。
     * @param filter 任务过滤器
     */
    void setTaskFilter(XFilter<B> filter);

    /**
     * 获取当前的TaskFilter
     * @return
     */
    XFilter<B> getTaskFilter();

    /**
     * 设置任务排序器，控制队列中任务的执行顺序。
     * 如果不设置TaskScheduler，则默认按照添加任务顺序执行。
     * @param scheduler 任务排序器
     */
    void setTaskScheduler(XTaskScheduler<B> scheduler);

    /**
     * 设置是否自动执行。
     * @param auto true表示开启自动执行；false表示关闭自动执行。
     */
    void setAutoRunning(boolean auto);

    /**
     * 当前是否是自动执行。
     * @return 是自动执行返回true；否则返回false。
     */
    boolean isAutoRunning();

    /**
     * 回调函数。任务完成后调用执行下一个或停止。
     * 注意：task在正常结束时回调此函数，
     * 因为调用任务的pause()或abort()导致中断的，不应该调用此函数
     * @param task 已结束的task
     * @param addBack 是否添加回等待队列
     */
    void notifyTaskFinished(T task, boolean addBack);

    /**
     * 注册监听（不重复注册，根据TaskMgrListener的id判断）
     * @param listener 外部监听者
     */
    void registerListener(XTaskMgrListener<B> listener);

    /**
     * 取消监听。
     * @param listener 外部监听者
     */
    void unregisterListener(XTaskMgrListener<B> listener);

    /**
     * 获取所有的外部监听。
     * @return 返回所有的外部监听
     */
    List<XTaskMgrListener<B>> getListeners();
}
