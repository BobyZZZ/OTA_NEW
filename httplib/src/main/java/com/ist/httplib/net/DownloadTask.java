package com.ist.httplib.net;

import android.util.Log;

import com.ist.httplib.LitepalManager;
import com.ist.httplib.OtaLibConfig;
import com.ist.httplib.bean.DownloadBean;
import com.ist.httplib.bean.ThreadBean;
import com.ist.httplib.net.callback.UpdateListener;
import com.ist.httplib.net.retrofit.HttpCreate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by zhengshaorui
 * Time on 2018/9/12
 */

public class DownloadTask {
    private static final String TAG = "DownloadTask wrl1";
    private ThreadPoolExecutor mExecutorService = new ThreadPoolExecutor(
            OtaLibConfig.getBuilder().getThreadCount(),
            OtaLibConfig.getBuilder().getThreadCount(),
            6, TimeUnit.SECONDS,new LinkedBlockingDeque<Runnable>());

    private UpdateListener mListener;
    private DownloadBean mDownloadBean;
    private AtomicLong mFileDownloadSize = new AtomicLong(0); //方便显示多线程的进度
    private long mSavedLength = 0;//记录已下载成功长度
    private final long mRetryTime = 5 * 1000;//5秒自动重试一次，12次共一分钟

    private List<DownloadThread> mDownloadTasks = new ArrayList<>();//方便检测多线程是否下载完成
    private volatile boolean isPause = false;
    private volatile boolean isRunning = false;

    private static class Holder {
        static final DownloadTask INSTANCE = new DownloadTask();
    }

    public static DownloadTask getInstance() {
        return Holder.INSTANCE;
    }

    private DownloadTask() {
    }

    public void pauseDownload(){
        isPause = true;
    }

    public boolean isPause(){
        return isPause;
    }

    public boolean isRunning(){
        return isRunning;
    }

    public void startDownloadWithoutBean(DownloadBean downloadBean) {
        long blocksize = downloadBean.fileLength / downloadBean.threadCount;
        Log.d(TAG, "download startDownload-33-"+downloadBean.fileLength+"<<<<<<< "+System.currentTimeMillis ());
        mFileDownloadSize.set(0);
        for (int i = 0; i < downloadBean.threadCount; i++) {
            long start = i * blocksize;
            long end = (i + 1) * blocksize - 1;

            if (i == downloadBean.threadCount - 1) { //最后一个除不尽，用文件长度代替
                end = downloadBean.fileLength;
            }
            ThreadBean bean = new ThreadBean();
            bean.url = downloadBean.fileUrl;
            bean.startPos = start;
            bean.endPos = end;
            bean.fileLength = downloadBean.fileLength;
            bean.theadId = i;
            bean.name = downloadBean.fileName;
            DownloadThread downloadThread = new DownloadThread(bean);
            mExecutorService.execute(downloadThread);
            mDownloadTasks.add(downloadThread);
        }
    }

    public void startDownloadWithCreateBean(DownloadBean downloadBean){
        long blocksize = downloadBean.fileLength / downloadBean.threadCount;
        Log.d(TAG, "download startDownload-22-"+downloadBean.fileLength+"<<<<<<< "+System.currentTimeMillis ());
        for (int i = 0; i < downloadBean.threadCount; i++) {
            long start = i * blocksize;
            long end = (i + 1) * blocksize - 1;

            if (i == downloadBean.threadCount - 1) { //最后一个除不尽，用文件长度代替
                end = downloadBean.fileLength;
            }
            ThreadBean bean = new ThreadBean();
            bean.url = downloadBean.fileUrl;
            bean.startPos = start;
            bean.endPos = end;
            bean.fileLength = downloadBean.fileLength;
            bean.theadId = i;
            bean.name = downloadBean.fileName;
            bean.version = downloadBean.version;

            //先保存数据库
            LitepalManager.getInstance().saveOrUpdate(bean);

            DownloadThread downloadThread = new DownloadThread(bean);
            mExecutorService.execute(downloadThread);
            mDownloadTasks.add(downloadThread);
        }
    }

