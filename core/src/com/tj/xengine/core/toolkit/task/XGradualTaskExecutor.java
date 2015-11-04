package com.tj.xengine.core.toolkit.task;

/**
 * <pre>
 * 渐变式任务执行器的抽象类。
 * 相比于XBaseTaskExecutor，在TODO和DOING之间多了2个渐变状态:STARTING和PAUSING。
 * 适用于启动和暂停时需要长时间异步操作的任务
 * 注意:
 * 1.状态机：有六个状态TODO,DOING,DONE,ERROR,STARTING,PAUSING
 *           和七个行为start,startFinish,pause,pauseFinish,abort,endSuccess,endError
 *      start = TODO/ERROR -> STARTING
 *      startFinish = STARTING -> DOING
 *      pause = DOING/STARTING -> PAUSING
 *      pauseFinish = PAUSING -> TODO
 *      abort = TODO/DOING/STARTING -> DONE
 *      endSuccess = DOING -> DONE
 *      endError = DOING/STARTING -> ERROR
 * 2.子类继承时，重写五个行为的回调方法即可：
 *      onStart(),onPause(),onAbort(),onEndSuccess(),onEndError()
 * 3.不允许在onStart()等5个自定义回调方法中，同步调用start()等7个行为方法；
 * User: jasontujun
 * Date: 13-9-27
 * Time: 上午10:03
 * </pre>
 */
public abstract class XGradualTaskExecutor<B extends XTaskBean>
        extends XBaseTaskExecutor<B> {

    private Integer mPostPauseStatus;// 暂停后的外部设置值

    public XGradualTaskExecutor(B bean) {
        super(bean);
    }

    public XGradualTaskExecutor(B bean, int status) {
        super(bean, status);
    }

    @Override
    public final boolean start(int... preStatus) {
        synchronized (this) {
            if (getStatus() != XTaskBean.STATUS_TODO
                    && getStatus() != XTaskBean.STATUS_ERROR
                    && (preStatus.length == 0
                    || getStatus() != preStatus[0]))
                return false;

            if (!onStart()) // 启动失败，直接结束
                return false;

            // 设置成STARTING状态，并回调
            setStatus(XTaskBean.STATUS_STARTING);
        }
        if (getListener() != null)
            getListener().onStart(getBean());
        return true;
    }

    @Override
    public final boolean pause(int... postStatus) {
        synchronized (this) {
            if (getStatus() != XTaskBean.STATUS_DOING
                    && getStatus() != XTaskBean.STATUS_STARTING)
                return false;

            if (postStatus.length > 0) {
                mPostPauseStatus = postStatus[0];
            }
            if (!onPause())// 暂停失败，直接结束
                return false;

            // 设置成PAUSING状态，并回调
            setStatus(XTaskBean.STATUS_PAUSING);
        }
        // PAUSING算是DOING的一种特殊状态，所以调用onDoing()来回调监听
        notifyDoing(-1);
        return true;
    }

    @Override
    public final boolean abort() {
        synchronized (this) {
            if (getStatus() != XTaskBean.STATUS_TODO
                    && getStatus() != XTaskBean.STATUS_DOING
                    && getStatus() != XTaskBean.STATUS_STARTING)
                return false;

            if (!onAbort())
                return false;

            setStatus(XTaskBean.STATUS_DONE);
        }
        if (getListener() != null)
            getListener().onAbort(getBean());
        return true;
    }

    public final boolean endSuccess() {
        synchronized (this) {
            if (getStatus() != XTaskBean.STATUS_DOING)
                return false;

            if (!onEndSuccess())
                return false;

            setStatus(XTaskBean.STATUS_DONE);
        }
        if (getListener() != null)
            getListener().onComplete(getBean());
        return true;
    }

    public final boolean endError(String errorCode, boolean retry) {
        synchronized (this) {
            if (getStatus() != XTaskBean.STATUS_DOING
                    && getStatus() != XTaskBean.STATUS_STARTING)
                return false;

            if (!onEndError(errorCode, retry))
                return false;

            setStatus(XTaskBean.STATUS_ERROR);
        }
        if (getListener() != null)
            getListener().onError(getBean(), errorCode, retry);
        return true;
    }

    /**
     * 启动完成时调用此方法，此方法会把状态改成STATUS_DOING。
     * 注意:此方法不可以在onStart()中调用，只允许在onStart()外或异步线程中调用。
     * @return
     */
    public final boolean startFinish() {
        synchronized (this) {
            if (getStatus() != XTaskBean.STATUS_STARTING)
                return false;

            setStatus(XTaskBean.STATUS_DOING);
        }
        notifyDoing(-1);
        return true;
    }

    /**
     * 暂停完成时调用此方法，此方法会把状态改成STATUS_TODO。
     * 注意:此方法不可以在onPause()中同步调用，只允许在onPause()外或异步线程中调用。
     * @return
     */
    public final boolean pauseFinish() {
        synchronized (this) {
            if (getStatus() != XTaskBean.STATUS_PAUSING)
                return false;

            if (mPostPauseStatus != null) {
                setStatus(mPostPauseStatus);
                mPostPauseStatus = null;
            } else {
                setStatus(XTaskBean.STATUS_TODO);
            }
        }
        if (getListener() != null)
            getListener().onPause(getBean());
        return true;
    }

}
