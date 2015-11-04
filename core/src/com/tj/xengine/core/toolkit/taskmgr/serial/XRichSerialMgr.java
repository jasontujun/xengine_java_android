package com.tj.xengine.core.toolkit.taskmgr.serial;

import com.tj.xengine.core.toolkit.task.XTaskBean;
import com.tj.xengine.core.toolkit.taskmgr.XMgrTaskExecutor;
import com.tj.xengine.core.toolkit.taskmgr.XTaskMgrListener;
import com.tj.xengine.core.toolkit.taskmgr.speed.calc.DefaultSpeedCalculator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * <pre>
 * 状态机更丰富的线性执行器。
 * 1.执行器内部由三个队列组成：
 *      不执行队列[新增] NotExecuted: 包含DEFAULT、ERROR的任务
 *      等待队列 TobeExecuted: 包含TODO、ERROR的任务
 *      执行队列 CurrentExecuted: 包含DOING、STARTING、PAUSING的任务
 * 2.各动作的队列转换和状态转换
 * 3.各动作的对应关系
 * Created by tujun on 2014/7/31.
 * </pre>
 */
public class XRichSerialMgr<B extends XTaskBean> extends XSerialMgrImpl<B> {

    protected volatile LinkedList<XMgrTaskExecutor<B>> mNotExecuted;// 不执行的任务队列

    public XRichSerialMgr() {
        super();
        mNotExecuted = new LinkedList<XMgrTaskExecutor<B>>();
    }

    @Override
    public XMgrTaskExecutor<B> getTaskById(String id) {
        if (id == null)
            return null;

        if (mCurrentExecuted != null && id.equals(getTaskId(mCurrentExecuted)))
            return mCurrentExecuted;

        for (XMgrTaskExecutor<B> task : mTobeExecuted) {
            if (id.equals(getTaskId(task)))
                return task;
        }

        for (XMgrTaskExecutor<B> task : mNotExecuted) {
            if (id.equals(getTaskId(task)))
                return task;
        }

        return null;
    }

