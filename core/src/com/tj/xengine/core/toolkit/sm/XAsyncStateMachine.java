package com.tj.xengine.core.toolkit.sm;

import java.util.List;

/**
 * 通用状态机的接口。
 * Created by jasontujun on 2015/3/21.
 */
public interface XAsyncStateMachine<T> extends XStateMachine<T> {

    /**
     * 状态机的针对状态变化的监听接口
     */
    interface Listener<T> {

        /**
         * 当状态机发生状态转变时，会回调此方法。
         * @param state 状态机转变后最新的状态
         * @param action 导致这个状态发生的动作
         * @param sm 状态机对象实例
         */
        void onState(T state, XAction<T> action, XAsyncStateMachine<T> sm);
    }

    /**
     * 启动状态机。
     */
	void start();

    /**
     * 暂停状态机。
     */
	void pause();

    /**
     * 注册监听
     * @param listener
     */
    void registerListener(Listener<T> listener);

    /**
     * 取消注册监听
     * @param listener
     */
    void unregisterListener(Listener<T> listener);
}
