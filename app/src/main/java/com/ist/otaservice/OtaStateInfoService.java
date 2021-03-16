package com.ist.otaservice;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ist.httplib.InvokeManager;
import com.ist.httplib.LitepalManager;
import com.ist.httplib.bean.BaseOtaRespone;
import com.ist.httplib.bean.ThreadBean;
import com.ist.httplib.mvp.contract.OtaContract;
import com.ist.httplib.mvp.present.DownloadPresent;
import com.ist.httplib.mvp.present.MainPresent;
import com.ist.httplib.net.NetErrorMsg;
import com.ist.httplib.status.UsbCheckStatus;
import com.ist.httplib.utils.RxUtils;
import com.ist.httplib.utils.VersionComparator;
import com.ist.otaservice.fragment.TipsFragment;
import com.ist.otaservice.utils.IstUtil;
import com.ist.otaservice.utils.OtaUtil;
import com.ist.otaservice.utils.SprefUtils;

import java.io.File;
import java.util.List;
//for 8386.23 Settting start ota service
public class OtaStateInfoService extends Service {
    public static  String TAG = "OtaStateInfoService wrl";

    private static final int CMD_GET_IS_UPDATING = 0;
    private static final int CMD_GET_IF_HAVE_OTA_PUSH = 1;
    private static final int CMD_GET_UPDATE_SPEED_AND_PROGRESS = 2;
    private static final int CMD_GET_IF_UPDATE_DONE= 3;
    private static final int CMD_GET_ALL_INFO = 4;
    private static final int CMD_GET_OR_SET_LINE= 5;
    private static final int CMD_GET_IF_UPDATE_VERITY = 6;
    private static final int CMD_GO_UPDATE_LOAD = 7;
    private static final int CMD_GO_UPDATE_START = 8;
    private static final int CMD_SET_UPDATE_CANCEL= 9;
    private static final int CMD_SET_UPDATE_DELETE = 10;
    private static final int CMD_GO_USB_UPDATE_VERITY = 11;
    private static final int CMD_GO_USB_UPDATE_LOAD = 12;
    private static final int CMD_NULL = 13;

