package com.tj.xengine.core.toolkit.task.runnable;

/**
 * <pre>
 * 有限重试次数的RetryRunnable
 * User: jasontujun
 * Date: 14-4-14
 * Time: 下午8:50
 * </pre>
 */
public abstract class XFiniteRetryRunnable<T> implements XRetryRunnable<T> {

    private volatile boolean isRunning;
    private long maxRetryCount;// 最大重试次数
    private long retryCount;

    protected XFiniteRetryRunnable(long max) {
        maxRetryCount = Math.max(max, 1);
        retryCount = 0;
        isRunning = true;
    }

    protected long getMaxRetryCount() {
        return maxRetryCount;
    }

    @Override
    public long getRetryCount() {
        return retryCount;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void cancel() {
        isRunning = false;
    }

    @Override
    public final void run() {
        T bean = getBean();

        // 准备活动
        if (!onPreExecute(bean)) {
            if (!isRunning) {
                onCancelled(bean);
            } else {
                onPreExecuteError(bean);
            }
            return;
        }

        // 核心重试逻辑
        long interval;
        while (isRunning && retryCount <= maxRetryCount) {
            if (onRepeatExecute(bean) || !isRunning)
                break;
            // 如果执行失败，等待一段时间后，再次执行
            retryCount++;
            try {
                interval = Math.max(getRetryInterval(getRetryCount()), 0);
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 结束
        if (isRunning)
            onPostExecute(bean);
        else
            onCancelled(bean);
    }
}
