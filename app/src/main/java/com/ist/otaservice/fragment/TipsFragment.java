package com.ist.otaservice.fragment;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ist.httplib.InvokeManager;
import com.ist.httplib.OtaLibConfig;
import com.ist.httplib.bean.BaseOtaRespone;
import com.ist.httplib.mvp.contract.OtaContract;
import com.ist.httplib.mvp.model.TipsModel;
import com.ist.httplib.mvp.present.TipsPresent;
import com.ist.httplib.net.NetErrorMsg;
import com.ist.httplib.status.UsbCheckStatus;
import com.ist.otaservice.Constant;
import com.ist.otaservice.CustomerConfig;
import com.ist.otaservice.MainApplication;
import com.ist.otaservice.R;
import com.ist.otaservice.utils.CusFragmentManager;
import com.ist.otaservice.utils.IstUtil;
import com.ist.otaservice.utils.OtaUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Created by zhengshaorui
 * Time on 2018/9/15
 */

public class TipsFragment extends BaseFragment<TipsPresent> implements View.OnClickListener,
        OtaContract.ITipsView {
    private static final String TAG = "TipsFragment";
    public static  Boolean TIPSUPDATING=false;
    private TipsPresent mPresent;
    private TextView mTextView;
    private TextView mTextViewLog;


    public static TipsFragment newInstance ( ) {
        return new TipsFragment ( );
    }

    @Override
    public int getLayoutId ( ) {
        return R.layout.fragment_tips;
    }

    @Override
    public TipsPresent getPresent ( ) {
        mPresent = TipsPresent.create ( this );
        return mPresent;
    }

    @Override
    public void initView ( View view ) {

        if ( "on".equals ( InvokeManager.get ( InvokeManager.SYSPROP_IST_DEBUG_FLAG , "off" ) ) ) {
            CustomerConfig.ISDEBUG = true;
            OtaLibConfig.getBuilder ( ).setDebug ( true );
        }

        mTextView = view.findViewById ( R.id.tips_checked_title );

        String version = "";
        mTextView.setText ( getString ( R.string.check_new_version , version ) );

        mTextViewLog = view.findViewById ( R.id.tips_checked_log );

        loadLogFormFile(mTextViewLog);
        view.findViewById ( R.id.btn_tips1 ).setOnClickListener ( this );
        view.findViewById ( R.id.btn_tips2 ).setOnClickListener ( this );
        view.findViewById ( R.id.btn_tips3 ).setOnClickListener ( this );
    }

    private void loadLogFormFile ( TextView tv ) {
        String mLog = "Log:fix some bugs";
        StringBuilder tvFileReader=new StringBuilder (  );
        File readerFile = new File(Constant.SAVE_PATH+File.separator+ Constant.LOG_FILE_NAME);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader (readerFile));
            String tempString = null;
            while ((tempString = reader.readLine()) != null) {
                tvFileReader.append(tempString);
                tvFileReader.append("\n");
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
        if(tvFileReader.toString ().length ()==0){
            tv.setText ( mLog );
        }else{
            tv.setText ( tvFileReader.toString () );
        }
    }

    @Override
    public boolean onPressBack ( ) {
        return false;
    }

    @Override
    public void onClick ( View v ) {
        if (v.getId() == R.id.btn_tips1) {
            getActivity().finish();
        }else if (v.getId() == R.id.btn_tips2) {
            getActivity().finish();
            IstUtil.setIgnoreFlag(true);
        }else if (v.getId() == R.id.btn_tips3) {
            TIPSUPDATING = true;
            CusFragmentManager.getInstance().replaceFragment(DownloadFragment.newInstance("on"), CusFragmentManager.RIGHT);
        }
    }

    @Override
    public void error ( NetErrorMsg status , String errorMsg ) {

    }
}
