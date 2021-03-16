package com.ist.otaservice.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ist.httplib.InvokeManager;
import com.ist.httplib.mvp.contract.OtaContract;
import com.ist.httplib.mvp.present.DownloadPresent;
import com.ist.httplib.net.HttpTaskManager;
import com.ist.httplib.net.NetErrorMsg;
import com.ist.httplib.utils.RxUtils;
import com.ist.otaservice.Constant;
import com.ist.otaservice.CustomerConfig;
import com.ist.otaservice.MainApplication;
import com.ist.otaservice.R;
import com.ist.otaservice.boot.BootCheckUpdateActivity;
import com.ist.otaservice.utils.ABUpdateUtil;
import com.ist.otaservice.utils.ActivityUtil;
import com.ist.otaservice.utils.IstUtil;
import com.ist.otaservice.utils.OtaUtil;
import com.ist.otaservice.utils.ToastUtil;

import java.io.File;

/**
 * Created by zhengshaorui
 * Time on 2018/9/15
 */

public class DownloadFragment extends BaseFragment<DownloadPresent> implements OtaContract.IDownloadView, View.OnClickListener {
    private static final String TAG = "DownloadFragment";
    /**
     * view
     */
    private ProgressBar mProgressBar;
    private TextView mProgressTv,mDownloadSpeedTv,mDownloadSizeTv;
    private TextView mDeviceNameTv,mDeviceVertionTv,mDownloadStatusTv;
    private Button mPauseBtn,mRestartBtn,mBackendBtn;
    private View mLlDownloadingBtnContainer;
    private View mLlNotDownloadingBtnContainer;
    private boolean mDownloadSuccess;//控制退出页面还是后台下载
    /**
     * logic
     */

