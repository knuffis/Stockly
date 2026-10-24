package com.stockly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.stockly.app.ui.InventoriesScreen
import com.stockly.app.ui.ItemsScreen
import com.stockly.app.ui.StocklyTheme

class MainActivity : ComponentActivity() {
    private val viewModel: StocklyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StocklyTheme {
                AnimatedContent(
                    targetState = viewModel.openInventoryId,
                    transitionSpec = {
                        val forward = targetState != null
                        (slideInHorizontally { w -> if (forward) w else -w } + fadeIn()) togetherWith
                            (slideOutHorizontally { w -> if (forward) -w / 3 else w } + fadeOut())
                    },
                    label = "screen",
                ) { openId ->
                    val open = viewModel.inventories.firstOrNull { it.id == openId }
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
}
