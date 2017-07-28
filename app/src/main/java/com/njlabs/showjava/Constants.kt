package com.njlabs.showjava

object Constants {

    val PROCESS_BROADCAST_ACTION = "com.njlabs.showjava.process.BROADCAST"
    val PROCESS_STATUS_KEY = "com.njlabs.showjava.process.STATUS_KEY"
    val PROCESS_STATUS_MESSAGE = "com.njlabs.showjava.process.STATUS_MESSAGE"
    val PROCESS_DIR = "com.njlabs.showjava.process.DIR"
    val PROCESS_PACKAGE_ID = "com.njlabs.showjava.process.PACKAGE_ID"
    val PROCESS_NOTIFICATION_ID = 1
    val STORAGE_PERMISSION_REQUEST = 3

    interface ACTION {
        companion object {
            val START_PROCESS = "com.njlabs.showjava.process.action.START"
            val STOP_PROCESS = "com.njlabs.showjava.process.action.STOP"
            val STOP_PROCESS_FOR_NEW = "com.njlabs.showjava.process.action.STOP_FOR_NEW"
        }
    }

    val VERIFICATION_URL = "https://api.codezero.xyz/com.njlabs.showjava/iap/verify/"
}