package com.tj.xengine.core.toolkit.taskmgr;

import com.tj.xengine.core.toolkit.task.XBaseTaskExecutor;
import com.tj.xengine.core.toolkit.task.XTaskBean;
import com.tj.xengine.core.toolkit.taskmgr.speed.calc.XSpeedCalculator;

/**
 * <pre>
 * 继承自XBaseTaskExecutor的抽象类。
 * XBaseMgrTaskExecutor代表可以添加进TaskMgr的任务。
 * </pre>
 */
public abstract class XBaseMgrTaskExecutor<B extends XTaskBean>
        extends XBaseTaskExecutor<B> implements XMgrTaskExecutor<B> {

    private XSpeedCalculator mSpeedCalculator;// 速度计算器
    private XTaskMgr<XMgrTaskExecutor<B>, B> mTaskMgr;// 任务管理器

    public XBaseMgrTaskExecutor(B bean) {
        super(bean);
    }

    public XBaseMgrTaskExecutor(B bean, int status) {
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
