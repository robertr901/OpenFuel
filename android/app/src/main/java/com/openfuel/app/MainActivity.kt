package com.openfuel.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.openfuel.app.ui.navigation.OpenFuelAppRoot
import com.openfuel.app.ui.theme.OpenFuelTheme

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
}
