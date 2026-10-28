package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserPreferences(
    @DocumentId val id: String = "",
    val userId: String = "",
    val workStartHour: Int = 8,
    val workEndHour: Int = 18,
    val breakDurationMinutes: Int = 15,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",
    val doNotDisturb: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val crossDeviceNotifications: Boolean = true,
    val activeDeviceId: String = "phone_primary",
    val deviceName: String = "Mi Teléfono (Android)",
    @ServerTimestamp val updatedAt: Timestamp? = null
)
