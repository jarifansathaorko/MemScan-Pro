package com.example.memorylab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.ui.theme.ImmersiveBackground
import com.example.ui.theme.MyApplicationTheme

/**
 * Standalone activity for MemoryLab test target.
 */
class MemoryLabActivity : ComponentActivity() {
    private val viewModel: MemoryLabViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = ImmersiveBackground
                ) { innerPadding ->
                    MemoryLabScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel,
                        onSwitchToScanner = { finish() }
                    )
                }
            }
        }
    }
}
