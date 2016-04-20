package com.tj.xengine.android.db;

/**
 * SQLite的五种数据类型
 * Created by jasontujun on 2016/4/16.
 */
public enum XDBDataType {

    /**
     * 字段的类型为整形
     */
    INTEGER,

    /**
     * 字段的类型为浮点数（以8字节存储）
     */
    REAL,

    /**
     * 字段的类型为字符串
     */
    TEXT,

    /**
     * 字段的类型为二进制
     */
    BLOB;
}
