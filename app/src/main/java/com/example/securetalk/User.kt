package com.example.securetalk

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = ""
)
