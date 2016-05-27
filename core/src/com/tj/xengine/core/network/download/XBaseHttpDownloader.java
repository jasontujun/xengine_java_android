package com.tj.xengine.core.network.download;

import com.tj.xengine.core.network.http.XHttp;
import com.tj.xengine.core.toolkit.taskmgr.XMgrTaskExecutor;
import com.tj.xengine.core.toolkit.taskmgr.XTaskMgr;
import com.tj.xengine.core.toolkit.taskmgr.XTaskMgrListener;
import com.tj.xengine.core.utils.XStringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 简单的Http下载管理器。
 * 注意：
 * 1.XDownloadBean默认以url为id，所以对Http下载管理器的操作默认是以url为基础,
 * 子类可以继承XDownloadBean并覆盖{@link XDownloadBean#getId()}方法，以修改此行为。
 * 2.子类可以继承XHttpDownloader并覆盖{@link #createTaskMgr()}方法，以自定义任务管理器。
 * 3.子类可以继承XHttpDownloader并覆盖{@link #createTask(XDownloadBean)}方法，以自定义下载任务。
 * Created by jasontujun on 2015/10/29.
 */
public abstract class XBaseHttpDownloader {

    // Http连接管理器
    protected XHttp mHttpClient;
    // 任务管理器
    protected XTaskMgr<XMgrTaskExecutor<XDownloadBean>, XDownloadBean> mTaskMgr;
    // 监听者列表
    protected List<Listener> mListeners;

    public XBaseHttpDownloader(XHttp http) {
        mHttpClient = http;
        init();
    }

    private void init() {
        // 初始化监听集合
        mListeners = new CopyOnWriteArrayList<Listener>();
        // 注册对任务管理器的内部监听
        mTaskMgr = createTaskMgr();
        mTaskMgr.registerListener(new InnerListener());
    }

    /**
     * 子类可以覆盖此方法，以实现自定义的下载任务管理器。
     */
    protected abstract XTaskMgr<XMgrTaskExecutor<XDownloadBean>, XDownloadBean> createTaskMgr();

    /**
     * 子类可以覆盖此方法，以实现自定义的下载任务。
     */
    protected abstract XMgrTaskExecutor<XDownloadBean> createTask(XDownloadBean bean);

    public boolean addTask(XDownloadBean bean) {
        return mTaskMgr.addTask(createTask(bean));
    }

    public boolean addTask(String url, String folder) {
        XDownloadBean bean = new XDownloadBean(url, folder);
        return mTaskMgr.addTask(createTask(bean));
    }

    public boolean addTask(String url, String folder, String fileName) {
        XDownloadBean bean = new XDownloadBean(url, folder, fileName);
        return mTaskMgr.addTask(createTask(bean));
    }

    public void addTasks(List<String> urls, List<String> folders) {
        List<XMgrTaskExecutor<XDownloadBean>> tasks = new ArrayList<XMgrTaskExecutor<XDownloadBean>>();
        for (int i = 0; i < urls.size(); i++) {
            XDownloadBean bean = new XDownloadBean(urls.get(i), folders.get(i));
            tasks.add(createTask(bean));
        }
        mTaskMgr.addTasks(tasks);
    }

    public void addTasks(List<String> urls, List<String> folders, List<String> fileNames) {
        List<XMgrTaskExecutor<XDownloadBean>> tasks = new ArrayList<XMgrTaskExecutor<XDownloadBean>>();
        for (int i = 0; i < urls.size(); i++) {
            XDownloadBean bean = new XDownloadBean(urls.get(i), folders.get(i), fileNames.get(i));
            tasks.add(createTask(bean));
        }
        mTaskMgr.addTasks(tasks);
    }

    public void removeTask(String id) {
        mTaskMgr.removeTaskById(id);
    }

    public void removeTask(List<String> ids) {
        mTaskMgr.removeTasksById(ids);
    }

    public void removeTask(XDownloadBean bean) {
        mTaskMgr.removeTaskById(bean.getId());
    }

    public boolean startDownload() {
        return mTaskMgr.start();
    }

    public boolean startDownload(String id) {
        return mTaskMgr.start(id);
    }

    public boolean resumeDownload() {
        return mTaskMgr.resume();
    }

    public boolean resumeDownload(String id) {
        return mTaskMgr.resume(id);
    }

    public boolean pauseDownload() {
        return mTaskMgr.pause();
    }

    public boolean pauseDownload(String id) {
        return mTaskMgr.pause(id);
    }

    public boolean stopDownload() {
        return mTaskMgr.stop();
    }

    public boolean stopDownload(String id) {
        return mTaskMgr.stop(id);
    }

    public void stopAndClear() {
        mTaskMgr.stopAndReset();
    }

    public void setAutoRunning(boolean auto) {
        mTaskMgr.setAutoRunning(auto);
    }

    public void registerListener(Listener listener) {
        if (!mListeners.contains(listener))
            mListeners.add(listener);
    }

    public void unregisterListener(Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * 下载管理器的监听接口。
     */
    public interface Listener {

        /**
         * 启动下载的回调函数
         */
        void onStart(String id);

        /**
         * 停止下载的回调函数。
         */
        void onStop(String id);

        /**
         * 所有任务都暂停的回调。
         * remove、stop等操作会触发此回调。
         */
        void onStopAll();

        /**
         * 下载中的回调函数。
         */
        void onDownloading(String id, long completeSize, long totalSize);

        /**
         * 下载中更新速度的回调函数。
         * @param speed 下载速度，单位：byte/s
         */
        void onSpeedUpdate(String id, long speed);

        /**
         * 下载成功结束的回调函数。
         */
        void onComplete(String id, File file);

        /**
         * 执行失败结束的回调函数。
         */
        void onError(String id, String errorCode, File file);

        /**
         * 所有待下载任务都下载完成时会回调此函数。
         */
        void onFinishAll();
    }


    private class InnerListener implements XTaskMgrListener<XDownloadBean> {
        @Override
        public void onAdd(XDownloadBean task) {

        }

        @Override
        public void onAddAll(List<XDownloadBean> tasks) {

        }

        @Override
        public void onRemove(XDownloadBean task) {

        }

        @Override
        public void onRemoveAll(List<XDownloadBean> tasks) {

        }

        @Override
        public void onStart(XDownloadBean bean) {
            for (Listener listener : mListeners) {
                listener.onStart(bean.getId());
            }
        }

        @Override
        public void onStop(XDownloadBean bean) {
            for (Listener listener : mListeners) {
                listener.onStop(bean.getId());
            }
        }

        @Override
        public void onStopAll() {
            for (Listener listener : mListeners) {
                listener.onStopAll();
            }
        }

        @Override
        public void onFinishAll() {
            for (Listener listener : mListeners) {
                listener.onFinishAll();
            }
        }

        @Override
        public void onDoing(XDownloadBean bean, long completeSize) {
            for (Listener listener : mListeners) {
                listener.onDownloading(bean.getId(), completeSize, bean.getTotalSize());
            }
        }

        @Override
        public void onComplete(XDownloadBean bean) {
            for (Listener listener : mListeners) {
                listener.onComplete(bean.getId(), new File(bean.getFolder(), bean.getFileName()));
            }
        }

        @Override
        public void onError(XDownloadBean bean, String errorCode) {
            for (Listener listener : mListeners) {
                File file = null;
                if (!XStringUtil.isEmpty(bean.getFileName())) {
                    file = new File(bean.getFolder(), bean.getFileName() + bean.getDownloadingSuffix());
                }
                listener.onError(bean.getId(), errorCode, file);
            }
        }

        @Override
        public void onSpeedUpdate(XDownloadBean bean, long speed) {
            for (Listener listener : mListeners) {
                listener.onSpeedUpdate(bean.getId(), speed);
            }
        }
    }
}
