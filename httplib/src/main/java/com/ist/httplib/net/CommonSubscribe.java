package com.ist.httplib.net;

import android.util.Log;

import com.ist.httplib.InvokeManager;
import com.ist.httplib.bean.BaseOtaRespone;
import com.ist.httplib.net.callback.UpdateListenerAdapter;
import com.ist.httplib.utils.VersionComparator;

import io.reactivex.observers.ResourceObserver;

public abstract class CommonSubscribe<T extends BaseOtaRespone> extends ResourceObserver<T> {
    private static final String TAG = "CommonSubscribe";
    private UpdateListenerAdapter mView;

    public CommonSubscribe(UpdateListenerAdapter iview) {
        mView = iview;

    }


    @Override
    public void onNext(T t) {
        BaseOtaRespone deviceBean = getDeviceVersion();

        if (t.customID.equals(deviceBean.customID)){
            int status  = VersionComparator.compare(t.version,deviceBean.version);
            Log.d(TAG, "zsr --> 服务器升级包版本: "+t.version+" / 当前系统版本: "+deviceBean.version + ",status: " + status);
            if (status > 0){
                onResponse(t);
            }else{
//                mView.checkUpdate(false,deviceBean);
                mView.error(NetErrorMsg.CURRENT_IS_NEWEST,"This is the latest version");
            }

        }else{
            mView.error(NetErrorMsg.ID_NOT_SAME,"customID not the same");
        }
    }

    public abstract  void onResponse(T t);

    @Override
    public void onError(Throwable e) {
        if (mView == null){
            return;
        }
        Log.d(TAG, "zsr --> onError: "+e.toString());
        mView.error(NetErrorMsg.SERVER_NOT_FOUND,e.toString());



    }

    @Override
    public void onComplete() {

    }

    private BaseOtaRespone getDeviceVersion() {
        BaseOtaRespone bean = new BaseOtaRespone();
        bean.customID = InvokeManager.get(InvokeManager.SYSPROP_CUSTOMER_NAME_SUB, "unknow");
        bean.version = InvokeManager.get(InvokeManager.SYSPROP_VERSION, "V1.0.0.0");
        return bean;
    }
}