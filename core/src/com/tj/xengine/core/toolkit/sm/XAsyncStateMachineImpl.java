package com.tj.xengine.core.toolkit.sm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 异步执行的通用状态机。
 * Created by jasontujun on 2015/3/21.
 */
public class XAsyncStateMachineImpl<T> implements XAsyncStateMachine<T> {

    private T mStartState;
    private T mEndState;
    private volatile T mCurrentState;// 当前状态
    private final Object currentStateLock = new Object();// 针对mCurrentState的修改
    private Set<T> mStates;// 状态集合
    private WorkerRunnable mActionWorker;// 工作线程(负责等待和执行动作)
    private BlockingQueue<XAction<T>> mActionQueue;// 未执行动作的缓存队列
    private List<Listener<T>> mListeners;// 外部监听者

    private ExecutorService mThreadPool;

    public XAsyncStateMachineImpl() {
        mStates = new HashSet<T>();
        mCurrentState = null;
        mActionWorker = null;
        mActionQueue = new LinkedBlockingQueue<XAction<T>>();
        mListeners = new CopyOnWriteArrayList<Listener<T>>();
    }

    @Override
    public boolean init(T startState,
                        T endState,
                        T[] states) {
        // 如果状态机已启动,则直接返回
        if (mActionWorker != null)
            return false;

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
    public synchronized void start() {
        // 如果状态机已启动,则直接返回
        if(mActionWorker != null)
            return;

        // 如果线程不存在，尝试创建线程
        if (mThreadPool == null) {
            try {
                mThreadPool = Executors.newSingleThreadExecutor();
            } catch (Exception e) {// 未知原因导致无法创建线程池
                e.printStackTrace();
                try {
                    if (mThreadPool != null) {
                        mThreadPool.shutdownNow();
                        mThreadPool = null;
                    }
                } catch (Exception e2) {}
            }
        }
        // 启动状态机工作线程
        mActionWorker = new WorkerRunnable();
        if (mThreadPool != null) {
            mActionWorker.future = mThreadPool.submit(mActionWorker);
        } else {
            new Thread(mActionWorker).start();
        }
    }

    @Override
    public synchronized void pause() {
        // 如果状态机未启动,则直接返回
        if (mActionWorker == null)
            return;

        // 暂停状态机工作线程
        mActionWorker.terminate();
        mActionWorker = null;
    }

    @Override
    public synchronized void end() {
        // 如果状态机工作线程已启动,则先暂停
        if (mActionWorker != null) {
            mActionWorker.terminate();
            mActionWorker = null;
        }
        // 清空动作缓存队列
        mActionQueue.clear();
        // 设置当前状态
        synchronized (currentStateLock) {
            mCurrentState = mEndState;
        }
    }

    @Override
    public synchronized void reset() {
        // 如果状态机工作线程已启动,则先暂停
        if (mActionWorker != null) {
            mActionWorker.terminate();
            mActionWorker = null;
        }
        // 清空动作缓存队列
        mActionQueue.clear();
        // 设置当前状态
        synchronized (currentStateLock) {
            mCurrentState = mStartState;
        }
    }

    @Override
    public synchronized boolean act(XAction<T> action) {
        if (action == null) {
            return false;
        }
        // 加入动作缓存队列，等待被执行，并立即返回
        return mActionQueue.offer(action);
    }

    @Override
    public synchronized boolean act(List<XAction<T>> actions) {
        if (actions == null || actions.size() == 0) {
            return false;
        }
        for (XAction action : actions) {
            // 加入动作缓存队列，等待被执行
            if (!mActionQueue.offer(action))
                return false;
        }
        return true;
    }

    @Override
    public T getCurrentState() {
        return mCurrentState;
    }

    @Override
    public void registerListener(Listener<T> listener) {
        if (!mListeners.contains(listener))
            mListeners.add(listener);
    }

    @Override
    public void unregisterListener(Listener<T> listener) {
        mListeners.remove(listener);
    }

    /**
     * 实际的动作执行线程。
     */
    private class WorkerRunnable implements Runnable {

        private volatile boolean isRunning;
        private volatile Future future;

        public WorkerRunnable() {
            isRunning = true;
        }

        public synchronized void terminate() {
            isRunning = false;
            if (future != null ) {
                future.cancel(true);
                future = null;
            }
        }

        @Override
        public void run() {
            try {
                while (isRunning) {
                    // 阻塞获取下一个action
                    XAction<T> action = mActionQueue.take();
                    final T preState = action.getPreState();
                    final T postState = action.getPostState();
                    // 线程被暂停结束
                    if (!isRunning) {
                        break;
                    }
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
                    boolean result = action.act();
                    synchronized (currentStateLock) {// 与end()和reset()方法进行互斥(针对mCurrentState的修改)
                        // 线程被暂停结束
                        if (!isRunning) {
                            break;
                        }
                        // 一旦action执行成功后，将当前状态改成后置状态
                        if (result) {
                            mCurrentState = postState;
                            // 通知状态的监听者(可以不直接回调，通过消息队列让实际回调在另一个线程)
                            for (Listener<T> listener : mListeners)
                                listener.onState(postState, action, XAsyncStateMachineImpl.this);
                            // 如果后置状态是IState.END，意味着状态机执行完毕，则停止整个状态机
                            if (mEndState != null && mEndState.equals(postState)) {
                                terminate();
                                XAsyncStateMachineImpl.this.pause();
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                // blockingQueue阻塞被中断
                e.printStackTrace();
            }
        }

    }

}
