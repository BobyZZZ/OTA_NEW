package com.ist.otaservice.boot;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.UserManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.ist.httplib.InvokeManager;
import com.ist.httplib.LitepalManager;
import com.ist.httplib.bean.BaseOtaRespone;
import com.ist.httplib.bean.ThreadBean;
import com.ist.httplib.net.HttpTaskManager;
import com.ist.httplib.net.NetErrorMsg;
import com.ist.httplib.net.callback.UpdateListenerAdapter;
import com.ist.httplib.utils.RxUtils;
import com.ist.httplib.utils.VersionComparator;
import com.ist.otaservice.Constant;
import com.ist.otaservice.CustomerConfig;
import com.ist.otaservice.MainActivity;
import com.ist.otaservice.MainApplication;
import com.ist.otaservice.R;
import com.ist.otaservice.utils.IstUtil;
import com.ist.otaservice.utils.OtaUtil;
import com.ist.otaservice.utils.SprefUtils;

import java.io.File;
import java.util.List;

/**
 * @author zhengshaorui
 * @date 2018/10/17
 * 该 service 用于开机第一次启动
 */
public class BootService extends Service {
    private static final String TAG = "BootService";
    private static final String CHANNEL_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
    private static final String WIFI_CHANGE = "android.net.wifi.STATE_CHANGE";
    private Context mContext;
    private boolean isFirstLauncher = true ;
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate: [BootService]");
        Log.d(TAG, "shouldAutoDetectNewVersion: " + IstUtil.shouldAutoDetectNewVersion());
        mContext = this;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constant.ACTION_BACKGROUND_NET_UPDATE);
        filter.addAction(CHANNEL_CHANGE);
        filter.addAction(WIFI_CHANGE);
        registerReceiver(LocalReceiver,filter);
        if (CustomerConfig.USE_DB) {
            //LitepalManager.getInstance().deleteall();
        }

    }


    BroadcastReceiver LocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            String action = intent.getAction();
            Log.e("Keven", "onReceive: action =="+action);
            if (Constant.ACTION_BACKGROUND_NET_UPDATE.equals(action)) {
                Intent bootactivity = new Intent(context, BootCheckUpdateActivity.class);
                bootactivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                bootactivity.putExtra("status", "net");
                context.startActivity(bootactivity);
            }else{
                Log.e(TAG, "onReceive: 111action =="+action);
               if (IstUtil.isNetworkPositive() && isFirstLauncher){

                   Log.e(TAG, "onReceive: 222action =="+action);
                    isFirstLauncher = false;
                    //final File file = new File(Constant.SAVE_PATH+File.separator+ Constant.FILE_NAME);
                    //if (!file.exists()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (isAnim()) {
                                MainApplication.HANDLER.postDelayed ( new Runnable ( ) {
                                    @Override
                                    public void run ( ) {
                                        if (IstUtil.shouldAutoDetectNewVersion()) {
                                            checkNetUpdate();
                                        }
                                    }
                                } , 10000 );
                            }
                        }else{
                            MainApplication.HANDLER.postDelayed ( new Runnable ( ) {
                                @Override
                                public void run ( ) {
                                    if (IstUtil.shouldAutoDetectNewVersion()) {
                                        checkNetUpdate();
                                    }
                                }
                            } , 10000 );
                        }
                    //}

               }
            }



        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (isAnim()) {
                checkLocalFile();
            }
        }else{
            checkLocalFile();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void checkLocalFile() {
        final File file = new File(Constant.SAVE_PATH+File.separator+ Constant.FILE_NAME);
        /**if (file.exists()) {
            OtaUtil.checkLocalHasUpdateFile(file, new OtaUtil.CheckListener() {
                @Override
                public void check(int progress) {
                    Log.d(TAG, "zsr --> check: "+progress);
                }

                @Override
                public void success() {
                }

                @Override
                public void fail(String errorMsg) {
                    Log.d(TAG, "zsr --> fail: "+errorMsg);
                    if (CustomerConfig.USE_DB) {
                        LitepalManager.getInstance().deleteall();
                    }
                    SprefUtils.saveSprefValue(SprefUtils.KEY_REMIND_ME,false);
                    file.delete();
                    checkNetUpdate();
                }
            });
        }**/
        boolean existsFile = false;
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
                            IstUtil.setIgnoreFlag(false);
                            if (CustomerConfig.USE_DB){
                                MainApplication.HANDLER.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        RxUtils.deleteDbFile();
                                    }
                                },1000);
                            }
                    }
                    Log.e ( TAG , "checkUpdateLocal existsFile 4==>" + tmpThreadBean.version );
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
        if (existsFile) {
            //
        }else{
            if (IstUtil.shouldAutoDetectNewVersion()) {
                checkNetUpdate();
            }
        }
    }

    private void checkNetUpdate() {

        HttpTaskManager.getInstance().checkUpdate(new UpdateListenerAdapter() {
            @Override
            public void checkUpdate(boolean isCanUpdate, BaseOtaRespone respone) {
                super.checkUpdate(isCanUpdate,respone);
                Log.d(TAG, "zsr --> checkUpdate: " + isCanUpdate + " " + respone);
                if (isCanUpdate) {
                    OtaUtil.loadUpdateLogToFile(respone);
                    Log.d(TAG, "zsr --> isCanUpdate: " + isCanUpdate + " " + respone);
					if(IstUtil.dailycheck()){//update once for a day
                        Log.d(TAG, "zsr --> dailycheck: " + isCanUpdate + " " + respone);
						return;
					}
                    if(IstUtil.getIgnoreFlag()){//return if user has ignored it
                        Log.d(TAG, "zsr --> getIgnoreFlag: " + isCanUpdate + " " + respone);
                        return;
                    }
                    //TUDO  set some info for setting to read
                    //TUDO save the log file for OTA
                    Intent intent = new Intent(mContext, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("tipsstart", true);
                    mContext.startActivity(intent);
					/*  removed for new Smart UI  20200608 wrl
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setCancelable(false)
                            .setTitle(getString(R.string.check_new_version,respone.version))
                            .setMessage(R.string.new_version_log_default)
                            .setPositiveButton(R.string.load_back_now, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    File file = new File(Constant.SAVE_PATH,Constant.FILE_NAME);
                                    if (file.exists()){
                                        file.delete();
                                    }
                                    if (!isAnim()){
                                        Toast.makeText(mContext, R.string.not_anim, Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    Intent intent = new Intent(mContext, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra("service", true);
                                    mContext.startActivity(intent);
                                }
                            }).setNegativeButton(R.string.remind_me, null);

                    Dialog dialog = builder.create();
                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

                    dialog.show();
                    */
                }else{
                    InvokeManager.set(InvokeManager.SYSPROP_OTA_HAVEPUSH_FLAG,"false");
                    File file = new File(Constant.SAVE_PATH,Constant.FILE_NAME);
                    if (file.exists()){
                        file.delete();
                    }
                }

            }

            @Override
            public void error(NetErrorMsg status, String errorMsg) {
                super.error(status, errorMsg);
                Log.d(TAG, "zsr --> error: " + status + " " + errorMsg);
            }
        });
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(LocalReceiver);
    }


    @TargetApi(Build.VERSION_CODES.M)
    private boolean isAnim(){
       UserManager userManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        /* try {
            Method method = userManager.getClass().getDeclaredMethod("isAdminUser");

            method.setAccessible(true);
            boolean isuser  = (boolean) method.invoke(userManager);
            Log.d(TAG, "zsr --> isAnim: "+isuser+ " "+userManager.isSystemUser());
            return isuser;
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        return userManager.isSystemUser();
    }
}
