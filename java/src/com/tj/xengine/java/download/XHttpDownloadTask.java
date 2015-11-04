package com.tj.xengine.java.download;

import com.tj.xengine.core.session.http.*;
import com.tj.xengine.core.toolkit.task.runnable.XFiniteRetryRunnable;
import com.tj.xengine.core.toolkit.taskmgr.XBaseMgrTaskExecutor;
import com.tj.xengine.core.utils.XStringUtil;
import com.tj.xengine.java.utils.XLog;
import com.tj.xengine.java.utils.XStorageUtil;

import java.io.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * 对单个文件进行单线程下载的http下载任务。
 * 支持下载暂停后的断点续传;
 * 支持下载过出错后的重试。
 * Created by jason on 2015/10/29.
 */
public class XHttpDownloadTask extends XBaseMgrTaskExecutor<XDownloadBean> {

    private static final String TAG = XHttpDownloadTask.class.getSimpleName();

    // 进度长时间没变且文件读写测试失败，IO异常
    public static final String ERROR_IO_ERROR = "-0001";
    // 存储空间不足，导致无法继续写文件
    public static final String ERROR_NO_SPACE = "-0002";
    // url为空
    public static final String ERROR_NO_URL = "-0004";
    // response为空
    public static final String ERROR_NO_RESPONSE = "-0005";
    // 返回码不是200或206
    public static final String ERROR_STATUS_CODE = "-0006";
    // 返回的数据流为空
    public static final String ERROR_NO_INPUT_STREAM = "-0007";
    // 返回的数据大小为0
    public static final String ERROR_NO_CONTENT_LENGTH = "-0008";
    // 写文件时IO异常
    public static final String ERROR_IO_EXCEPTION = "-0009";

    public static final String DOWNLOADING_FILE_SUFFIX = ".downloading";// 下载过程中的临时文件后缀
    private static final int MAX_RETRY_COUNT = 10;// 重试次数
    private static final int BUFFER_SIZE = 16 * 1024;// 写文件的缓存大小
    private static final int DOWNLOAD_INTERVAL_TIME = 0;// 刷新下载进度的时间间隔

    private static ExecutorService dThreadPool;
    static {
        try {
            dThreadPool = Executors.newCachedThreadPool();
        } catch (Exception e) {// 未知原因导致无法创建线程池
            try {
                if (dThreadPool != null) {
                    dThreadPool.shutdownNow();
                    dThreadPool = null;
                }
            } catch (Exception e2) {}
        }
    }

    private static Future execute(Runnable runnable) {
        if (dThreadPool != null) {
            return dThreadPool.submit(runnable);
        } else {
            new Thread(runnable).start();
            return null;
        }
    }

    private DownloadFileRunnable mRunnable;
    private XHttp mHttpClient;

    public XHttpDownloadTask(XDownloadBean bean, XHttp httpClient) {
        super(bean);
        mHttpClient = httpClient;
    }

    public XHttpDownloadTask(XDownloadBean bean, int status, XHttp httpClient) {
        super(bean, status);
        mHttpClient = httpClient;
    }

    @Override
    protected boolean onStart() {
        if (mRunnable != null)
            return false;

        // 创建Runnable但不执行
        mRunnable = new DownloadFileRunnable(MAX_RETRY_COUNT);
        Future future = execute(mRunnable);
        mRunnable.setFuture(future);
        return true;
    }

    @Override
    protected boolean onPause() {
        if (mRunnable == null)
            return false;

        mRunnable.cancel();
        mRunnable = null;
        return true;
    }

    @Override
    protected boolean onAbort() {
        if (mRunnable != null) {
            mRunnable.cancel();
            mRunnable = null;
        }
        return true;
    }

    @Override
    protected boolean onEndSuccess() {
        mRunnable = null;
        return true;
    }

    @Override
    protected boolean onEndError(String errorCode, boolean retry) {
        mRunnable = null;
        return true;
    }

    @Override
    public long getCompleteSize() {
        return  XStringUtil.isEmpty(getBean().getFileName()) ? 0 :
                new File(getBean().getFolder(), getBean().getFileName()).length();
    }


    private class DownloadFileRunnable extends XFiniteRetryRunnable<XDownloadBean> {

