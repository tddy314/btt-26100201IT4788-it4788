package com.example.human_posture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    private val model: PoseViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF007F73), onPrimary = Color.White,
                primaryContainer = Color(0xFFD6F4EA), secondary = Color(0xFF47647C),
                background = Color(0xFFF4F7FA), surface = Color.White,
                onSurface = Color(0xFF132A3B), onBackground = Color(0xFF132A3B),
            )) { PoseLab(model) }
        }
    }
}
