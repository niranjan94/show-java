package com.njlabs.showjava

object Constants {
    const val PROCESS_BROADCAST_ACTION = "com.njlabs.showjava.process.BROADCAST"
    const val PROCESS_STATUS_KEY = "com.njlabs.showjava.process.STATUS_KEY"
    const val PROCESS_STATUS_MESSAGE = "com.njlabs.showjava.process.STATUS_MESSAGE"
    const val PROCESS_DIR = "com.njlabs.showjava.process.DIR"
    const val PROCESS_PACKAGE_ID = "com.njlabs.showjava.process.PACKAGE_ID"
    const val PROCESS_NOTIFICATION_ID = 1
    const val STORAGE_PERMISSION_REQUEST = 3
    const val FILE_PICKER_RESULT = 9600

    interface ACTION {
        companion object {
            const val START_PROCESS = "com.njlabs.showjava.process.action.START"
            const val STOP_PROCESS = "com.njlabs.showjava.process.action.STOP"
            const val STOP_PROCESS_FOR_NEW = "com.njlabs.showjava.process.action.STOP_FOR_NEW"
        }
    }
}