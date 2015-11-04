package com.tj.xengine.core.toolkit.taskmgr;

import com.tj.xengine.core.toolkit.task.XTaskBean;

import java.util.List;

/**
 * <pre>
 * 任务管理器监听接口
 * User: jasontujun
 * Date: 13-9-27
 * Time: 下午4:11
 * </pre>
 */
public interface XTaskMgrListener<T extends XTaskBean> {

    /**
     * 添加任务后的回调函数（在UI线程）
     * @param task
     */
    void onAdd(T task);

    /**
     * 批量添加任务后的回调函数（在UI线程）
     * @param tasks 真正添加进队列的任务
     */
    void onAddAll(List<T> tasks);

    /**
     * 删除任务后的回调函数（在UI线程）
     * @param task
     */
    void onRemove(T task);

    /**
     * 批量删除任务后的回调函数（在UI线程）
     * @param tasks
     */
    void onRemoveAll(List<T> tasks);

    /**
     * 启动的回调函数
     * start、resume等操作会触发此回调。
     * @param task
     */
    void onStart(T task);


    /**
     * 停止的回调函数。
     * stop、pause等操作会触发此回调。
     * @param task
     */
    void onStop(T task);

    /**
     * 所有任务都暂停。
     * remove、stop、stopAndReset等操作会触发此回调。
     * pause等操作不会触发此回调。
     */
    void onStopAll();

    /**
     * 完成所有执行任务。
     */
    void onFinishAll();

    /**
     * 执行过程中的回调函数。
     * @param task
     * @param completeSize
     */
    void onDoing(T task, long completeSize);

    /**
     * 执行成功结束的回调函数。
     * @param task
     */
    void onComplete(T task);

    /**
     * 执行失败结束的回调函数。
     * @param task
     * @param errorCode
     */
    void onError(T task, String errorCode);

    /**
     * 执行速度更新的回调函数（在异步线程）
     * @param task
     * @param speed
     */
    void onSpeedUpdate(T task, long speed);
}
