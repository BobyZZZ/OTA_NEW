package com.ist.otaservice.utils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;

import com.ist.httplib.InvokeManager;
import com.ist.otaservice.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ABUpdateUtil {
    private static final String TAG = "ABUpdateUtil";

    private static final int MSG_SUCCESS = 0;
    private static final int MSG_PROGRESS = 1;
    private static final int MSG_ERROR = 2;
    private static final int MSG_ERROR_UPDATE_START_FAIL = 3;
    private static final String LOGCAT_CMD = "logcat -s update_engine";
    //ActionProcessor:info InstallPlanAction DownloadAction FilesystemVerifierAction PostinstallRunnerAction
    //last info :Update successfully applied, waiting to reboot.
    private static final String ABUPDATE_LOGCAT_MSG_ERRORCODE = "ErrorCode";
    private static final String ABUPDATE_LOGCAT_MSG_KSUCCESS = "kSuccess";
    private static final String ABUPDATE_LOGCAT_MSG_PROGRESS = "progress";
    private static final String ABUPDATE_LOGCAT_MSG_REBOOT = "Update successfully applied, waiting to reboot";

    public static final int MODE_NET = 1;
    public static final int MODE_USB = 2;
    /*ABupdate*/
    private static Handler mHandler = new Handler (  ){
        @Override
        public void handleMessage ( Message msg ) {
            String t = (String)msg.obj;
            switch(msg.what){
                case MSG_SUCCESS:
                    mHandler.removeMessages ( MSG_ERROR_UPDATE_START_FAIL );
                    mPDialog.dismiss ();
                    ABUpdateUtil.showDialogReboot(R.string.abupdate_title,R.string.abupdate_msg_success
                            ,R.string.abupdate_rebootnow,R.string.abupdate_rebootlater);
                    break;
                case MSG_PROGRESS:
                    mHandler.removeMessages ( MSG_ERROR_UPDATE_START_FAIL );
                    String[] tmp = t.split ( ABUPDATE_LOGCAT_MSG_PROGRESS );
                    if(tmp[1] != null){
                        mPDialog.setMessage ( mContext.getResources ().getString (R.string.abupdate_progress)+ "     -->    " + tmp[1] );
                    }
                    break;
                case MSG_ERROR:
                    Log.v(TAG, "wrl ABupdate MSG_ERROR =" +t);
                    mHandler.removeMessages ( MSG_ERROR_UPDATE_START_FAIL );
                    mPDialog.dismiss ();
                    ABUpdateUtil.showDialogReboot(R.string.abupdate_title,R.string.abupdate_msg_error
                            ,R.string.abupdate_rebootnow,R.string.abupdate_rebootlater);
                    break;
                case MSG_ERROR_UPDATE_START_FAIL:
                    mHandler.removeMessages ( MSG_ERROR_UPDATE_START_FAIL );
                    mPDialog.dismiss ();
                    ABUpdateUtil.showDialog(R.string.abupdate_title,R.string.abupdate_strat_fail,R.string.comfire);
                    break;
            }
        }
    };
    private static ProgressDialog mPDialog;
    private static Context mContext;

    /*ABupdate*/
    public static void startUpgrade ( int mode, Context context) {
        if(!IstUtil.getInABUpdateMode ()){
            initStart();
            IstUtil.setInABUpdateMode ( true );
            mHandler.sendEmptyMessageDelayed ( MSG_ERROR_UPDATE_START_FAIL ,60000);
            mContext = context;
            ProgressDialog progressDialog = new ProgressDialog (context);
            progressDialog.setTitle(mContext.getResources ().getString (R.string.abupdate_title2));
            progressDialog.setMessage(mContext.getResources ().getString (R.string.abupdate_progress));
            progressDialog.setIndeterminate(true);// 是否形成一个加载动画  true表示不明确加载进度形成转圈动画  false 表示明确加载进度
            progressDialog.setCancelable(false);//点击返回键或者dialog四周是否关闭dialog  true表示可以关闭 false表示不可关闭
            progressDialog.show();
            mPDialog = progressDialog;
            if(mode == MODE_NET){
                InvokeManager.set ( InvokeManager.SYSPROP_CTL_START, "gootaupdatenet" );
                Log.v(TAG, "wrl  gootaupdatenet ABupdate");
            }else {
                InvokeManager.set ( InvokeManager.SYSPROP_CTL_START, "gootaupdateusb" );
                Log.v(TAG, "wrl  gootaupdateusb ABupdate");
            }
        }else{
            ABUpdateUtil.showDialogReboot( R.string.abupdate_title ,
                    R.string.abupdate_strat_deniy ,R.string.abupdate_rebootnow ,R.string.abupdate_rebootlater );
        }
    }

    private static void showDialogReboot(int title, int msg,int positiveMsg,int negativeMsg){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext) .setTitle(mContext.getResources ().getString (title))
                .setMessage(mContext.getResources ().getString (msg))
                .setPositiveButton(mContext.getResources ().getString (positiveMsg), new DialogInterface .OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ABUpdateUtil.reboot();
                    }
                }).setNegativeButton(mContext.getResources ().getString (negativeMsg), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
        builder.create().show();
    }
    private static void showDialog(int title, int msg,int positiveMsg){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setTitle(mContext.getResources ().getString (msg))
                .setMessage(mContext.getResources ().getString (title))
                .setPositiveButton(mContext.getResources ().getString (positiveMsg), new DialogInterface .OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
        builder.create().show();
    }
    private static void initStart ( )
    {
        new Thread ( new Runnable ( ) {
            @Override
            public void run ( ) {
                Process mLogcatProc = null;
                BufferedReader reader = null;
                try {
                    mLogcatProc = Runtime.getRuntime().exec(LOGCAT_CMD);
                    reader = new BufferedReader(new InputStreamReader (mLogcatProc.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if(line.contains ( ABUPDATE_LOGCAT_MSG_PROGRESS )){
                            Message msg3 = new Message();
                            msg3.what = MSG_PROGRESS;
                            msg3.obj = line;
                            mHandler.sendMessage(msg3);
                        }else if(line.contains ( ABUPDATE_LOGCAT_MSG_REBOOT )){
                            Message msg3 = new Message();
                            msg3.what = MSG_SUCCESS;
                            msg3.obj = line;
                            mHandler.sendMessage(msg3);
                        }else if(line.contains ( ABUPDATE_LOGCAT_MSG_ERRORCODE )){
                            if(!line.contains ( ABUPDATE_LOGCAT_MSG_KSUCCESS )) {
                                Message msg3 = new Message ( );
                                msg3.what = MSG_ERROR;
                                msg3.obj = line;
                                mHandler.sendMessage ( msg3 );
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }finally{
                    try {
                        if(mLogcatProc !=null ){
                            mLogcatProc.destroy ();
                        }
                        if(reader !=null ){
                            reader.close ();
                        }
                    }catch( Exception e){
                        e.printStackTrace();
                    }
                }
            }
        } ).start ();
    }
    private static void reboot() {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        pm.reboot ( "user" );
    }
}
