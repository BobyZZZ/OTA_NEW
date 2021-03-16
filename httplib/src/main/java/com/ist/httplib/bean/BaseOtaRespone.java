package com.ist.httplib.bean;

/**
 * Created by zhengshaorui
 * Time on 2018/9/12
 */

public class BaseOtaRespone<T> {
    public String  customID;
    public String version;
    public String fileUrl;
    public boolean isDebug;
    public String debugUrl;
    public String log;
    public T data;


    @Override
    public String toString() {
        return "BaseOtaRespone{" +
                "customID='" + customID + '\'' +
                ", version='" + version + '\'' +
                ", fileUrl='" + fileUrl + '\'' +
                ", isDebug=" + isDebug +
                ", debugUrl='" + debugUrl + '\'' +
                ", log='" + log + '\'' +
                ", data=" + data +
                '}';
    }
}
