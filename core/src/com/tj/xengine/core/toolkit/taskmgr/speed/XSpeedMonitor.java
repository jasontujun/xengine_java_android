package com.tj.xengine.core.toolkit.taskmgr.speed;

import com.tj.xengine.core.toolkit.taskmgr.speed.calc.XSpeedCalculable;

import java.util.List;

/**
 * <pre>
 * 监测当前任务速度的监视线程。
 * T 表示监测的任务类型。
 * Created with IntelliJ IDEA.
 * User: jasontujun
 * Date: 13-10-22
 * Time: 下午4:07
 * </pre>
 */
public interface XSpeedMonitor<T extends XSpeedCalculable> {

    /**
     * 启动速度监测的异步线程。
     * 如果已经启动，则什么都不做
     */
    void start();

    /**
     * 停止速度监测的异步线程。
     * 如果已经停止，则什么都不做
     */
    void stop();

    /**
     * 获取当前运行(被监测对象)的任务列表。
     * @return
     */
    List<T> getRunningTasks();

    /**
     * 回调函数：通知对应任务更新当前速度。
     * @param task 当前任务
     * @param speed 当前速度（单位：byte/s）
     */
    void notifyUpdateSpeed(T task, long speed);
}
