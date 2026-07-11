package com.androidblunders.rakshak.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Auto-activates call protection. Statically registered for
 * `android.intent.action.PHONE_STATE`, so it fires even when the app UI is
 * closed (as long as the process can be started by the system broadcast).
 *
 *   OFFHOOK (call connected) → [CallRecordingService.start]
 *   IDLE    (call ended)     → [CallRecordingService.stop]
 *
 * RINGING is ignored — we only record once a call is actually connected.
 * Requires `READ_PHONE_STATE`.
 */
class PhoneCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                Log.i(TAG, "Call connected → starting protection")
                runCatching { CallRecordingService.start(context) }
                    .onFailure { Log.e(TAG, "Could not start recording service", it) }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                Log.i(TAG, "Call ended → stopping protection")
                runCatching { CallRecordingService.stop(context) }
            }
            else -> Unit // RINGING or unknown — wait for OFFHOOK.
        }
    }

    private companion object { const val TAG = "PhoneCallReceiver" }
}
