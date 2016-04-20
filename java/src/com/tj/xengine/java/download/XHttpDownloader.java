package com.tj.xengine.java.download;

import com.tj.xengine.core.network.http.XHttp;
import com.tj.xengine.core.network.http.XHttpConfig;
import com.tj.xengine.core.toolkit.taskmgr.XMgrTaskExecutor;
import com.tj.xengine.core.toolkit.taskmgr.XTaskMgrListener;
import com.tj.xengine.core.toolkit.taskmgr.serial.XSerialMgr;
import com.tj.xengine.core.toolkit.taskmgr.serial.XSerialMgrImpl;
import com.tj.xengine.core.toolkit.taskmgr.serial.XSerialSpeedMonitor;
import com.tj.xengine.core.utils.XStringUtil;
import com.tj.xengine.java.network.http.java.XJavaHttpClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 简单的Http下载管理器。
 * Created by jasontujun on 2015/10/29.
 */
public class XHttpDownloader {

    // Http连接管理器
    private XHttp mHttpClient;
    // 任务管理器
    private XSerialMgr<XDownloadBean> mTaskMgr;
    // 监听者列表
    protected List<Listener> mListeners;

    public XHttpDownloader() {
        this("xengine");
    }

    public XHttpDownloader(String userAgent) {
        mHttpClient = new XJavaHttpClient(XHttpConfig.builder()
                .setUserAgent(userAgent)
                .setConnectionTimeOut(60 * 1000)
                .setResponseTimeOut(60 * 1000)
                .setHandleRedirect(true)
                .setHandleCookie(true)
                .build());
        init();
    }

    public XHttpDownloader(XHttp http) {
        mHttpClient = http;
        init();
    }

    private void init() {
        // 初始化监听集合
        mListeners = new CopyOnWriteArrayList<Listener>();
        mTaskMgr = new XSerialMgrImpl<XDownloadBean>();
        // 注册对任务管理器的内部监听
        mTaskMgr.registerListener(new InnerListener());
        mTaskMgr.setSpeedMonitor(new XSerialSpeedMonitor<XDownloadBean>(mTaskMgr));
    }

    public boolean addTask(String url, String folder) {
        XDownloadBean bean = new XDownloadBean(url, folder);
        return mTaskMgr.addTask(new XHttpDownloadTask(bean, mHttpClient));
    }

    public boolean addTask(String url, String folder, String fileName) {
        XDownloadBean bean = new XDownloadBean(url, folder, fileName);
        return mTaskMgr.addTask(new XHttpDownloadTask(bean, mHttpClient));
    }

    public void addTasks(List<String> urls, List<String> folders) {
        List<XMgrTaskExecutor<XDownloadBean>> tasks = new ArrayList<XMgrTaskExecutor<XDownloadBean>>();
        for (int i = 0; i < urls.size(); i++) {
            XDownloadBean bean = new XDownloadBean(urls.get(i), folders.get(i));
            tasks.add(new XHttpDownloadTask(bean, mHttpClient));
        }
        mTaskMgr.addTasks(tasks);
    }

    public void addTasks(List<String> urls, List<String> folders, List<String> fileNames) {
        List<XMgrTaskExecutor<XDownloadBean>> tasks = new ArrayList<XMgrTaskExecutor<XDownloadBean>>();
        for (int i = 0; i < urls.size(); i++) {
            XDownloadBean bean = new XDownloadBean(urls.get(i), folders.get(i), fileNames.get(i));
            tasks.add(new XHttpDownloadTask(bean, mHttpClient));
        }
        mTaskMgr.addTasks(tasks);
    }

    public void removeTask(String url) {
        mTaskMgr.removeTaskById(url);
    }

    public void removeTask(List<String> urls) {
        mTaskMgr.removeTasksById(urls);
    }

    public void removeTask(XDownloadBean bean) {
        mTaskMgr.removeTaskById(bean.getId());
    }

    public boolean startDownload() {
        return mTaskMgr.start();
    }

    public boolean startDownload(String url) {
        return mTaskMgr.start(url);
    }

    public boolean resumeDownload() {
        return mTaskMgr.resume();
    }

    public boolean resumeDownload(String url) {
        return mTaskMgr.resume(url);
    }

    public boolean pauseDownload() {
        return mTaskMgr.pause();
    }

    public boolean pauseDownload(String taskId) {
        return mTaskMgr.pause(taskId);
    }

    public boolean stopDownload() {
        return mTaskMgr.stop();
    }

    public boolean stopDownload(String taskId) {
        return mTaskMgr.stop(taskId);
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
        void onStart(String url);

        /**
         * 停止下载的回调函数。
         */
        void onStop(String url);

        /**
         * 所有任务都暂停的回调。
         * remove、stop等操作会触发此回调。
         */
        void onStopAll();

        /**
         * 下载中的回调函数。
         */
        void onDownloading(String url, long completeSize, long totalSize);

        /**
         * 下载中更新速度的回调函数。
         * @param speed 下载速度，单位：byte/s
         */
        void onSpeedUpdate(String url, long speed);

        /**
         * 下载成功结束的回调函数。
         */
        void onComplete(String url, File file);

        /**
         * 执行失败结束的回调函数。
         */
        void onError(String url, String errorCode, File file);

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
                listener.onStart(bean.getUrl());
            }
        }

        @Override
        public void onStop(XDownloadBean bean) {
            for (Listener listener : mListeners) {
                listener.onStop(bean.getUrl());
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
                listener.onDownloading(bean.getUrl(), completeSize, bean.getTotalSize());
            }
        }

        @Override
        public void onComplete(XDownloadBean bean) {
            for (Listener listener : mListeners) {
                listener.onComplete(bean.getUrl(), new File(bean.getFolder(), bean.getFileName()));
            }
        }

        @Override
        public void onError(XDownloadBean bean, String errorCode) {
            for (Listener listener : mListeners) {
                File file = null;
                if (!XStringUtil.isEmpty(bean.getFileName())) {
                    file = new File(bean.getFolder(), bean.getFileName() + XHttpDownloadTask.DOWNLOADING_FILE_SUFFIX);
                }
                listener.onError(bean.getUrl(), errorCode, file);
            }
        }

        @Override
        public void onSpeedUpdate(XDownloadBean bean, long speed) {
            for (Listener listener : mListeners) {
                listener.onSpeedUpdate(bean.getUrl(), speed);
            }
        }
    }
}
