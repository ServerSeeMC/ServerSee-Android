package cn.lemwood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
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
import cn.lemwood.ui.viewmodel.ServerViewModel

import cn.lemwood.ui.theme.ServerSeeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val repository = ServerRepository(database.serverDao())
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ServerViewModel(repository) as T
            }
        }
        val viewModel = ViewModelProvider(this, viewModelFactory)[ServerViewModel::class.java]

        setContent {
            ServerSeeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onServerClick = { serverId ->
                                    navController.navigate("detail/$serverId")
                                }
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
