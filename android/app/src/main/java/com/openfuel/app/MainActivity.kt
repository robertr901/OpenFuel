package com.openfuel.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.openfuel.app.ui.navigation.OpenFuelAppRoot
import com.openfuel.app.ui.theme.OpenFuelTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenFuelTheme {
                OpenFuelAppRoot()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val app = application as? OpenFuelApp ?: return
        lifecycleScope.launch {
            app.container.entitlementService.refreshEntitlements()
        }
    }
}
