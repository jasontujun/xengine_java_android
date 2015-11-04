package com.tj.xengine.core.toolkit.taskmgr.parallel;

import com.tj.xengine.core.toolkit.filter.XFilter;
import com.tj.xengine.core.toolkit.task.XTaskBean;
import com.tj.xengine.core.toolkit.task.XTaskListener;
import com.tj.xengine.core.toolkit.taskmgr.XMgrTaskExecutor;
import com.tj.xengine.core.toolkit.taskmgr.XTaskMgrListener;
import com.tj.xengine.core.toolkit.taskmgr.XTaskScheduler;
import com.tj.xengine.core.toolkit.taskmgr.speed.XSpeedMonitor;
import com.tj.xengine.core.toolkit.taskmgr.speed.calc.DefaultSpeedCalculator;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <pre>
 * 并行执行器，实现ParallelMgr接口的子类。
 * 封装了并行执行、增删任务、启动、恢复、暂停、速度监视等操作。
 * 1.每个时刻，有一个或多个(不超过设置上限)任务正在执行；
 * 2.每个任务都在TODO,DOING,DONE,ERROR四个状态间转换；
 * 3.如果任务从DOING到ERROR，回调SerialMgr时，
 *   会将该任务丢弃，继续执行下一个；
 * 4.如果任务从DOING到TODO，回调SerialMgr时，
 *   会将该任务重新添加进等待队列，不继续执行；
 * @see com.tj.xengine.core.toolkit.task.XTaskExecutor
 * User: tujun
 * Date: 13-8-1
 * Time: 下午3:35
 * </pre>
 */
public class XParallelMgrImpl<B extends XTaskBean> implements XParallelMgr<B> {

    protected volatile boolean mIsWorking;// 标识运行状态
    protected volatile boolean mAuto;// 标识是否自动执行
    protected LinkedList<XMgrTaskExecutor<B>> mCurrentExecuted;// 正在运行的任务队列
    protected LinkedList<XMgrTaskExecutor<B>> mTobeExecuted;// 待执行的任务队列
    protected XTaskScheduler<B> mScheduler;// 任务排序器(外部设置)
    protected XFilter<B> mFilter;// 任务过滤器
    protected InnerTaskComparator mInnerComparator;// 实际用来排序的比较器
    protected XSpeedMonitor<XMgrTaskExecutor<B>> mSpeedMonitor;// 速度监视器
    protected List<XTaskMgrListener<B>> mListeners;// 外部监听者
    protected XTaskListener<B> mInnerTaskListener;// 内部管理器对每个Task的监听
    protected int mParallelLimit;// 并行任务的数量上限

    public XParallelMgrImpl(int parallelLimit) {
        mParallelLimit = Math.max(parallelLimit, 1);
        mCurrentExecuted = new LinkedList<XMgrTaskExecutor<B>>();
        mTobeExecuted = new LinkedList<XMgrTaskExecutor<B>>();
        mInnerComparator = new InnerTaskComparator();
        mListeners = new CopyOnWriteArrayList<XTaskMgrListener<B>>();
        mIsWorking = false;
        mAuto = true;
        mInnerTaskListener = new XTaskListener<B>() {
            @Override
            public void onStart(B task) {
                for (XTaskMgrListener<B> listener : mListeners)
                    listener.onStart(task);
            }

            @Override
            public void onPause(B task) {
                for (XTaskMgrListener<B> listener : mListeners)
                    listener.onStop(task);
            }

            @Override
            public void onAbort(B task) {}

            @Override
            public void onDoing(B task, long completeSize) {
                for (XTaskMgrListener<B> listener : mListeners)
                    listener.onDoing(task, completeSize);
            }

            @Override
            public void onComplete(B task) {
                for (XTaskMgrListener<B> listener : mListeners)
                    listener.onComplete(task);

                XMgrTaskExecutor<B> taskExecutor = getTaskById(task.getId());
                if (taskExecutor != null)
                    notifyTaskFinished(taskExecutor, false);
            }

            @Override
            public void onError(B task, String errorCode, boolean retry) {
                for (XTaskMgrListener<B> listener : mListeners)
                    listener.onError(task, errorCode);

                XMgrTaskExecutor<B> taskExecutor = getTaskById(task.getId());
                if (taskExecutor != null)
                    notifyTaskFinished(taskExecutor, retry);
            }
        };
    }

