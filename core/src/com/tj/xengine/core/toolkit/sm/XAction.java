package com.tj.xengine.core.toolkit.sm;


/**
 * 动作的接口。
 * 表示状态机中的动作元素。
 * 动作在执行前状态机需要满足前置状态，执行后需要让状态机进入后置状态。
 * Created by jasontujun on 2015/3/21.
 */
public interface XAction {

    /**
     * 前置条件被满足时，会调用此方法，此方法中包含实际执行的操作。
     * 状态机会根据该方法的返回值，判断是否进入该action指定的后置状态，
     * 如果返回true状态机"可能"将会进入后置状态
     * (也有可能在执行act()过程中状态机被暂停或终止，状态机就不会进入后置状态)，
     * 如果返回false状态机的当前状态不会改变。
     * @return 返回动作执行结果，成功返回true，失败返回false。
     */
	boolean act();

    /**
     * 前置条件不被满足时，会调用此方法，此方法中做一些被拒绝的处理。
     */
    void reject();

    /**
     * 获取该动作的前置状态
     * @return 返回该动作的前置状态
     */
	String getPreState();

    /**
     * 获取该动作的后置状态
     * @return 返回该动作的后置状态
     */
	String getPostState();
}
