package com.personal.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.chat.data.database.MessageEntity
import com.personal.chat.ui.components.BottomHeavyLayout
import com.personal.chat.ui.theme.CyberAccent
import com.personal.chat.ui.theme.ObsidianDark
import com.personal.chat.ui.theme.SlateGray
import com.personal.chat.ui.theme.SolidCard
import com.personal.chat.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(viewModel: ChatViewModel, onNavigateToKeys: () -> Unit) {
    val messages = viewModel.activeMessages
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val hasKey by viewModel.activeApiKey.collectAsState()
    
    var selectedMessageForEdit by remember { mutableStateOf<MessageEntity?>(null) }
    var editSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    BottomHeavyLayout(
        topContent = {
            Column(modifier = Modifier.fillMaxSize().background(ObsidianDark)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = viewModel.activeConversation.value?.title ?: "Select Conversation",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (!hasKey) {
                        Button(
                            onClick = onNavigateToKeys,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Set API Key", fontSize = 12.sp)
                        }
                    }
                }

                if (messages.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("No context found. Initiate session below.", color = SlateGray, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            MessageItem(
                                message = msg,
                                onLongClick = {
                                    if (msg.role == "assistant" || msg.role == "user") {
                                        selectedMessageForEdit = msg
                                        editSheetOpen = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomInteractivePanel = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (viewModel.isNetworkStreaming.value) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = CyberAccent,
                        trackColor = SlateGray
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = viewModel.inputBuffer.value,
                        onValueChange = { viewModel.inputBuffer.value = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Query AI model...", color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ObsidianDark,
                            unfocusedContainerColor = ObsidianDark,
                            focusedIndicatorColor = CyberAccent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    IconButton(
                        onClick = { viewModel.submitUserPrompt() },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberAccent)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Transmit", tint = ObsidianDark)
                    }
                }
            }
        }
    )

    if (editSheetOpen && selectedMessageForEdit != null) {
        val editingMessage = selectedMessageForEdit!!
        var textValue by remember { mutableStateOf(editingMessage.content) }
        var applyToContext by remember { mutableStateOf(editingMessage.editMode == "APPLY_TO_CONTEXT") }

        ModalBottomSheet(onDismissRequest = { editSheetOpen = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Edit Linear History Message State", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                
                TextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5,
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Apply to Context", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            "Inject adjustments directly into downstream requests", 
                            fontSize = 11.sp, 
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = applyToContext,
                        onCheckedChange = { applyToContext = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { editSheetOpen = false }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = {
                            viewModel.applyMessageModification(editingMessage, textValue, applyToContext)
                            editSheetOpen = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberAccent)
                    ) {
                        Text("Save Changes", color = ObsidianDark)
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageItem(message: MessageEntity, onLongClick: () -> Unit) {
    val isUser = message.role == "user"
    val background = if (isUser) CyberAccent.copy(alpha = 0.15f) else SolidCard
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(background)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
                .padding(12.dp)
        ) {
            Column {
                if (message.isPinned) {
                    Icon(
                        Icons.Default.PushPin, 
                        contentDescription = "Pinned", 
                        tint = CyberAccent, 
                        modifier = Modifier.size(12.dp).align(Alignment.End)
                    )
                }
                Text(
                    text = message.content,
                    color = Color.White,
                    fontSize = 14.sp
                )
                
                if (message.editHistoryJson.length > 2) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Edited (${if (message.editMode == "APPLY_TO_CONTEXT") "Context" else "UI Default"})",
                        fontSize = 10.sp,
                        color = CyberAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
