package com.quectel.openglyuv.display.utils;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by wf on 2018/6/11.
 */
public class SDcardUtil {

    public static final String TAG = "CameraDemo.SDcardUtil";
    private static String time;
    /**
     * 获取外置SD卡路径
     */
    public static List<String> getExtSDCardPathList() {
        List<String> paths = new ArrayList<String>();
        String extFileStatus = Environment.getExternalStorageState();
        File extFile = Environment.getExternalStorageDirectory();
        //首先判断一下外置SD卡的状态，处于挂载状态才能获取的到
        if (extFileStatus.equals(Environment.MEDIA_MOUNTED) && extFile.exists() && extFile.isDirectory() && extFile.canWrite()) {
            //外置SD卡的路径
            paths.add(extFile.getAbsolutePath());
        }
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("mount");
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            int mountPathIndex = 1;
            while ((line = br.readLine()) != null) {
                // format of sdcard file system: vfat/fuse
                if ((!line.contains("fat") && !line.contains("fuse") && !line
                        .contains("storage"))
                        || line.contains("secure")
                        || line.contains("asec")
                        || line.contains("firmware")
                        || line.contains("shell")
                        || line.contains("obb")
                        || line.contains("legacy") || line.contains("data")) {
                    continue;
                }
                String[] parts = line.split(" ");
                int length = parts.length;
                if (mountPathIndex >= length) {
                    continue;
                }
                String mountPath = parts[mountPathIndex];
                if (!mountPath.contains("/") || mountPath.contains("data")
                        || mountPath.contains("Data")) {
                    continue;
                }
                File mountRoot = new File(mountPath);
                if (!mountRoot.exists() || !mountRoot.isDirectory()
                        || !mountRoot.canWrite()) {
                    continue;
                }
                boolean equalsToPrimarySD = mountPath.equals(extFile
                        .getAbsolutePath());
                if (equalsToPrimarySD) {
                    continue;
                }
                //扩展存储卡即TF卡或者SD卡路径
                paths.add(mountPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return paths;
    }


    public static String getTFSDCardPath() {
        List<String> list;
        list = getExtSDCardPathList();
        return list.get(list.size() - 1);
    }


    /**
     * 获取外置SD卡存储文件的绝对路径
     * Android 4.4以后
     *
     * @param context
     */
    public static String getExternalFileDir(Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append(getStoragePath(context, true).toString()).append(File.separator).toString();
        //sb.append("/mnt/media_rw/9408-790F").append(File.separator).toString();
        return sb.toString();
    }


    /**
     * 通过映射，获取外置内存卡路径
     *
     * @param mContext
     * @param is_removale
     * @return
     */
    public static String getStoragePath(Context mContext, boolean is_removale) {

        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
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
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "abing external cache dir paths = " + mContext.getExternalCacheDir().getPath());
        return mContext.getExternalCacheDir().getPath();
    }

    public static String getVideoName() {
//        int deviceId = SpUtil.readInt(CommonUtil.DEVICE_ID, 0);//设备ID号
        time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return time + "_"  + ".mp4";
    }

    public void deleteFilesProceed(String fileAbsolutePath, long n){
        Log.d(TAG, "fileAbsolutePath = " + fileAbsolutePath);

        int count = 0;
        File file = new File(fileAbsolutePath);
        Log.d(TAG, "checkPerssion(file) " + checkFilePerssion(file));

        File[] subFile = file.listFiles();
        if (subFile != null){
            for (int i = 0; i < subFile.length; i++) {
                if (!subFile[i].isDirectory()) {
                    if (++count > n){
                        break;
                    }
                    subFile[i].delete();
                    Log.d(TAG, "deleted : " + subFile[i].getName() + ", file.getUsableSpace = " + file.getUsableSpace());
                }
            }
        } else {
            Log.d(TAG, "directory is null");
        }
    }

    public int checkFilePerssion(File file){
        int nodeperssion = 0;
        if (file.canRead()){
            nodeperssion = nodeperssion + 4;
        }

        if (file.canWrite()){
            nodeperssion = nodeperssion + 2;
        }

        if (file.canExecute()){
            nodeperssion = nodeperssion + 1;
        }

        return nodeperssion;
    }
}
