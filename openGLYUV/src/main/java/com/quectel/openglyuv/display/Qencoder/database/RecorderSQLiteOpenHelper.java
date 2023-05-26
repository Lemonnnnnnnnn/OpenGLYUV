package com.quectel.openglyuv.display.Qencoder.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Carson_Ho on 16/11/18.
 */
public class RecorderSQLiteOpenHelper extends SQLiteOpenHelper {


    //数据库版本号
    private static Integer Version = 1;
    private static String TAG = "RSQLHelper";
    private Context mContext = null;

    private boolean scanDirectory = false;
    private Lock lock = new ReentrantLock();

    public void setScanDirectory(boolean scanDirectory) {
        this.scanDirectory = scanDirectory;
    }

    //在SQLiteOpenHelper的子类当中，必须有该构造函数
    public RecorderSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                                    int version) {
        //必须通过super调用父类当中的构造函数
        super(context, name, factory, version);
        this.mContext = context;
    }

    //参数说明
    //context:上下文对象
    //name:数据库名称
    //param:factory
    //version:当前数据库的版本，值必须是整数并且是递增的状态
    public RecorderSQLiteOpenHelper(Context context, String name, int version) {
        this(context, name, null, version);
    }


    public RecorderSQLiteOpenHelper(Context context, String name) {
        this(context, name, Version);
    }

    //只有在第一次创建数据库的时候才会回调该接口
    @Override
    public void onCreate(SQLiteDatabase db) {

        Log.i(TAG, " create database and table");
        //创建了数据库并创建一个叫records的表
        //SQLite数据创建支持的数据类型： 整型数据，字符串类型，日期类型，二进制的数据类型
        String sql = "create table if not exists recorder(id  INTEGER PRIMARY KEY AUTOINCREMENT,recorderPath varchar(512),insertTime int)";
        //execSQL用于执行SQL语句
        //完成数据库的创建
        db.execSQL(sql);
        //数据库实际上是没有被创建或者打开的，直到getWritableDatabase() 或者 getReadableDatabase() 方法中的一个被调用时才会进行创建或者打开
//        if (scanDirectory) {
//            RecorderUtil.scanDirectoryAndInsertRealize(mContext);
//        }

    }

    //数据库升级时调用
    //如果DATABASE_VERSION值被改为2,系统发现现有数据库版本不同,即会调用onUpgrade（）方法
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        System.out.println("更新数据库版本为:" + newVersion);
    }

    public void insertRecorder(String path, long insertTime) {
        synchronized (lock) {
            SQLiteDatabase sqLiteDatabase = DBManager.getInstance(this).getWritableDatabase();
            // 创建ContentValues对象
            ContentValues values = new ContentValues();

            // 向该对象中插入键值对
            values.put("recorderPath", path);
            values.put("insertTime", insertTime);

            // 调用insert()方法将数据插入到数据库当中
            sqLiteDatabase.insert("recorder", null, values);
            //关闭数据库
            DBManager.getInstance(this).closeDatabase();
        }

    }

    public void updateRecorderPath(String path, String updatePath) {
        synchronized (lock) {
            String sqlData = null;
            SQLiteDatabase sqLiteDatabase = DBManager.getInstance(this).getWritableDatabase();
            sqlData = "update recorder set  recorderPath = '" + updatePath + "' where recorderPath = '" + path + "'";
            sqLiteDatabase.execSQL(sqlData); //删除两条 可能有备份码流
            //关闭数据库
            DBManager.getInstance(this).closeDatabase();
        }
    }

    //    @Keep
    public String queryFirstRecorderPathAndDelete() {
        String path = null;
        synchronized (lock) {
            SQLiteDatabase sqLiteDatabase = DBManager.getInstance(this).getReadableDatabase();

            Cursor cursor = sqLiteDatabase.rawQuery("select * from recorder order by id asc limit 0,2", null); //删除两条 可能有备份码流

            long insertTime;
            String id;
            File dFile;
            int tryTimes = 3;  //检测删除的次数
            //将光标移动到下一行，从而判断该结果集是否还有下一条数据
            //如果有则返回true，没有则返回false
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
            DBManager.getInstance(this).closeDatabase();
        }
        return path;
    }

    public void truncateTable(String tableName) {
        synchronized (lock) {
            String sqlData = null;
            SQLiteDatabase sqLiteDatabase = DBManager.getInstance(this).getWritableDatabase();
            sqlData = "delete from " + tableName;
            sqLiteDatabase.execSQL(sqlData); //清空表中数据

            sqlData = "update sqlite_sequence set seq=0 where name='" + tableName + "'";  //重置自增长数据

            sqLiteDatabase.execSQL(sqlData); //更新索引
            //关闭数据库
            DBManager.getInstance(this).closeDatabase();
        }
    }
}
