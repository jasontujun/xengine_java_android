package com.tj.xengine.core.network.download;

import com.tj.xengine.core.network.http.XHttp;
import com.tj.xengine.core.network.http.XHttpRequest;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.network.http.XHttpUtil;
import com.tj.xengine.core.toolkit.task.runnable.XFiniteRetryRunnable;
import com.tj.xengine.core.toolkit.taskmgr.XBaseMgrTaskExecutor;
import com.tj.xengine.core.utils.XStringUtil;

import java.io.*;
import java.util.List;
import java.util.concurrent.Future;


/**
 * 对单个文件进行单线程下载的http下载任务。
 * 支持下载暂停后的断点续传;
 * 支持下载过出错后的重试。
 * Created by jasontujun on 2015/10/29.
 */
public abstract class XBaseHttpDownloadTask extends XBaseMgrTaskExecutor<XDownloadBean> {

    private static final String TAG = XBaseHttpDownloadTask.class.getSimpleName();

    // 进度长时间没变且文件读写测试失败，IO异常
    public static final String ERROR_IO_ERROR = "-0001";
    // 存储空间不足，导致无法继续写文件
    public static final String ERROR_NO_SPACE = "-0002";
    // url为空
    public static final String ERROR_NO_URL = "-0004";
    // response为空
    public static final String ERROR_NO_RESPONSE = "-0005";
    // 返回码不是200或200+
    public static final String ERROR_STATUS_CODE = "-0006";
    // 返回的数据流为空
    public static final String ERROR_NO_INPUT_STREAM = "-0007";
    // 返回的数据大小为0
    public static final String ERROR_NO_CONTENT_LENGTH = "-0008";
    // 写文件时IO异常
    public static final String ERROR_IO_EXCEPTION = "-0009";

    protected static final String DOWNLOADING_FILE_SUFFIX = ".downloading";// 下载过程中的临时文件后缀
    protected static final int MAX_RETRY_COUNT = 3;// 重试次数
    protected static final int DEFAULT_RETRY_INTERVAL = 30 * 1000;// 重试间隔(单位:毫秒)
    protected static final int BUFFER_SIZE = 16 * 1024;// 写文件的缓存大小
    protected static final int PROGRESS_INTERVAL_TIME = 0;// 通知下载进度的时间间隔(单位:毫秒)

    private DownloadFileRunnable mRunnable;
    protected XHttp mHttpClient;

    public XBaseHttpDownloadTask(XDownloadBean bean, XHttp httpClient) {
        super(bean);
        mHttpClient = httpClient;
    }

    public XBaseHttpDownloadTask(XDownloadBean bean, int status, XHttp httpClient) {
        super(bean, status);
        mHttpClient = httpClient;
    }

    protected abstract void log(String message);

    /**
     * 检查容量是否已满。
     * 子类可以重写此方法。
     * @return 容量已满返回true，否则返回false.
     */
    protected abstract boolean checkStorageFull(String dir, long minSize);

    /**
     * 检查文件IO是否正常，比如由于USB占用,或其他原因无法创建或读写文件。
     * 子类可以重写此方法。
     * @return IO正常返回true，否则返回false.
     */
    protected abstract boolean checkIO(String dir);

    /**
     * 获取失败重试次数。
     * 子类可以重写此方法。
     */
    protected int getRetryCount() {
        return MAX_RETRY_COUNT;
    }

    /**
     * 获取下载重试的间隔时间(单位:毫秒)。
     * 子类可以重写此方法。
     */
    protected long getRetryIntervalTime(long retryCount) {
        return DEFAULT_RETRY_INTERVAL;
    }

    /**
     * 获取下载过程中的临时文件名的后缀。
     * 子类可以重写此方法。
     */
    protected String getDownloadingFileSuffix() {
        return DOWNLOADING_FILE_SUFFIX;
    }

    /**
     * 获取下载缓冲区大小。
     * 子类可以重写此方法。
     */
    protected int getBufferSize() {
        return BUFFER_SIZE;
    }