        private Future mFuture;// 在暂停线程时用于中断阻塞的Future对象
        private String errorCode;// 错误码
        private byte[] mBuffer;// 写文件的缓存数组
        private boolean isDownloadSuccess;

        protected DownloadFileRunnable(long max) {
            super(max);
            isDownloadSuccess = false;
        }

        public void setFuture(Future future) {
            mFuture = future;
        }

        @Override
        public void cancel() {
            super.cancel();
            if (mFuture != null)
                mFuture.cancel(true);
        }

        @Override
        public long getRetryInterval(long retryCount) {
            return 30 * 1000;
        }

        @Override
        public XDownloadBean getBean() {
            return XHttpDownloadTask.this.getBean();
        }

        @Override
        public boolean onPreExecute(XDownloadBean bean) {
            // url为空，直接结束下载
            if (XStringUtil.isEmpty(bean.getUrl())) {
                errorCode = ERROR_NO_URL;
                return false;
            }
            // 判断存储空间是否已满，如果已满则结束下载
            XLog.log(TAG, "检测容量是否已满？");
            if (XStorageUtil.isFull(bean.getFolder(), BUFFER_SIZE)) {
                XLog.log(TAG, "存储已满，无法下载...");
                errorCode = ERROR_NO_SPACE;
                return false;
            }
            mBuffer = new byte[BUFFER_SIZE];
            return true;
        }

        @Override
        public void onPreExecuteError(XDownloadBean bean) {
            XHttpDownloadTask.this.endError(errorCode, false);
        }