    @Override
    public boolean isEmptyParallel() {
        return mCurrentExecuted.size() == 0;
    }

    @Override
    public boolean isFullParallel() {
        return mCurrentExecuted.size() >= mParallelLimit;
    }

    @Override
    public boolean isAllStop() {
        for (XMgrTaskExecutor<B> task : mCurrentExecuted) {
            if (task.getStatus() == XTaskBean.STATUS_DOING
                    || task.getStatus() == XTaskBean.STATUS_STARTING)
                return false;
        }
        return true;
    }

    /**
     * 如果所有任务都是暂停状态，
     * 则设置运行标识为false，停止速度监视器
     * @return 如果所有任务都是暂停状态，则返回true;否则返回false
     */
    private boolean setStopIfAllStop() {
        if (!isAllStop())
            return false;

        if (mSpeedMonitor != null)
            mSpeedMonitor.stop();
        mIsWorking = false;
        return true;
    }

    @Override
    public String getTaskId(XMgrTaskExecutor<B> task) {
        return task.getId();
    }

    @Override
    public XMgrTaskExecutor<B> getTaskById(String id) {
        if (id == null)
            return null;

        for (XMgrTaskExecutor<B> task : mCurrentExecuted) {
            if (id.equals(getTaskId(task)))
                return task;
        }
        for (XMgrTaskExecutor<B> task : mTobeExecuted) {
            if (id.equals(getTaskId(task)))
                return task;
        }
        return null;
    }

    @Override
    public synchronized boolean addTask(XMgrTaskExecutor<B> task) {
        if (getTaskById(getTaskId(task)) != null)// 判断是否重复
            return false;

        task.setTaskMgr(this);
        task.setListener(mInnerTaskListener);
        task.setStatus(XTaskBean.STATUS_TODO);
        if (task.getSpeedCalculator() == null) // 若没有速度计算器，则设置默认的
            task.setSpeedCalculator(new DefaultSpeedCalculator());
        mTobeExecuted.offer(task);
        for (XTaskMgrListener<B> listener : mListeners)
            listener.onAdd(task.getBean());

        return true;
    }

    @Override
    public synchronized void addTasks(List<XMgrTaskExecutor<B>> tasks) {
        if (tasks == null || tasks.size() == 0)
            return;

        List<B> added = new ArrayList<B>();
        for (XMgrTaskExecutor<B> task : tasks) {
            if (task == null)
                continue;
            if (getTaskById(getTaskId(task)) != null)// 判断是否重复
                continue;
            added.add(task.getBean());
            task.setTaskMgr(this);
            task.setListener(mInnerTaskListener);
            task.setStatus(XTaskBean.STATUS_TODO);
            if (task.getSpeedCalculator() == null)
                task.setSpeedCalculator(new DefaultSpeedCalculator());
            mTobeExecuted.offer(task);
        }
        if (added.size() > 0)
            for (XTaskMgrListener<B> listener : mListeners)
                listener.onAddAll(added);
    }

    @Override
    public synchronized void removeTask(XMgrTaskExecutor<B> task) {
        if (task == null)
            return;

        task.abort();// 终止当前任务
        boolean isRemoved;
        if (mCurrentExecuted.contains(task)) {// 如果要删除的任务在执行队列中
            isRemoved = mCurrentExecuted.remove(task);
        } else {
            isRemoved = mTobeExecuted.remove(task);
        }
        if (setStopIfAllStop()) {// 如果当前没有任务运行，则标记结束
            for (XTaskMgrListener<B> listener : mListeners)
                listener.onStopAll();
        }
        if (isRemoved)
            for (XTaskMgrListener<B> listener : mListeners)
                listener.onRemove(task.getBean());
    }

    @Override
    public synchronized void removeTaskById(String taskId) {
        removeTask(getTaskById(taskId));
    }

