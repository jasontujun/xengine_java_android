package com.tj.xengine.core.toolkit.task;

/**
 * <pre>
 * “可进化”的任务执行器的抽象类。
 * 该类本身具有一个状态机，当“进化”后，原有状态机变成目标XTaskExecutor对象的状态机。
 * 该类适用于以下场景：
 *      1.无法一开始就确定具体执行方式，需要远程服务器或耗时操作才能确定不同执行方式的任务。
 *      2.任务本身业务逻辑比较复杂，可在执行过程中动态变化成另外一种执行逻辑：
 *        比如下载同一个文件的过程中，可以动态切换成http下载和p2p下载。
 * User: jasontujun
 * Date: 15-1-19
 * Time: 下午5:00
 * </pre>
 */
public abstract class XEvolvableTaskExecutor<B extends XTaskBean>
        extends XBaseTaskExecutor<B> {

    private volatile XBaseTaskExecutor<B> mEvolvedTask;

    public XEvolvableTaskExecutor(B bean) {
        super(bean);
    }

    public XEvolvableTaskExecutor(B bean, int status) {
        super(bean, status);
    }

    /**
     * 进化成另一个任务。
     */
    protected synchronized void evolve(XBaseTaskExecutor<B> evolvedTask) {
        mEvolvedTask = evolvedTask;
        if (mEvolvedTask != null) {
            mEvolvedTask.setListener(new EvolvedListener(getListener()));
        }
    }

    /**
     * 获取进化后的任务对象。
     */
    protected XBaseTaskExecutor<B> getEvolvedTask() {
        return mEvolvedTask;
    }

    @Override
    public synchronized void setStatus(int status) {
        super.setStatus(status);
        if (mEvolvedTask != null)
            mEvolvedTask.setStatus(status);
    }

    @Override
    public void setListener(XTaskListener<B> listener) {
        super.setListener(listener);
        if (mEvolvedTask != null)
            mEvolvedTask.setListener(new EvolvedListener(listener));
    }

    @Override
    public boolean start(int... preStatus) {
        if (mEvolvedTask != null) {
            return mEvolvedTask.start(preStatus);
        } else {
            return super.start(preStatus);
        }
    }

    @Override
    public boolean pause(int... postStatus) {
        if (mEvolvedTask != null) {
            return mEvolvedTask.pause(postStatus);
        } else {
            return super.pause(postStatus);
        }
    }

    @Override
    public boolean abort() {
        if (mEvolvedTask != null) {
            return mEvolvedTask.abort();
        } else {
            return super.abort();
        }
    }

    @Override
    public boolean endSuccess() {
        if (mEvolvedTask != null) {
            return mEvolvedTask.endSuccess();
        } else {
            return super.endSuccess();
        }
    }

    @Override
    public boolean endError(String errorCode, boolean retry) {
        if (mEvolvedTask != null) {
            return mEvolvedTask.endError(errorCode, retry);
        } else {
            return super.endError(errorCode, retry);
        }
    }

    /**
     * 启动任务的回调函数。
     * @return 启动成功返回true;否则返回false
     */
    protected abstract boolean onStart();

    /**
     * 暂停任务的回调函数。
     * @return 暂停成功返回true;否则返回false
     */
    protected abstract boolean onPause();

    /**
     * 终止任务的回调函数。
     * @return 终止成功返回true;否则返回false
     */
    protected abstract boolean onAbort();

    /**
     * 任务成功结束的回调。
     * @return 没有发生异常返回true;否则返回false
     */
    protected abstract boolean onEndSuccess();

    /**
     * 任务失败结束的回调。
     * @param errorCode 错误码
     * @param retry 如果需要重试，则为true；否则为false
     * @return 没有发生异常返回true;否则返回false
     */
    protected abstract boolean onEndError(String errorCode, boolean retry);

    /**
     * 对mEvolvedTask的状态监听，让XEvolvableTaskExecutor和mEvolvedTask的状态时刻保持一致。
     */
    private class EvolvedListener implements XTaskListener<B> {
        private XTaskListener<B> mOriginListener;

        public EvolvedListener(XTaskListener<B> originListener) {
            mOriginListener = originListener;
        }

        @Override
        public void onStart(B task) {
            XEvolvableTaskExecutor.super.setStatus(task.getStatus());
            if (mOriginListener != null)
                mOriginListener.onStart(task);
        }

        @Override
        public void onPause(B task) {
            XEvolvableTaskExecutor.super.setStatus(task.getStatus());
            if (mOriginListener != null)
                mOriginListener.onPause(task);
        }

        @Override
        public void onAbort(B task) {
            XEvolvableTaskExecutor.super.setStatus(task.getStatus());
            if (mOriginListener != null)
                mOriginListener.onAbort(task);
        }

        @Override
        public void onDoing(B task, long completeSize) {
            XEvolvableTaskExecutor.super.setStatus(task.getStatus());
            if (mOriginListener != null)
                mOriginListener.onDoing(task, completeSize);
        }

        @Override
        public void onComplete(B task) {
            XEvolvableTaskExecutor.super.setStatus(task.getStatus());
            if (mOriginListener != null)
                mOriginListener.onComplete(task);
        }

        @Override
        public void onError(B task, String errorCode, boolean retry) {
            XEvolvableTaskExecutor.super.setStatus(task.getStatus());
            if (mOriginListener != null)
                mOriginListener.onError(task, errorCode, retry);
        }
    }

}
