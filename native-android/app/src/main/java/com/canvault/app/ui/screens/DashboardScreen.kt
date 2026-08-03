package com.canvault.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canvault.app.R
import com.canvault.app.data.CanStatus
import com.canvault.app.data.InventoryRepository
import com.canvault.app.ui.components.CanCard
import com.canvault.app.ui.components.EmptyState
import com.canvault.app.ui.components.StatCard
import com.canvault.app.ui.theme.CanVaultColors

@Composable
fun DashboardScreen(
    repository: InventoryRepository,
    contentPadding: PaddingValues,
    onScan: () -> Unit,
    onAdd: () -> Unit,
    onOpenColorCombo: () -> Unit,
    onOpenMarket: () -> Unit,
    onOpenCan: (String) -> Unit,
) {
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    val active = snapshot.cans.filter { it.status != CanStatus.ARCHIVED }
    val remainingMl = active.sumOf { can ->
        val volume = can.volumeMl ?: 0
        val fill = can.fillPercent ?: 0
        volume * fill / 100
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF7F7F5))
                        .padding(5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.canvault_logo),
                        contentDescription = "CANVAULT Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("CANVAULT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Deine Farben. Dein Bestand.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 }),
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        CanVaultColors.Mint.copy(alpha = 0.22f),
                                        MaterialTheme.colorScheme.surface,
                                    ),
                                ),
                            )
                            .padding(20.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Schnell erfassen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Produkt-Barcode scannen, Daten bestätigen und die Dose lokal speichern.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = onScan,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                            ) {
                                Icon(Icons.Rounded.DocumentScanner, contentDescription = null)
                                Text("Barcode scannen", Modifier.padding(start = 8.dp))
                            }
                            OutlinedButton(
                                onClick = onAdd,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Text("Manuell hinzufügen", Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                onClick = onOpenColorCombo,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp),
                    )
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text(
                            "Color Combo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            "Harmonische Paletten aus Füllstand, Menge und deinen exakten Farben",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }

        item {
            Card(
                onClick = onOpenMarket,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text("Can-Markt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Alle Dosenlinien, verifizierter Katalog und Preisvergleich",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Text("Bestand", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Aktive Dosen", active.size.toString(), Modifier.weight(1f))
                StatCard("Geschätzt übrig", "${remainingMl} ml", Modifier.weight(1f), CanVaultColors.Warning)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Farben", active.map { it.colorName.lowercase() }.distinct().size.toString(), Modifier.weight(1f))
                StatCard("Dosenlinien", active.map { it.canLineId }.distinct().size.toString(), Modifier.weight(1f))
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Zuletzt hinzugefügt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        if (active.isEmpty()) {
            item {
                EmptyState(
                    title = "Noch keine Dose im Vault",
                    body = "Scanne einen Produkt-Barcode oder lege deine erste Dose manuell an.",
                )
            }
        } else {
            items(active.sortedByDescending { it.createdAt }.take(4), key = { it.id }) { can ->
                CanCard(can = can, onClick = { onOpenCan(can.id) })
            }
        }
    }
}