    public void startDownloadWithExistBean(DownloadBean downloadBean, List<ThreadBean> threadBeans){
        Log.d(TAG, "download startDownload-11-"+mDownloadBean.fileLength+"<<<<<<< "+System.currentTimeMillis ());
        for (int i = 0; i < threadBeans.size(); i++) {
            ThreadBean bean = threadBeans.get(i);
            DownloadThread downloadthread = new DownloadThread(bean);
            mExecutorService.execute(downloadthread);
            mDownloadTasks.add(downloadthread);
        }
        Log.d(TAG, "zsr --> 有数据库啦: " + mFileDownloadSize.get() * 100f / mDownloadBean.fileLength);
    }

    public void startDownload(DownloadBean downloadBean){
        mDownloadBean = downloadBean;
        mListener = downloadBean.listener;
        mDownloadTasks.clear();
        mFileDownloadSize.set(0);
        isPause = false;
        long blocksize = downloadBean.fileLength / downloadBean.threadCount;
        //先看数据库是否已经存在
        if (OtaLibConfig.getBuilder().isUsbDb()) {
            List<ThreadBean> threadBeans = LitepalManager.getInstance().getAllThreadBean();
            if (threadBeans != null && !threadBeans.isEmpty()){
                startDownloadWithExistBean ( downloadBean , threadBeans);
            } else {
                startDownloadWithCreateBean ( downloadBean );
            }
        }else{
            startDownloadWithoutBean ( downloadBean );
        }


    }

    private synchronized void checkFinish(long fileLength){
        boolean isFinish = true;
        for (DownloadThread downloadThread : mDownloadTasks) {
            if (!downloadThread.isTheadFinished){
                isFinish = false;
                break;
            }
        }
        if (isFinish){
            for (DownloadThread downloadThread : mDownloadTasks) {
                //check if there is a thread with state==2 ,if it is,means that
                // one thread is  isFinish by trycounts>the max count,and the Download is not done
                //and we should send a message to restart by user
                if (downloadThread.threadState==2){
                    mListener.error(NetErrorMsg.OTHERS,"Connection timed out");
                    return;
                }
            }
            Log.d(TAG, "zyc -> checkFinish 已下载大小: " + mFileDownloadSize.get() + ",升级包大小：" + mDownloadBean.fileLength);
            mFileDownloadSize.set(0);
            isRunning = false;
            File file = new File(mDownloadBean.filePath,mDownloadBean.fileName);
            if (file.exists()){
                if (file.length() == fileLength){
                    mListener.complete(null);
                    //删除线程
                    Log.d(TAG, "zsr --> 全部完成啦");
                    if (OtaLibConfig.getBuilder().isUsbDb()) {
                        //LitepalManager.getInstance().deleteall();
                    }
                }else{
                    mListener.error(NetErrorMsg.FILE_LENGTH_NOT_SAME,"file length not same");
                }
            }

        }else{
            for (DownloadThread downloadThread : mDownloadTasks) {
                if (downloadThread.threadState==0){
                    return;
                }
            }
            isRunning = false;
            mListener.error(NetErrorMsg.OTHERS,"Connection timed out");
        }
    }

    /**
     * 实际下载类
     */
    class DownloadThread extends Thread {
        ThreadBean bean;
        int cutdatas = 20;

        int threadState=0;
        boolean isTheadFinished = false;
        boolean goTheadKill = false;

        public DownloadThread ( ThreadBean bean ) {
            this.bean = bean;
        }

