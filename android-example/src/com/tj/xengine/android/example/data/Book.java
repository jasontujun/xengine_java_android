package com.tj.xengine.android.example.data;

import com.tj.xengine.android.db.annotation.XColumn;
import com.tj.xengine.android.db.annotation.XTable;
import com.tj.xengine.core.data.annotation.XId;

import java.util.Date;

/**
 * Created by jason on 2016/4/16.
 */
@XTable(name = "book")
public class Book {

    @XId
    @XColumn(name = "id")
    public String id;

    @XColumn(name = "name", notNull = true, unique = true)
    public String name;

    @XColumn(name = "author")
    public String author;

    @XColumn(name = "price")
    public int price;

    @XColumn(name = "publish")
    public Date publish;
}
