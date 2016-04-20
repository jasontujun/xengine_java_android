package com.tj.xengine.core.toolkit.sm;

import java.util.List;

/**
 * 通用状态机的接口。
 * T代表State的类型。
 * Created by jasontujun on 2015/3/21.
 */
public interface XStateMachine<T> {

    /**
     * 初始化状态机。
     * 如果没有指定endState，则状态机永远不会进入终止状态。
     * @param startState 起始状态
     * @param endState 终止状态(可以为空)
     * @param states 状态机中所有的状态
     */
	boolean init(T startState, T endState, T[] states);

    /**
     * 终止状态机。停止并将状态设置成结束状态。
     */
    void end();

    /**
     * 重启状态机。停止并将状态设置成初始状态
     */
    void reset();

    /**
     * 触发单个动作，从而尝试让状态机进入下一个状态。
     * 如果状态机已达到终止状态，意味着状态机已停止，则action会被直接忽略。
     * @param action 单个动作
     */
	boolean act(XAction<T> action);

    /**
     * 触发一系列动作，从而尝试让状态机进入下一个状态。
     * 多个动作会按照List中的顺序被依次执行，
     * 但中间任意action执行失败，不会影响后门的action被执行。
     * 如果状态机已达到终止状态，意味着状态机已停止，则后续的action会被直接忽略。
     * @param actions 动作链
     */
    boolean act(List<XAction<T>> actions);

    /**
     * 获取当前状态。
     * @return 返回当前状态，有可能为null。
     */
	T getCurrentState();
}
