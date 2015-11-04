package com.tj.xengine.core.toolkit.taskmgr;


import com.tj.xengine.core.toolkit.task.XGradualTaskExecutor;
import com.tj.xengine.core.toolkit.task.XTaskBean;
import com.tj.xengine.core.toolkit.taskmgr.speed.calc.XSpeedCalculator;

/**
 * <pre>
 * 继承自XGradualTaskExecutor的抽象类。
 * XGradualMgrTaskExecutor代表可以添加进TaskMgr的任务。
 * </pre>
 */
public abstract class XGradualMgrTaskExecutor<B extends XTaskBean>
        extends XGradualTaskExecutor<B> implements XMgrTaskExecutor<B> {

    private XSpeedCalculator mSpeedCalculator;// 速度计算器
    private XTaskMgr<XMgrTaskExecutor<B>, B> mTaskMgr;// 任务管理器

    public XGradualMgrTaskExecutor(B bean) {
        super(bean);
    }

    public XGradualMgrTaskExecutor(B bean, int status) {
        super(bean, status);
    }

    @Override
    public void setTaskMgr(XTaskMgr<XMgrTaskExecutor<B>, B> taskMgr) {
        mTaskMgr = taskMgr;
    }

    @Override
    public XTaskMgr<XMgrTaskExecutor<B>, B> getTaskMgr() {
        return mTaskMgr;
    }

    @Override
    public void setSpeedCalculator(XSpeedCalculator speedCalculator) {
        mSpeedCalculator = speedCalculator;
    }

    @Override
    public XSpeedCalculator getSpeedCalculator() {
        return mSpeedCalculator;
    }
}
