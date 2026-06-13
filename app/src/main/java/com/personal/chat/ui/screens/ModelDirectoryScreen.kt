package com.personal.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.chat.data.database.ModelEntity
import com.personal.chat.ui.components.BottomHeavyLayout
import com.personal.chat.ui.theme.CyberAccent
import com.personal.chat.ui.theme.ObsidianDark
import com.personal.chat.ui.theme.SlateGray
import com.personal.chat.ui.theme.SolidCard
import com.personal.chat.ui.viewmodel.ModelViewModel

@Composable
fun ModelDirectoryScreen(viewModel: ModelViewModel) {
    val models by viewModel.availableModels.collectAsState()
    val isSyncing by viewModel.isSyncing

    BottomHeavyLayout(
        topContent = {
            Column(modifier = Modifier.fillMaxSize().background(ObsidianDark).padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Model Directory", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = { viewModel.startManualSync() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = CyberAccent)
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (isSyncing) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyberAccent)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(models, key = { it.id }) { model ->
                            ModelRow(
                                model = model,
                                onFavoriteToggle = { viewModel.toggleFavorite(model.id, model.isFavorite) }
                            )
                        }
                    }
                }
            }
        },
        bottomInteractivePanel = {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Autosync triggers daily.", 
                    color = Color.White, 
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Toggle hearts to highlight high-priority models on home panels.", 
                    color = Color.Gray, 
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    )
}

@Composable
fun ModelRow(model: ModelEntity, onFavoriteToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SolidCard)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(model.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Limit: ${model.contextLength} tokens", color = Color.Gray, fontSize = 11.sp)
            Text(
                "In: $${model.promptPricing}/M | Out: $${model.completionPricing}/M", 
                color = CyberAccent, 
                fontSize = 11.sp
            )
        }
        IconButton(onClick = onFavoriteToggle) {
            Icon(
                imageVector = if (model.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (model.isFavorite) CyberAccent else Color.Gray
            )
        }
    }
}