        @Override
        public boolean onRepeatExecute(XDownloadBean bean) {
            File downloadingFile = null;
            // 如果文件名已指定，则创建文件夹和临时下载文件.downloading
            if (!XStringUtil.isEmpty(bean.getFileName())) {
                File dir = new File(bean.getFolder());
                if (!dir.exists()) {
                    boolean result = dir.mkdirs();
                    XLog.log(TAG, "下载目录" + dir.getAbsolutePath() + "不存在,尝试创建,结果:" + result);
                }
                downloadingFile = new File(bean.getFolder(), bean.getFileName() + DOWNLOADING_FILE_SUFFIX);
                XLog.log(TAG, "下载路径为:" + downloadingFile.getAbsolutePath());
            }

            // 已下载大小
            long downloadSize = 0;
            if (downloadingFile != null && downloadingFile.exists()) {
                downloadSize = downloadingFile.length();
            }
            XLog.log(TAG, "已经下载大小(起始位置):" + downloadSize);
            XHttpRequest request = mHttpClient.newRequest(bean.getUrl())
                    .setMethod(XHttpRequest.HttpMethod.GET);
            if (downloadSize != 0) {
                request.addHeader("Range", "bytes=" + downloadSize + "-");
            }
            XHttpResponse response = null;
            try {
                // 发送下载请求
                response = mHttpClient.execute(request);
                // http无响应
                if (response == null) {
                    XLog.log(TAG, "下载请求[" + bean.getUrl() + "]无响应,response == null");
                    errorCode = ERROR_NO_RESPONSE;
                    return false;
                }
                // 如果被中断
                if (!isRunning()) {
                    XLog.log(TAG, "Is Cancelled1");
                    return false;
                }
                int statusCode = response.getStatusCode();
                XLog.log(TAG, "下载请求[" + bean.getUrl() + "]返回状态码:" + statusCode);
                // 服务器返回416，表示服务器不能满足客户在请求中指定的Range头
                if (statusCode == 416) {
                    if (downloadSize > 0) {
                        // 很可能是已经下完，立即终止下载
                        bean.setTotalSize(downloadSize);
                        XHttpDownloadTask.this.notifyDoing(downloadSize);// 通知进度
                        isDownloadSuccess = true;
                        return true;
                    } else {
                        errorCode = ERROR_STATUS_CODE;
                        return false;
                    }
                }
                // 错误返回码，重新请求
                if (statusCode != 200 && statusCode != 206) {
                    XLog.log(TAG, "下载请求[" + bean.getUrl() + "]返回状态码错误，准备下次重试");
                    errorCode = ERROR_STATUS_CODE;
                    return false;
                }

                InputStream inputStream = response.getContent();
                // http没返回InputStream
                if (inputStream == null) {
                    XLog.log(TAG, "下载请求[" + bean.getUrl() + "]服务器响应没有内容，InputStream == null");
                    errorCode = ERROR_NO_INPUT_STREAM;
                    return false;
                }

                // 计算总大小
                long totalSize = -1;
                boolean isChunked = false;
                String transferEncoding = (response.getHeader(XHttp.TRANSFER_ENCODING) != null
                        && response.getHeader(XHttp.TRANSFER_ENCODING).size() > 0) ?
                        response.getHeader(XHttp.TRANSFER_ENCODING).get(0) : null;
                if (!XStringUtil.isEmpty(transferEncoding) && transferEncoding.equalsIgnoreCase(XHttp.CHUNKED)) {
                    // chunked分块传输模式，无法事先获取总大小，直接顺序下载
                    XLog.log(TAG, "下载请求[" + bean.getUrl() + "]服务器返回Transfer-Encoding=chunked,忽略Content-Length.");
                    isChunked = true;
                } else {
                    // 非chunked模式，可以事先获取总大小，用于必须判断文件是否下完
                    long length = response.getContentLength();
                    XLog.log(TAG, "下载请求[" + bean.getUrl() + "]服务器返回ContentLength=" + length);
                    if (length <= 0) {
                        if (downloadSize > 0) {
                            // 很可能已经下完了..
                            bean.setTotalSize(downloadSize);
                            XHttpDownloadTask.this.notifyDoing(downloadSize);// 通知进度
                            isDownloadSuccess = true;
                            return true;
                        } else {
                            // ContentLength为-1，且没有已下载的部分数据，直接结束下载
                            errorCode = ERROR_NO_CONTENT_LENGTH;
                            return false;
                        }
                    }
                    totalSize = downloadSize + length;// 真实总大小
                }
                bean.setTotalSize(totalSize);

                // 最终下载文件已存在，且文件大小等于服务器指定大小，直接结束下载
                if (!XStringUtil.isEmpty(bean.getFileName())) {
                    File finalFile = new File(bean.getFolder(), bean.getFileName());
                    if (finalFile.exists() && finalFile.length() >= totalSize) {
                        XHttpDownloadTask.this.notifyDoing(finalFile.length());// 通知进度
                        isDownloadSuccess = true;
                        return true;
                    } else {
                        finalFile.delete();// 否则删除，重新下载
                    }
                }

                // 如果文件名未指定，则从url和Content-Disposition中获取文件名
                if (downloadingFile == null) {
                    List<String> cdHeader = response.getHeader(XHttp.CONTENT_DISPOSITION);
                    String cd = (cdHeader != null && cdHeader.size() > 0) ? cdHeader.get(0) : null;
                    String fileName = XHttpUtil.parseFileNameFromHttp(request.getUrl(), cd);
                    // 如果从http请求和响应中获取不到文件名，则用一个32位的随机字符串作为文件名
                    if (XStringUtil.isEmpty(fileName)) {
                        fileName = XStringUtil.getRandomString(32);
                    }
                    bean.setFileName(fileName);
                    // 如果文件夹不存在，则创建
                    File dir = new File(bean.getFolder());
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    downloadingFile = new File(bean.getFolder(), bean.getFileName() + DOWNLOADING_FILE_SUFFIX);
                    XLog.log(TAG, "[非指定]下载路径为:" + downloadingFile.getAbsolutePath());
                }
                // 写入文件
                FileOutputStream os = null;
                BufferedInputStream bis = null;
                try {
                    bis = new BufferedInputStream(inputStream);
                    os = new FileOutputStream(downloadingFile, true);
                    int bufferStart = 0;// 之前read读取的数据量
                    int numRead = 0;// 一次read读取的数据量
                    // 用于控制刷新进度的变量
                    long curUpdateTime = 0;
                    long lastUpdateTime = 0;
                    while (true) {
                        if (!isRunning()) {// 如果被中断，则整体退出
                            XLog.log(TAG, "Is Cancelled2");
                            return false;
                        }
                        numRead = bis.read(mBuffer, bufferStart, BUFFER_SIZE - bufferStart);
                        // 已经没有数据了，退出循环
                        if (numRead == -1) {
                            if (bufferStart > 0) {// buffer未填充满，但已经没数据了，则写入文件
                                os.write(mBuffer, 0, bufferStart);
                            }
                            break;
                        }
                        downloadSize = downloadSize + numRead;// 递增已下载大小
                        // buffer未填满
                        if (numRead + bufferStart < BUFFER_SIZE) {
                            bufferStart = numRead + bufferStart;
                        }
                        // buffer已填满，则写入文件
                        else {
                            os.write(mBuffer, 0, BUFFER_SIZE);
                            bufferStart = 0;// buffer重新开始填充

                            if (!isChunked) {
                                XLog.log(TAG, "下载请求[" + bean.getUrl() + "]已下载大小:" + downloadSize + ", " +
                                        (int) (100 * (double) downloadSize / (double) totalSize) + "%");
                            } else {
                                XLog.log(TAG, "下载请求[" + bean.getUrl() + "]已下载大小[chunked模式]:" + downloadSize);
                            }
                            // 为了防止频繁刷新界面，间隔大于2秒，且增幅大于10KB通知一次
                            curUpdateTime = System.currentTimeMillis();
                            if (curUpdateTime - lastUpdateTime >= DOWNLOAD_INTERVAL_TIME) {
                                lastUpdateTime = curUpdateTime;
                                XHttpDownloadTask.this.notifyDoing(downloadSize);// 通知进度
                            }
                        }
                    }
                    // 判断是否下载完成
                    if (isChunked) {
                        bean.setTotalSize(downloadingFile.length());
                        XHttpDownloadTask.this.notifyDoing(downloadingFile.length());// 通知进度
                        isDownloadSuccess = true;
                        return true;
                    }else {
                        if (downloadingFile.length() >= totalSize) {
                            XHttpDownloadTask.this.notifyDoing(downloadingFile.length());// 通知进度
                            isDownloadSuccess = true;
                            return true;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    errorCode = ERROR_IO_EXCEPTION;
                    // 可能是存储空间已满,无法创建或写文件,错误结束
                    XLog.log(TAG, "IO异常,检测容量是否已满？");
                    if (XStorageUtil.isFull(bean.getFolder(), BUFFER_SIZE)) {
                        XLog.log(TAG, "存储已满，无法继续下载..");
                        errorCode = ERROR_NO_SPACE;
                        isDownloadSuccess = false;
                        return true;// 存储已满，立即中断下载
                    }
                    // 可能是IO异常(USB占用无法写),无法创建或读写文件,错误结束
                    XLog.log(TAG, "检测本地读写IO是否异常？");
                    if (!XStorageUtil.isIOWorks(bean.getFolder())) {
                        XLog.log(TAG, "检测结果:IO异常..");
                        errorCode = ERROR_IO_ERROR;
                        isDownloadSuccess = false;
                        return true;// 本地IO操作出现问题，中断下载
                    }
                } finally {
                    try {
                        if (bis != null)
                            bis.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        if (os != null)
                            os.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            } finally {
                if (response != null)
                    response.consumeContent();
            }
        }

        @Override
        public void onPostExecute(XDownloadBean bean) {
            mBuffer = null;// 回收缓存数组

            if (isDownloadSuccess) {
                // 将.downloading的临时后缀去掉
                File downloadingFile = new File(bean.getFolder(), bean.getFileName() + DOWNLOADING_FILE_SUFFIX);
                File finalFile = new File(bean.getFolder(), bean.getFileName());
                if (downloadingFile.exists()) {
                    boolean result = downloadingFile.renameTo(finalFile);
                    XLog.log(TAG, "下载请求[" + bean.getUrl() + "]去掉" + DOWNLOADING_FILE_SUFFIX + "后缀,结果:" + result);
                    if (result) {
                        XLog.log(TAG, "下载请求[" + bean.getUrl() + "]下载结束，成功!最终下载文件为:" + finalFile.getAbsolutePath());
                    } else {
                        XLog.log(TAG, "下载请求[" + bean.getUrl() + "]下载结束，成功!最终下载文件为:" + downloadingFile.getAbsolutePath());
                    }
                }
                XHttpDownloadTask.this.endSuccess();
            } else {
                XLog.log(TAG, "下载请求[" + bean.getUrl() + "]下载失败了，errorCode:" + errorCode);
                XHttpDownloadTask.this.endError(errorCode, true);
            }
        }

        @Override
        public void onCancelled(XDownloadBean bean) {
            XLog.log(TAG, "下载请求[" + bean.getUrl() + "]下载中断..");
            mBuffer = null;// 回收缓存数组
        }
    }
}
