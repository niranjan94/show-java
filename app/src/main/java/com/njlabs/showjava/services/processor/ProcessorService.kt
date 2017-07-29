package com.njlabs.showjava.services.processor

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ProcessorService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