        @Override
        public void run ( ) {
            super.run ( );
            {
                for (int i = 0; i < cutdatas; i++) {
                    Log.d ( TAG , "cutdatas check --" + bean.hashCode ( ) + ">>>>>>>doneslice: " +"bean.dataCheck-"+i+"="+bean.dataCheck[i] );
                }
                //cut to 20times
                long datelength = bean.endPos - bean.startPos;
                long blocksize = datelength / cutdatas;

                for (int i = 0; i < cutdatas; i++) {
                    long start = i * blocksize + bean.startPos;
                    long end = (i + 1) * blocksize - 1 + bean.startPos;
                    if ( i == cutdatas - 1 ) { //最后一个除不尽，用文件长度代替
                        end = bean.endPos;
                    }
                    if ( bean.dataCheck[i] == 0 ) {
                        int count=0;//12times to retry
                        while (count<12&&!isPause&&!goTheadKill) {
                            long timeStart = System.currentTimeMillis ();
                            if(downLoadDataSlice ( start , end , i )){
                                bean.dataCheck[i]=1;
                                if ( OtaLibConfig.getBuilder ( ).isUsbDb ( ) ) {
                                    LitepalManager.getInstance ( ).saveOrUpdate ( bean );
                                }
                                Log.d ( TAG , "download slice success--" + bean.theadId + ">>>>>>>doneslice: " +"i="+i );
                                break;
                            }
                            Log.d ( TAG , "download missingone!!!--" + bean.theadId + ">>>>>>>doneslice: " +"i="+i );
                            if(!goTheadKill){
                                count++;
                                long retryTime = System.currentTimeMillis ()-timeStart;
                                Log.d ( TAG , "download retryTime!!!--" + retryTime);
                                //if retry time less than 10s ,sleep for some seconds
                                if(retryTime< mRetryTime){
                                    try {
                                        Thread.sleep ( mRetryTime -retryTime );
                                    } catch (InterruptedException e) {
                                        e.printStackTrace ( );
                                    }
                                }
                            }else{
                                return;
                            }
                        }
                        if(count >= 12){
                            // 3 times check failed ,kill thread
                            threadState=2;
                            checkFinish ( bean.fileLength );
                            return;
                        }
                    }else{
                        if(!goTheadKill){
//                                mFileDownloadSize += blocksize;
                            mFileDownloadSize.addAndGet(blocksize);
                            Log.d(TAG, "开始下载，计算已下载大小，threadbean : " + bean.theadId + ",slice: " + i + ",mFileDownloadSize: " + mFileDownloadSize);
                        }else{
                            return;
                        }
                    }
                }
                for (int i = 0; i < cutdatas; i++) {
                    Log.d ( TAG , "cutdatas check2 --" + bean.hashCode ( ) + ">>>>>>>doneslice: " +"bean.dataCheck-"+i+"="+bean.dataCheck[i] );
                }
                if (isPause || goTheadKill) {
                    for (int j = 0; j < cutdatas; j++) {
                        if (bean.dataCheck[j] == 1) {
                            mSavedLength += blocksize;
                        }
                    }
                    Log.d(TAG,"取消下载，已下载大小：threadbean : " + bean.theadId + ",mSaveLength: " + mSavedLength);
                }
                if ( OtaLibConfig.getBuilder ( ).isUsbDb ( ) ) {
                    LitepalManager.getInstance ( ).saveOrUpdate ( bean );
                }
                if(isPause){
                    threadState=0;
                    return;
                }
                threadState=1;
                isTheadFinished = true;
                checkFinish ( bean.fileLength );
            }
            /**
            InputStream is = null;
            RandomAccessFile raf = null;
            try {
                Log.d ( TAG , "download --" + bean.hashCode ( ) + ">>>>>>> " + System.currentTimeMillis ( ) );

                Call<ResponseBody> call = HttpCreate.getService ( ).download ( bean.url , "bytes=" + bean.startPos + "-" + bean.endPos );
                Log.d ( TAG , "download --" + bean.hashCode ( ) + "<<<<<<< " + System.currentTimeMillis ( ) );

                Response<ResponseBody> response = call.execute ( );
                if ( response != null && response.isSuccessful ( ) ) {
                    Log.d ( TAG , "download --" + bean.hashCode ( ) + "======= " + System.currentTimeMillis ( ) );

                    is = response.body ( ).byteStream ( );
                    //设置本地的存储
                    File file = new File ( mDownloadBean.filePath , mDownloadBean.fileName );
                    raf = new RandomAccessFile ( file , "rwd" );
                    raf.seek ( bean.startPos );
                    byte[] bytes = new byte[4 * 1024];
                    int len;
                    while ((len = is.read ( bytes )) != -1) {
                        //Log.d ( TAG , "download read--" + bean.hashCode ( ) + "<<<<<<< " + System.currentTimeMillis ( ) );

                        raf.write ( bytes , 0 , len );
                        mFileDownloadSize += len;

                        final int progress = (int) (mFileDownloadSize * 100.0f / bean.fileLength);
                        mListener.doProgress ( progress , mFileDownloadSize , bean.fileLength );

                        //记录每个线程的结束点的值
                        bean.threadLength += len;
                        if ( isPause ) {
                            //保存到数据库
                            isRunning = false;
                            if ( OtaLibConfig.getBuilder ( ).isUsbDb ( ) ) {
                                LitepalManager.getInstance ( ).saveOrUpdate ( bean );
                                Log.d ( TAG , "download saveOrUpdate--" + bean.hashCode ( ) + "<<<<<<< " + System.currentTimeMillis ( ) );

                            }
                            return;
                        }


                    }
                    Log.d ( TAG , "download isTheadFinished--" + bean.hashCode ( ) + ">>>>>>> " + System.currentTimeMillis ( ) );

                    isTheadFinished = true;
                } else {
                    Log.d ( TAG , "download RESPONSE_IS_NULL--" + bean.hashCode ( ) + ">>>>>>> " + System.currentTimeMillis ( ) );

                    //RxUtils.deleteDbFile();
                    //mFileDownloadSize = 0;
                    mListener.error ( NetErrorMsg.RESPONSE_IS_NULL , "response.body() == null" );
                }
                isRunning = true;
                checkFinish ( bean.fileLength );
            } catch (final Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace ( );
                //不保存啦
                //RxUtils.deleteDbFile();
                String errorMsg = e.toString ( );
                //isRunning = false;
                //isPause = true;
                if ( errorMsg.contains ( "Connection timed out" ) ) {
                    Log.d ( TAG , "download IOException-timed out-" + bean.hashCode ( ) + "...... " + System.currentTimeMillis ( ) );

                    mListener.error ( NetErrorMsg.TIME_OUT , e.toString ( ) );
                } else {
                    mListener.error ( NetErrorMsg.OTHERS , e.toString ( ) );
                    Log.d ( TAG , "download IOException--" + bean.hashCode ( ) + "`````` " + System.currentTimeMillis ( ) );

                }

            } finally {
                try {
                    if ( is != null ) {
                        is.close ( );
                    }
                    if ( raf != null ) {
                        raf.close ( );
                    }
                    Log.d ( TAG , "download finally--" + bean.hashCode ( ) + ",,,,,, " + System.currentTimeMillis ( ) );

                } catch (IOException e) {
                    e.printStackTrace ( );
                }
            }
            **/
        }

