package com.ist.httplib.utils;

import android.content.Context;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.format.Formatter;
import android.util.Log;

import com.ist.httplib.OtaLibConfig;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhengshaorui
 * Time on 2018/9/15
 */

public class DiskUtil {
    private static final String TAG = "DiskUtil";
    public static final int MB   = 1048576;
    public static final int GB   = 1048576 * 1024;

    public static List<String> getUSBInfo(){
        List<String> paths = new ArrayList<>();
        StorageManager storageManager = (StorageManager) OtaLibConfig.getBuilder().getContext().getSystemService(Context.STORAGE_SERVICE);
            try {
            Class<?> storageClass = Class.forName("android.os.storage.StorageManager");
            Method method = storageClass.getDeclaredMethod("getVolumeList");
            method.setAccessible(true);
            StorageVolume[] volumes = (StorageVolume[]) method.invoke(storageManager);
            for (StorageVolume volume : volumes) {
                Class<?> volumeClass = Class.forName("android.os.storage.StorageVolume");
                Method getpath = volumeClass.getDeclaredMethod("getPath");
                String path = (String) getpath.invoke(volume);
                Log.e("Keven", "getUSBInfo: path =="+path);
                if ("/storage/emulated/0".equals(path)){
                    continue;
                }
//                if (path.contains("/mnt/usb")) {
                if (path.contains("/storage")) {//add zyc : for 866 and 966
                    paths.add(path);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return paths;
    }

    public static String formatFileSize(Context context,long sizeBytes) {
        if (sizeBytes == 0) {
            return "0KB";
        }
        String result;
        float l;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (sizeBytes >= GB) {
                l = sizeBytes * 1f / GB;
                result = String.format("%.2f",l) + "GB";
            } else {
                l = sizeBytes * 1f / MB;
                result = String.format("%.2f",l) + "MB";

            }
        } else {
            result =  Formatter.formatFileSize(context,sizeBytes);
        }
        return result;
    }
}
