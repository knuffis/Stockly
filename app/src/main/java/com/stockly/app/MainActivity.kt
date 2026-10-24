package com.stockly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.stockly.app.ui.InventoriesScreen
import com.stockly.app.ui.ItemsScreen

class MainActivity : ComponentActivity() {
    private val viewModel: StocklyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val colors = if (isSystemInDarkTheme()) {
                darkColorScheme(primary = Color(0xFF7FD3B6), secondary = Color(0xFF9CCBBA))
            } else {
                lightColorScheme(primary = Color(0xFF00695C), secondary = Color(0xFF4A635C))
            }
            MaterialTheme(colorScheme = colors) {
                val open = viewModel.openInventory
                if (open == null) {
                    InventoriesScreen(viewModel)
                } else {
                    BackHandler { viewModel.close() }
                    ItemsScreen(viewModel, open)
                }
            }
        }
    }
}
