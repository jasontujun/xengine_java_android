package com.tj.xengine.core.session.http.handler;

import com.tj.xengine.core.session.http.XHttpResponse;

/**
 * ��Http��������������Ľӿڡ�
 * Created by jason on 2015/10/28.
 */
public interface XHttpHandler<T> {

    /**
     * ��XHttpResponse��ȡ��������ת���ɶ�Ӧ��T�������ݡ�
     * @param response http��Ӧ����
     * @return ����T�������ݡ����ת��ʧ�ܣ�����null��
     */
    T handleResponse(XHttpResponse response);
}
