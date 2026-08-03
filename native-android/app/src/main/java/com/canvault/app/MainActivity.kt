package com.canvault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.canvault.app.data.InventoryRepository
import com.canvault.app.data.ProjectRepository
import com.canvault.app.data.SharedCatalogRepository
import com.canvault.app.ui.CanVaultApp
import com.canvault.app.ui.theme.CanVaultTheme

class MainActivity : ComponentActivity() {
    private val repository by lazy { InventoryRepository(applicationContext) }
    private val sharedCatalogRepository by lazy { SharedCatalogRepository(applicationContext) }
    private val projectRepository by lazy { ProjectRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CanVaultTheme {
                CanVaultApp(repository, sharedCatalogRepository, projectRepository)
            }
        }
    }
}
