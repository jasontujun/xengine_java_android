package com.tj.xengine.core.toolkit.task.runnable;

/**
 * <pre>
 * 无限重试次数的RetryRunnable
 * User: jasontujun
 * Date: 14-4-14
 * Time: 下午8:28
 * </pre>
 */
public abstract class XInfiniteRetryRunnable<T> implements XRetryRunnable<T> {

    private volatile boolean isRunning;

    protected XInfiniteRetryRunnable() {
        isRunning = true;
    }

    @Override
    public long getRetryCount() {
        return INFINITE_RETRY;
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
        while (isRunning) {
            // 如果任务执行完成或者外部中断，则退出循环
            if (onRepeatExecute(bean) || !isRunning)
                break;
            try {
                interval = Math.max(getRetryInterval(getRetryCount()), 0);
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        // 结束
        if (isRunning)
            onPostExecute(bean);// 正常结束情况下，调用onPostExecute
        else
            onCancelled(bean);// 被中断情况下，调用onCancelled
    }
}
