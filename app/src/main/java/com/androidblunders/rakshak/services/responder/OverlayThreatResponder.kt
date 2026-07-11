package com.androidblunders.rakshak.services.responder

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.androidblunders.rakshak.core.contract.ThreatResponder
import com.androidblunders.rakshak.core.model.ThreatLevel
import com.androidblunders.rakshak.services.OverlayService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An implementation of [ThreatResponder] that manages the SYSTEM_ALERT_WINDOW overlay.
 * 
 * When a threat escalates to ACTIVE_THREAT or GENTLE_GUIDANCE, this responder
 * starts the OverlayService to draw the protective UI over the scammer's call screen.
 * It removes the overlay when the threat subsides.
 */
@Singleton
class OverlayThreatResponder @Inject constructor(
    @ApplicationContext private val context: Context
) : ThreatResponder {

    override suspend fun onThreatLevel(level: ThreatLevel) {
        if (level == ThreatLevel.ACTIVE_THREAT || level == ThreatLevel.GENTLE_GUIDANCE) {
            if (Settings.canDrawOverlays(context)) {
                val intent = Intent(context, OverlayService::class.java).apply {
                    putExtra(OverlayService.EXTRA_THREAT_LEVEL, level.name)
                }
                // Start the service to show the overlay
                context.startService(intent)
            } else {
                // Overlay permission not granted.
                // In a production app, we would fallback to firing a high-priority
                // full-screen notification or activity here.
            }
        } else {
            // Dismiss overlay if active by sending a SAFE signal
            val intent = Intent(context, OverlayService::class.java).apply {
                putExtra(OverlayService.EXTRA_THREAT_LEVEL, ThreatLevel.IDLE.name)
            }
            context.startService(intent)
        }
    }
}