    private DownloadPresent mDownloadPresent;
    private MainPresent mMainPresent;
    private FakeDownloadView mFakeDownloadView = new FakeDownloadView ();
    private FakeMainView mFakeMainView = new FakeMainView ();
    //need sync between thread
    public static  OtaStateInfo mOtaStateInfo = new OtaStateInfo();
    private int mLastLengthIndex = -1;
    private int mLastLengthCount = 10;
    private long mLastLength[] = {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
    private boolean cmdDealDone;
    private int cmdDealDoneRet;
    private   OtaStateInfo onSaveOtaStateInfo;
    private boolean onSaveState;
    private File mUSBfile;

    private Handler mHandler =new Handler (  ){
        @Override
        public void handleMessage ( Message msg ) {
            super.handleMessage ( msg );
            switch (msg.what){
                case 0:
                    mOtaStateInfo.downloadSpeed = "0KB/s";
                    break;
                case 1:
                    Log.e(TAG, "goUpdateVerity ==>success");
                    //mOtaStateInfo.ifUpdateVetifyDone=1;
                    cmdDealDone=true;
                    cmdDealDoneRet = 1;
                    break;
            }
        }
    };
    private boolean heartBeat=true;
    @Override
    public void onCreate ( ) {
        super.onCreate ( );

        Log.e("wrl", "OtaStateInfoService: Constant.onCreate ==");
        mOtaStateInfo.ifInfoUsed=-1;
        if( !TipsFragment.TIPSUPDATING ){
            getDownloadPresent();
            getMainPresent ();
            mOtaStateInfo.ifTipsDownloading = 0;
        }
        new Thread ( new Runnable ( ) {
            @Override
            public void run ( ) {
                int i = 0;
                while(heartBeat){
                    i++;
                    Log.e(TAG, "wrl heart beat: Constant. ==》"+i);
                    try {
                        Thread.sleep ( 1000 );
                    } catch (InterruptedException e) {
                        e.printStackTrace ( );
                    }
                }
            }
        } ).start ();
    }

    @Override
    public int onStartCommand ( Intent intent , int flags , int startId ) {
        if (intent != null && intent.getBooleanExtra ( "startdirect",false )){
            mDownloadPresent.startDownload();
            resetOtaStateInfo();
            mOtaStateInfo.ifDownloading=1;
            Log.e(TAG, "wrl onStartCommand startdirect: Constant. ==》true"+(intent.getBooleanExtra ( "startdirect",false )));
        }else{
            Log.e(TAG, "wrl onStartCommand startdirect: Constant. ==》false"/*+(intent.getBooleanExtra ( "startdirect",false ))*/);
        }

        return super.onStartCommand ( intent , flags , startId );
    }

    @Override
    public boolean onUnbind ( Intent intent ) {
        Log.e(TAG, "wrl onUnbind: ==》");
        //TUDO
        if(mOtaStateInfo.ifDownloading==1) {
            onSaveOtaStateInfo = mOtaStateInfo;
        }
        return true;
    }

    @Override
    public void onDestroy ( ) {
        super.onDestroy ( );
        heartBeat=false;
    }

    @Override
    public void onRebind ( Intent intent ) {
        Log.e(TAG, "wrl onRebind: ==》");
        super.onRebind ( intent );
        if( !TipsFragment.TIPSUPDATING ){
            getDownloadPresent();
            getMainPresent ();
            mOtaStateInfo.ifTipsDownloading = 0;
        }
        if(mOtaStateInfo.ifDownloading==1){
            onSaveState = true;
            mOtaStateInfo = onSaveOtaStateInfo;
        }
    }

    @Nullable
    @Override
    public IBinder onBind ( Intent intent ) {
        return otaStateInfoManager;
    }
    private OtaStateInfoManager.Stub otaStateInfoManager = new OtaStateInfoManager.Stub() {

        @Override
        public OtaStateInfo getOtaStateInfo ( int cmd ) throws RemoteException {
            updateOtaStateInfo(cmd);
            return mOtaStateInfo;
        }

        @Override
        public int sendOtaCmd ( int cmd ) throws RemoteException {
            int ret = -1;
            switch(cmd){
                case CMD_GO_USB_UPDATE_LOAD:
                    Log.e(TAG, "CMD_GO_USB_UPDATE_LOAD ==>");
                    goLoadUSBUpdate();
                    cmdDealDone=false;
                    ret = -1;
                    break;
                case CMD_GO_USB_UPDATE_VERITY:
                    Log.e(TAG, "CMD_GO_USB_UPDATE_LOAD ==>");
                    goLoadUSBUpdateVerity();
                    cmdDealDone=false;
                    ret = -1;
                    break;
                case CMD_GO_UPDATE_START://done
                    Log.e(TAG, "CMD_GO_UPDATE_START ==>");
                    mOtaStateInfo.ifDownloading=1;
                    mDownloadPresent.startDownload();
                    ret = 1;
                    break;
                case CMD_SET_UPDATE_CANCEL:
                    cmdDealDone=false;
                    Log.e(TAG, "CMD_SET_UPDATE_CANCEL ==>");
                    goUpdateCancel();
                    mOtaStateInfo.ifDownloading=0;
                    ret = -1;
                    break;
                case CMD_SET_UPDATE_DELETE:
                    cmdDealDone=false;
                    Log.e(TAG, "CMD_SET_UPDATE_DELETE ==>");
                    goUpdateDelete();
                    ret = -1;
                    break;
                case CMD_GO_UPDATE_LOAD://done
                    Log.e(TAG, "CMD_GO_UPDATE_LOAD ==>");
                    final File file = new File(Constant.SAVE_PATH + File.separator + Constant.FILE_NAME);
                    OtaUtil.updateOta(getApplicationContext (), file);
                    ret = 1;
                    break;
                case CMD_GET_IF_UPDATE_VERITY://done
                    cmdDealDone=false;
                    Log.e(TAG, "CMD_GET_IF_UPDATE_VERITY ==>");
                    goUpdateVerity();
                    ret = -1;
                    break;
                case CMD_NULL:
                    if(cmdDealDone){
                        ret = cmdDealDoneRet;
                    }
                    break;
            }
            return ret;
        }
    };

    private void goUpdateDelete ( ) {
        MainApplication.HANDLER.postDelayed(new Runnable() {
            @Override
            public void run() {
                RxUtils.deleteDbFile();
                cmdDealDone = true;
                cmdDealDoneRet = 1;
            }
        },1000);
    }

    private void goUpdateCancel ( ) {
        if (CustomerConfig.USE_DB){
            mDownloadPresent.pauseDownload();
            MainApplication.HANDLER.postDelayed(new Runnable() {
                @Override
                public void run() {
                    cmdDealDone = true;
                    cmdDealDoneRet = 1;
                }
            },3000);
        }
    }

    private void goUpdateVerity ( ) {
        final File file = new File(Constant.SAVE_PATH + File.separator + Constant.FILE_NAME);
        OtaUtil.checkLocalHasUpdateFile(file, new OtaUtil.CheckListener() {
            @Override
            public void check(int progress) {
            }

            @Override
            public void success() {
                mHandler.sendEmptyMessageDelayed ( 1,2000 );
            }

            @Override
            public void fail(String errorMsg) {
                mHandler.removeMessages ( 1 );
                Log.e(TAG, "goUpdateVerity ==>fail");
                //mOtaStateInfo.ifUpdateVetifyDone=2;
                cmdDealDone=true;
                cmdDealDoneRet = 2;
                RxUtils.deleteDbFile();
            }
        });
    }
    private void goLoadUSBUpdate ( ) {
        OtaUtil.updateOta(getApplicationContext (), mUSBfile);
    }
    private void goLoadUSBUpdateVerity ( ) {
        OtaUtil.checkLocalHasUpdateFile(mUSBfile, new OtaUtil.CheckListener() {
            @Override
            public void check(int progress) {
            }

            @Override
            public void success() {
                mHandler.sendEmptyMessageDelayed ( 1,2000 );
            }

            @Override
            public void fail(String errorMsg) {
                mHandler.removeMessages ( 1 );
                Log.e(TAG, "goLoadUSBUpdateVerity ==>fail");
                //mOtaStateInfo.ifUpdateVetifyDone=2;
                cmdDealDone=true;
                cmdDealDoneRet = 2;
                RxUtils.deleteDbFile();
            }
        });
    }

    private void updateOtaStateInfo ( int cmd ) {
        //resetOtaStateInfo();
        switch(cmd){
            case CMD_GET_ALL_INFO:
                if(IstUtil.isNetworkPositive()) {
                    if ( TipsFragment.TIPSUPDATING ) {
                        Log.e ( TAG , "CMD_GET_ALL_INFO ==>ifTipsDownloading 1" );
                        resetOtaStateInfo ( );
                        mOtaStateInfo.ifUSBUpdateExist = 0;
                        mOtaStateInfo.ifTipsDownloading = 1;
                        return;
                    }
                    if ( onSaveState ) {
                        Log.e ( TAG , "CMD_GET_ALL_INFO ==>onSaveState " + onSaveState );
                        resetOtaStateInfo ( );
                        mOtaStateInfo.ifUSBUpdateExist = 0;
                        onSaveState = false;
                    } else if ( mOtaStateInfo.ifDownloading == 1 ) {
                        resetOtaStateInfo ( );
                    } else {
                        Log.e ( TAG , "CMD_GET_ALL_INFO ==>onSaveState " + onSaveState );
                        mOtaStateInfo.ifInfoUsed = 1;
                        mMainPresent.checkLocalUpdate ( );
                    }
                }else{
                    Log.e ( TAG , "CMD_GET_ALL_INFO ==>onSaveState " + onSaveState );
                    mOtaStateInfo.ifInfoUsed = 1;
                    mMainPresent.checkLocalUpdate ( );
                }
                break;
            case CMD_GET_IF_HAVE_OTA_PUSH:
                mOtaStateInfo.ifInfoUsed=1;
                Log.e(TAG, "CMD_GET_IF_HAVE_OTA_PUSH ==>");
                //if ( IstUtil.isNetworkPositive()) {//settings will check net state  itself
                    MainApplication.HANDLER.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mMainPresent.checkNetUpdate();
                        }
                    }, 1500);
                break;
            case CMD_GET_UPDATE_SPEED_AND_PROGRESS:
                Log.e(TAG, "CMD_GET_UPDATE_SPEED_AND_PROGRESS ==>");
                //nothing needs to do
                break;
            case CMD_NULL:
                Log.e(TAG, "CMD_NULL ==>");
                break;
        }
    }

    private void resetOtaStateInfo ( ) {
        mOtaStateInfo.ifInfoUsed=0;
        //mOtaStateInfo.ifDownloadFinish=0;
        //mOtaStateInfo.ifDownloading=-1;
        //mOtaStateInfo.ifUpdateFileExist=-1;
        //mOtaStateInfo.ifUpdatePush=-1;
        //mOtaStateInfo.ifUpdateVetifyDone=0;
        //mOtaStateInfo.downloadSpeed=0;
        //mOtaStateInfo.downloadProgree=0;
    }

    public DownloadPresent getDownloadPresent() {
        mDownloadPresent = DownloadPresent.create(mFakeDownloadView);
        return mDownloadPresent;
    }
    public MainPresent getMainPresent() {
        mMainPresent = MainPresent.create(mFakeMainView);
        return mMainPresent;
    }
    class FakeDownloadView implements OtaContract.IDownloadView{
        @Override
        public void updateProgress ( int progress ) {
            resetOtaStateInfo();
            mOtaStateInfo.downloadProgree=progress;
            Log.e(TAG, "FakeDownloadView updateProgress ==>"+progress);
        }

        @Override
        public void updateOtherInfo ( long currentSize , long totalSize ) {
            mHandler.removeMessages ( 0 );
            mHandler.sendEmptyMessageDelayed ( 0,3000 );
            //TUDO need to count for five time to make speed
            resetOtaStateInfo();
            if(mLastLengthIndex>=0) {
                long add = currentSize - mLastLength[mLastLengthIndex];
                mLastLength[mLastLengthIndex]=currentSize;
                long size = (add/mLastLengthCount);
                String speed = Formatter.formatFileSize(getApplicationContext (), size) + "/s";
                if (size > 0 && size > 100) {
                        mOtaStateInfo.downloadSpeed = speed;

                }
            }else{
                for(int i=0;i<mLastLengthCount;i++){
                    mLastLength[i] = currentSize;
                }
                mOtaStateInfo.downloadSpeed = "0KB/s" ;
            }
            mLastLengthIndex++;
            if(mLastLengthIndex == mLastLengthCount){
                mLastLengthIndex = 0;
            }
            Log.e(TAG, "FakeDownloadView updateOtherInfo ==>"+currentSize);
        }

        @Override
        public void downloadSuccess ( ) {

            resetOtaStateInfo();
            mOtaStateInfo.downloadProgree=100;
            Log.e(TAG, "FakeDownloadView downloadSuccess ==>");
        }

        @Override
        public void error ( NetErrorMsg status , String errorMsg ) {
            Log.e(TAG, "FakeDownloadView error ==>"+errorMsg);
            //TUDO
            switch(status) {
                case FILE_LENGTH_NOT_SAME:
                    RxUtils.deleteDbFile ( );
                    mOtaStateInfo.downloadProgree = -10;
                    mHandler.removeMessages ( 0 );
                    break;
                default:
                    mOtaStateInfo.downloadProgree = -10;
                    mHandler.removeMessages ( 0 );
                    break;
            }
        }
    }
    class FakeMainView implements OtaContract.IMainView{

        @Override
        public void isLocalHasFile ( boolean isLocalhasFile , File file ) {
            Log.e(TAG, "FakeMainView isLocalHasFile ==>");
        }

        @Override
        public void checkUsbFile ( UsbCheckStatus usbCheckStatus , File file ) {
            Log.e(TAG, "FakeMainView checkUsbFile ==>"+usbCheckStatus);
            switch(usbCheckStatus){
                case SUCCESS:
                    if (file != null) {
                        resetOtaStateInfo();
                        mOtaStateInfo.ifUSBUpdateExist = 1;
                        mUSBfile=file;
                    }
                    break;
                    /*
                case MORE_THAN_ONE :
                    //Toast.makeText(mContext, getString(R.string.more_usb), Toast.LENGTH_SHORT).show();
                    break;
                case NO_UPGRADE_FILE :
                    //Toast.makeText(mContext, getString(R.string.no_updatefile_usb), Toast.LENGTH_SHORT).show();
                    break;
                case NO_USB :
                    //Toast.makeText(mContext, getString(R.string.no_usb_here), Toast.LENGTH_SHORT).show();
                    break;
                     */
                default :
                    mOtaStateInfo.ifUSBUpdateExist = 0;
                    if(checkTipsUpdating()){
                        checkUpdateLocal();
                    }
                    break;

            }
        }

        @Override
        public void checkNetUpdate ( boolean isCanUpdate , BaseOtaRespone respone ) {
            resetOtaStateInfo();
            mOtaStateInfo.ifUpdatePush=isCanUpdate?1:0;
            if(isCanUpdate){
                OtaUtil.loadUpdateLogToFile(respone);
            }
            Log.e(TAG, "FakeMainView checkNetUpdate ==>"+isCanUpdate+"-"+respone.toString ());
        }

        @Override
        public void error ( NetErrorMsg status , String errorMsg ) {
            Log.e(TAG, "FakeMainView error ==>");
            resetOtaStateInfo();
            mOtaStateInfo.ifUpdatePush=0;
            Log.e(TAG, "FakeMainView error ==>"+status+"-"+errorMsg);
        }
    }

    private void checkUpdateLocal ( ) {
        boolean existsFile = false;
        final File file = new File(Constant.SAVE_PATH+File.separator+ Constant.FILE_NAME);

        if (file.exists ()) {
            if ( CustomerConfig.USE_DB ) {
                List<ThreadBean> list = LitepalManager.getInstance ( ).getAllThreadBean ( );
                if (list != null && !list.isEmpty()) {
                    ThreadBean tmpThreadBean = list.get ( 1 );
                    int status = VersionComparator.compare(tmpThreadBean.version,InvokeManager.get(InvokeManager.SYSPROP_VERSION,"V1.0.0"));
                    if ( status > 0 ) {
                        for (int j = 0; j < list.size(); j++) {
                            ThreadBean bean = list.get ( j );
                            for (int i = 0; i < bean.dataCheck.length; i++) {
                                if ( bean.dataCheck[i] == 0 ) {
                                    existsFile = false;
                                    break;
                                }
                                if ( i == (bean.dataCheck.length - 1) ) {
                                    existsFile = true;
                                    Log.e ( TAG , "checkUpdateLocal existsFile 5==>" + existsFile );
                                }
                            }
                        }
                        Log.e ( TAG , "checkUpdateLocal existsFile 1==>" + existsFile );
                    }else{
                        Log.e ( TAG , "checkUpdateLocal rmove local pagcage  6==>" + existsFile );
                        if (CustomerConfig.USE_DB){
                            MainApplication.HANDLER.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    RxUtils.deleteDbFile();
                                }
                            },1000);
                        }
                    }
                    Log.e ( TAG , "checkUpdateLocal existsFile 4==>" + tmpThreadBean.version+"-"+ status  );
                }else{
                    Log.e ( TAG , "checkUpdateLocal rmove local pagcage  9==>" + existsFile );
                    if (CustomerConfig.USE_DB){
                        MainApplication.HANDLER.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                RxUtils.deleteDbFile();
                            }
                        },1000);
                    }
                }
            }
            Log.e ( TAG , "checkUpdateLocal existsFile 2==>" + existsFile );
        }else{
            if (CustomerConfig.USE_DB){
                MainApplication.HANDLER.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        RxUtils.deleteDbFile();
                    }
                },1000);
            }
        }
        Log.e(TAG, "checkUpdateLocal existsFile 3==>"+existsFile);
        if (existsFile) {
            resetOtaStateInfo();
            mOtaStateInfo.ifUpdateFileExist = 1;
        }else{
            MainApplication.HANDLER.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mMainPresent.checkNetUpdate();
                }
            }, 2000);
            mOtaStateInfo.ifUpdateFileExist = 0;
        }
    }

    private boolean checkTipsUpdating ( ) {
        if( TipsFragment.TIPSUPDATING ){
            resetOtaStateInfo();
            mOtaStateInfo.ifTipsDownloading = 1;
            return false;
        }else{
            mOtaStateInfo.ifTipsDownloading = 0;
            return true;
        }
    }
}
