// OtaStateInfoManager.aidl
package com.ist.otaservice;

import com.ist.otaservice.OtaStateInfo;
// Declare any non-default types here with import statements
interface OtaStateInfoManager {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */

    OtaStateInfo getOtaStateInfo(int cmd);
    int sendOtaCmd(int cmd);
}
