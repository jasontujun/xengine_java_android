package com.tj.xengine.android.toolkit.handler;

import android.os.Handler;

/**
 * <pre>
 * User: jasontujun
 * Date: 15-1-27
 * Time: 下午4:51
 * </pre>
 */
public interface XHandler {

    /**
     * @return 获取真正的handler对象
     */
    Handler getHandler();

    /**
     * @return 返回handler是否存活。
     */
    boolean isAlive();

    /**
     * 结束handler。
     * @return 结束成功返回true;否则返回false
     */
    boolean quit();
}