    @Override
    public synchronized boolean addTask(XMgrTaskExecutor<B> task) {
        if (getTaskById(getTaskId(task)) != null)// 判断是否重复
            return false;

        // 已完成状态的任务直接抛弃
        if (task.getStatus() == XTaskBean.STATUS_DONE)
            return false;

        task.setTaskMgr(this);
        task.setListener(mInnerTaskListener);
        if (task.getSpeedCalculator() == null)
            task.setSpeedCalculator(new DefaultSpeedCalculator());
        // 默认和错误状态的放入不执行队列中
        if (task.getStatus() == XTaskBean.STATUS_DEFAULT ||
                task.getStatus() == XTaskBean.STATUS_ERROR) {
            mNotExecuted.offer(task);
        }
        // 未执行和执行中状态的放入不执行队列中
        else {
            task.setStatus(XTaskBean.STATUS_TODO);
            mTobeExecuted.offer(task);
        }
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
            if (task.getStatus() == XTaskBean.STATUS_DONE)// 已完成状态的任务直接抛弃
                continue;
            added.add(task.getBean());
            task.setTaskMgr(this);
            task.setListener(mInnerTaskListener);
            if (task.getSpeedCalculator() == null)
                task.setSpeedCalculator(new DefaultSpeedCalculator());
            // 默认和错误状态的放入不执行队列中
            if (task.getStatus() == XTaskBean.STATUS_DEFAULT ||
                    task.getStatus() == XTaskBean.STATUS_ERROR) {
                mNotExecuted.offer(task);
            }
            // 未执行和执行中状态的放入不执行队列中
            else {
                task.setStatus(XTaskBean.STATUS_TODO);
                mTobeExecuted.offer(task);
            }
        }
        if (added.size() > 0)
            for (XTaskMgrListener<B> listener : mListeners)
                listener.onAddAll(added);
    }

    @Override
    public synchronized void removeTaskById(String taskId) {
        removeTask(getTaskById(taskId));
    }

    @Override
    public synchronized void removeTask(XMgrTaskExecutor<B> task) {
        if (task == null)
            return;

        task.abort();// 终止当前任务
        boolean isRemoved = false;
        if (mCurrentExecuted == task) {// 如果要删除的任务是当前的任务
            mCurrentExecuted = null;
            isRemoved = true;
        }
        if (!isRemoved) // 如果要删除的任务在等待队列中
            isRemoved = mTobeExecuted.remove(task);
        if (!isRemoved) // 如果要删除的任务在不执行队列中
            isRemoved = mNotExecuted.remove(task);
        if (mCurrentExecuted == null) {// 如果当前没有任务运行，则标记结束
            if (mSpeedMonitor != null)
                mSpeedMonitor.stop();
            mIsWorking = false;
            for (XTaskMgrListener<B> listener : mListeners)
                listener.onStopAll();
        }
        if (isRemoved) {
            task.setStatus(XTaskBean.STATUS_DEFAULT);
            for (XTaskMgrListener<B> listener : mListeners)
                listener.onRemove(task.getBean());
        }
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
    public synchronized void removeTasks(List<XMgrTaskExecutor<B>> tasks) {
        if (tasks == null || tasks.size() == 0)
            return;

        List<B> removed = new ArrayList<B>();
        for (XMgrTaskExecutor<B> task : tasks) {
            if (task == null)
                continue;
            task.abort();// 终止当前任务
            boolean isRemoved = false;
            if (mCurrentExecuted == task) {// 如果要删除的任务是当前的任务
                mCurrentExecuted = null;
                isRemoved = true;
            }
            if (!isRemoved) // 如果要删除的任务在等待队列中
                isRemoved = mTobeExecuted.remove(task);
            if (!isRemoved) // 如果要删除的任务在不执行队列中
                isRemoved = mNotExecuted.remove(task);
            if (isRemoved)
                removed.add(task.getBean());
        }
        if (mCurrentExecuted == null) {// 如果当前没有任务运行，则标记结束
            if (mSpeedMonitor != null)
                mSpeedMonitor.stop();
            mIsWorking = false;
            for (XTaskMgrListener<B> listener : mListeners)
                listener.onStopAll();
        }
        if (removed.size() > 0) {
            for (B rTask : removed)
                rTask.setStatus(XTaskBean.STATUS_DEFAULT);
            for (XTaskMgrListener<B> listener : mListeners)
                listener.onRemoveAll(removed);
        }
    }

    @Override
    public synchronized void setRunningTask(String taskId) {
        XMgrTaskExecutor<B> task = getTaskById(taskId);
        if (mCurrentExecuted != null || task == null)
            return;
        if (mCurrentExecuted == task)
            return;
        if (mNotExecuted.contains(task)) {// 如果在不执行队列中，设置为TODO状态
            task.setStatus(XTaskBean.STATUS_TODO);
            mNotExecuted.remove(task);
        } else {// 如果在等待队列中
            mTobeExecuted.remove(task);
        }
        mCurrentExecuted = task;
    }

    @Override
    public synchronized boolean start(String taskId) {
        XMgrTaskExecutor<B> task = getTaskById(taskId);
        // 如果指定的task不存在，则什么都不做，返回false
        if (task == null)
            return false;

        // 如果被过滤掉，则不启动
        if (mFilter != null && mFilter.doFilter(task.getBean()) == null)
            return false;

        // 先尝试启动指定任务
        if (!task.start(XTaskBean.STATUS_DEFAULT))
            return false;

        mIsWorking = true;
        // 如果当前任务不是指定id任务，暂停当前任务，再指定id的为当前任务
        if (mCurrentExecuted != task) {
            // 暂停老的当前任务
            if (mCurrentExecuted != null) {
                mCurrentExecuted.pause();
                // 添加回等待队列
                mTobeExecuted.addFirst(mCurrentExecuted);
            }
            // 指定新的当前任务
            if (mNotExecuted.contains(task)) {
                mNotExecuted.remove(task);// 如果在不执行队列中
            } else {
                mTobeExecuted.remove(task);// 如果在等待队列中
            }
            mCurrentExecuted = task;
        }
        if (mSpeedMonitor != null)
            mSpeedMonitor.start();
        return true;
    }

    @Override
    public synchronized boolean stop(String taskId) {
        XMgrTaskExecutor<B> task = getTaskById(taskId);
        // 如果指定Id的任务不存在，或不在执行队列中，则什么都不做，返回false
        if (task == null || mCurrentExecuted != task)
            return false;
        // 如果指定Id的任务存在，且在运行队列中，暂停该任务
        if (!mCurrentExecuted.pause(XTaskBean.STATUS_DEFAULT))
            return false;
        if (mSpeedMonitor != null)
            mSpeedMonitor.stop();
        mIsWorking = false;
        // 添加进不执行队列
        mNotExecuted.offer(mCurrentExecuted);
        mCurrentExecuted = null;
        for (XTaskMgrListener<B> listener : mListeners)
            listener.onStopAll();
        return true;
    }

    @Override
    public synchronized void stopAndReset() {
        mIsWorking = false;
        // 停止速度监听
        if (mSpeedMonitor != null)
            mSpeedMonitor.stop();
        // 结束并清空当前任务
        if (mCurrentExecuted != null) {
            mCurrentExecuted.pause();
            mCurrentExecuted = null;
        }
        // 清空等待队列中的任务
        mTobeExecuted.clear();
        // 清空不执行队列中的任务
        mNotExecuted.clear();
        // 通知监听者
        for (XTaskMgrListener<B> listener : mListeners)
            listener.onStopAll();
    }

    @Override
    public synchronized void notifyTaskFinished(XMgrTaskExecutor<B> task, boolean addBack) {
        if (task == null)
            return;

        // 如果task不是当前正在执行的任务（可能是没执行就被外部pause或abort了）
        if (task != mCurrentExecuted) {
            // 如果是TODO状态，说明是没执行就被外部pause了
            if (task.getStatus() == XTaskBean.STATUS_TODO) {
                if (addBack) {// 如果addBack为true，加回等待队列
                    if (!mTobeExecuted.contains(task))
                        mTobeExecuted.offer(task);
                } else {// 如果addBack为false，加回不执行队列
                    mTobeExecuted.remove(task);
                    if (!mNotExecuted.contains(task)) {
                        task.setStatus(XTaskBean.STATUS_DEFAULT);
                        mNotExecuted.offer(task);
                    }
                }
            }
            return;
        }

        // task是当前正在执行的任务
        // 如果是DOING状态,非法状态，什么都不做
        if (task.getStatus() == XTaskBean.STATUS_DOING)
            return;

        // 如果是TODO结束的，ERROR结束的，或是DONE结束的，寻找下一个任务
        if (mSpeedMonitor != null)
            mSpeedMonitor.stop();
        mCurrentExecuted = findNextTask();
        // 下一个任务为空，但当前等待队列不为空，则说明等待队列中所有的任务都是异常状态
        boolean allError = (mCurrentExecuted == null && mTobeExecuted.size() > 0);
        // 如果是TODO或ERROR结束的
        if (task.getStatus() == XTaskBean.STATUS_TODO ||
                task.getStatus() == XTaskBean.STATUS_ERROR ||
                task.getStatus() == XTaskBean.STATUS_DEFAULT) {
            if (addBack) {// 如果addBack为true，加回等待队列
                if (task.getStatus() == XTaskBean.STATUS_DEFAULT) {
                    // 如果状态为DEFAULT，则改为TODO
                    task.setStatus(XTaskBean.STATUS_TODO);
                }
                mTobeExecuted.offer(task);
            } else {// 如果addBack为false，加回不执行队列
                if (task.getStatus() == XTaskBean.STATUS_TODO) {
                    // 如果状态为TODO，则改为DEFAULT
                    task.setStatus(XTaskBean.STATUS_DEFAULT);
                }
                mNotExecuted.offer(task);
            }
        }
        // 如果等待队列中所有的任务都是异常状态，则全部重置成TODO，方便下次全部自动执行
        if (allError) {
            for (XMgrTaskExecutor<B> errorTask : mTobeExecuted)
                errorTask.setStatus(XTaskBean.STATUS_TODO);
        }

        // 如果已经标记停止，则什么都不做
        if (!mIsWorking || !mAuto) {
            // 回调onStopAll()
            for (XTaskMgrListener<B> listener : mListeners)
                listener.onStopAll();
            return;
        }

        // 如果有任务(没被过滤)，则继续执行任务
        if (mCurrentExecuted != null &&
                (mFilter == null || mFilter.doFilter(mCurrentExecuted.getBean()) != null)) {
            if (mCurrentExecuted.start()) {
                if (mSpeedMonitor != null)
                    mSpeedMonitor.start();
            }
        }
        // 没有任务，标记结束
        else {
            mIsWorking = false;
            if (mTobeExecuted.size() == 0) {
                // 当前没有执行任务，等待队列也没任务，则回调onFinishAll()
                for (XTaskMgrListener<B> listener : mListeners)
                    listener.onFinishAll();
            } else {
                // 当前没有执行任务，等待队列有任务，则回调onStopAll()
                for (XTaskMgrListener<B> listener : mListeners)
                    listener.onStopAll();
            }
        }
    }
}
