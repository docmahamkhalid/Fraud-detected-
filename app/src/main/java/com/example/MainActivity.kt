package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.firebase.configureAppCheck
import com.example.data.notifications.SecurityNotificationHelper
import com.example.ui.navigation.ScamShieldAppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        SecurityNotificationHelper.ensureNotificationChannels(this)
        enableEdgeToEdge()
        setContent {
            ScamShieldAppRoot()
        }
    }
}
