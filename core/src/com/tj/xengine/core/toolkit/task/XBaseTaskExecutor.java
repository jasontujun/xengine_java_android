package com.tj.xengine.core.toolkit.task;

/**
 * <pre>
 * 普通任务执行器的抽象类。
 * 封装了一个任务的核心状态机逻辑。
 * 注意:
 * 1.状态机：有四个状态TODO,DOING,DONE,ERROR
 *           和五个行为start,pause,abort,endSuccess,endError
 *      start = TODO/ERROR -> DOING
 *      pause = DOING -> TODO
 *      abort = TODO/DOING -> DONE
 *      endSuccess = DOING -> DONE
 *      endError = DOING -> ERROR
 * 2.子类继承时，重写五个行为的回调方法即可：
 *      onStart(),onPause(),onAbort(),onEndSuccess(),onEndError()
 * 3.不允许在onStart()等5个自定义回调方法中，同步调用start()等5个行为方法；
 * User: jasontujun
 * Date: 13-9-27
 * Time: 上午10:03
 * </pre>
 */
public abstract class XBaseTaskExecutor<B extends XTaskBean>
        implements XTaskExecutor<B> {

    private volatile int mStatus;// 状态
    private B mBean;// 任务数据
    private XTaskListener<B> mListener;

    public XBaseTaskExecutor(B bean) {
        mBean = bean;
        mStatus = bean.getStatus();
    }

    public XBaseTaskExecutor(B bean, int status) {
        mBean = bean;
        mStatus = status;
    }

    @Override
    public B getBean() {
        return mBean;
    }

    @Override
    public String getId() {
        return mBean.getId();
    }

    @Override
    public synchronized void setStatus(int status) {
        mStatus = status;
        mBean.setStatus(status);
    }

    @Override
    public int getStatus() {
        return mStatus;
    }

    @Override
    public void setListener(XTaskListener<B> listener) {
        mListener = listener;
    }

    @Override
    public XTaskListener<B> getListener() {
        return mListener;
    }

    @Override
    public boolean start(int... preStatus) {
        synchronized (this) {
            if (getStatus() != XTaskBean.STATUS_TODO
                    && getStatus() != XTaskBean.STATUS_ERROR
                    && (preStatus.length == 0
                    || getStatus() != preStatus[0]))
                return false;

            if (!onStart())
                return false;

            setStatus(XTaskBean.STATUS_DOING);
        }
        if (mListener != null)
            mListener.onStart(getBean());
        return true;
    }

    @Override
    public boolean pause(int... postStatus) {
        synchronized (this) {
            if (getStatus() != XTaskBean.STATUS_DOING)
                return false;

            if (!onPause())
                return false;

            if (postStatus.length > 0) {
                setStatus(postStatus[0]);
            } else {
                setStatus(XTaskBean.STATUS_TODO);
            }
        }
        if (mListener != null)
            mListener.onPause(getBean());
        return true;
    }

    @Override
    public boolean abort() {
        synchronized (this) {
            if (getStatus() != XTaskBean.STATUS_TODO
                    && getStatus() != XTaskBean.STATUS_DOING)
                return false;

            if (!onAbort())
                return false;

            setStatus(XTaskBean.STATUS_DONE);
        }
        if (mListener != null)
            mListener.onAbort(getBean());
        return true;
    }

    public boolean endSuccess() {
        synchronized (this) {
            if (getStatus() != XTaskBean.STATUS_DOING)
                return false;

            if (!onEndSuccess())
                return false;

            setStatus(XTaskBean.STATUS_DONE);
        }
        if (mListener != null)
            mListener.onComplete(getBean());
        return true;
    }

    public boolean endError(String errorCode, boolean retry) {
        synchronized (this) {
            if (getStatus() != XTaskBean.STATUS_DOING)
                return false;

            if (!onEndError(errorCode, retry))
                return false;

            setStatus(XTaskBean.STATUS_ERROR);
        }
        if (mListener != null)
            mListener.onError(getBean(), errorCode, retry);
        return true;
    }

    /**
     * 通知外部任务正在执行的进度
     */
    public void notifyDoing(long completeSize) {
        if (mListener != null)
            mListener.onDoing(getBean(), completeSize);
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

}
