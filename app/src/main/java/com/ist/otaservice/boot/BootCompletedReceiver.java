package com.ist.otaservice.boot;

import android.content.BroadcastReceiver;  
import android.content.Context;  
import android.content.Intent;  
import android.util.Log;

import com.ist.otaservice.R;
import com.ist.otaservice.utils.IstUtil;
import com.ist.otaservice.utils.OtaUtil;

public class BootCompletedReceiver extends BroadcastReceiver {
    public static final String TAG = "BootCompletedReceiver";  
	static final String ACTION = "android.intent.action.BOOT_COMPLETED";  
    @Override  
    public void onReceive(Context context, Intent intent) {  
        if (ACTION.equals(intent.getAction())) {
            if(IstUtil.getPlanform()==IstUtil.PLANFORM_DEVICE_966){/*ABupdate*/
                IstUtil.setInABUpdateMode ( false );
            }
			Intent intent2 = new Intent(context, BootService.class);
            context.startService(intent2);
        } else {  
            Log.e(TAG, "Received unexpected intent " + intent.toString());  
        }  
    }  
}  