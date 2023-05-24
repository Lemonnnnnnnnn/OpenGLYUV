package com.quectel.openglyuv.display.utils;

import java.lang.reflect.Method;


public class SystemPropertiesProxy {

    public static void setProp(String key, String string) {
        String value = string;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("set", String.class, String.class);
            get.invoke(c, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getProp(String key, String defaultValue) {
        String value = defaultValue;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            value = (String) (get.invoke(c, key, "unknown"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return value;
        }
    }

}