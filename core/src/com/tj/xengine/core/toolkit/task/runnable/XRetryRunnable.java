package com.tj.xengine.core.toolkit.task.runnable;

/**
 * <pre>
 * 定义了一套执行和重试流程的Runnable。
 *      1.前期准备工作，执行一次onPreExecute()
 *      2.核心逻辑，多次重试执行onRepeatExecute()
 *        如果执行完成不再重试，则返回true，否则返回false
 *      3.如果是正常结束的(onRepeatExecute返回true或达到重试最大上限)，
 *        则最后回调onPostExecute()
 *      4.如果是被中断的(调用cancel())，则最后回调onCancelled()
 * User: jasontujun
 * Date: 14-4-14
 * Time: 下午8:40
 * </pre>
 */
public interface XRetryRunnable<T> extends Runnable {

    static final int INFINITE_RETRY = -1;

    /**
     * 获取当前的重试执行onRepeatExecute的累计次数
     * @return 如果任务是无限重试的，则返回{@link #INFINITE_RETRY}；否则返回当前重试次数
     * @see #INFINITE_RETRY
     */
    long getRetryCount();

    /**
     * 获取重试onRepeatExecute的时间间隔(单位:毫秒,必须大于等于0)
     * @param retryCount 当前重试次数(如果是无限重试的，则该值为-1)
     * @return 返回重试的时间间隔
     * @see #getRetryCount()
     */
    long getRetryInterval(long retryCount);

    /**
     * 判断任务是否在运行
     * @return 如果正在运行，返回true;否则返回false.
     */
    boolean isRunning();

    /**
     * 中断任务的运行(非立即结束)
     */
    void cancel();

    /**
     * 获取数据对象
     * @return
     */
    T getBean();

    /**
     * 执行前的准备工作。
     * @param bean
     * @return 如果执行出错，返回false，任务结束，会触发onPreExecuteError();
     * 如果是耗时的准备工作，需要判断isRunning()，一旦被暂停了，返回false，会触发onCancelled();
     * 执行成功，返回true，任务继续，会触发onRepeatExecute().
     */
    boolean onPreExecute(T bean);

    /**
     * 执行前的准备工作失败的回调
     * @param bean
     */
    void onPreExecuteError(T bean);

    /**
     * 主要执行的工作(会被多次重复执行)
     * @param bean
     * @return 如果任务完成，则返回true，会触发onPostExecute()；
     * 否则返回false，并间隔一段时间重复执行该方法
     */
    boolean onRepeatExecute(T bean);

    /**
     * 任务执行完的后续工作
     * @param bean
     */
    void onPostExecute(T bean);


    /**
     * 线程被中断的善后工作
     * @param bean
     */
    void onCancelled(T bean);
}
