package org.indin.blisslaunchero.features.launcher

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AppProvider : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
}
