package com.example.securetalk

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val chatService = ChatService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Encryption
        EncryptionManager.init(this)

        // Firebase initialization check
        try {
            val app = FirebaseApp.getInstance()
            Log.d("FirebaseCheck", "Firebase initialized: ${app.name}")
        } catch (e: Exception) {
            Log.e("FirebaseCheck", "Firebase initialization failed!", e)
        }

        enableEdgeToEdge()
        setContent {
            var currentUser by remember { mutableStateOf(auth.currentUser) }
            var selectedChatInfo by remember { mutableStateOf<ChatSelection?>(null) }

            // Ensure user profile exists in Firestore
            LaunchedEffect(currentUser) {
                if (currentUser != null) {
                    chatService.syncUser()
                }
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (currentUser == null) {
                        AuthScreen(
                            onAuthSuccess = {
                                currentUser = auth.currentUser
                            }
                        )
                    } else if (selectedChatInfo == null) {
                        UsersScreen(
                            chatService = chatService,
                            onChatClick = { chatId, title, isGroup ->
                                if (chatId.isNotEmpty()) {
                                    selectedChatInfo = ChatSelection(chatId, title, isGroup)
                                }
                            },
                            onLogout = {
                                auth.signOut()
                                currentUser = null
                            }
                        )
                    } else {
                        ChatScreen(
                            chatService = chatService,
                            chatId = selectedChatInfo!!.chatId,
                            title = selectedChatInfo!!.title,
                            isGroup = selectedChatInfo!!.isGroup,
                            onBack = {
                                selectedChatInfo = null
                            }
                        )
                    }
                }
            }
        }
    }
}

data class ChatSelection(
    val chatId: String,
    val title: String,
    val isGroup: Boolean
)
