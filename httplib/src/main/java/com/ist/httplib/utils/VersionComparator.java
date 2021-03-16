package com.ist.httplib.utils;

import android.util.Log;

import java.util.Comparator;

/**
 * add zhouyc:比较版本
 */
public class VersionComparator{
    String TAG = "VersionComparator";

    /**
     * @param o1
     * @param o2
     * @return 1: o1 > o2
     *         0: o1 = o2
     *        -1: o1 < o2
     */
    public static int compare(String o1, String o2) {
        return new InnerCompare().compare(o1,o2);
    }

    private static class InnerCompare implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            int[] v1 = spiltVersionToInteger(o1);
            int[] v2 = spiltVersionToInteger(o2);
            int smallLength = v1.length >= v2.length ? v2.length : v1.length;
            int bigLength = v1.length >= v2.length ? v1.length : v2.length;
            //比较版本长度相同部分
            for (int i = 0; i < smallLength; i++) {
                if (v1[i] == v2[i]) {
                    continue;
                }
                return v1[i] > v2[i] ? 1 : -1;
            }
            //再遍历不同部分
            if (smallLength != bigLength) {
                if (v1.length > v2.length) {
                    for (int i = smallLength; i < bigLength; i++) {
                        if (v1[i] > 0) {
                            return 1;
                        }
                    }
                } else if (v1.length < v2.length) {
                    for (int i = smallLength; i < bigLength; i++) {
                        if (v2[i] > 0) {
                            return -1;
                        }
                    }
                }
            }
            return 0;
        }

        private int[] spiltVersionToInteger(String version) {
            //必须v开头
            if (version == null || !(version.startsWith("V") || version.startsWith("v"))) {
                return new int[]{-1,-1,-1};
            }
            String intString = version.substring(1);
            String[] split = intString.split("\\.");
            int[] result = new int[split.length];
            for (int i = 0; i < split.length; i++) {
                result[i] = Integer.parseInt(split[i]);
            }
            return result;
        }
    }
}