    @Override
    public synchronized void removeTasks(List<XMgrTaskExecutor<B>> tasks) {
        if (tasks == null || tasks.size() == 0)
            return;

        List<B> removed = new ArrayList<B>();
        for (XMgrTaskExecutor<B> task : tasks) {
            if (task == null)
                continue;
            task.abort();// 终止任务
            if (mCurrentExecuted.contains(task)) {// 如果要删除的任务在执行队列中
                mCurrentExecuted.remove(task);
                removed.add(task.getBean());
            } else {
                if (mTobeExecuted.remove(task))// 如果删除成功，添加进列表
                    removed.add(task.getBean());
            }
        }
        if (setStopIfAllStop()) {// 如果当前没有任务运行，则标记结束
            for (XTaskMgrListener<B> listener : mListeners)
                listener.onStopAll();
        }
        if (removed.size() > 0)
            for (XTaskMgrListener<B> listener : mListeners)
                listener.onRemoveAll(removed);
    }

    @Override
    public synchronized void removeTasksById(List<String> taskIds) {
        if (taskIds == null || taskIds.size() == 0)
            return;

        List<XMgrTaskExecutor<B>> tasks = new ArrayList<XMgrTaskExecutor<B>>();
        for (String taskId : taskIds) {
            XMgrTaskExecutor<B> task = getTaskById(taskId);
            if (task != null)
                tasks.add(task);
        }
        removeTasks(tasks);
    }

    @Override
    public synchronized void setRunningTask(String taskId) {
        // 如果运行队列已满，则什么都不做
        if (isFullParallel())
            return;

        // 如果运行队列未满，则将指定任务从等待队列添加进运行队列
        XMgrTaskExecutor<B> task = getTaskById(taskId);
        if (!mCurrentExecuted.contains(task) && task != null) {
            mTobeExecuted.remove(task);
            mCurrentExecuted.addLast(task);
        }
    }

    @Override
    public List<XMgrTaskExecutor<B>> getRunningTask() {
//        return new ArrayList<ParallelTask>(mCurrentExecuted);
        // 为了效率起见，牺牲安全性
        return mCurrentExecuted;
    }

    @Override
    public List<XMgrTaskExecutor<B>> getWaitingTask() {
//        return new ArrayList<ParallelTask>(mTobeExecuted);
        // 为了效率起见，牺牲安全性
        return mTobeExecuted;
    }

    @Override
    public synchronized boolean start() {
        // 如果运行队列未满，则启动多个等待队列中的任务直到满
        while (!isFullParallel()) {
            XMgrTaskExecutor<B> task = findNextTask(null);
            if (task == null)
                break;
            // 如果下一个任务是被过滤掉的，说明已经没有可执行的任务了，退出循环
            if (mFilter != null && mFilter.doFilter(task.getBean()) == null)
                break;
            mCurrentExecuted.offer(task);
        }
        // 如果运行队列为空，则什么都不做
        if (isEmptyParallel())
            return false;
        // 启动运行队列的所有任务
        for (XMgrTaskExecutor<B> task : mCurrentExecuted) {
            // 如果过滤掉，则直接启动下一个
            if (mFilter != null && mFilter.doFilter(task.getBean()) == null)
                continue;
            mIsWorking = true;
            task.start();
        }
        if (mIsWorking && mSpeedMonitor != null)
            mSpeedMonitor.start();
        return true;
    }

    @Override
    public synchronized boolean start(String taskId) {
        XMgrTaskExecutor<B> task = getTaskById(taskId);
        // 如果指定Id的任务不存在，则什么都不做，返回false
        if (task == null)
            return false;

        // 如果被过滤掉，则不启动
        if (mFilter != null && mFilter.doFilter(task.getBean()) == null)
            return false;

        // 先尝试启动指定任务
        if (!task.start())
            return false;

        mIsWorking = true;
        // 如果指定Id的任务不在运行队列中
        if (!mCurrentExecuted.contains(task)) {
            // 如果运行队列已满，则替换一个任务
            if (isFullParallel()) {
                XMgrTaskExecutor<B> oldTask = mCurrentExecuted.poll();
                oldTask.pause();
                mTobeExecuted.addFirst(oldTask);// 添加回等待队列
            }
            // 调整指定id的任务所在的队列
            mTobeExecuted.remove(task);
            mCurrentExecuted.addFirst(task);
        }
        if (mSpeedMonitor != null)
            mSpeedMonitor.start();
        return true;
    }