    /**
     * 获取通知下载进度的时间间隔(单位:毫秒)。
     * 子类可以重写此方法。
     */
    protected int getProgressIntervalTime() {
        return PROGRESS_INTERVAL_TIME;
    }

    /**
     * 一旦任务最终执行失败，是否添加回执行队列中。
     * 子类可以重写此方法。
     */
    protected boolean backToDownloadMgr() {
        return true;
    }

    /**
     * 异步执行下载任务。
     * 子类可以重写此方法，实现自定义的异步执行方式。
     * @return 返回异步执行任务的Future队形，可以空。
     * 如果返回的Future不为空，则会用于暂停Runnable执行。
     */
    protected Future asyncExecute(Runnable runnable) {
//        if (mThreadPool != null) {
//            return mThreadPool.submit(runnable);
//        }
        new Thread(runnable).start();
        return null;
    }

    /**
     * 获取正在的下载url。
     * 子类可以重写此方法，修改成自定义的异步执行方式。
     */
    protected String requestRealUrl(XDownloadBean bean) {
        return bean.getUrl();
    }

    @Override
    protected boolean onStart() {
        if (mRunnable != null)
            return false;

        // 创建Runnable但不执行
        mRunnable = new DownloadFileRunnable(getRetryCount());
        Future future = asyncExecute(mRunnable);
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

        private String mUrl;
        private File mDownloadingFile;// 下载存储的本地文件
        private Future mFuture;// 在暂停线程时用于中断阻塞的Future对象
        private String errorCode;// 错误码
        private int mBufferSize;
        private byte[] mBuffer;// 写文件的缓存数组
        private boolean isDownloadSuccess;

        protected DownloadFileRunnable(long max) {
            super(max);
            isDownloadSuccess = false;
            mBufferSize = getBufferSize();
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
            return getRetryIntervalTime(retryCount);
        }

        @Override
        public XDownloadBean getBean() {
            return XBaseHttpDownloadTask.this.getBean();
        }

        @Override
        public boolean onPreExecute(XDownloadBean bean) {
            // 判断存储空间是否已满，如果已满则结束下载
            if (checkStorageFull(bean.getFolder(), mBufferSize)) {
                log("存储已满，无法下载...");
                errorCode = ERROR_NO_SPACE;
                return false;
            }

            mUrl = requestRealUrl(bean);
            if (XStringUtil.isEmpty(mUrl)) {
                // url为空，直接结束下载
                errorCode = ERROR_NO_URL;
                return false;
            }
            // 如果文件名已指定，则创建文件夹和临时下载文件(带临时后缀的)
            mDownloadingFile = null;
            if (!XStringUtil.isEmpty(bean.getFileName())) {
                File dir = new File(bean.getFolder());
                if (!dir.exists()) {
                    boolean result = dir.mkdirs();
                    log("下载目录" + dir.getAbsolutePath() + "不存在,尝试创建,结果:" + result);
                }
                mDownloadingFile = new File(bean.getFolder(),
                        bean.getFileName() + getDownloadingFileSuffix());
                bean.setDownloadingSuffix(getDownloadingFileSuffix());
                log("下载路径为:" + mDownloadingFile.getAbsolutePath());
            }
            // 创建缓冲区
            mBuffer = new byte[mBufferSize];
            return true;
        }

        @Override
        public void onPreExecuteError(XDownloadBean bean) {
            if (ERROR_NO_SPACE.equals(errorCode)) {
                XBaseHttpDownloadTask.this.endError(errorCode, false);
            } else {
                XBaseHttpDownloadTask.this.endError(errorCode, backToDownloadMgr());
            }
        }

        @Override
        public boolean onRepeatExecute(XDownloadBean bean) {
            // 已下载大小
            long downloadSize = 0;
            if (mDownloadingFile != null && mDownloadingFile.exists()) {
                downloadSize = mDownloadingFile.length();
            }
            log("已经下载大小(起始位置):" + downloadSize);
            XHttpRequest request = mHttpClient.newRequest(mUrl)
                    .setMethod(XHttpRequest.Method.GET);
            if (downloadSize != 0) {
                request.addHeader("Range", "bytes=" + downloadSize + "-");
            }
            XHttpResponse response = null;
            try {
                // 发送下载请求
                response = mHttpClient.execute(request);
                // http无响应
                if (response == null) {
                    log("下载请求[" + mUrl + "]无响应,response == null");
                    errorCode = ERROR_NO_RESPONSE;
                    return false;
                }
                // 如果被中断
                if (!isRunning()) {
                    log("Is Cancelled1");
                    return false;
                }
                int statusCode = response.getStatusCode();
                log("下载请求[" + mUrl + "]返回状态码:" + statusCode);
                // 服务器返回416，表示服务器不能满足客户在请求中指定的Range头
                if (statusCode == 416) {
                    if (downloadSize > 0) {
                        // 很可能是已经下完，立即终止下载
                        bean.setTotalSize(downloadSize);
                        XBaseHttpDownloadTask.this.notifyDoing(downloadSize);// 通知进度
                        isDownloadSuccess = true;
                        return true;
                    } else {
                        errorCode = ERROR_STATUS_CODE;
                        return false;
                    }
                }
                // 错误返回码，重新请求
                if (statusCode < 200 && statusCode >= 300) {
                    log("下载请求[" + mUrl + "]返回状态码错误，准备下次重试");
                    errorCode = ERROR_STATUS_CODE;
                    return false;
                }

                InputStream inputStream = response.getContent();
                // http没返回InputStream
                if (inputStream == null) {
                    log("下载请求[" + mUrl + "]服务器响应没有内容，InputStream == null");
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
                    log("下载请求[" + mUrl + "]服务器返回Transfer-Encoding=chunked,忽略Content-Length.");
                    isChunked = true;
                } else {
                    // 非chunked模式，可以事先获取总大小，用于必须判断文件是否下完
                    long length = response.getContentLength();
                    log("下载请求[" + mUrl + "]服务器返回ContentLength=" + length);
                    if (length <= 0) {
                        if (downloadSize > 0) {
                            // 很可能已经下完了..
                            bean.setTotalSize(downloadSize);
                            XBaseHttpDownloadTask.this.notifyDoing(downloadSize);// 通知进度
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
                        XBaseHttpDownloadTask.this.notifyDoing(finalFile.length());// 通知进度
                        isDownloadSuccess = true;
                        return true;
                    } else {
                        finalFile.delete();// 否则删除，重新下载
                    }
                }

                // 如果文件名未指定，则从url和Content-Disposition中获取文件名
                if (mDownloadingFile == null) {
                    List<String> cdHeader = response.getHeader(XHttp.CONTENT_DISPOSITION);
                    String cd = (cdHeader != null && cdHeader.size() > 0) ? cdHeader.get(0) : null;
                    String fileName = XHttpUtil.parseFileNameFromHttp(mUrl, cd);
                    // 如果从http请求和响应中获取不到文件名，则用一个32位的随机字符串作为文件名
                    if (XStringUtil.isEmpty(fileName)) {
                        fileName = XStringUtil.getRandomString(32);
                    }
                    // 设定默认文件名
                    bean.setFileName(fileName);
                    // 如果文件夹不存在，则创建
                    File dir = new File(bean.getFolder());
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    mDownloadingFile = new File(bean.getFolder(),
                            bean.getFileName() + getDownloadingFileSuffix());
                    bean.setDownloadingSuffix(getDownloadingFileSuffix());
                    log("[非指定]下载路径为:" + mDownloadingFile.getAbsolutePath());
                }
                // 写入文件
                FileOutputStream os = null;
                BufferedInputStream bis = null;
                try {
                    bis = new BufferedInputStream(inputStream);
                    os = new FileOutputStream(mDownloadingFile, true);
                    int bufferStart = 0;// 之前read读取的数据量
                    int numRead = 0;// 一次read读取的数据量
                    // 用于控制刷新进度的变量
                    long curUpdateTime = 0;
                    long lastUpdateTime = 0;
                    while (true) {
                        if (!isRunning()) {// 如果被中断，则整体退出
                            log("Is Cancelled2");
                            return false;
                        }
                        numRead = bis.read(mBuffer, bufferStart, mBufferSize - bufferStart);
                        // 已经没有数据了，退出循环
                        if (numRead == -1) {
                            if (bufferStart > 0) {// buffer未填充满，但已经没数据了，则写入文件
                                os.write(mBuffer, 0, bufferStart);
                            }
                            break;
                        }
                        downloadSize = downloadSize + numRead;// 递增已下载大小
                        // buffer未填满
                        if (numRead + bufferStart < mBufferSize) {
                            bufferStart = numRead + bufferStart;
                        }
                        // buffer已填满，则写入文件
                        else {
                            os.write(mBuffer, 0, mBufferSize);
                            bufferStart = 0;// buffer重新开始填充

                            if (!isChunked) {
                                log("下载请求[" + mUrl + "]已下载大小:" + downloadSize + ", " +
                                        (int) (100 * (double) downloadSize / (double) totalSize) + "%");
                            } else {
                                log("下载请求[" + mUrl + "]已下载大小[chunked模式]:" + downloadSize);
                            }
                            // 为了防止过于频繁通知进度，间隔大于指定时长，才进行进度通知
                            curUpdateTime = System.currentTimeMillis();
                            if (curUpdateTime - lastUpdateTime >= getProgressIntervalTime()) {
                                lastUpdateTime = curUpdateTime;
                                XBaseHttpDownloadTask.this.notifyDoing(downloadSize);// 通知进度
                            }
                        }
                    }
                    // 判断是否下载完成
                    if (isChunked) {
                        bean.setTotalSize(mDownloadingFile.length());
                        XBaseHttpDownloadTask.this.notifyDoing(mDownloadingFile.length());// 通知进度
                        isDownloadSuccess = true;
                        return true;
                    }else {
                        if (mDownloadingFile.length() >= totalSize) {
                            XBaseHttpDownloadTask.this.notifyDoing(mDownloadingFile.length());// 通知进度
                            isDownloadSuccess = true;
                            return true;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    errorCode = ERROR_IO_EXCEPTION;
                    // 可能是存储空间已满,无法创建或写文件,错误结束
                    log("IO异常,检测容量是否已满？");
                    if (checkStorageFull(bean.getFolder(), mBufferSize)) {
                        log("存储已满，无法继续下载..");
                        errorCode = ERROR_NO_SPACE;
                        isDownloadSuccess = false;
                        return true;// 存储已满，立即中断下载
                    }
                    // 可能是IO异常(USB占用无法写),无法创建或读写文件,错误结束
                    log("检测本地读写IO是否异常？");
                    if (!checkIO(bean.getFolder())) {
                        log("检测结果:IO异常..");
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
                // 将临时后缀去掉
                if (!XStringUtil.isEmpty(getDownloadingFileSuffix()) &&
                        mDownloadingFile != null && mDownloadingFile.exists()) {
                    File finalFile = new File(bean.getFolder(), bean.getFileName());
                    boolean result = mDownloadingFile.renameTo(finalFile);
                    log("下载请求[" + mUrl + "]下载结束，成功!最终下载文件为:" +
                            (result ? finalFile.getAbsolutePath() : mDownloadingFile.getAbsolutePath()));
                }
                XBaseHttpDownloadTask.this.endSuccess();
            } else {
                log("下载请求[" + mUrl + "]下载失败了，errorCode:" + errorCode);
                if (ERROR_NO_SPACE.equals(errorCode) || ERROR_IO_ERROR.equals(errorCode)) {
                    XBaseHttpDownloadTask.this.endError(errorCode, false);
                } else {
                    XBaseHttpDownloadTask.this.endError(errorCode, backToDownloadMgr());
                }
            }
        }

        @Override
        public void onCancelled(XDownloadBean bean) {
            log("下载请求[" + mUrl + "]下载中断..");
            mBuffer = null;// 回收缓存数组
        }
    }
}