        private boolean downLoadDataSlice ( long start , long end ,int i) {
                boolean ret = false;
                InputStream is = null;
                RandomAccessFile raf = null;
                try {
                    Call<ResponseBody> call = HttpCreate.getService ( ).download ( bean.url , "bytes=" + start + "-" + end );

                    Response<ResponseBody> response = call.execute ( );
                    if ( response != null && response.isSuccessful ( ) ) {
                        is = response.body ( ).byteStream ( );
                        //设置本地的存储
                        File file = new File ( mDownloadBean.filePath , mDownloadBean.fileName );
                        raf = new RandomAccessFile ( file , "rwd" );
                        raf.seek ( start );
                        byte[] bytes = new byte[4 * 1024];
                        int len;
                        while ((len = is.read ( bytes )) != -1) {
                            if(!mDownloadTasks.contains ( this )){
                                Log.d ( TAG , "download goTheadKill--<<<<<<< " + System.currentTimeMillis ( ) );
                                goTheadKill = true;
                                isRunning = false;
                                ret = false;
                                break;
                            }
                            //Log.d ( TAG , "download read--" + bean.hashCode ( ) + "<<<<<<< " + System.currentTimeMillis ( ) );
                            raf.write ( bytes , 0 , len );
//                                mFileDownloadSize += len;
                            mFileDownloadSize.addAndGet(len);

                            final int progress = (int) (mFileDownloadSize.get() * 100.0f / bean.fileLength);
                            mListener.doProgress ( progress , mFileDownloadSize.get() , bean.fileLength );

                            //记录每个线程的结束点的值
                            //bean.threadLength += len;
                            if ( isPause ) {
                                //保存到数据库
                                isRunning = false;
                                ret = false;
                                break;
                            }
                        }
                        if ( !isPause && !goTheadKill) {
                            ret = true;
                        }
                    } else {
                        //mListener.error ( NetErrorMsg.RESPONSE_IS_NULL , "response.body() == null" );
                        ret = false;
                    }
                    isRunning = true;
                } catch (final Exception e) {
                    bean.dataCheck[i]=0;
                    if ( OtaLibConfig.getBuilder ( ).isUsbDb ( ) ) {
                        LitepalManager.getInstance ( ).saveOrUpdate ( bean );
                    }
                    e.printStackTrace ( );
                    ret = false;
                } finally {
                    try {
                        if ( is != null ) {
                            is.close ( );
                        }
                        if ( raf != null ) {
                            raf.close ( );
                        }
                    } catch (IOException e) {
                        e.printStackTrace ( );
                    }
                }

            return ret;
        }
    }
}
