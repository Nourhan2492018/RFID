package com.hprt.rfiddemo.config

import android.os.PowerManager
import android.os.PowerManager.WakeLock


class AndroidWakeLock(internal var pmr: PowerManager)
{
    internal var wakelock: WakeLock? = null

    fun WakeLock() {
        if (wakelock == null) {
            wakelock = pmr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.javaClass.canonicalName)
        }
        wakelock!!.acquire()
    }

    fun ReleaseWakeLock() {
        if (wakelock != null && wakelock!!.isHeld) {
            wakelock!!.release()
            wakelock = null
        }
    }
}
