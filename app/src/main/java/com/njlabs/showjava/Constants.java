package com.njlabs.showjava;

/**
 * Created by Niranjan on 02-04-2015.
 */
public final class Constants {

    public static final String PROCESS_BROADCAST_ACTION = "com.njlabs.showjava.process.BROADCAST";
    public static final String PROCESS_STATUS_KEY = "com.njlabs.showjava.process.STATUS_KEY";
    public static final String PROCESS_STATUS_MESSAGE = "com.njlabs.showjava.process.STATUS_MESSAGE";
    public static final String PROCESS_DIR = "com.njlabs.showjava.process.DIR";
    public static final String PROCESS_PACKAGE_ID = "com.njlabs.showjava.process.PACKAGE_ID";
    public static final int PROCESS_NOTIFICATION_ID = 1;
    public static final int STORAGE_PERMISSION_REQUEST = 3;
    public interface ACTION {
        String START_PROCESS = "com.njlabs.showjava.process.action.START";
        String STOP_PROCESS = "com.njlabs.showjava.process.action.STOP";
        String STOP_PROCESS_FOR_NEW = "com.njlabs.showjava.process.action.STOP_FOR_NEW";
    }

    public static final String VERIFICATION_URL = "https://api.codezero.xyz/com.njlabs.showjava/iap/verify/";

}