package com.shubham.healthlens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shubham.healthlens.feature.medicine.presentation.MedicineRoute
import com.shubham.healthlens.ui.theme.HealthLensTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthLensTheme(dynamicColor = false) {
                MedicineRoute()
            }
        }
    }
}
