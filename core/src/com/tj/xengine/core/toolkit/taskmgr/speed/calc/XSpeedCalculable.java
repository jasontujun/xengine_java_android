package com.tj.xengine.core.toolkit.taskmgr.speed.calc;

/**
 * <pre>
 * 可计算速度的接口
 * User: jasontujun
 * Date: 14-2-13
 * Time: 上午11:26
 * </pre>
 */
public interface XSpeedCalculable {

    /**
     * 获取对象的已完成大小(单位：byte)，用于计算速度
     * @return 返回已完成大小
     */
    long getCompleteSize();

    /**
     * 设置任务的速度计算器
     * @param speedCalculator
     */
    void setSpeedCalculator(XSpeedCalculator speedCalculator);

    /**
     * 获取对象的速度计算器
     * @return
     */
    XSpeedCalculator getSpeedCalculator();
}
