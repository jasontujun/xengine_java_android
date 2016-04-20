package com.tj.xengine.core.toolkit.sm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 同步执行的通用状态机。
 * Created by jasontujun on 2016/4/20.
 */
public class XSyncStateMachineImpl<T> implements XStateMachine<T> {

    private T mStartState;
    private T mEndState;
    private volatile T mCurrentState;// 当前状态
    private Set<T> mStates;// 状态集合

    public XSyncStateMachineImpl() {
        mStates = new HashSet<T>();
        mCurrentState = null;
    }

    @Override
    public boolean init(T startState,
                        T endState,
                        T[] states) {
        // 清空集合和当前状态
        mStates.clear();
        mCurrentState = null;

        // 初始化状态集合
        mStartState = startState;
        mEndState = endState;
        for (T state : states) {
            if (state != null) {
                mStates.add(state);
            }
        }
        mStates.add(startState);
        mStates.add(endState);

        // 初始化当前状态
        mCurrentState = startState;
        return true;
    }

    @Override
    public synchronized void end() {
        if (mCurrentState == null) {// 状态机未初始化
            return;
        }
        if (mEndState != null) {
            mCurrentState = mEndState;
        }
    }

    @Override
    public synchronized void reset() {
        if (mCurrentState == null) {// 状态机未初始化
            return;
        }
        if (mStartState != null) {
            mCurrentState = mStartState;
        }
    }

    @Override
    public synchronized boolean act(XAction<T> action) {
        if (action == null) {
            return false;
        }
        if (mCurrentState == null) {// 状态机未初始化
            return false;
        }
        if (mEndState != null && mEndState.equals(mCurrentState)) {
            // 状态机已终止
            return false;
        }
        final T preState = action.getPreState();
        final T postState = action.getPostState();
        // 检验该action的前置后置状态是否在此状态机中
        if (!mStates.contains(preState) || !mStates.contains(postState)) {
            action.reject();// 通知action的监听者该action不执行
            return false;
        }
        // 检验该action的前置状态是否满足
        if (preState == null || mCurrentState == null ||
                !action.getPreState().equals(mCurrentState)) {
            action.reject();// 通知action的监听者该action不执行
            return false;
        }
        // 执行action的实际操作(默认为阻塞执行)
        boolean result = action.act();
        // 一旦action执行成功后，将当前状态改成后置状态
        if (result) {
            mCurrentState = postState;
        }
        return result;
    }

    @Override
    public synchronized boolean act(List<XAction<T>> actions) {
        if (actions == null || actions.size() == 0) {
            return false;
        }
        if (mCurrentState == null) {// 状态机未初始化
            return false;
        }
        T preState;
        T postState;
        boolean result = false;
        for (XAction<T> action : actions) {
            if (mEndState != null && mEndState.equals(mCurrentState)) {
                // 状态机已终止
                return result;
            }
            preState = action.getPreState();
            postState = action.getPostState();
            // 检验该action的前置后置状态是否在此状态机中
            if (!mStates.contains(preState) || !mStates.contains(postState)) {
                action.reject();// 通知action的监听者该action不执行
                continue;
            }
            // 检验该action的前置状态是否满足
            if (preState == null || mCurrentState == null ||
                    !action.getPreState().equals(mCurrentState)) {
                action.reject();// 通知action的监听者该action不执行
                continue;
            }
            // 执行action的实际操作(默认为阻塞执行)
            result = action.act();
            // 一旦action执行成功后，将当前状态改成后置状态
            if (result) {
                mCurrentState = postState;
            }
        }
        return result;
    }

    @Override
    public T getCurrentState() {
        return mCurrentState;
    }

}
