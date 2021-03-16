package com.ist.httplib.mvp.present;

import android.util.Log;

import com.ist.httplib.bean.BaseOtaRespone;
import com.ist.httplib.mvp.base.BasePresent;
import com.ist.httplib.mvp.contract.OtaContract;
import com.ist.httplib.mvp.model.MainModel;
import com.ist.httplib.mvp.model.TipsModel;
import com.ist.httplib.net.HttpTaskManager;
import com.ist.httplib.net.NetErrorMsg;
import com.ist.httplib.net.callback.UpdateListenerAdapter;

/**
 * Created by zhengshaorui
 * Time on 2018/9/26
 */

public class TipsPresent extends BasePresent<OtaContract.ITipsView> {

    private OtaContract.ITipsView mIView;
    private final TipsModel mTipsModel;

    public static TipsPresent create(OtaContract.ITipsView iTipsView){
        return new TipsPresent(iTipsView);
    }
    private TipsPresent(OtaContract.ITipsView iTipsView){
        mIView = iTipsView;
        mTipsModel = TipsModel.create(iTipsView);

    }

}
