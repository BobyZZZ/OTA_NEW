package com.ist.httplib.bean;

import org.litepal.crud.LitePalSupport;

import java.util.Arrays;

/**
 * Created by zhengshaorui
 * Time on 2018/9/12
 */

public class ThreadBean extends LitePalSupport{
    public int theadId;
    public String url;
    public String name;
    public long startPos;
    public long endPos;
    public long fileLength; //文件的长度
    public long threadLength; //单个线程文件长度
    public String version;//need to sign the version of update.zip
    public byte[] dataCheck= new byte[20]; //each ThreadBean needs to count 20 datas

    @Override
    public String toString() {
        return "ThreadBean{" +
                "theadId=" + theadId +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", startPos=" + startPos +
                ", endPos=" + endPos +
                ", fileLength=" + fileLength +
                ", threadLength=" + threadLength +
                ", version='" + version + '\'' +
                ", dataCheck=" + Arrays.toString(dataCheck) +
                '}';
    }
}
