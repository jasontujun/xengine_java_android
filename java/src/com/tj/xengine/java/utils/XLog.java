package com.tj.xengine.java.utils;

/**
 * 全局log类，简单封装一下，可以手动开关各个log级别的日志。
 * Created by 赵之韵.
 * Email: ttxzmorln@163.com
 * Date: 11-11-13
 * Time: 下午4:17
 */
public abstract class XLog {

    /**
     * 是否打开的标志
     */
    private static boolean enabled = true;

    /**
     * 设置是否开启
     * @param enable
     */
    public static void setEnabled(boolean enable) {
        enabled = enable;
    }


    public static void log(String tag, String msg) {
        if(enabled) {
            System.out.println("[" + tag + "]" + msg);
        }
    }
}
