package com.tj.xengine.android.toolkit.externalroot;

import android.content.Context;
import com.tj.xengine.core.toolkit.filter.XFilter;

import java.util.List;

/**
 * <pre>
 * �豸���ⲿ�洢��·����ȡ�ӿ�
 * User: jasontujun
 * Date: 14-5-27
 * Time: ����5:41
 * </pre>
 */
public interface XExternalRoot {

    /**
     * ��ʼ���ⲿ�洢��·��(��ʱ�ķ����������첽�̵߳���)
     * @param context
     */
    void init(Context context);

    /**
     * ���ù�������Ȼ���ʼ���ⲿ�洢��·��(��ʱ�ķ����������첽�̵߳���)
     * @param context
     * @param filter ������
     */
    void init(Context context, XFilter<String> filter);

    /**
     * �жϳ�ʼ���Ƿ����
     * @return  ���δ��ʼ�����ڳ�ʼ���У�����false����ʼ����ɷ���true
     */
    boolean isInitFinish();

    /**
     * ��ȡ�����ⲿ�洢��·���ļ��ϡ�
     * @return ���δ��ʼ�����ڳ�ʼ���з���null����ʼ���귵��·��
     */
    List<String> getRoots();

    /**
     * ��ȡһ���ⲿ�洢��·����
     * ���ȷ���Environment.getExternalStorage()��·����
     * ���û�У��򷵻������ⲿ�洢�洢��·����һ����
     * @return ����һ�����õĸ�·�������δ��ʼ�����ڳ�ʼ���У����ʼ���굫�޸�·�����򷵻�null
     */
    String getRoot();

    /**
     * ������ȡһ���ⲿ�洢��·��(�����ʼ��)��
     * ���ȷ���Environment.getExternalStorage()��·����
     * ���û�У��򷵻������ⲿ�洢�洢��·����һ����
     * ע���÷������Ƿ��س�ʼ���Ľ��·��������ͨ���˷����ж��豸���ⲿ�洢��·��
     * @return ����һ���ⲿ�洢��·�������û�У��򷵻�null
     */
    String getRootWithoutInit(Context context);
}
