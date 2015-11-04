package com.tj.xengine.android.utils;

import android.util.Log;

/**
 * 全局log类，简单封装一下，可以手动开关各个log级别的日志。
 * Created by 赵之韵.
 * Email: ttxzmorln@163.com
 * Date: 11-11-13
 * Time: 下午4:17
 */
public abstract class XLog {

    /**
     * 是否打开Debug的标志
     */
    private static boolean isDebugEnabled = true;

    /**
     * 是否打开Info的标志
     */
    private static boolean isInfoEnabled = true;

    /**
     * 是否打开Error的标志
     */
    private static boolean isErrorEnabled = true;

    /**
     * 设置是否开启Debug
     * @param enable
     */
    public static void setDebugEnabled(boolean enable) {
        isDebugEnabled = enable;
    }

    /**
     * 设置是否开启Info
     * @param enable
     */
    public static void setInfoEnabled(boolean enable) {
        isInfoEnabled = enable;
    }

    /**
     * 设置是否开启Error
     * @param enable
     */
    public static void setErrorEnabled(boolean enable) {
        isErrorEnabled = enable;
    }

    /**
     * 等同于Log.i
     */
    public static void i(String tag, String msg) {
        if(isInfoEnabled) {
            Log.i(tag, msg);
        }
    }

    /**
     * 等同于Log.d
     */
    public static void d(String tag, String msg) {
        if(isDebugEnabled) {
            Log.d(tag, msg);
        }
    }

    /**
     * 等同于Log.e
     */
    public static void e(String tag, String msg) {
        if(isErrorEnabled) {
            Log.e(tag, msg);
        }
    }
}
