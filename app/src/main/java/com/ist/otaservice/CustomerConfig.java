package com.ist.otaservice;


import android.text.TextUtils;
import android.util.Log;

import com.ist.httplib.InvokeManager;

/**
 * Created by zhengshaorui
 * Time on 2018/9/13
 */

public class CustomerConfig {
    private static final String TAG = "CustomerConfig";
    //是否启用本地服务器
    public static boolean ISDEBUG = false;
    //是否启动断点续传，即数据库保存下载进度
    public static boolean USE_DB = true;
    private static final String CURRENT_ID = InvokeManager.get(InvokeManager.SYSPROP_CUSTOMER_NAME_SUB,"IST_0101");


    public static String getCustomId(){
        Log.e("Keven", "getCustomId: CURRENT_ID =="+CURRENT_ID);
        return CURRENT_ID;
    }

    public static String getBaseUrl(){
        String url = InvokeManager.get(InvokeManager.SYSPROP_OTA_URL_PART1, "");//http://192.168.0.250:8080/download/866/0601/update.json
        String url1 = InvokeManager.get(InvokeManager.SYSPROP_OTA_URL_PART2, "");
        String sn = InvokeManager.get(InvokeManager.SYSPROP_OTA_URL_PART_SN, "");
        String addr = InvokeManager.get(InvokeManager.SYSPROP_OTA_URL_PART_ADDR, "");
        String setting = InvokeManager.get(InvokeManager.SYSPROP_OTA_URL_PART_STATE, "release");

        StringBuffer sb = new StringBuffer();
        if (isISTServer()) {
            //ist服务器根据硬件状态、版控、客户id进行拼接
            sb.append(url);//http://www.isolution-cloud.com:8234/ota/update.json
            //?H=HW_502&V=IST_0601&S=V001&state=release
            sb.append("?H=").append(InvokeManager.get(InvokeManager.SYSPROP_PRODUCT_HARDWAREID,"HW_502"));
            sb.append("&V=").append(InvokeManager.get(InvokeManager.SYSPROP_CUSTOMER_NAME_SUB,"IST_0101"));
            sb.append("&S=").append(InvokeManager.get(InvokeManager.SYSPROP_VERSION_CUSTOMER,"V001"));
            if (!TextUtils.isEmpty(sn)) {
                sb.append("&sn=").append(sn);
            }
            if (!TextUtils.isEmpty(sn)) {
                sb.append("&addr=").append(addr);
            }
            sb.append("&state=").append(setting);
        } else {
            //客户服务器直接拼接url1和url2
            sb.append(url).append(url1);
        }
        String finalUrl = sb.toString().trim();
        Log.d(TAG, "baseUrl: " + finalUrl);
        return finalUrl;
    }


    /**
     * 是否使用的是ist服务器
     * @return
     */
    private static boolean isISTServer() {
        return InvokeManager.get(InvokeManager.SYSPROP_OTA_URL_PART1, "").contains(InvokeManager.IST_SERVER);
    }
}


