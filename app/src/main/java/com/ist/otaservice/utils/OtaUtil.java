package com.ist.otaservice.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.RecoverySystem;
import android.util.Log;
import android.widget.Toast;

import com.ist.httplib.LitepalManager;
import com.ist.httplib.OtaLibConfig;
import com.ist.httplib.bean.BaseOtaRespone;
import com.ist.otaservice.Constant;
import com.ist.otaservice.MainApplication;
import com.ist.otaservice.R;
import com.ist.otaservice.manager.ThreadManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * Created by zhengshaorui
 * Time on 2018/9/12
 */

public class OtaUtil {
    private static final String TAG = "OtaUtil";
    static final String SYSTEM_FILE_PATH = "system/build.prop";
    private static Context sContext = MainApplication.CONTEXT;

    public static void updateOta(final Context context, final File otaFile) {
        if(IstUtil.getPlanform()==IstUtil.PLANFORM_DEVICE_966){
            ABUpdateUtil.startUpgrade( ABUpdateUtil.MODE_USB ,context);
        }else {
            ThreadManager.getInstance ( ).execute ( new Runnable ( ) {
                @Override
                public void run ( ) {
                    if ( otaFile.exists ( ) ) {
                        Log.d ( TAG , "zsr --> onClick: " + otaFile.getAbsolutePath ( ) );

                        try {
                            if ( OtaLibConfig.getBuilder ( ).isUsbDb ( ) ) {
                                LitepalManager.getInstance ( ).deleteall ( );
                            }
                            RecoverySystem.installPackage ( context , otaFile );

                        /*String path = otaFile.getAbsolutePath();
                        if(path.contains("/update.zip")){
                            path = otaFile.getAbsolutePath().replace("/update.zip","");
                        }
                        Log.d("Keven", "run: path =="+path);
                        Log.d("Keven", "run: pathssss =="+ Environment.getExternalStorageDirectory().getAbsolutePath());
                       if(path.contains("/storage/emulated/0")) {
                           Log.e("Keven", "run: 111111" );
                           updateSystemForPath(path);
                       }else if(path.contains("/storage/sda1")){
                           Log.e("Keven", "run: 22222" );
                           updateSystem();
                       }*/

                        } catch (Exception e) {
                            Log.d ( TAG , "zsr --> error: " + e.toString ( ) );
                            e.printStackTrace ( );
                        }

                    }
                }
            } );
        }
    }





    public static void updateSystemForPath(String filepath){
        try {
            Class<?> IstCustomerManagerClass = Class.forName("com.ist.android.tv.IstCustomerManager");
            Method method_updateSystemForPath = IstCustomerManagerClass.getMethod("updateSystemForPath",String.class);
            Method methodGetInstance = IstCustomerManagerClass.getMethod("getInstance");
            Object mIstCustomerManager = methodGetInstance.invoke(IstCustomerManagerClass);
            method_updateSystemForPath.setAccessible(true);
            method_updateSystemForPath.invoke(mIstCustomerManager,filepath);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public static void updateSystem(){
        try {
            Class<?> IstCustomerManagerClass = Class.forName("com.ist.android.tv.IstCustomerManager");
            Method method_updateSystem = IstCustomerManagerClass.getMethod("updateSystem");
            Method methodGetInstance = IstCustomerManagerClass.getMethod("getInstance");
            Object mIstCustomerManager = methodGetInstance.invoke(IstCustomerManagerClass);
            method_updateSystem.setAccessible(true);
            method_updateSystem.invoke(mIstCustomerManager);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void startUpgrade(final Context context, final File file, int msgid, final int nateid){
        startUpgrade(context,file,msgid,nateid,null);
    }

    /**
     * 开始升级
     * @param context
     * @param file
     * @param msgid
     * @param nateid
     */
    public static void startUpgrade(final Context context, final File file, int msgid, final int nateid,final  Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);
        builder.setMessage(msgid);
        builder.setPositiveButton(R.string.upgrade, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SprefUtils.saveSprefValue(SprefUtils.KEY_REMIND_ME,false);
                OtaUtil.updateOta(context, file);
                if (activity != null){
                    activity.finish();
                }
            }
        }).setNegativeButton(nateid, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (nateid == R.string.delete){
                    file.delete();
                    Toast.makeText(context, context.getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
                }
                if (nateid == R.string.remind_me){
                    SprefUtils.saveSprefValue(SprefUtils.KEY_REMIND_ME,true);
                }
                if (activity != null){
                    activity.finish();
                }
            }
        });

        Dialog dialog = builder.create();
        dialog.show();
        if (activity != null) {
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    activity.finish();
                }
            });
        }
    }

    /**
     * case：直接打开压缩包删除里边的一个文件，然后本地升级会出现如下情况：
     * 校验到99的时候抛出异常，然后到100，所以不能只根据progress == 100就视为成功
     */
    private static boolean verifyFailed = false;
    /**
     * 判断文件是否可升级
     *
     * @return
     */
    public static void checkLocalHasUpdateFile(final File file,final CheckListener listener) {

        if (file.exists()) {
            verifyFailed = false;
            ThreadManager.getInstance().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        RecoverySystem.verifyPackage(file, new RecoverySystem.ProgressListener() {
                            @Override
                            public void onProgress(int progress) {
                                io.reactivex.Observable.just(progress)
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new Consumer<Integer>() {
                                            @Override
                                            public void accept(Integer integer) throws Exception {
                                                listener.check(integer);
                                                Log.e("Keven", "accept: integer =="+integer + "---verifyFailed: " + verifyFailed);
                                                if (!verifyFailed &&integer == 100){
                                                    //PopUtils.downloadComplete(context, file,msgid,leftid);
                                                    listener.success();
                                                }
                                            }
                                        });
                            }
                        },null);
                    } catch (Exception e) {
                        verifyFailed = true;
                        Log.d(TAG, "zsr --> verifyPackage error: "+e.toString());
                        e.printStackTrace();
                        Observable.just(e.toString())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Consumer<String>() {
                                    @Override
                                    public void accept(String msg) throws Exception {
                                        listener.fail(msg);
                                    }
                                });
                    }

                }
            });

        }
    }

    public static void loadUpdateLogToFile ( BaseOtaRespone respone ) {
        String t =respone.log;
        if(respone.log==null){
            t="";
        }
        if(respone.log=="null"){
            t="";
        }
        FileOutputStream out = null;
        try {
            Log.d(TAG, "wrl --> loadUpdateLogToFile:  " + t);
            File file = new File ( Constant.SAVE_PATH , Constant.LOG_FILE_NAME );
            out = new FileOutputStream ( file );
            byte[]  bytes = t.getBytes();
            out.write(bytes);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
         }finally {
            if ( out != null ) {
                try {
                    out.close ( );
                } catch (IOException e) {
                    e.printStackTrace ( );
                }
            }
        }
    }


    public interface CheckListener{
        void check(int progress);
        void success();
        void fail(String errorMsg);
    }



}
