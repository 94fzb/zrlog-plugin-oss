package com.zrlog.plugin.oss;

import java.security.MessageDigest;

public class Md5Utils {

    private static final char[] md5String = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String md5(byte[] bytes) {
        try {
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(bytes);
            byte[] md = mdInst.digest();

            int j = md.length;

            char[] str = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[(k++)] = md5String[(byte0 >>> 4 & 0xF)];
                str[(k++)] = md5String[(byte0 & 0xF)];
            }

            return new String(str).toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }
}
