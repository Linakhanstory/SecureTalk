package com.example.securetalk

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    chatService: ChatService,
    onChatClick: (chatId: String, title: String, isGroup: Boolean) -> Unit,
    onLogout: () -> Unit
) {
    // Use real-time listeners for both chats and users
    val users by chatService.listenForUsers().collectAsState(initial = emptyList())
    val chats by chatService.listenForUserChats().collectAsState(initial = emptyList())
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SecureTalk") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 1 && users.isNotEmpty()) { // On Users tab
                ExtendedFloatingActionButton(
                    onClick = { 
                        scope.launch {
                            val userIds = users.map { it.uid }
                            if (userIds.isNotEmpty()) {
                                val gid = chatService.createGroupChat("New Group", userIds)
                                if (gid.isNotEmpty()) {
                                    onChatClick(gid, "New Group", true)
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Group, contentDescription = null) },
                    text = { Text("Create Group") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Chats") },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Users") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                if (chats.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No conversations yet.")
                    }
                } else {
                    LazyColumn {
                        items(chats) { chat ->
                            val title = if (chat.isGroup) chat.name ?: "Group" else {
                                // For private chat, we could fetch details, but fallback to a placeholder or participants
                                chat.participants.firstOrNull { it != chatService.currentUserId } ?: "Private Chat"
                            }
                            ListItem(
                                headlineContent = { Text(title) },
                                supportingContent = { 
                                    Text(
                                        text = chat.lastMessage,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.clickable { 
                                    if (chat.chatId.isNotEmpty()) {
                                        onChatClick(chat.chatId, title, chat.isGroup)
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            } else {
                if (users.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No other users found.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn {
                        items(users) { user ->
                            ListItem(
                                headlineContent = { Text(user.displayName.ifEmpty { user.email }) },
                                supportingContent = { Text(user.email) },
                                modifier = Modifier.clickable { 
                                    scope.launch {
                                        val chatId = chatService.getOrCreatePrivateChat(user.uid)
                                        if (chatId.isNotEmpty()) {
                                            onChatClick(chatId, user.displayName.ifEmpty { user.email }, false)
                                        }
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
