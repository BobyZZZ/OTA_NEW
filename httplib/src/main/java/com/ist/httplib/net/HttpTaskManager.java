package com.ist.httplib.net;

import android.os.StatFs;
import android.util.Log;

import com.ist.httplib.CustomConverManager;
import com.ist.httplib.InvokeManager;
import com.ist.httplib.LitepalManager;
import com.ist.httplib.OtaLibConfig;
import com.ist.httplib.bean.BaseOtaRespone;
import com.ist.httplib.bean.DownloadBean;
import com.ist.httplib.bean.ThreadBean;
import com.ist.httplib.net.callback.UpdateListener;
import com.ist.httplib.net.callback.UpdateListenerAdapter;
import com.ist.httplib.net.retrofit.HttpCreate;
import com.ist.httplib.net.retrofit.HttpServer;
import com.ist.httplib.utils.DiskUtil;
import com.ist.httplib.utils.RxUtils;
import com.ist.httplib.utils.VersionComparator;

import java.io.File;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.ResourceObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

/**
 * Created by zhengshaorui
 * Time on 2018/9/12
 */

public class HttpTaskManager {
    public static boolean isDowning = false;

    private static final String TAG = "HttpTaskManager";
    private HttpServer mHttpServer;
    private UpdateListener mUpdateListener;
    private BaseOtaRespone mOtaBean;
    private OtaLibConfig.Builder mBuilder;

    private static class Holder {
        static final HttpTaskManager INSTANCE = new HttpTaskManager();
    }

    public static HttpTaskManager getInstance() {
        return Holder.INSTANCE;
    }

    private HttpTaskManager() {
        mHttpServer = HttpCreate.getService();
        mBuilder = OtaLibConfig.getBuilder();

    }


    public HttpTaskManager registerListener(UpdateListener listener){
        mUpdateListener = listener;

        return this;
    }

    /**
     * 检测是否升级
     */
    public void  checkUpdate(final UpdateListenerAdapter listenerAdapter){
        CustomConverManager.getInstance()
                .config(mBuilder.getUrl(),listenerAdapter);
        Log.d(TAG, "zsr --> checkUpdate: "+mBuilder.getCustomerId()+" isDebug: "+OtaLibConfig.getBuilder().isDebug());
        if (OtaLibConfig.getBuilder().isDebug()){
            CustomConverManager.getInstance().localTestCheck();
            return;
        }
        CustomConverManager.getInstance().kangjiaCheck();
    }


    /**
     * 开始下载
     */
    public void startDownload() {
        isDowning = true;
        Log.d(TAG, "zsr --> startDownload: "+mBuilder.getUrl());
        mHttpServer.get(mBuilder.getUrl())
                .compose(RxUtils.<BaseOtaRespone>rxScheduers())
                .subscribeWith(new ResourceObserver<BaseOtaRespone>() {
                    @Override
                    public void onNext(BaseOtaRespone otaRespone) {
                        mOtaBean = otaRespone;
                       // Log.d(TAG, "zsr --> onNext: "+mOtaBean.toString());
                        checkStart();
                    }

                    @Override
                    public void onError(Throwable e) {
                        mUpdateListener.error(NetErrorMsg.OTHERS,e.toString());
                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    /**
     * 检查更新
     */
    private void checkStart() {
        String url = null;
        if (mOtaBean.isDebug) {
            url = mOtaBean.debugUrl;
        }else{
            url = mOtaBean.fileUrl;
        }
        mHttpServer.getFileLength(url)
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<ResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        DownloadBean bean = new DownloadBean();
                        bean.fileName = mBuilder.getFileName();
                        //  bean.fileUrl = mOtaBean.fileUrl;
                        bean.threadCount = mBuilder.getThreadCount();
                        if (mOtaBean.isDebug) {
                            bean.fileUrl = mOtaBean.debugUrl;
                        }else{
                            bean.fileUrl = mOtaBean.fileUrl;
                        }
                        bean.filePath = mBuilder.getFilePath();
                        bean.listener = mUpdateListener;
                        bean.fileLength = responseBody.contentLength();
                        Log.d(TAG, "zyc-> bean.fileLength: " + bean.fileLength);
                        bean.version = InvokeManager.get(InvokeManager.SYSPROP_OTA_HAVEPUSH_FLAG,"false");
                        //内部存储至少要大于ota包
                        Log.d(TAG, "zsr --> accept update.zip's size: "+ DiskUtil.formatFileSize(mBuilder.getContext(),bean.fileLength)+
                                ",available size: "+ DiskUtil.formatFileSize(mBuilder.getContext(),getAvailDiskSize(mBuilder.getFilePath())));
                        //if (!LitepalManager.getInstance().isDbExsits()) {
                        List<ThreadBean> threadBeans = LitepalManager.getInstance().getAllThreadBean();
                        boolean hasDownloadBefore = false;
                        if (threadBeans != null && !threadBeans.isEmpty()) {
                            boolean exists = new File(bean.filePath, bean.fileName).exists();
                            //add zhouyc:添加容错处理(case:下载完成后adb或其它方式删除掉更新包),下载文件不存在时，删除下载记录；
                            if (!exists) {
                                Log.d(TAG, "have download records but file not exit,so just delete records");
                                LitepalManager.getInstance().deleteall();//需要同步删除
                            } else {
                                ThreadBean tmpThreadBean = threadBeans.get ( 1 );
                                int status  = VersionComparator.compare(bean.version,tmpThreadBean.version);
                                hasDownloadBefore = VersionComparator.compare(mOtaBean.version,tmpThreadBean.version) == 0;
                                if ( status > 0 || !hasDownloadBefore) {
                                    //比较数据库中版本和服务器上要下载的版本是否相等，不等则删除
                                    RxUtils.deleteDbFile ( );
                                }
                            }
                        }

                        if (getAvailDiskSize(mBuilder.getFilePath()) > bean.fileLength || hasDownloadBefore) {
                            Log.d(TAG, "zyc -> 之前下载过，不需要提示内存不够");
                            DownloadTask.getInstance().startDownload(bean);
                        } else {
                            mUpdateListener.error(NetErrorMsg.CACHE_NOT_ENOUGH, "cache not enough");
                        }
                        /**
                        if (threadBeans != null && !threadBeans.isEmpty()) {
                            Log.d(TAG, "wrl --> threadBeans: getAllThreadBean true1111");
                            DownloadTask.getInstance().startDownload(bean);

                            if (getAvailDiskSize(mBuilder.getFilePath()) > bean.fileLength) {
                                DownloadTask.getInstance().startDownload(bean);
                            } else {
                                mUpdateListener.error(NetErrorMsg.CACHE_NOT_ENOUGH, "cache not enough");
                            }
                        }else{
                            Log.d(TAG, "wrl --> threadBeans: getAllThreadBean false1111");
                            DownloadTask.getInstance().startDownload(bean);
                        }
                        **/
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace ();
                        mUpdateListener.error(NetErrorMsg.SERVER_NOT_FOUND,e.toString());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    public void pause(){
        DownloadTask.getInstance().pauseDownload();
    }



    public boolean isPause(){
        return DownloadTask.getInstance().isPause();
    }

    public  boolean isRunning(){
        return DownloadTask.getInstance().isRunning();
    }


    /**
     * 获取已经存储的大小
     * @return
     */
    private  long getAvailDiskSize(String path){
        StatFs sf = new StatFs(path);
        long blockSize = sf.getBlockSizeLong();
        long availCount = sf.getAvailableBlocksLong();

        return blockSize * availCount;
    }
}
