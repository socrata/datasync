package com.socrata.datasync.config.userpreferences;

public class UserPreferencesUtil {
    static String prefixDomain(String s) {
        if(s.contains("//")) return s;
        else return "https://" + s;
    }
}
