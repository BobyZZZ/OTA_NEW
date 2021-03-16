package com.ist.httplib.mvp.model;

import android.content.Context;
import android.util.Log;

import com.ist.httplib.OtaLibConfig;
import com.ist.httplib.mvp.contract.OtaContract;
import com.ist.httplib.status.UsbCheckStatus;
import com.ist.httplib.utils.DiskUtil;
import com.ist.httplib.utils.SprefUtils;

import java.io.File;
import java.util.List;

/**
 * Created by zhengshaorui
 * Time on 2018/9/26
 */

public class TipsModel {

    private OtaLibConfig.Builder mBuilder;
    private Context mContext;
    private OtaContract.ITipsView mIView;
    public static TipsModel create(OtaContract.ITipsView iView){
        return new TipsModel(iView);
    }
    private TipsModel(OtaContract.ITipsView iView){
        mBuilder = OtaLibConfig.getBuilder();
        mContext = mBuilder.getContext();
        mIView = iView;
    }

}
