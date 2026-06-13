package com.personal.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.chat.data.database.ApiKeyEntity
import com.personal.chat.ui.components.BottomHeavyLayout
import com.personal.chat.ui.theme.CyberAccent
import com.personal.chat.ui.theme.ObsidianDark
import com.personal.chat.ui.theme.SlateGray
import com.personal.chat.ui.theme.SolidCard
import com.personal.chat.ui.viewmodel.KeyManagerViewModel

@Composable
fun KeyManagerScreen(viewModel: KeyManagerViewModel) {
    val keysList by viewModel.keysList.collectAsState()
    
    var newAlias by remember { mutableStateOf("") }
    var newKeyText by remember { mutableStateOf("") }

    BottomHeavyLayout(
        topContent = {
            Column(modifier = Modifier.fillMaxSize().background(ObsidianDark).padding(16.dp)) {
                Text("API Manager", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(keysList, key = { it.alias }) { keyEntity ->
                        KeyItemRow(
                            keyEntity = keyEntity,
                            onToggleActive = { viewModel.makeKeyActive(keyEntity.alias) },
                            onValidate = { viewModel.triggerKeyValidation(keyEntity.alias) },
                            onDelete = { viewModel.removeKey(keyEntity.alias) }
                        )
                    }
                }
            }
        },
        bottomInteractivePanel = {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = newAlias,
                        onValueChange = { newAlias = it },
                        modifier = Modifier.weight(0.4f),
                        placeholder = { Text("Alias") },
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                    TextField(
                        value = newKeyText,
                        onValueChange = { newKeyText = it },
                        modifier = Modifier.weight(0.6f),
                        placeholder = { Text("sk-or-...") },
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                }
                Button(
                    onClick = {
                        viewModel.registerNewKey(newAlias, newKeyText)
                        newAlias = ""
                        newKeyText = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAccent)
                ) {
                    Text("Add Keys", color = ObsidianDark)
                }
            }
        }
    )
}

@Composable
fun KeyItemRow(
    keyEntity: ApiKeyEntity,
    onToggleActive: () -> Unit,
    onValidate: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(SolidCard).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(keyEntity.alias, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                "Status: ${if (keyEntity.status == "UNCHECKED") "Unverified" else keyEntity.status}", 
                fontSize = 11.sp, 
                color = if (keyEntity.status == "VALID") CyberAccent else Color.Gray
            )
        }
        Switch(checked = keyEntity.isActive, onCheckedChange = { onToggleActive() })
        IconButton(onClick = onValidate) {
            Icon(Icons.Default.Verified, contentDescription = null, tint = CyberAccent)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
        }
    }
}
