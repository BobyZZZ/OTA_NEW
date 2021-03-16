package com.ist.otaservice.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import java.util.List;

public class ActivityUtil {
    static String TAG = "ActivityUtil";

    public static boolean isTopActivity(Context context,Activity activity) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            List<ActivityManager.RunningTaskInfo> runningTasks = am.getRunningTasks(1);
            if (runningTasks != null && !runningTasks.isEmpty()) {
                ActivityManager.RunningTaskInfo runningTaskInfo = runningTasks.get(0);
                ComponentName topActivity = runningTaskInfo.topActivity;
                String packageName = runningTaskInfo.topActivity.getPackageName();
                String className = runningTaskInfo.topActivity.getClassName();
//                Log.d(TAG, "getTopActivity packageName: " + packageName + ",className: " + className);
                ComponentName targetComp = activity.getComponentName();
/*                Log.d(TAG, "getTopActivity targetComp's packageName: " + targetComp.getPackageName()
                        + ",targetComp's className: " + targetComp.getClassName());*/
                return topActivity.equals(targetComp);
            }
        }
        return false;
    }
}
