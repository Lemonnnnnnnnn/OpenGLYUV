package com.quectel.openglyuv.display.utils;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;


import com.quectel.openglyuv.display.Qencoder.database.RecorderSQLiteOpenHelper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Chapin
 */

//@Keep
public class StorageUtil {

    //@Keep
    private static String userRootPath = null; //用于自定义 录像文件的根目录
    private static String TAG = "StorageUtil";

    //@Keep
    public static void setRootStoragePath(String path) {
        userRootPath = path;
    }

    private static long CAPACITY_LEFT_IN_SDCARD = 2048L;  //2G;
    // storage delete Threshold, it is used for "Loop Coverage" function
    private static long CAPACITY_REMOVE_LIMIT = 200L;
    // debug switch for qcarlib
    public static boolean GCARLIB_DEBUG = false;


    private static long recycleCapacity = 20 * 1024L;
    private static final long RESERVED_CAPACITY = 200L;

    private static RecorderSQLiteOpenHelper rsqliteHelper = null;

    public static long getCapacityLeftInSdcard() {
        if (recycleCapacity != 0) {
            return recycleCapacity;
        }
        return CAPACITY_LEFT_IN_SDCARD;
    }

    public static void setRecycleCapacityMB(int capacityMB) {
        recycleCapacity = capacityMB;
    }

    public static long getRecycleCapacity() {
        return recycleCapacity;
    }

    public static String removeTempInPath(String path) {
        return path.replace("/temp", "");
    }

    //@Keep
    public static String getStoragePath(Context mContext, boolean is_removale) {

        if (userRootPath != null) {
            return userRootPath;
        }
        if (mContext == null) {
            Log.d(TAG, "mContext == null");
            //return "";
        }
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (is_removale == removable) {
                    return path;
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * public static DiskStat getDiskCapacity(String path) {
     * StatFs stat = new StatFs(path);
     * <p>
     * long blockSize = stat.getBlockSizeLong();
     * long totalBlockCount = stat.getBlockCountLong();
     * long feeBlockCount = stat.getAvailableBlocksLong();
     * return new DiskStat(blockSize * feeBlockCount / 1024 / 1024, blockSize
     * totalBlockCount / 1024 / 1024);
     * }
     */

    public static long getDiskRemainCapacityMB(String diskPath) {
        StatFs stat = new StatFs(diskPath);
        long capacity = stat.getBlockSizeLong() * stat.getAvailableBlocksLong() / 1024 / 1024;
        return capacity;
    }

    /**
     * 获取目录下所有文件(按时间排序)
     *
     * @param path
     * @return
     */
    public static void getStorageFilesSort(String path, ArrayList<File> list) {
        getStorageFiles(path, list);
        if (list != null && list.size() > 0) {
            //升序排列，时间靠前的排在前面
            Collections.sort(list, new Comparator<File>() {
                public int compare(File file, File newFile) {
                    if (file.lastModified() > newFile.lastModified()) {
                        return 1;
                    } else if (file.lastModified() == newFile.lastModified()) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
            });
        }
    }

    /**
     * 获取目录下所有文件
     *
     * @param path
     * @param files
     * @return void
     */
    public static void getStorageFiles(String path, ArrayList<File> files) {
        File realFile = new File(path);
        if (realFile.isDirectory()) {
            File[] subfiles = realFile.listFiles();
            for (File file : subfiles) {
                if (file.isDirectory()) {
                    getStorageFiles(file.getAbsolutePath(), files);
                } else {
                    //for example main_0_2_20200708025845.mp4|merge_0_20200708025845.mp4
                    if (file.getName().matches("^[main|collision|child].*_.*_.*[mp4|ts|h264]$") || file.getName().matches("^[merge].*_.*[mp4|ts]$"))
                        files.add(file);
                }
            }
        }
    }

    public static String getNextVideoFilePath(Context context, String videoType, String channelID) {
        if (TextUtils.isEmpty(videoType) || TextUtils.isEmpty(channelID)) {
            Log.e(TAG, "empty videoType or channelID");
            return "";
        }
//        String rootPath = getStoragePath(context, true);
        String rootPath = context.getExternalFilesDir(null).getAbsolutePath();
        if (TextUtils.isEmpty(rootPath)) {
            Log.e(TAG, "no sdcard found");
            return "";
        }
        File path;
        String filePath = rootPath + "/DVR/" + channelID;
        path = new File(filePath);
        if (!path.exists()){
            path.mkdirs();
        }
        String fileName = filePath+"/"+videoType + "_ch_"
                + channelID + "_" + getTimeStamp() + ".mp4";
        Log.d(TAG, "filePath =" + fileName);
        File videoFile = new File(fileName);
        if (!videoFile.exists()) {
            boolean ret = false;
            try {
                ret = videoFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!ret) {
                Log.d(TAG, "createNewFile failed");
                return "";
            }
        }
        /**
         SQHelper mSQHelper = SQHelper.getInstance(context);
         long remaining = getDiskRemainCapacityMB(rootPath);
         Log.d(TAG, "remaining =" + remaining);
         if (remaining < recycleCapacity) {
         while (remaining < recycleCapacity + RESERVED_CAPACITY) {
         boolean ret = mSQHelper.queryFirstRecorderPathAndDelete();
         if (!ret) {
         Log.d(TAG, "getNextVideoFilePath failed");
         return "";
         }
         }
         }
         */
        long remaining = getDiskRemainCapacityMB(rootPath);
        Log.d(TAG, "remaining =" + remaining);
        if (remaining < recycleCapacity) {
            while (remaining < recycleCapacity + RESERVED_CAPACITY) {
                String deletedPath = getRsqliteHelper(context).queryFirstRecorderPathAndDelete();
                if (TextUtils.isEmpty(deletedPath)) {
                    Log.d(TAG, "getNextVideoFilePath failed");
                    return "";
                }
            }
        }
        return fileName;
    }

    public static String getNextVideoFilePathOld() {
        String path = getAndroidMoviesFolder().getAbsolutePath() + "/"
                + new SimpleDateFormat("yyyyMM_dd-HHmmss", Locale.US).format(new Date()) + ".mp4";
        File videoFile = new File(path);
        if (!videoFile.exists()) {
            boolean ret = false;
            try {
                ret = videoFile.createNewFile();
                Log.d(TAG, "createNewFile");
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!ret) {
                Log.d(TAG, "createNewFile failed");
                return "";
            }
        }
        return path;
    }

    public static File getAndroidMoviesFolder() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
    }

    private static String getTimeStamp() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMM_dd-HHmmss", Locale.US);
        return simpleDateFormat.format(new Date(System.currentTimeMillis()));
    }

    public static synchronized RecorderSQLiteOpenHelper getRsqliteHelper(Context context) {
        if (rsqliteHelper == null) {
            rsqliteHelper = new RecorderSQLiteOpenHelper(context, "AISrecorder.db");
        }

        return rsqliteHelper;
    }
}
