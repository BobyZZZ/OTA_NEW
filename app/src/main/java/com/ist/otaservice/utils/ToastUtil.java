package com.ist.otaservice.utils;

import android.app.Application;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ist.otaservice.MainApplication;

public class ToastUtil {

    private static Toast toast;


    private static Toast initToast(CharSequence message, int duration, boolean isCustomToast) {
        if (toast == null) {
            toast = Toast.makeText( MainApplication.CONTEXT, message, duration);

        }
        {
            toast.setText(message);
        }


//        toast.setDuration(duration);
//        toast.setGravity(Gravity.LEFT | Gravity.TOP,
//                (int) MainApplication.CONTEXT.getResources().getDimension(R.dimen.y900),
//                (int) MainApplication.CONTEXT.getResources().getDimension(R.dimen.y880));

        return toast;
    }


    /**
     * 短时间显示Toast
     *
     * @param message
     */
    public static void showShort(CharSequence message) {
        initToast(message, Toast.LENGTH_SHORT,true).show();
    }


    /**
     * 短时间显示Toast
     *
     * @param strResId
     */
    public static void showShort(int strResId) {
//		Toast.makeText(context, strResId, Toast.LENGTH_SHORT).show();
        initToast(MainApplication.CONTEXT.getResources().getText(strResId), Toast.LENGTH_SHORT,true).show();
    }

    /**
     * 长时间显示Toast
     *
     * @param message
     */
    public static void showLong(CharSequence message) {
        initToast(message, Toast.LENGTH_LONG,true).show();
    }

    /**
     * 长时间显示Toast
     *
     * @param strResId
     */
    public static void showLong(int strResId) {
        initToast(MainApplication.CONTEXT.getResources().getText(strResId), Toast.LENGTH_LONG,true).show();
    }

    /**
     * 自定义显示Toast时间
     *
     * @param message
     * @param duration
     */
    public static void show(CharSequence message, int duration) {
        initToast(message, duration,true).show();
    }

    /**
     * 自定义显示Toast时间
     *
     * @param context
     * @param strResId
     * @param duration
     */
    public static void show( Context context, int strResId, int duration) {
        initToast(context.getResources().getText(strResId), duration,true).show();
    }


}
