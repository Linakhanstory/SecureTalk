package com.example.securetalk

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Chat(
    @DocumentId val chatId: String = "",
    val name: String? = null,
    val isGroup: Boolean = false,
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    @ServerTimestamp val lastTimestamp: Timestamp? = null,
    val unseenCount: Map<String, Int> = emptyMap(),
    val groupAdmin: String? = null,
    val typing: Map<String, Boolean> = emptyMap()
)