    @Override
    public synchronized boolean resume() {
        if (isEmptyParallel())
            return false;

        for (XMgrTaskExecutor<B> task : mCurrentExecuted) {
            // 如果过滤掉，则直接启动下一个
            if (mFilter != null && mFilter.doFilter(task.getBean()) == null)
                continue;
            mIsWorking = true;
            task.start();
        }
        if (mIsWorking && mSpeedMonitor != null)
            mSpeedMonitor.start();
        return true;
    }

    @Override
    public synchronized boolean resume(String taskId) {
        XMgrTaskExecutor<B> task = getTaskById(taskId);
        // 如果指定的task不存在，则什么都不做，返回false
        if (task == null)
            return false;

        // 如果被过滤掉，则不启动
        if (mFilter != null && mFilter.doFilter(task.getBean()) == null)
            return false;

        // 如果指定Id的任务存在，且在运行队列中，恢复该任务
        if (mCurrentExecuted.contains(task)) {
            mIsWorking = true;
            if (task.start()) {
                if (mSpeedMonitor != null)
                    mSpeedMonitor.start();
            }
            return true;
        }

        // 如果指定Id的任务存在，且在等待队列中，运行队列已满，什么都不做
        if (isFullParallel())
            return false;

        // 如果指定Id的任务存在，且在等待队列中，且运行队列未满，启动该任务
        mIsWorking = true;
        mTobeExecuted.remove(task);
        mCurrentExecuted.offer(task);
        if (task.start()) {
            if (mSpeedMonitor != null)
                mSpeedMonitor.start();
        }
        return true;
    }

    @Override
    public synchronized boolean pause() {
        if (isEmptyParallel())
            return false;
        // 尝试暂停任务
        for (XMgrTaskExecutor<B> task : mCurrentExecuted)
            task.pause();
        if (setStopIfAllStop()) {
            for (XTaskMgrListener<B> listener : mListeners)
                listener.onStopAll();
        }
        return true;
    }

    @Override
    public synchronized boolean pause(String taskId) {
        XMgrTaskExecutor<B> task = getTaskById(taskId);
        // 如果指定Id的任务不存在，或在等待队列中，则什么都不做，返回false
        if (task == null || !mCurrentExecuted.contains(task))
            return false;
        // 如果指定Id的任务存在，且在运行队列中，暂停该任务
        if (!task.pause())
            return false;
        if (setStopIfAllStop()) {
            for (XTaskMgrListener<B> listener : mListeners)
                listener.onStopAll();
        }
        return true;
    }

    @Override
    public synchronized boolean pauseByFilter(XFilter<B> filter) {
        setTaskFilter(filter);// 设置当前的任务过滤器
        List<XMgrTaskExecutor<B>> stopTasks = new ArrayList<XMgrTaskExecutor<B>>();
        for (XMgrTaskExecutor<B> task : mCurrentExecuted) {
            if (filter != null && filter.doFilter(task.getBean()) == null) {
                if (task.pause())
                    stopTasks.add(task);
            }
        }
        // 没有任务暂停成功，则返回false
        if (stopTasks.size() == 0)
            return false;
        if (setStopIfAllStop()) {
            for (XTaskMgrListener<B> listener : mListeners)
                listener.onStopAll();
        }
        return true;
    }

    @Override
    public synchronized boolean stop() {
        if (isEmptyParallel())
            return false;
        // 尝试暂停任务
        List<XMgrTaskExecutor<B>> stopTasks = new ArrayList<XMgrTaskExecutor<B>>();
        for (XMgrTaskExecutor<B> task : mCurrentExecuted) {
            if (task.pause())
                stopTasks.add(task);
        }
        // 没有任务暂停成功，则返回false
        if (stopTasks.size() == 0)
            return false;
        // 添加回等待队列
        mCurrentExecuted.removeAll(stopTasks);
        mTobeExecuted.addAll(0, stopTasks);
        if (setStopIfAllStop()) {
            for (XTaskMgrListener<B> listener : mListeners)
                listener.onStopAll();
        }
        return true;
    }

