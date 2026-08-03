package com.canvault.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.canvault.app.data.InventoryRepository
import com.canvault.app.data.ProjectRepository
import com.canvault.app.data.SharedCatalogRepository
import com.canvault.app.ui.screens.AddCanScreen
import com.canvault.app.ui.screens.CanDetailScreen
import com.canvault.app.ui.screens.CanMarketScreen
import com.canvault.app.ui.screens.ColorComboScreen
import com.canvault.app.ui.screens.DashboardScreen
import com.canvault.app.ui.screens.InventoryScreen
import com.canvault.app.ui.screens.MoreScreen
import com.canvault.app.ui.screens.ProjectDetailScreen
import com.canvault.app.ui.screens.ProjectsScreen
import com.canvault.app.ui.screens.ScanPrefill
import com.canvault.app.ui.screens.ScannerScreen
import com.canvault.app.ui.screens.StorageScreen
import com.canvault.app.ui.sound.CanVaultSoundProvider
import com.canvault.app.ui.sound.LocalCanVaultSounds
import com.canvault.app.ui.sound.UiSoundEffect

private data class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val destinations = listOf(
    Destination("dashboard", "Übersicht", Icons.Rounded.Dashboard),
    Destination("inventory", "Inventar", Icons.Rounded.Inventory2),
    Destination("add", "Hinzufügen", Icons.Rounded.AddCircle),
    Destination("archive", "Speicher", Icons.Rounded.Folder),
    Destination("projects", "Projekte", Icons.Rounded.Construction),
)

@Composable
fun CanVaultApp(
    repository: InventoryRepository,
    sharedCatalogRepository: SharedCatalogRepository,
    projectRepository: ProjectRepository,
) {
    CanVaultSoundProvider {
        CanVaultAppContent(repository, sharedCatalogRepository, projectRepository)
    }
}

@Composable
private fun CanVaultAppContent(
    repository: InventoryRepository,
    sharedCatalogRepository: SharedCatalogRepository,
    projectRepository: ProjectRepository,
) {
    val navController = rememberNavController()
    val sounds = LocalCanVaultSounds.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var scanPrefill by remember { mutableStateOf<ScanPrefill?>(null) }
    val showBottomBar = destinations.any { it.route == currentRoute }
    val navigateTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo("dashboard") {
                inclusive = false
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                sounds.play(UiSoundEffect.NAVIGATION)
                                navigateTopLevel(destination.route)
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier,
        ) {
            composable("dashboard") {
                DashboardScreen(
                    repository = repository,
                    contentPadding = contentPadding,
                    onScan = {
                        sounds.play(UiSoundEffect.SCAN)
                        navController.navigate("scanner")
                    },
                    onAdd = {
                        sounds.play(UiSoundEffect.PRIMARY)
                        navController.navigate("add")
                    },
                    onOpenColorCombo = {
                        sounds.play(UiSoundEffect.SHAKE)
                        navController.navigate("color-combo")
                    },
                    onOpenMarket = {
                        sounds.play(UiSoundEffect.STANDARD)
                        navController.navigate("market")
                    },
                    onOpenProjects = {
                        sounds.play(UiSoundEffect.NAVIGATION)
                        navigateTopLevel("projects")
                    },
                    onOpenCan = { navController.navigate("can/$it") },
                )
            }
            composable("inventory") {
                InventoryScreen(
                    repository = repository,
                    contentPadding = contentPadding,
                    onOpenCan = { navController.navigate("can/$it") },
                    onScan = {
                        sounds.play(UiSoundEffect.SCAN)
                        navController.navigate("scanner")
                    },
                    onOpenColorCombo = {
                        sounds.play(UiSoundEffect.SHAKE)
                        navController.navigate("color-combo")
                    },
                )
            }
            composable("add") {
                AddCanScreen(
                    repository = repository,
                    sharedCatalogRepository = sharedCatalogRepository,
                    contentPadding = contentPadding,
                    prefill = scanPrefill,
                    onPrefillConsumed = { scanPrefill = null },
                    onScan = {
                        sounds.play(UiSoundEffect.SCAN)
                        navController.navigate("scanner")
                    },
                    onBack = {
                        if (!navController.popBackStack()) navigateTopLevel("dashboard")
                    },
                    onSaved = {
                        sounds.play(UiSoundEffect.SUCCESS)
                        navigateTopLevel("inventory")
                    },
                )
            }
            composable("archive") {
                StorageScreen(
                    repository = repository,
                    contentPadding = contentPadding,
                    onOpenCan = { navController.navigate("can/$it") },
                )
            }
            composable("projects") {
                ProjectsScreen(
                    projectRepository = projectRepository,
                    inventoryRepository = repository,
                    contentPadding = contentPadding,
                    onOpenProject = { navController.navigate("project/$it") },
                    onOpenMore = { navController.navigate("more") },
                )
            }
            composable("more") {
                MoreScreen(
                    repository = repository,
                    contentPadding = contentPadding,
                    onBack = { navController.popBackStack() },
                    onOpenMarket = { navController.navigate("market") },
                )
            }
            composable("scanner") {
                ScannerScreen(
                    repository = repository,
                    sharedCatalogRepository = sharedCatalogRepository,
                    onBack = { navController.popBackStack() },
                    onResult = { result ->
                        sounds.play(UiSoundEffect.SUCCESS)
                        scanPrefill = result
                        navController.navigate("add") {
                            popUpTo("scanner") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable("market") {
                CanMarketScreen(
                    sharedCatalogRepository = sharedCatalogRepository,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("color-combo") {
                ColorComboScreen(
                    repository = repository,
                    onBack = {
                        if (!navController.popBackStack()) navigateTopLevel("dashboard")
                    },
                )
            }
            composable("can/{canId}") { entry ->
                CanDetailScreen(
                    repository = repository,
                    canId = entry.arguments?.getString("canId").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable("project/{projectId}") { entry ->
                ProjectDetailScreen(
                    projectRepository = projectRepository,
                    inventoryRepository = repository,
                    sharedCatalogRepository = sharedCatalogRepository,
                    projectId = entry.arguments?.getString("projectId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onOpenProject = { navController.navigate("project/$it") },
                )
            }
        }
    }
}
