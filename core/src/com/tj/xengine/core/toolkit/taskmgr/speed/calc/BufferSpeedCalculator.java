package com.tj.xengine.core.toolkit.taskmgr.speed.calc;

/**
 * <pre>
 * 带缓存的速度计算器。
 * 取最近N次速度的平均值作为当前速度。
 * User: jasontujun
 * Date: 14-3-10
 * Time: 下午6:54
 * </pre>
 */
public class BufferSpeedCalculator implements XSpeedCalculator {

    public static final int DEFAULT_BUFFER_SIZE = 5;

    private int bufferSize = 5;// 缓存数组大小
    private int contentSize;// 缓存数组中元素个数
    private long[] recentSize;// 大小数组
    private long[] recentTime;// 时间数组
    private int curIndex;// 下一个元素的下标

    public BufferSpeedCalculator() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public BufferSpeedCalculator(int bSize) {
        if (bSize < 1)
            bufferSize = DEFAULT_BUFFER_SIZE;
        else
            bufferSize = bSize;
        recentSize = new long[bufferSize];
        recentTime = new long[bufferSize];
        curIndex = 0;
        contentSize = 0;
    }

    @Override
    public void clear() {
        curIndex = 0;
        contentSize = 0;
    }

    @Override
    public long getSpeed(long curSize) {
        long speed = 0;
        long curTime = System.currentTimeMillis();
        if (contentSize > 0) {// 如果当前有
            int firstIndex = (curIndex - contentSize + bufferSize) % bufferSize;
            long firstSize = recentSize[firstIndex];
            long firstTime = recentTime[firstIndex];
            long deltaSize = curSize - firstSize;// 单位：byte
            double deltaTime = (curTime - firstTime) / 1000.0;// 单位：秒
            speed = (long) (deltaSize / deltaTime);// 单位：byte/s
        }
        recentSize[curIndex] = curSize;
        recentTime[curIndex] = curTime;
        curIndex = (curIndex + 1) % bufferSize;// 下标前移
        contentSize = Math.min(contentSize + 1, bufferSize);// 递增元素个数
        return speed;
    }
}
