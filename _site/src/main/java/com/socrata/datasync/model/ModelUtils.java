package com.socrata.datasync.model;

/**
 * Created by franklinwilliams on 7/22/15.
 */
public class ModelUtils {

    public static String generatePlaceholderName(int index){
        String dummyName = "column_" + index;
        return dummyName;
    }
}
