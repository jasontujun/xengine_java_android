package com.tj.xengine.core.toolkit.task;

/**
 * <pre>
 * 任务执行器的接口。
 * 每个任务执行类包含一个任务数据类，
 * B表示任务数据的类型，B继承自TaskBean。
 * User: jasontujun
 * Date: 13-9-27
 * Time: 上午9:34
 * </pre>
 */
public interface XTaskExecutor<B extends XTaskBean> {

    /**
     * 开始或继续执行。
     * @param preStatus 增加兼容的启动前的状态
     * @return 开始或继续执行是否成功
     */
    boolean start(int... preStatus);

    /**
     * 暂停执行。
     * @param postStatus 设置暂停后的状态
     * @return 暂停执行是否成功
     */
    boolean pause(int... postStatus);

    /**
     * 终止并清除任务（删除相关内存和文件中的数据）。
     * @return 终止并清除任务是否成功
     */
    boolean abort();

    /**
     * 获取任务的数据bean
     * @return
     */
    B getBean();

    /**
     * 获取任务的唯一Id。
     * @return
     */
    String getId();

    /**
     * 设置任务的状态
     * @param status
     */
    void setStatus(int status);

    /**
     * 获取任务的状态。
     * @return
     */
    int getStatus();

    /**
     * 设置监听
     * @param listener
     */
    void setListener(XTaskListener<B> listener);

    /**
     * 获取监听
     * @return
     */
    XTaskListener<B> getListener();
}
