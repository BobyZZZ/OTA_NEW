package com.ist.httplib;


import android.graphics.Bitmap;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Created by zhengshaorui
 * Time on 2018/9/11
 */

public class InvokeManager {
    private static final String TAG = "InvokeManager";
    //为了方便混淆，用反射的方式
    /*-----------------所以属性均放到这里控制---------------------------------------------------------*/
    //当前客户编译名称：
    public static final String  SYSPROP_CUSTOMER_NAME_SUB = "ro.build.customer.base.sub";
    //当前客户版控信息：
    public static final String  SYSPROP_VERSION_CUSTOMER = "persist.sys.versionCustomer";
    //当前固件的版本号：
    public static final String  SYSPROP_VERSION = "persist.sys.product.version";
    //是否debug模式：
    public static final String  SYSPROP_IST_DEBUG_FLAG = "ist.ota.debug";
    //方案名称：
    public static final String  SYSPROP_PRODUCT_NAME = "ro.product.name";
    //方案名称：
    public static final String  SYSPROP_PRODUCT_MODEL = "ro.product.model";
    //用于区分版型方案的硬件名称ID：
    public static final String  SYSPROP_PRODUCT_HARDWAREID = "ro.build.hardware.platform";
    //自动更新检测标记位：
    public static final String  SYSPROP_OAT_AUTODET_FLAG = "persist.sys.otaautodet";
    //966用于启动服务的属性：
    public static final String  SYSPROP_CTL_START = "ctl.start";
    //当检测有ota更新时设置的标记位：
    public static final String  SYSPROP_OTA_HAVEPUSH_FLAG = "persist.sys.otapush";
    //OTA URL PART1：
    public static final String  SYSPROP_OTA_URL_PART1 = "persist.sys.ota_url";
    //OTA URL PART2：
    public static final String  SYSPROP_OTA_URL_PART2 = "persist.sys.ota_url1";
    //OTA URL PART RELEASE and DEBUG state flag：
    public static final String  SYSPROP_OTA_URL_PART_STATE = "persist.sys.otadebug";
    //OTA URL PART SN：
    public static final String  SYSPROP_OTA_URL_PART_SN = "persist.sys.ota_url.sn";
    //OTA URL PART ADDR：
    public static final String  SYSPROP_OTA_URL_PART_ADDR = "persist.sys.ota_url.addr";
    //IST服务器
    public static final String  IST_SERVER = "www.isolution-cloud.com:8234";
    /*------------------------------------------------------------------------------------------------*/
    public static void  set(String key,String value){
        try {
            Class<?> pClass = Class.forName("android.os.SystemProperties");
            Method method = pClass.getMethod("set",String.class,String.class);
            method.setAccessible(true);
            method.invoke(pClass,key,value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String get(String key,String defaultValue){
        try {
            Class<?> pClass = Class.forName("android.os.SystemProperties");
            Method method = pClass.getMethod("get",String.class,String.class);
            method.setAccessible(true);
            return (String) method.invoke(pClass,key,defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public static Bitmap screenshot(int widht, int height){
        Bitmap bitmap = null;
        try {
            Class<?> sClass = Class.forName("android.view.SurfaceControl");
            Method method = sClass.getMethod("screenshot",int.class,int.class);
            method.setAccessible(true);
            bitmap = (Bitmap) method.invoke(sClass,widht,height);

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "zsr --> screenshot: "+e.toString());
        }
        return bitmap;
    }
}
