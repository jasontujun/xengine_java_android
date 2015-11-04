package com.tj.xengine.android.toolkit.externalroot;

import android.content.Context;
import android.text.TextUtils;
import com.tj.xengine.android.utils.XRootUtil;
import com.tj.xengine.core.toolkit.filter.XFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * �����ȡ�ⲿ�洢��·����ʵ����(����)��
 * User: jasontujun
 * Date: 14-6-3
 * Time: ����4:53
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
        // ��getExternalStorage()���صĸ�·����ӵ���һ��λ��
        String apiRoot = XRootUtil.getRootByApi();
        if (!TextUtils.isEmpty(apiRoot) &&
                (filter == null || filter.doFilter(apiRoot) != null))
            mRoots.add(apiRoot);
        // ��ͨ�������ȡ���и�·��
        List<String> roots = XRootUtil.getRootsByReflection(context);
        // ��������޷���ȡ���и�·��������ͨ��cmd��ȡ
        if (roots == null || roots.size() == 0)
            roots = XRootUtil.getRootsByCmd();
        if (roots != null) {
            for (String root : roots) {
                // ��·�����ظ�����û�����˵�
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
        else // ��һ��������getExternalStorage()��ȡ�ĸ�·��
            return mRoots.size() == 0 ? null : mRoots.get(0);
    }

    @Override
    public String getRootWithoutInit(Context context) {
        // ���ȷ���getExternalStorage()��ȡ�ĸ�·��
        String apiRoot = XRootUtil.getRootByApi();
        if (!TextUtils.isEmpty(apiRoot))
            return apiRoot;

        // Ȼ�󷵻�ͨ�������ȡ�ĵ�һ����·��
        List<String> roots = XRootUtil.getRootsByReflection(context);
        if (roots != null && roots.size() > 0)
            return roots.get(0);

        return null;
    }

    /**
     * ������ȡһ��appר�����ô��ļ����ⲿ�洢��·��(�����ʼ��)��
     * ��ʽ��/root/Android/data/"package name"/files
     * ע������ͨ���˷����ж��豸���ⲿ�洢��·��
     * @return ����һ���ⲿ�洢��·�������û�У��򷵻�null
     */
    public String getAppFilesRootWithoutInit(Context context) {
        String root = getRootWithoutInit(context);
        if (!TextUtils.isEmpty(root))
            root = XRootUtil.root2AppFilesPath(context, root);
        return root;
    }

    /**
     * ������ȡһ��appר��������ʱ�ļ����ⲿ�洢��·��(�����ʼ��)��
     * ��ʽ��/root/Android/data/"package name"/cache
     * ע������ͨ���˷����ж��豸���ⲿ�洢��·��
     * @return ����һ���ⲿ�洢��·�������û�У��򷵻�null
     */
    public String getAppCacheRootWithoutInit(Context context) {
        String root = getRootWithoutInit(context);
        if (!TextUtils.isEmpty(root))
            root = XRootUtil.root2AppCachePath(context, root);
        return root;
    }
}
