package qzwx.app.qtodo

import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.navigation.compose.*
import qzwx.app.qtodo.navigation.*
import qzwx.app.qtodo.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QDemoTheme {
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentRoute in NavRoutes.bottomNavScreens) {
                            BottomNavBar(navController = navController)
                        }
                    }) { PaddingValues ->
                    Q_NavHost(
                        navController = navController,
                        modifier = Modifier.padding(PaddingValues)
                    )
                }
            }
        }
    }
}


