package com.tj.xengine.android.toolkit.handler;

import android.os.Handler;
import android.os.Message;
import com.tj.xengine.core.toolkit.switcher.XBooleanSwitcher;

/**
 * <pre>
 * 延迟响应处理的Handler。
 * 在没有调用startHandle()前，该Handler可以缓存所有发到该Handler的消息，
 * 并在调用startHandle()后开始顺序处理所有缓存的消息。
 * 该类可以应用在Service中需要等待初始化后才能响应外部intent调用的情况下，
 * 作为缓存外部Intent请求的队列。
 * User: jasontujun
 * Date: 15-1-27
 * Time: 下午4:37
 * </pre>
 */
public class XLazyHandler implements XHandler {

    private final XBooleanSwitcher mSwitcher;// 开关
    private XDeamonHandler mDeamonHandler;// 后台Handler

    public XLazyHandler(String name, Handler.Callback callback) {
        mSwitcher = new XBooleanSwitcher(false);
        mDeamonHandler = new XDeamonHandler(name, new InnerCallback(callback));
    }

    /**
     * 打开开关，开始处理消息(解除Handler中的阻塞等待状态)
     */
    public void startHandle() {
        if (mSwitcher.isOn())
            return;

        synchronized (mSwitcher) {
            mSwitcher.turnOn();
            mSwitcher.notifyAll();
        }
    }

    @Override
    public Handler getHandler() {
        return mDeamonHandler.getHandler();
    }

    @Override
    public boolean isAlive() {
        return mDeamonHandler.isAlive();
    }

    @Override
    public boolean quit() {
        return mDeamonHandler.quit();
    }

    /**
     * 内部callback优先处理消息，处理过程中会阻塞的等待开关打开。
     * 打开后会回调真正的callback去除处理消息。
     */
    private class InnerCallback implements Handler.Callback {

        private Handler.Callback originCallback;

        public InnerCallback(Handler.Callback originCallback) {
            this.originCallback = originCallback;
        }

        @Override
        public boolean handleMessage(Message message) {
            // 等待开关打开
            synchronized (mSwitcher) {
                try {
                    while (!mSwitcher.isOn()) {
                        mSwitcher.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 真正开始处理消息
            return originCallback == null || originCallback.handleMessage(message);
        }
    }
}
