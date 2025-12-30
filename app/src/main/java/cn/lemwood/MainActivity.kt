package cn.lemwood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cn.lemwood.data.local.AppDatabase
import cn.lemwood.data.repository.ServerRepository
import cn.lemwood.ui.screens.DetailScreen
import cn.lemwood.ui.screens.HomeScreen
import cn.lemwood.ui.screens.SettingsScreen
import cn.lemwood.ui.viewmodel.ServerViewModel
import cn.lemwood.ui.theme.ServerSeeTheme
import cn.lemwood.ui.components.GlobalBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val repository = ServerRepository(database.serverDao())
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ServerViewModel(repository, applicationContext) as T
            }
        }
        val viewModel = ViewModelProvider(this, viewModelFactory)[ServerViewModel::class.java]

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val colorSchemeType by viewModel.colorScheme.collectAsState()
            val primaryColor by viewModel.primaryColor.collectAsState()
            
            val bgType by viewModel.backgroundType.collectAsState()
            val bgPath by viewModel.backgroundPath.collectAsState()
            val bgScale by viewModel.backgroundScale.collectAsState()
            val bgVolume by viewModel.backgroundVolume.collectAsState()
            val bgAlpha by viewModel.backgroundAlpha.collectAsState()

            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            ServerSeeTheme(
                darkTheme = darkTheme,
                colorSchemeType = colorSchemeType,
                seedColorHex = primaryColor
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    GlobalBackground(
                        type = bgType,
                        path = bgPath,
                        scale = bgScale,
                        volume = bgVolume
                    )
                    
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (bgType != "none") {
                            MaterialTheme.colorScheme.background.copy(alpha = bgAlpha)
                        } else {
                            MaterialTheme.colorScheme.background
                        }
                    ) {
                        val navController = rememberNavController()
                        
                        NavHost(navController = navController, startDestination = "home") {
                            composable("home") {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onServerClick = { serverId ->
                                        navController.navigate("detail/$serverId")
                                    },
                                    onSettingsClick = {
                                        navController.navigate("settings")
                                    }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("detail/{serverId}") { backStackEntry ->
                                val serverId = backStackEntry.arguments?.getString("serverId")?.toIntOrNull() ?: 0
                                DetailScreen(
                                    serverId = serverId,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
