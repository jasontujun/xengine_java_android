package com.tj.xengine.core.toolkit.taskmgr.speed.calc;

/**
 * <pre>
 * 速度计算器的默认实现类。
 * 简单的基于相邻两次差值和相邻两次时间差计算出的速度。
 * User: jasontujun
 * Date: 13-10-22
 * Time: 下午3:21
 * </pre>
 */
public class DefaultSpeedCalculator implements XSpeedCalculator {

    private long mLastSize;// 上次记录的大小
    private long mLastTime;// 上次记录的时间

    @Override
    public void clear() {
        mLastSize = 0;
        mLastTime = 0;
    }

    @Override
    public long getSpeed(long size) {
        // 如果上次的大小小于
        if (size < mLastSize)
            mLastSize = size;

        long curTime = System.currentTimeMillis();
        long deltaSize = size - mLastSize;// 单位：byte
        double deltaTime = (curTime - mLastTime) / 1000.0;// 单位：秒
        long speed = (long) (deltaSize / deltaTime);// 单位：byte/s
        mLastSize = size;
        mLastTime = curTime;
        return speed;
    }
}
