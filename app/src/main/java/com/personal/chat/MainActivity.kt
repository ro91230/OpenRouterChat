package com.personal.chat

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.personal.chat.data.network.OpenRouterService
import com.personal.chat.ui.screens.ChatScreen
import com.personal.chat.ui.screens.KeyManagerScreen
import com.personal.chat.ui.screens.ModelDirectoryScreen
import com.personal.chat.ui.theme.CyberAccent
import com.personal.chat.ui.theme.ObsidianDark
import com.personal.chat.ui.theme.PersonalChatTheme
import com.personal.chat.ui.viewmodel.ChatViewModel
import com.personal.chat.ui.viewmodel.KeyManagerViewModel
import com.personal.chat.ui.viewmodel.ModelViewModel
import kotlinx.coroutines.flow.first
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private var isAuthenticated = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        showLockscreenBiometricPrompt()

        val app = application as ChatApplication
        val container = app.container

        setContent {
            PersonalChatTheme {
                if (isAuthenticated.value) {
                    val navController = rememberNavController()
                    
                    val chatViewModel: ChatViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ChatViewModel(
                                container.chatRepository,
                                container.processChatUseCase,
                                container.apiKeyDao
                            ) as T
                        }
                    })

                    val modelViewModel: ModelViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ModelViewModel(container.modelRepository) as T
                        }
                    })

                    val keyManagerViewModel: KeyManagerViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return KeyManagerViewModel(container.apiKeyDao, OpenRouterService()) as T
                        }
                    })

                    LaunchedEffect(Unit) {
                        container.chatRepository.createConversation(
                            title = "Technical Prompt Assistant",
                            systemPrompt = "You are an Elite Senior System Architect.",
                            temperature = 0.5f,
                            maxTokens = 2000,
                            contextCap = 15,
                            modelId = "google/gemini-2.5-pro"
                        )
                        val entity = container.chatRepository.getConversationsFlow().first().first()
                        chatViewModel.loadConversation(entity)
                    }

                    Scaffold(
                        bottomBar = {
                            NavigationBar(containerColor = ObsidianDark) {
                                NavigationBarItem(
                                    selected = true,
                                    onClick = { navController.navigate("chat") },
                                    icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                                    label = { Text("Chat") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CyberAccent,
                                        unselectedIconColor = Color.Gray
                                    )
                                )
                                NavigationBarItem(
                                    selected = false,
                                    onClick = { navController.navigate("models") },
                                    icon = { Icon(Icons.Default.Dns, contentDescription = null) },
                                    label = { Text("Models") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CyberAccent,
                                        unselectedIconColor = Color.Gray
                                    )
                                )
                                NavigationBarItem(
                                    selected = false,
                                    onClick = { navController.navigate("keys") },
                                    icon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                                    label = { Text("Keys") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CyberAccent,
                                        unselectedIconColor = Color.Gray
                                    )
                                )
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "chat",
                            modifier = Modifier.padding(innerPadding).fillMaxSize()
                        ) {
                            composable("chat") {
                                ChatScreen(chatViewModel, onNavigateToKeys = { navController.navigate("keys") })
                            }
                            composable("models") {
                                ModelDirectoryScreen(modelViewModel)
                            }
                            composable("keys") {
                                KeyManagerScreen(keyManagerViewModel)
                            }
                        }
                    }
                } else {
                    Surface(modifier = Modifier.fillMaxSize(), color = ObsidianDark) {}
                }
            }
        }
    }

    private fun showLockscreenBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isAuthenticated.value = true
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@MainActivity, "Auth Error: $errString", Toast.LENGTH_SHORT).show()
                finish()
            }
        })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Identity Verification Required")
            .setSubtitle("Authenticate ownership to view local credentials")
            .setAllowedAuthenticators(BiometricPrompt.Authenticators.BIOMETRIC_STRONG or BiometricPrompt.Authenticators.DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(info)
    }
}
