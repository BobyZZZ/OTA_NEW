package com.ist.otaservice.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.ist.httplib.InvokeManager;
import com.ist.otaservice.MainApplication;

/**
 * Created by zhengshaorui
 * Time on 2018/9/13
 */

public class IstUtil {
    private static final String TAG = "IstUtil";
    private static Context sContext = MainApplication.CONTEXT;

    public static int PLANFORM_DEVICE_866 = 1;
    public static int PLANFORM_DEVICE_966 = 2;
    private static String HARDWAREID = InvokeManager.get(InvokeManager.SYSPROP_PRODUCT_HARDWAREID,"unknow");

    /**
     * 平台判定：866，966
     * @return
     */
    public static int getPlanform(){
        int ret =-1;
        String planform = HARDWAREID.substring ( 0,4 );
        if(planform.equals ("HW_6")){
            ret = PLANFORM_DEVICE_966;
        }else if(planform.equals("HW_5")){
            ret = PLANFORM_DEVICE_866;
        }
        return ret;
    }

    /**
     * 判断是否有网络
     * @return
     */
    public static boolean isNetworkPositive() {
        ConnectivityManager connectivityManager = (ConnectivityManager)sContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isWIFIC = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        boolean isETHERNETC = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET).isConnected();
        if (isWIFIC || isETHERNETC){
            return true;
        }
        return false;
    }




    public static  void showBarView(View mainview,View barview, final boolean showbar, int time){
        ObjectAnimator mainAnimator;
        ObjectAnimator barAnimator;
        if (showbar){
            //mBarLy.setVisibility(View.VISIBLE);
            mainAnimator = ObjectAnimator.ofFloat(mainview,"translationX",0,1280);
            barAnimator = ObjectAnimator.ofFloat(barview,"alpha",0,1);

        }else{
            //  mMainLy.setVisibility(View.VISIBLE);
            barAnimator = ObjectAnimator.ofFloat(barview,"alpha",1,0);
            mainAnimator = ObjectAnimator.ofFloat(mainview,"translationX",1280,0);
        }

        mainAnimator.setInterpolator(new LinearInterpolator());
        mainAnimator.setDuration(time);
        mainAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);


            }
        });
        mainAnimator.start();

        barAnimator.setInterpolator(new LinearInterpolator());
        barAnimator.setDuration(time);
        barAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

            }
        });
        barAnimator.start();
    }


    // 两次点击按钮之间的点击间隔不能少于1000毫秒
    private static final int MIN_CLICK_DELAY_TIME = 1000;
    private static long lastClickTime;

    public static boolean isFastClick() {
        boolean flag = false;
        long curClickTime = System.currentTimeMillis();
        if ((curClickTime - lastClickTime) <= MIN_CLICK_DELAY_TIME) {
            flag = true;
        }
        lastClickTime = curClickTime;
        return flag;
    }
	
    // 两次系统推送之间的间隔不能少于1天
    private static final long MIN_DAILY_DELAY_TIME = 1000*60*60*24;
    public static boolean dailycheck() {
		long timenow = System.currentTimeMillis();
		long timeota = getOtaTimeMillis();
		if(timeota == 0){
			setOtaTimeMillis(timenow);
			return false;
		}else if(timenow-timeota>MIN_DAILY_DELAY_TIME){
			setOtaTimeMillis(timenow);
			return false;
		}else{
			return true;
		}
	}

    public static long getOtaTimeMillis() {
		SharedPreferences sharedPreferences= sContext.getSharedPreferences("data", Context .MODE_PRIVATE);
		long time=sharedPreferences.getLong("otatime",0);
		return time;
	}
    public static void setOtaTimeMillis(long time) {
		SharedPreferences sharedPreferences= sContext.getSharedPreferences("data",Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putLong("otatime", time);    
		editor.commit();
	}
    public static boolean getIgnoreFlag() {
        SharedPreferences sharedPreferences= sContext.getSharedPreferences("data", Context .MODE_PRIVATE);
        boolean flag=sharedPreferences.getBoolean("ignoreflag",false);
        return flag;
    }
    public static void setIgnoreFlag(boolean flag) {
        SharedPreferences sharedPreferences= sContext.getSharedPreferences("data",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("ignoreflag", flag);
        editor.commit();
    }

    public static boolean getAlertDialogIgnoreFlag() {
        SharedPreferences sharedPreferences= sContext.getSharedPreferences("data", Context .MODE_PRIVATE);
        boolean flag=sharedPreferences.getBoolean("dialogignoreflag",false);
        return flag;
    }
    public static void setAlertDialogIgnoreFlag(boolean flag) {
        SharedPreferences sharedPreferences= sContext.getSharedPreferences("data",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("dialogignoreflag", flag);
        editor.commit();
    }

    /**
     * 是否设置自动检测新版本
     * @return
     */
    public static boolean shouldAutoDetectNewVersion() {
        return InvokeManager.get(InvokeManager.SYSPROP_OAT_AUTODET_FLAG,"false").equals("true");
    }

    /**
     * ABUpdateMode flag 标记，用于控制ota升级时不会触发第二次升级
     * @return
     */
    public static boolean getInABUpdateMode() {
        SharedPreferences sharedPreferences= sContext.getSharedPreferences("data", Context .MODE_PRIVATE);
        boolean flag=sharedPreferences.getBoolean("abupdate",false);
        return flag;
    }
    public static void setInABUpdateMode(boolean flag) {
        SharedPreferences sharedPreferences= sContext.getSharedPreferences("data",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("abupdate", flag);
        editor.commit();
    }

}
