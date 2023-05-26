package com.quectel.openglyuv.display.Qencoder.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.quectel.openglyuv.display.Qencoder.Constants;

import java.io.File;

public class SQHelper extends SQLiteOpenHelper {

    private final static String TAG = "SQHelper";
    private final static int DATA_BASE_VERSION = 1;
    private static SQHelper mSQHelper;
    private SQLiteDatabase msqLiteDatabase;
    //private Lock mLock;

    public static SQHelper getInstance(Context context) {
        if (null == mSQHelper) {
            mSQHelper = new SQHelper(context, Constants.DATA_BASE_RECORDER, null, DATA_BASE_VERSION);
        }
        return mSQHelper;
    }

    private SQHelper(Context context, String tableName, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, tableName, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d(TAG, "sqLiteDatabase =" + sqLiteDatabase.toString());
        msqLiteDatabase = sqLiteDatabase;
        String sql = "create table if not exists recorder(id  INTEGER PRIMARY KEY AUTOINCREMENT," +
                "recorderPath varchar(512),insertTime int)";
        sqLiteDatabase.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public void insertRecord(String recordName, long insetTime) {
        synchronized (this) {
            Log.d(TAG, "insertRecord||recordName =" + recordName);
            SQLiteDatabase sqLiteDatabase = getWritableDatabase();
            // 创建ContentValues对象
            ContentValues values = new ContentValues();
            // 向该对象中插入键值对
            values.put("recorderPath", recordName);
            values.put("insertTime", insetTime);
            // 调用insert()方法将数据插入到数据库当中
            sqLiteDatabase.insert(Constants.DATA_BASE_RECORDER, null, values);
            //关闭数据库
            sqLiteDatabase.close();
            Log.d(TAG, "insertRecord||done");
        }
    }

    @SuppressLint("Range")
    public boolean queryFirstRecorderPathAndDelete() {
        Log.d(TAG, "queryFirstRecorderPathAndDelete");
        String path;
        boolean isSucceed = false;
        synchronized (this) {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("select * from recorder order by id asc limit 0,2", null); //删除两条 可能有备份码流

            long insertTime;
            String id;
            File dFile;
            int tryTimes = 3;  //检测删除的次数
            //将光标移动到下一行，从而判断该结果集是否还有下一条数据
            //如果有则返回true，没有则返回false
            Log.d(TAG, "cursor processing");
            while (cursor.moveToNext()) {
                id = cursor.getString(cursor.getColumnIndex("id"));
                path = cursor.getString(cursor.getColumnIndex("recorderPath"));
                insertTime = cursor.getLong(cursor.getColumnIndex("insertTime"));
                //输出查询结果
                Log.d(TAG, "查询到的数据是:" + "id = " + id + " path: " + path + "  " + "insertTime: " + insertTime);

                dFile = new File(path);
                int i = 0;
                for (; i < tryTimes; i++) {
                    if (dFile.delete()) {
                        sqLiteDatabase.delete("recorder", " id = ?", new String[]{id});
                        isSucceed = true;
                        break;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
                //如果删除失败
                //  1、检验文件是否存在，如果不存在，直接删除数据库记录
                //  2、如果文件存在，将数据库记录更新到最新的，留到最后删除，防止删除失败，一直在删除该文件
                if (i == 3) {
                    if (!dFile.exists()) {
                        sqLiteDatabase.delete("recorder", " id = ?", new String[]{id});
                        Log.w(TAG, " check file path = " + path + " not exsit delete data in database");
                    } else {
                        Cursor lastQCurSor = sqLiteDatabase.rawQuery("select * from recorder order by id desc limit 0,1", null); //查找最新的记录
                        //将光标移动到下一行，从而判断该结果集是否还有下一条数据
                        //如果有则返回true，没有则返回false
                        while (lastQCurSor.moveToNext()) {
                            id = lastQCurSor.getString(lastQCurSor.getColumnIndex("id"));
                            //输出查询结果
                            Log.w(TAG, "queryLastRecorder 查询到的数据是:" + "id: " + id);
                            String sqlData = "update recorder set  id = '" + (Integer.parseInt(id) + 1) + "' where recorderPath = '" + path + "'";
                            sqLiteDatabase.execSQL(sqlData);
                        }
                        lastQCurSor.close();
                    }
                }
            }
            cursor.close(); //关闭光标
            //关闭数据库
            sqLiteDatabase.close();
        }
        return isSucceed;
    }

    public void truncateTable(String tableName) {
        synchronized (this) {
            String sqlData = null;
            SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
            sqlData = "delete from " + tableName;
            sqLiteDatabase.execSQL(sqlData); //清空表中数据

            sqlData = "update sqlite_sequence set seq=0 where name='" + tableName + "'";  //重置自增长数据
            sqLiteDatabase.execSQL(sqlData); //更新索引
            //关闭数据库
            sqLiteDatabase.close();
        }
    }
}
