package com.example.securetalk

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatService {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    /**
     * Ensures the current user has a valid document in the 'users' collection.
     * This makes them visible to other users.
     */
    suspend fun syncUser() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        if (uid.isBlank()) return
        
        try {
            val userRef = db.collection("users").document(uid)
            val userData = mapOf(
                "uid" to uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.email?.split("@")?.get(0) ?: "User")
            )
            userRef.set(userData, SetOptions.merge()).await()
            Log.d("ChatService", "User synced successfully: $uid")
        } catch (e: Exception) {
            Log.e("ChatService", "Failed to sync user profile", e)
        }
    }

    /**
     * Real-time listener for all other users.
     */
    fun listenForUsers(): Flow<List<User>> = callbackFlow {
        val myUid = currentUserId
        val listener = db.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            val users = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        val finalUid = if (user.uid.isBlank()) doc.id else user.uid
                        if (finalUid != myUid && finalUid.isNotBlank()) {
                            user.copy(uid = finalUid)
                        } else null
                    } else null
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
            trySend(users)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Real-time listener for messages in a specific chat.
     */
    fun listenForMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        if (chatId.isBlank()) {
            trySend(emptyList())
            return@callbackFlow
        }
        
        val query = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val messages = snapshot?.toObjects(Message::class.java)?.map { message ->
                message.copy(text = EncryptionManager.decrypt(message.text))
            } ?: emptyList()
            trySend(messages)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Sends an encrypted message.
     */
    suspend fun sendMessage(chatId: String, text: String) {
        if (text.isBlank() || chatId.isBlank()) return

        try {
            val encryptedText = EncryptionManager.encrypt(text)
            val message = Message(
                senderId = currentUserId,
                text = encryptedText,
                isRead = false
            )

            val chatRef = db.collection("chats").document(chatId)
            val messageRef = chatRef.collection("messages").document()

            db.runBatch { batch ->
                batch.set(messageRef, message)
                val chatUpdate = mapOf(
                    "lastMessage" to encryptedText,
                    "lastTimestamp" to FieldValue.serverTimestamp()
                )
                batch.set(chatRef, chatUpdate, SetOptions.merge())
            }.await()
        } catch (e: Exception) {
            Log.e("ChatService", "Failed to send message", e)
        }
    }

    /**
     * Retrieves or creates a private chat document ID.
     */
    suspend fun getOrCreatePrivateChat(receiverId: String): String {
        val myUid = currentUserId
        if (myUid.isBlank() || receiverId.isBlank()) return ""
        
        val chatId = getChatId(myUid, receiverId)
        try {
            val chatRef = db.collection("chats").document(chatId)
            val snapshot = chatRef.get().await()
            
            if (!snapshot.exists()) {
                val newChat = Chat(
                    chatId = chatId,
                    participants = listOf(myUid, receiverId),
                    isGroup = false
                )
                chatRef.set(newChat).await()
            }
            return chatId
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * Creates a new group chat.
     */
    suspend fun createGroupChat(name: String, participantIds: List<String>): String {
        val myUid = currentUserId
        if (myUid.isBlank()) return ""
        
        try {
            val chatId = db.collection("chats").document().id
            val participants = (participantIds + myUid).distinct()
            val newChat = Chat(
                chatId = chatId,
                name = name,
                isGroup = true,
                participants = participants,
                groupAdmin = myUid
            )
            db.collection("chats").document(chatId).set(newChat).await()
            return chatId
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * Listens for chats where the user is a participant.
     */
    fun listenForUserChats(): Flow<List<Chat>> = callbackFlow {
        val uid = currentUserId
        if (uid.isBlank()) {
            trySend(emptyList())
            return@callbackFlow
        }
        
        val query = db.collection("chats")
            .whereArrayContains("participants", uid)
        
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            val chats = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val chat = doc.toObject(Chat::class.java)
                    chat?.copy(
                        chatId = doc.id,
                        lastMessage = EncryptionManager.decrypt(chat.lastMessage)
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
            
            trySend(chats.sortedByDescending { it.lastTimestamp })
        }
        awaitClose { listener.remove() }
    }

    suspend fun markMessagesAsRead(chatId: String) {
        if (chatId.isBlank()) return
        try {
            val snapshot = db.collection("chats").document(chatId)
                .collection("messages")
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = db.batch()
            var hasUpdates = false
            for (doc in snapshot.documents) {
                if (doc.getString("senderId") != currentUserId) {
                    batch.update(doc.reference, "isRead", true)
                    hasUpdates = true
                }
            }
            if (hasUpdates) batch.commit().await()
        } catch (e: Exception) { }
    }

    fun getChatId(u1: String, u2: String): String {
        if (u1.isBlank() || u2.isBlank()) return ""
        return if (u1 < u2) "${u1}_${u2}" else "${u2}_${u1}"
    }

    fun listenForTypingStatus(chatId: String): Flow<Map<String, Boolean>> = callbackFlow {
        if (chatId.isBlank()) {
            trySend(emptyMap())
            return@callbackFlow
        }
        
        val listener = db.collection("chats").document(chatId)
            .addSnapshotListener { snapshot, _ ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    val typing = snapshot?.get("typing") as? Map<String, Boolean> ?: emptyMap()
                    trySend(typing)
                } catch (e: Exception) {
                    trySend(emptyMap())
                }
            }
        awaitClose { listener.remove() }
    }

    fun setTypingStatus(chatId: String, isTyping: Boolean) {
        val uid = currentUserId
        if (chatId.isBlank() || uid.isBlank()) return
        
        try {
            db.collection("chats").document(chatId)
                .update("typing.$uid", isTyping)
        } catch (e: Exception) { }
    }
}
