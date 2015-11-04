package com.tj.xengine.android.toolkit.externalroot;

import android.content.Context;
import com.tj.xengine.core.toolkit.filter.XFilter;

import java.util.List;

/**
 * <pre>
 * 设备的外部存储根路径获取接口
 * User: jasontujun
 * Date: 14-5-27
 * Time: 下午5:41
 * </pre>
 */
public interface XExternalRoot {

    /**
     * 初始化外部存储根路径(耗时的方法，请在异步线程调用)
     * @param context
     */
    void init(Context context);

    /**
     * 设置过滤器，然后初始化外部存储根路径(耗时的方法，请在异步线程调用)
     * @param context
     * @param filter 过滤器
     */
    void init(Context context, XFilter<String> filter);

    /**
     * 判断初始化是否完成
     * @return  如果未初始化或在初始化中，返回false；初始化完成返回true
     */
    boolean isInitFinish();

    /**
     * 获取所有外部存储根路径的集合。
     * @return 如果未初始化或在初始化中返回null；初始化完返回路劲
     */
    List<String> getRoots();

    /**
     * 获取一个外部存储根路径。
     * 优先返回Environment.getExternalStorage()的路径，
     * 如果没有，则返回其他外部存储存储根路径的一个。
     * @return 返回一个可用的根路径；如果未初始化或在初始化中，或初始化完但无根路径，则返回null
     */
    String getRoot();

    /**
     * 立即获取一个外部存储根路径(无需初始化)。
     * 优先返回Environment.getExternalStorage()的路径，
     * 如果没有，则返回其他外部存储存储根路径的一个。
     * 注：该方法不是返回初始化的结果路径，不能通过此方法判断设备无外部存储根路径
     * @return 返回一个外部存储根路径；如果没有，则返回null
     */
    String getRootWithoutInit(Context context);
}
