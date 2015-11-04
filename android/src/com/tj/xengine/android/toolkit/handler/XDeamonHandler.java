package com.tj.xengine.android.toolkit.handler;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

/**
 * <pre>
 * 后台Handler。即运行在非UI线程的Handler。
 * 该类可以应用在UI线程初始化工作比较繁重耗时情况下，
 * 作为后台操作队列来顺序异步执行初始化工作，优化主线程性能。
 * User: jasontujun
 * Date: 15-1-27
 * Time: 下午3:06
 * </pre>
 */
public class XDeamonHandler implements XHandler {

    private HandlerThread mDeamonThread;
    private Handler mDeamonHandler;

    public XDeamonHandler(String name, Handler.Callback callback) {
        mDeamonThread = new HandlerThread(name);
        mDeamonThread.start();
        mDeamonHandler = new Handler(mDeamonThread.getLooper(), callback);
    }

    public XDeamonHandler(String name) {
        this(name, new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return true;
            }
        });
    }

    @Override
    public Handler getHandler() {
        return mDeamonHandler;
    }

    @Override
    public boolean isAlive() {
        return mDeamonThread.isAlive();
    }

    @Override
    public boolean quit() {
        if (Build.VERSION.SDK_INT >= 18) {
            return mDeamonThread.quitSafely();
        } else {
            return mDeamonThread.quit();
        }
    }
}
