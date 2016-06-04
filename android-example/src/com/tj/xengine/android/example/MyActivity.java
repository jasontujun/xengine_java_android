package com.tj.xengine.android.example;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.tj.xengine.android.data.listener.XAsyncDatabaseListener;
import com.tj.xengine.android.data.XListIdDBDataSourceImpl;
import com.tj.xengine.core.data.XWithDatabase;
import com.tj.xengine.android.data.listener.XHandlerIdDataSourceListener;
import com.tj.xengine.android.db.XDatabase;
import com.tj.xengine.android.example.data.Book;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.*;

import java.util.Date;
import java.util.List;

public class MyActivity extends Activity {

    private static final String TAG = "xengine-android-test";

    private static final String SOURCE_NAME = "bookSource";

    private EditText inputId;
    private EditText inputName;
    private EditText inputAuthor;
    private EditText inputPrice;
    private MyAdapter myAdapter;
    private XWithId.Listener<Book> mDataSourceListener;
    private XWithDatabase.Listener<Book> mDbListener;
    private XAsyncDatabaseListener<Book> mSyncDbListener;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        XLog.d(TAG, "onCreate()!");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        XDatabase.getInstance().init(getApplicationContext(), "xengine_test", 1);
        XDefaultDataRepo repo = XDefaultDataRepo.getInstance();
        XListIdDBDataSourceImpl ss = new XListIdDBDataSourceImpl<Book>(Book.class, SOURCE_NAME);
        ss.setReplaceOverride(true);
        repo.registerDataSource(ss);// 此处可能已经添加！

        inputId = (EditText) findViewById(R.id.input_id);
        inputName = (EditText) findViewById(R.id.input_name);
        inputAuthor = (EditText) findViewById(R.id.input_author);
        inputPrice = (EditText) findViewById(R.id.input_price);
        ListView listView = (ListView) findViewById(R.id.list_book);
        Button addBtn = (Button) findViewById(R.id.btn_add);

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Book book = new Book();
                book.id = inputId.getText().toString();
                book.name = inputName.getText().toString();
                book.author = inputAuthor.getText().toString();
                book.price = Integer.parseInt(inputPrice.getText().toString());
                book.publish = new Date();

