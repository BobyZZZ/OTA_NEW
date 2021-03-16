package com.ist.httplib;


import android.util.Log;

import com.ist.httplib.bean.BaseOtaRespone;
import com.ist.httplib.bean.customer.KangjiaBean;
import com.ist.httplib.bean.customer.SkyworthBean;
import com.ist.httplib.net.CommonSubscribe;
import com.ist.httplib.net.NetErrorMsg;
import com.ist.httplib.net.callback.UpdateListenerAdapter;
import com.ist.httplib.net.retrofit.HttpCreate;
import com.ist.httplib.net.retrofit.HttpServer;
import com.ist.httplib.utils.RxUtils;

/**
 * Created by zhengshaorui
 * Time on 2018/10/17
 */

public class CustomConverManager {
    private static final String TAG = "CustomConverManager";
    private String mUrl;
    private UpdateListenerAdapter mListenerAdapter;
    private HttpServer mHttpServer;
    private static class Holder {
        static final CustomConverManager INSTANCE = new CustomConverManager();
    }

    public static CustomConverManager getInstance() {
        return Holder.INSTANCE;
    }

    private CustomConverManager() {
        mHttpServer = HttpCreate.getService();
    }

    public CustomConverManager config(String url, UpdateListenerAdapter listenerAdapter){
        mUrl = url;
        mListenerAdapter = listenerAdapter;
        return this;
    }

    /**
     * 本地测试
     */
    public void localTestCheck() {
        mHttpServer.get(mUrl)
                .compose(RxUtils.<BaseOtaRespone>rxScheduers())
                .subscribeWith(new CommonSubscribe<BaseOtaRespone>(mListenerAdapter) {
                    @Override
                    public void onResponse(BaseOtaRespone otaRespone) {
                        mListenerAdapter.checkUpdate(true,otaRespone);
                    }
                });

    }

    /**
     * 创维服务器规则校验
     */
    public void skyworthCheck() {
        mHttpServer.getSkyWorkJson(mUrl)
                .compose(RxUtils.<BaseOtaRespone<SkyworthBean>>rxScheduers())
                .subscribeWith(new CommonSubscribe<BaseOtaRespone<SkyworthBean>>(mListenerAdapter) {
                    @Override
                    public void onResponse(BaseOtaRespone<SkyworthBean> otaRespone) {
                        SkyworthBean bean = otaRespone.data;
                        String modelName = InvokeManager.get(InvokeManager.SYSPROP_PRODUCT_MODEL,"MS648");
                        if (bean.modelName.equals(modelName)){
                            mListenerAdapter.checkUpdate(true,otaRespone);
                        }else{
                            mListenerAdapter.error(NetErrorMsg.RULES_NOT_SAME,"Different rules");
                        }

                    }
                });
    }


    /**
     * 康佳服务器规则校验
     */
    public void kangjiaCheck() {
        Log.d(TAG, "check update: " + mUrl);
        mHttpServer.getKangjiaJson(mUrl)
                .compose(RxUtils.<BaseOtaRespone<KangjiaBean>>rxScheduers())
                .subscribeWith(new CommonSubscribe<BaseOtaRespone<KangjiaBean>>(mListenerAdapter) {
                    @Override
                    public void onResponse(BaseOtaRespone<KangjiaBean> otaRespone) {
                        KangjiaBean bean = otaRespone.data;
                        InvokeManager.set(InvokeManager.SYSPROP_OTA_HAVEPUSH_FLAG, otaRespone.version);
                        mListenerAdapter.checkUpdate(true,otaRespone);

                    }
                });
    }

}