    @Override
    public synchronized boolean stop(String taskId) {
        XMgrTaskExecutor<B> task = getTaskById(taskId);
        // 如果指定Id的任务不存在，或在等待队列中，则什么都不做，返回false
        if (task == null || !mCurrentExecuted.contains(task))
            return false;
        // 如果指定Id的任务存在，且在运行队列中，暂停该任务(将任务从运行队列移回等待队列)
        if (!task.pause())
            return false;
        // 添加回等待队列
        mCurrentExecuted.remove(task);
        mTobeExecuted.addFirst(task);
        if (setStopIfAllStop()) {
            for (XTaskMgrListener<B> listener : mListeners)
                listener.onStopAll();
        }
        return true;
    }

    @Override
    public synchronized boolean stopByFilter(XFilter<B> filter) {
        setTaskFilter(filter);// 设置当前的任务过滤器
        List<XMgrTaskExecutor<B>> stopTasks = new ArrayList<XMgrTaskExecutor<B>>();
        for (XMgrTaskExecutor<B> task : mCurrentExecuted) {
            if (filter != null && filter.doFilter(task.getBean()) == null) {
                if (task.pause())
                    stopTasks.add(task);
            }
        }
        // 没有任务暂停成功，则返回false
        if (stopTasks.size() == 0)
            return false;
        // 添加回等待队列
        mCurrentExecuted.removeAll(stopTasks);
        mTobeExecuted.addAll(0, stopTasks);
        if (setStopIfAllStop()) {
            for (XTaskMgrListener<B> listener : mListeners)
                listener.onStopAll();
        }
        return true;
    }

    @Override
    public synchronized void stopAndReset() {
        mIsWorking = false;
        // 终止并清空当前任务
        for (XMgrTaskExecutor<B> task : mCurrentExecuted)
            task.pause();
        mCurrentExecuted.clear();
        // 清空等待队列中的任务
        mTobeExecuted.clear();
        // 停止速度监听
        if (mSpeedMonitor != null)
            mSpeedMonitor.stop();
        // 通知监听者
        for (XTaskMgrListener<B> listener : mListeners)
            listener.onStopAll();
    }

    @Override
    public void setSpeedMonitor(XSpeedMonitor<XMgrTaskExecutor<B>> speedMonitor) {
        mSpeedMonitor = speedMonitor;
    }

    @Override
    public void setTaskFilter(XFilter<B> filter) {
        mFilter = filter;
    }

    @Override
    public XFilter<B> getTaskFilter() {
        return mFilter;
    }

    @Override
    public void setTaskScheduler(XTaskScheduler<B> scheduler) {
        mScheduler = scheduler;
    }

    @Override
    public void setAutoRunning(boolean auto) {
        mAuto = auto;
    }

    @Override
    public boolean isAutoRunning() {
        return mAuto;
    }

    /**
     * 寻找下一个任务。
     * 策略：1.将任务排序，过滤，返回第一个是TODO状态的任务(其他状态的任务忽略)
     *       2.如果没有符合1要求的任务，则返回第一个TODO状态但被过滤的任务
     *       3.如果没有以上的任务，则返回null
     * @return 返回下一个待执行的任务，如果没有可执行的任务，则返回null
     * @see #setTaskScheduler(XTaskScheduler)
     */
    protected XMgrTaskExecutor<B> findNextTask(XMgrTaskExecutor<B> curTask) {
        // 用TaskScheduler排序
        if (mScheduler != null) {
            mInnerComparator.setCurTask(curTask);
            Collections.sort(mTobeExecuted, mInnerComparator);
        }

        // 用TaskFilter过滤，找到第一个是TODO状态的任务
        final XFilter<B> finalFilter = mFilter;
        XMgrTaskExecutor<B> nextTask = null;// 最终的结果，下一个待执行任务
        XMgrTaskExecutor<B> filteredTask = null;// 第一个符合状态但被过滤掉的任务
        for (XMgrTaskExecutor<B> task : mTobeExecuted) {
            // 不是TODO状态的任务，跳过
            if (task.getStatus() != XTaskBean.STATUS_TODO)
                continue;
            // TODO状态，且没被过滤掉的任务
            if ((finalFilter == null || finalFilter.doFilter(task.getBean()) != null)) {
                nextTask = task;
                break;
            }
            // TODO状态，但被过滤掉的任务
            else {
                if (filteredTask == null)
                    filteredTask = task;
            }
        }
        // 如果没有符合的任务，则返回第一个状态正确但被过滤的任务
        if (nextTask == null) {
            nextTask = filteredTask;
        }
        // 如果找到下一个任务，则将其从等待队列中移除
        if (nextTask != null)
            mTobeExecuted.remove(nextTask);
        return nextTask;
    }