                XListDataSource<Book> source = (XListDataSource<Book>)
                        XDefaultDataRepo.getInstance().getSource(SOURCE_NAME);
                source.add(book);
            }
        });

        XListIdDBDataSourceImpl<Book> s2 = (XListIdDBDataSourceImpl<Book>)
                XDefaultDataRepo.getInstance().getSource(SOURCE_NAME);
        if (myAdapter == null) {
            myAdapter = new MyAdapter(s2);
        }
        listView.setAdapter(myAdapter);
        if (mDbListener == null) {
            mDbListener = new XWithDatabase.Listener<Book>() {
                @Override
                public void onSaveFinish(boolean result) {
                    if (result) {
                        XLog.d(TAG, "saveToDatabase success!");
                        myAdapter.notifyDataSetChanged();
                    } else {
                        XLog.d(TAG, "saveToDatabase error!");
                    }
                    XListIdDBDataSourceImpl<Book> source = (XListIdDBDataSourceImpl<Book>)
                            XDefaultDataRepo.getInstance().getSource(SOURCE_NAME);
                    source.unregisterDbListener(mDbListener);
                }

                @Override
                public void onLoadFinish(boolean result, List<Book> items) {
                    if (result) {
                        XLog.d(TAG, "initDataFromDB success! size = " + items.size());
                        myAdapter.notifyDataSetChanged();
                    } else {
                        XLog.d(TAG, "initDataFromDB error!");
                    }
                }
            };
            s2.registerDbListener(mDbListener);
        }
        if (mDataSourceListener == null) {
            mDataSourceListener = new XHandlerIdDataSourceListener<Book>() {
                @Override
                public void onChangeInUI() {
                    XLog.d(TAG, "onChange.");
                    myAdapter.notifyDataSetChanged();
                }

                @Override
                public void onReplaceInUI(List<Book> newItems, List<Book> oldItems) {
                    XLog.d(TAG, "onReplace. newItems.size=" + newItems.size() + ",oldItems.size=" + oldItems.size());
                    myAdapter.notifyDataSetChanged();
                }

                @Override
                public void onAddInUI(Book item) {
                    XLog.d(TAG, "onAdd." + item);
                    myAdapter.notifyDataSetChanged();
                }

                @Override
                public void onAddAllInUI(List<Book> items) {
                    XLog.d(TAG, "onAddAll. size=" + items.size());
                    myAdapter.notifyDataSetChanged();
                }

                @Override
                public void onDeleteInUI(Book item) {
                    XLog.d(TAG, "onDelete." + item);
                    myAdapter.notifyDataSetChanged();
                }

                @Override
                public void onDeleteAllInUI(List<Book> items) {
                    XLog.d(TAG, "onDeleteAll. size=" + items.size());
                    myAdapter.notifyDataSetChanged();
                }
            };
            s2.registerListener(mDataSourceListener);
        }
        if (mSyncDbListener == null) {
            mSyncDbListener = new XAsyncDatabaseListener<Book>(Book.class, (XWithId<Book>) s2);
            s2.registerListener(mSyncDbListener);// 测试实时同步数据库
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        XLog.d(TAG, "onPostCreate()!");
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        XLog.d(TAG, "onRestoreInstanceState()!");
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onStart() {
        XLog.d(TAG, "onStart()!");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        XLog.d(TAG, "onRestart()!");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        XLog.d(TAG, "onResume()!");
        super.onResume();
        XWithDatabase<Book> source = (XWithDatabase<Book>)
                XDefaultDataRepo.getInstance().getSource(SOURCE_NAME);
        source.loadFromDatabase();
    }

    @Override
    protected void onPostResume() {
        XLog.d(TAG, "onPostResume()!");
        super.onPostResume();
    }

    @Override
    protected void onPause() {
        XLog.d(TAG, "onPause()!");
        super.onPause();
//        XListIdDBDataSourceImpl<Book> source = (XListIdDBDataSourceImpl<Book>)
//                XDefaultDataRepo.getInstance().getSource(SOURCE_NAME);
//        source.saveToDatabase();
    }

    @Override
    public void onStop() {
        XLog.d(TAG, "onStop()!");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        XLog.d(TAG, "onDestroy()!");
        super.onDestroy();
        XListIdDBDataSourceImpl<Book> source = (XListIdDBDataSourceImpl<Book>)
                XDefaultDataRepo.getInstance().getSource(SOURCE_NAME);
        source.unregisterDbListener(mDbListener);
        source.unregisterListener(mDataSourceListener);
        source.unregisterListener(mSyncDbListener);
    }


    private class MyAdapter extends BaseAdapter {

        private XListDataSource<Book> source;

        public MyAdapter(XListDataSource<Book> source) {
            this.source = source;
        }

        @Override
        public int getCount() {
            return source.size();
        }

        @Override
        public Object getItem(int i) {
            return source.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        private class ViewHolder {
            public TextView idView;
            public TextView nameView;
            public TextView authorView;
            public TextView priceView;
            public TextView publishView;
            public Button delBtn;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            Object item = getItem(i);
            if (item == null) {
                return null;
            }
            final Book book = (Book) item;

            ViewHolder holder = null;
            if (convertView == null) {
                convertView = View.inflate(MyActivity.this, R.layout.list_item, null);
                holder = new ViewHolder();
                holder.idView = (TextView) convertView.findViewById(R.id.txt_id);
                holder.nameView = (TextView) convertView.findViewById(R.id.txt_name);
                holder.authorView = (TextView) convertView.findViewById(R.id.txt_author);
                holder.priceView = (TextView) convertView.findViewById(R.id.txt_price);
                holder.publishView = (TextView) convertView.findViewById(R.id.txt_publish);
                holder.delBtn = (Button) convertView.findViewById(R.id.btn_del);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.idView.setText(book.id);
            holder.nameView.setText(book.name);
            holder.authorView.setText(book.author);
            holder.priceView.setText("" + book.price);
            holder.publishView.setText(book.publish == null ? "空" : book.publish.toString());
            holder.delBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    source.delete(book);
                }
            });
            return convertView;
        }
    }
}
