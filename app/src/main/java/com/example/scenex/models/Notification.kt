package com.example.scenex.models

import com.google.firebase.firestore.PropertyName

data class Notification(
    val notificationId: String = "",
    val receiverId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val referenceId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    
    @get:PropertyName("read")
    @set:PropertyName("read")
    var read: Boolean = false
)