    @Override
    public synchronized void notifyTaskFinished(XMgrTaskExecutor<B> task, boolean addBack) {
        if (task == null)
            return;

        // 如果不是在执行队列中的任务（可能是没执行就被外部pause或abort了）
        if (!mCurrentExecuted.contains(task)) {
            // 如果是TODO状态添，且addBack为true，才能加回等待队列
            if (addBack && task.getStatus() == XTaskBean.STATUS_TODO) {
                if (!mTobeExecuted.contains(task))
                    mTobeExecuted.offer(task);
            } else {
                mTobeExecuted.remove(task);// 否则，直接丢弃该任务
            }
            return;
        }

        // 是执行队列中的的任务
        if (task.getStatus() == XTaskBean.STATUS_DOING)// 正在执行,非法状态
            return;

        // 如果是TODO结束的，ERROR结束的，或是DONE结束的，寻找下一个任务
        if (mSpeedMonitor != null)
            mSpeedMonitor.stop();
        mCurrentExecuted.remove(task);
        XMgrTaskExecutor<B> nextTask = findNextTask(task);
        if (nextTask != null)
            mCurrentExecuted.offer(nextTask);
        // 下一个任务为空，但当前等待队列不为空，则说明等待队列中所有的任务都是异常状态
        boolean allError = (nextTask == null && mTobeExecuted.size() > 0);
        // 如果是TODO或ERROR结束的，且addBack为true，添加回等待队列
        if (addBack && task.getStatus() != XTaskBean.STATUS_DONE
                && !mTobeExecuted.contains(task))
            mTobeExecuted.offer(task);
        // 如果等待队列中所有的任务都是异常状态，则全部重置成TODO，方便下次全部自动执行
        if (allError) {
            for (XMgrTaskExecutor<B> errorTask : mTobeExecuted)
                errorTask.setStatus(XTaskBean.STATUS_TODO);
        }

        // 如果已经标记停止，或者不自动执行，则什么都不做
        if (!mIsWorking || !mAuto) {
            // 回调onStopAll()
            if (isAllStop()) {
                for (XTaskMgrListener<B> listener : mListeners)
                    listener.onStopAll();
            }
            return;
        }

        // 如果有下一个任务(没被过滤)，则继续执行任务
        if (nextTask != null &&
                (mFilter == null || mFilter.doFilter(nextTask.getBean()) != null)) {
            nextTask.start();
            if (mSpeedMonitor != null)
                mSpeedMonitor.start();
        }
        // 没有下一个任务
        else {
            if (isEmptyParallel() && mTobeExecuted.size() == 0) {
                // 执行队列没有任务，等待队列也没任务，则回调onFinishAll()
                mIsWorking = false;
                for (XTaskMgrListener<B> listener : mListeners)
                    listener.onFinishAll();
            } else if (isAllStop()) {
                // 当前有任务，但都暂停或完成了，则回调onStopAll()
                mIsWorking = false;
                for (XTaskMgrListener<B> listener : mListeners)
                    listener.onStopAll();
            }
        }
    }

    @Override
    public void registerListener(XTaskMgrListener<B> listener) {
        if (!mListeners.contains(listener))
            mListeners.add(listener);
    }

    @Override
    public void unregisterListener(XTaskMgrListener<B> listener) {
        mListeners.remove(listener);
    }

    @Override
    public List<XTaskMgrListener<B>> getListeners() {
        return mListeners;
    }

    /**
     * 内部Comparator<T>子类，用于对mTobeExecuted进行优先级排序。
     * 通过传入的TaskScheduler来实际进行排序比较。
     */
    private class InnerTaskComparator implements Comparator<XMgrTaskExecutor<B>> {

        private XMgrTaskExecutor<B> curTask;

        public void setCurTask(XMgrTaskExecutor<B> curTask) {
            this.curTask = curTask;
        }

        @Override
        public int compare(XMgrTaskExecutor<B> lhs, XMgrTaskExecutor<B> rhs) {
            return mScheduler.compare(lhs.getBean(), rhs.getBean(),
                    mCurrentExecuted == null ? null : curTask.getBean());
        }
    }
}
