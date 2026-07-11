package com.androidblunders.rakshak.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.androidblunders.rakshak.core.model.ThreatLevel
import com.androidblunders.rakshak.presentation.ActiveThreatInterceptor
import com.androidblunders.rakshak.presentation.GentleGuidanceMode
import com.androidblunders.rakshak.ui.theme.RakshakTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    
    // Lifecycle components needed for Compose in a Service
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val threatLevelName = intent?.getStringExtra(EXTRA_THREAT_LEVEL) ?: return START_NOT_STICKY
        val threatLevel = try {
            ThreatLevel.valueOf(threatLevelName)
        } catch (e: IllegalArgumentException) {
            return START_NOT_STICKY
        }

        if (threatLevel == ThreatLevel.IDLE || threatLevel == ThreatLevel.LOW || threatLevel == ThreatLevel.MEDIUM) {
            removeOverlay()
            stopSelf()
            return START_NOT_STICKY
        }

        showOverlay(threatLevel)
        return START_STICKY
    }
    
    private fun showOverlay(threatLevel: ThreatLevel) {
        if (composeView == null) {
            composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            windowManager.addView(composeView, params)
        }

        // Update the Compose content based on the threat level
        composeView?.setContent {
            RakshakTheme {
                when (threatLevel) {
                    ThreatLevel.ACTIVE_THREAT -> {
                        ActiveThreatInterceptor(
                            onHangUp = {
                                removeOverlay()
                                stopSelf()
                            }
                        )
                    }
                    ThreatLevel.GENTLE_GUIDANCE -> {
                        GentleGuidanceMode(
                            onHangUp = {
                                removeOverlay()
                                stopSelf()
                            }
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    private fun removeOverlay() {
        composeView?.let {
            if (it.isAttachedToWindow) {
                windowManager.removeView(it)
            }
            composeView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        serviceScope.cancel()
        
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_THREAT_LEVEL = "extra_threat_level"
    }
}
