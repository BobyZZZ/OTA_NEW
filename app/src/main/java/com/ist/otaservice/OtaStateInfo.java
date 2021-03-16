package com.ist.otaservice;

import android.os.Parcel;
import android.os.Parcelable;

public class OtaStateInfo implements Parcelable {

    public int downloadProgree;
    public String downloadSpeed;
    public int ifInfoUsed;
    public int ifDownloadFinish;
    public int ifDownloading;
    public int ifUpdateFileExist;
    public int ifUpdatePush;
    public int ifUpdateVetifyDone;
    public int ifUSBUpdateExist;
    public int updateStateNow;
    public int ifTipsDownloading;

    public OtaStateInfo(){
    }


    protected OtaStateInfo ( Parcel in ) {
        downloadProgree = in.readInt ( );
        downloadSpeed = in.readString ( );
        ifInfoUsed = in.readInt ( );
        ifDownloadFinish = in.readInt ( );
        ifDownloading = in.readInt ( );
        ifUpdateFileExist = in.readInt ( );
        ifUpdatePush = in.readInt ( );
        ifUpdateVetifyDone = in.readInt ( );
        ifUSBUpdateExist = in.readInt ( );
        updateStateNow = in.readInt ( );
        ifTipsDownloading = in.readInt ( );
    }

    public static final Creator<OtaStateInfo> CREATOR = new Creator<OtaStateInfo> ( ) {
        @Override
        public OtaStateInfo createFromParcel ( Parcel in ) {
            return new OtaStateInfo ( in );
        }

        @Override
        public OtaStateInfo[] newArray ( int size ) {
            return new OtaStateInfo[size];
        }
    };

    @Override
    public int describeContents ( ) {
        return 0;
    }

    @Override
    public void writeToParcel ( Parcel dest , int flags ) {
        dest.writeInt ( downloadProgree );
        dest.writeString ( downloadSpeed );
        dest.writeInt ( ifInfoUsed );
        dest.writeInt ( ifDownloadFinish );
        dest.writeInt ( ifDownloading );
        dest.writeInt ( ifUpdateFileExist );
        dest.writeInt ( ifUpdatePush );
        dest.writeInt ( ifUpdateVetifyDone );
        dest.writeInt ( ifUSBUpdateExist );
        dest.writeInt ( updateStateNow );
        dest.writeInt ( ifTipsDownloading );
    }
}
