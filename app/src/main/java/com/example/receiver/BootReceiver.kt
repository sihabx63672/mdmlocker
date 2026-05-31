package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.LockerRepository
import com.example.service.LockerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Boot completed broadcast received. Checking lock state...")
            val repository = LockerRepository(context)

            // Query lock state on a background coroutine
            CoroutineScope(Dispatchers.IO).launch {
                val settings = repository.getSettings()
                if (settings.isLocked) {
                    Log.d("BootReceiver", "Device is remotely locked! Starting Lock Service...")
                    val serviceIntent = Intent(context, LockerService::class.java).apply {
                        putExtra("TRIGGER_OVERLAY", true)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        }
    }
}