    private int mLastLengthCount = 10;
    private long mLastLength[] = {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
    private int mLastLengthIndex = -1;
    private DownloadPresent mPresent;
    private boolean successDownload = false;
    private long lastUpdateOtherInfoTime;
    private Handler mHandler =new Handler (  ){
        @Override
        public void handleMessage ( Message msg ) {
            super.handleMessage ( msg );
            switch (msg.what){
                case 0:
                    super.handleMessage ( msg );
                    mDownloadSpeedTv.setText("0KB/s");
                    mHandler.sendEmptyMessageDelayed ( 1,20000 );
                    break;
                case 1:
                    mDownloadSpeedTv.setText(R.string.net_off_tips);
                    break;
                case 2:
                    dismissProgressDialog();
                    final File file = new File(Constant.SAVE_PATH + File.separator + Constant.FILE_NAME);
                    Log.d(TAG, "zsr --> success: "+file.getAbsolutePath()+" "+file.getName());
                    OtaUtil.startUpgrade(mContext, file, R.string.local_file_can_upgrade, R.string.remind_me);
                    successDownload = true;
                    break;
            }
        }
    };
    public static DownloadFragment newInstance(String status){
        DownloadFragment fragment = new DownloadFragment();
        Bundle bundle = new Bundle();
        bundle.putString(Constant.START_DOWNLOAD,status);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_download;
    }

    @Override
    public DownloadPresent getPresent() {
        mPresent = DownloadPresent.create(this);
        return mPresent;
    }

    @Override
    public void initView(View view) {
        mProgressBar = view.findViewById(R.id.progress);
        mProgressTv = view.findViewById(R.id.progress_tv);
        //mDownloadSpeedTv = view.findViewById(R.id.download_speed_tv);
        mDownloadSpeedTv = view.findViewById(R.id.download_speed_tv_2);//change for new UI
        mDownloadSizeTv = view.findViewById(R.id.download_size_tv);
        mPauseBtn = view.findViewById(R.id.btn_puase);
        mDeviceNameTv = view.findViewById(R.id.download_device_name);
        mDeviceVertionTv = view.findViewById(R.id.download_device_version);
        mDownloadStatusTv = view.findViewById(R.id.download_status);
        mRestartBtn = view.findViewById(R.id.btn_redownload);
        mBackendBtn = view.findViewById(R.id.btn_download_backend);

        mLlDownloadingBtnContainer = view.findViewById(R.id.ll_downloading_container);
        mLlNotDownloadingBtnContainer = view.findViewById(R.id.ll_not_downloading_container);
        view.findViewById(R.id.btn_back).setOnClickListener(this);

        mPauseBtn.setOnClickListener(this);
        mRestartBtn.setOnClickListener(this);
        mBackendBtn.setOnClickListener(this);
        initData();
    }

    private void showDownloadingBtnLayout(boolean downloading) {
        mLlDownloadingBtnContainer.setVisibility(downloading ? View.VISIBLE : View.GONE);
        mLlNotDownloadingBtnContainer.setVisibility(downloading ? View.GONE : View.VISIBLE);
        if (downloading) {
            mDownloadSpeedTv.setVisibility(View.VISIBLE);
            mPauseBtn.setVisibility(View.VISIBLE);
        }
    }

    private void resetData() {
        mLastLengthIndex = -1;
    }

    private void initData() {

        Bundle bundle = getArguments();
        if (bundle != null){
            if ("on".equals(bundle.getString(Constant.START_DOWNLOAD,"off"))){
                mPresent.startDownload();
                mDownloadStatusTv.setText(R.string.downloading);
            }else if ("restart".equals(bundle.getString(Constant.START_DOWNLOAD,"off"))){
                //先暂停再重新下载
                if (CustomerConfig.USE_DB) {
                    HttpTaskManager.getInstance().pause();
                    MainApplication.HANDLER.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mPresent.startDownload();
                        }
                    }, 500);
                    mDownloadStatusTv.setText(R.string.downloading);
                }else{
                    mPresent.startDownload();
                    mDownloadStatusTv.setText(R.string.downloading);
                }

            }
        }
        mDeviceNameTv.setText(InvokeManager.get(InvokeManager.SYSPROP_PRODUCT_NAME,"Model"));
        mDeviceVertionTv.setText(InvokeManager.get(InvokeManager.SYSPROP_VERSION,"V1.0.0.0"));

    }

    @Override
    public void error(NetErrorMsg status, String errorMsg) {
        mLastLengthIndex = -1;
        if (!ActivityUtil.isTopActivity(mActivity,mActivity)) {
            ToastUtil.showLong(R.string.background_download_fail);
        }
        mHandler.removeMessages ( 0 );
        mHandler.removeMessages ( 1 );
        Log.d(TAG, "zsr --> error: "+status+" "+errorMsg);
        Log.e("Keven", "error: Constant.SAVE_PATH =="+Constant.SAVE_PATH);
        File file = new File(Constant.SAVE_PATH, Constant.FILE_NAME);
        mPauseBtn.setText(R.string.start);
        mPresent.pauseDownload();
        switch(status){
            case RESPONSE_IS_NULL :
                mDownloadStatusTv.setText(R.string.file_damaged);
                mPauseBtn.setText(R.string.try_again);
                mDownloadSpeedTv.setText(R.string.file_damaged);
                break;
            case TIME_OUT :
                mDownloadStatusTv.setText(R.string.time_out);
                mPauseBtn.setText(R.string.try_again);
                mDownloadSpeedTv.setText(R.string.time_out);
                break;
            case FILE_LENGTH_NOT_SAME :
                if (file.exists()){
                    if (isAdded()) {
                        //showDeleteDialog(file);
                        RxUtils.deleteDbFile();
                        mDownloadStatusTv.setText(R.string.file_damaged);
                        mDownloadSpeedTv.setText(R.string.file_damaged);
                        mPauseBtn.setText(R.string.try_again);
                    }
                }
                break;
            case CACHE_NOT_ENOUGH:
                showDeleteDialog(file);
/*                if (file.exists()) {
                        RxUtils.deleteDbFile();
                        mDownloadStatusTv.setText(R.string.file_damaged);
                        mDownloadSpeedTv.setText(R.string.file_damaged);
                        mPauseBtn.setText(R.string.try_again);
                }*/
                break;
            case OTHERS :
                mDownloadStatusTv.setText(R.string.others_error);
                mPauseBtn.setText(R.string.try_again);
                mDownloadSpeedTv.setText(R.string.others_error);
                break;
            default :
                break;

        }
    }

    @Override
    public void updateProgress(int progress) {
//        Log.d(TAG, "zsr --> updateProgress: "+progress + ",isTop: " + ActivityUtil.isTopActivity(mActivity,mActivity));
        if (isAdded()) {
            mProgressBar.setProgress(progress);
            mProgressTv.setText(progress + "%");
        }

    }

    @Override
    public void updateOtherInfo(long currentSize, long totalSize) {
        mHandler.removeMessages ( 0 );
        mHandler.removeMessages ( 1 );
        mHandler.sendEmptyMessageDelayed ( 0,3000 );
        lastUpdateOtherInfoTime=  System.currentTimeMillis ();
        //Log.d(TAG, "zsr --> updateProgress: "+Formatter.formatFileSize(mContext, (currentSize - mLastLength[mLastLengthIndex])));
        if (isAdded()) {
            if(mLastLengthIndex>=0) {
                long add = currentSize - mLastLength[mLastLengthIndex];
                mLastLength[mLastLengthIndex]=currentSize;
                long size = (add/mLastLengthCount);
                String speed = Formatter.formatFileSize(mContext, size) + "/s";
                String current = Formatter.formatFileSize(mContext, (currentSize));
                String total = Formatter.formatFileSize(mContext, (totalSize));
//                if (size > 0 && size > 100) {
                        mDownloadSpeedTv.setText ( speed );
//                }
                mDownloadSizeTv.setText(getString(R.string.download_size, current, total));
            }else{
                for(int i=0;i<mLastLengthCount;i++){
                    mLastLength[i] = currentSize;
                }
                mDownloadSpeedTv.setText ( "0KB/s" );
            }
            mLastLengthIndex++;
            if(mLastLengthIndex == mLastLengthCount){
                mLastLengthIndex = 0;
            }

            mDownloadStatusTv.setText(R.string.downloading);
            mPauseBtn.setText(R.string.cancel_download);



        }
    }

    @Override
    public void downloadSuccess() {
        mDownloadSuccess = true;
        if (ActivityUtil.isTopActivity(mActivity,mActivity)) {
            mProgressBar.setProgress(100);
            mProgressTv.setText(100 + "%");
            mDownloadSizeTv.setText("");
            mDownloadSpeedTv.setText("");
            mDownloadStatusTv.setText(R.string.download_success);
            mPauseBtn.setVisibility(View.INVISIBLE);
            mDownloadSpeedTv.setVisibility(View.INVISIBLE);
            mBackendBtn.setText(R.string.back);
            TipsFragment.TIPSUPDATING = false;
            //开始校验升级
            final File file = new File(Constant.SAVE_PATH + File.separator + Constant.FILE_NAME);
            if (file.exists()) {
                if(IstUtil.getPlanform()==IstUtil.PLANFORM_DEVICE_966){/*ABupdate*/
                    ABUpdateUtil.startUpgrade( ABUpdateUtil.MODE_NET ,getActivity());
                }else {
                //请在这里做校验升级
                showProgressDialog ( R.string.check );
                OtaUtil.checkLocalHasUpdateFile ( file , new OtaUtil.CheckListener ( ) {
                    @Override
                    public void check ( int progress ) {
                        if ( mDialog != null && mDialog.isShowing ( ) ) {
                            mDialog.setMessage ( getString ( R.string.check ) + "     -->    " + progress + "%" );
                        }
                    }

                    @Override
                    public void success ( ) {
                        mHandler.sendEmptyMessageDelayed ( 2 , 2000 );
                    }

                    @Override
                    public void fail ( String errorMsg ) {
                        mHandler.removeMessages ( 2 );
                        MainApplication.HANDLER.postDelayed ( new Runnable ( ) {
                            @Override
                            public void run ( ) {
                                dismissProgressDialog ( );
                                ToastUtil.show ( getString ( R.string.check_your_file ) , Toast.LENGTH_SHORT );
                                showDownloadingBtnLayout ( false );
                            }
                        } , 500 );
                        RxUtils.deleteDbFile ( );
                    }
                } );
            }
            } else {
                dismissProgressDialog();
            }
        }else{
            //这个时候后台下载成功了，但是没有界面，所以，我们需要通过弹出一个
            //activity，让它去提示下载成功
//            mContext.sendBroadcast(new Intent(Constant.ACTION_BACKGROUND_NET_UPDATE));
            mContext.startActivity(new Intent(mContext,BootCheckUpdateActivity.class));
            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPresent.onDestroy();
        Log.d(TAG, "zhouyc onDestroyView: ");
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){
            case R.id.btn_puase:
                if (!IstUtil.isFastClick()) {
                    if (!mPresent.isPause()) {
                        //mPresent.pauseDownload();
                        goUpdateCancel();
                    } else {
                        if (!IstUtil.isNetworkPositive()){
                            ToastUtil.show ( mActivity,R.string.no_network,Toast.LENGTH_SHORT );
                            return;
                        }
                        mPauseBtn.setText(R.string.cancel_download);
                        mDownloadStatusTv.setText(R.string.downloading);
                        mDownloadSpeedTv.setText("");
                        mPresent.startDownload();
                    }
                }else{
                    ToastUtil.show ( mActivity,R.string.click_too_fast,Toast.LENGTH_SHORT );
                }
                 break;
            case R.id.btn_redownload:
                mPresent.reDownload();
                resetData();
                showDownloadingBtnLayout(true);
                break;
            case R.id.btn_download_backend:
//                moveTaskToBack();
                if (mDownloadSuccess) {
                    getActivity().finish();
                } else {
                    mActivity.moveTaskToBack(true);//TUDO make it real doing backend!
                }
                break;
            case R.id.btn_back:
                getActivity().finish();
                break;
            default :
                break;

        }
    }
    private void moveTaskToBack ( ) {
        killUIAndRestart();
/*        if(IstUtil.getAlertDialogIgnoreFlag ()) {
            killUIAndRestart();
        }else{
            final List<Integer> choice = new ArrayList<> ();
            final String[] items = {getActivity ().getResources ().getString ( R.string.dialog_ignore )};
            //默认都未选中
            boolean[] isSelect = {false};
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setIcon(R.mipmap.ic_launcher)
                    .setTitle(R.string.dialog_tips)
                    .setMultiChoiceItems(items, isSelect, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                            if (b) {
                                choice.add(i);
                            } else {
                                choice.remove(choice.indexOf(i));
                            }
                        }
                    }).setPositiveButton(R.string.comfire, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(choice.size ()==0){
                                Log.d(TAG, "zsr --> setAlertDialogIgnoreFlag: false");
                            }else{
                                IstUtil.setAlertDialogIgnoreFlag(true);
                                Log.d(TAG, "zsr --> setAlertDialogIgnoreFlag: true");
                            }
                            killUIAndRestart();
                        }
                    });

            builder.create().show();
        }*/
    }

    private void killUIAndRestart ( ) {
        mPresent.pauseDownload ( );
        TipsFragment.TIPSUPDATING = false;
        final String ACTION = "android.intent.action.OtaStateInfoService";
        mActivity.moveTaskToBack ( true );//TUDO make it real doing backend!
        final Intent intent = new Intent ( ACTION );
        intent.setPackage ( "com.ist.otaservice" );
        intent.putExtra ( "startdirect" , true );
        MainApplication.HANDLER.postDelayed ( new Runnable ( ) {
            @Override
            public void run ( ) {
                getActivity ( ).startService ( intent );
                getActivity ( ).finish ( );
            }
        } , 3000 );
    }

    private void showDeleteDialog(final File file){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(R.string.cache_not_enough)
                .setPositiveButton(R.string.delete_redownload, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RxUtils.deleteDbFile();
                        mPresent.startDownload();
                    }
                }).setNegativeButton(R.string.cancel,null).create().show();


    }

    @Override
    public boolean onPressBack() {
        Log.d(TAG, "zhouyc onPressBack isRunning: " + mPresent.isRunning());
        if (mPresent.isRunning()) {
            new AlertDialog.Builder(mActivity)
                    .setMessage(R.string.back_download_tip)
                    .setNegativeButton(R.string.cancel_download, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mPresent.pauseDownload();
                            getActivity ().finish ();

/*                        if (CustomerConfig.USE_DB){
                            MainApplication.HANDLER.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    RxUtils.deleteDbFile();
                                }
                            },500);
                        }
                            CusFragmentManager.getInstance().replaceFragment(MainFragment.newInstance(), CusFragmentManager.LEFT);*/
                        }
                    })/*.setPositiveButton(R.string.back_download, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mActivity.moveTaskToBack(true);
            }
        })*/.create().show();
            //moveTaskToBack();
            //mActivity.moveTaskToBack(true);//TUDO make it real doing backend!
        } else {
            getActivity ().finish ();
        }
        return true;
    }
    private void goUpdateCancel ( ) {
        mPresent.pauseDownload();
        MainApplication.HANDLER.postDelayed(new Runnable() {
            @Override
            public void run() {
                getActivity ().finish ();
            }
        },0);
    }
}
