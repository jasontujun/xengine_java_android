package com.tj.xengine.android.toolkit.externalroot;

import android.content.Context;
import android.text.TextUtils;
import com.tj.xengine.android.utils.XRootUtil;
import com.tj.xengine.core.toolkit.filter.XFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * 方便获取外部存储根路径的实现类(单例)。
 * User: jasontujun
 * Date: 14-6-3
 * Time: 下午4:53
 * </pre>
 */
public class XExternalRootImpl implements XExternalRoot {

    private static class SingletonHolder {
        final static XExternalRootImpl INSTANCE = new XExternalRootImpl();
    }

    public static XExternalRootImpl getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static final int INIT_TODO = 0;
    private static final int INIT_DOING = 1;
    private static final int INIT_DONE = 2;
    private int mStatus;
    private List<String> mRoots;

    private XExternalRootImpl() {
        mStatus = INIT_TODO;
        mRoots = new ArrayList<String>();
    }

    @Override
    public void init(Context context) {
        init(context, null);
    }

    @Override
    public synchronized void init(Context context, XFilter<String> filter) {
        mStatus = INIT_DOING;
        mRoots.clear();
        // 将getExternalStorage()返回的根路径添加到第一个位置
        String apiRoot = XRootUtil.getRootByApi();
        if (!TextUtils.isEmpty(apiRoot) &&
                (filter == null || filter.doFilter(apiRoot) != null))
            mRoots.add(apiRoot);
        // 先通过反射获取所有根路径
        List<String> roots = XRootUtil.getRootsByReflection(context);
        // 如果反射无法获取所有根路径，尝试通过cmd获取
        if (roots == null || roots.size() == 0)
            roots = XRootUtil.getRootsByCmd();
        if (roots != null) {
            for (String root : roots) {
                // 根路径不重复，且没被过滤掉
                if (!mRoots.contains(root) &&
                        (filter == null || filter.doFilter(root) != null))
                    mRoots.add(root);
            }
        }
        mStatus = INIT_DONE;
    }

    @Override
    public boolean isInitFinish() {
        return mStatus == INIT_DONE;
    }

    @Override
    public List<String> getRoots() {
        if (!isInitFinish())
            return null;
        else
            return new ArrayList<String>(mRoots);
    }

    @Override
    public String getRoot() {
        if (!isInitFinish())
            return null;
        else // 第一个优先是getExternalStorage()获取的根路径
            return mRoots.size() == 0 ? null : mRoots.get(0);
    }

    @Override
    public String getRootWithoutInit(Context context) {
        // 优先返回getExternalStorage()获取的根路径
        String apiRoot = XRootUtil.getRootByApi();
        if (!TextUtils.isEmpty(apiRoot))
            return apiRoot;

        // 然后返回通过反射获取的第一个根路径
        List<String> roots = XRootUtil.getRootsByReflection(context);
        if (roots != null && roots.size() > 0)
            return roots.get(0);

        return null;
    }

    /**
     * 立即获取一个app专属放置大文件的外部存储根路径(无需初始化)。
     * 格式：/root/Android/data/"package name"/files
     * 注：不能通过此方法判断设备无外部存储根路径
     * @return 返回一个外部存储根路径；如果没有，则返回null
     */
    public String getAppFilesRootWithoutInit(Context context) {
        String root = getRootWithoutInit(context);
        if (!TextUtils.isEmpty(root))
            root = XRootUtil.root2AppFilesPath(context, root);
        return root;
    }

    /**
     * 立即获取一个app专属放置临时文件的外部存储根路径(无需初始化)。
     * 格式：/root/Android/data/"package name"/cache
     * 注：不能通过此方法判断设备无外部存储根路径
     * @return 返回一个外部存储根路径；如果没有，则返回null
     */
    public String getAppCacheRootWithoutInit(Context context) {
        String root = getRootWithoutInit(context);
        if (!TextUtils.isEmpty(root))
            root = XRootUtil.root2AppCachePath(context, root);
        return root;
    }
}
