package com.tj.xengine.core.toolkit.taskmgr.speed.calc;

/**
 * <pre>
 * 速度计算器的接口。
 * User: jasontujun
 * Date: 13-10-22
 * Time: 下午3:15
 * </pre>
 */
public interface XSpeedCalculator {

    /**
     * 清空缓存数据。
     * 一般在速度监控终止时调用。
     */
    void clear();

    /**
     * 获取当前速度。
     * @param size 当前的文件大小（单位：byte）
     * @return 返回当前速度（单位：byte/s）
     */
    long getSpeed(long size);
}
