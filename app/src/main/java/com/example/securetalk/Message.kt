package com.example.securetalk

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Message(
    val senderId: String = "",
    val text: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null,
    val isRead: Boolean = false
)
