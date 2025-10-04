package com.example.personalassistant.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "events")
data class Event(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val title: String,
        val description: String,
        val date: String, // Format: yyyy-MM-dd
        val time: String? = null, // Format: HH:mm
        val isCompleted: Boolean = false,
        val createdAt: Long = System.currentTimeMillis(),
        val reminderTime: String? = null,
        val category: String = "general",
        val priority: Int = 1 // 1-low, 2-medium, 3-high
) : Parcelable {

    fun getFormattedDateTime(): String {
        return if (time != null) {
            "$date в $time"
        } else {
            date
        }
    }

    fun isToday(): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return date == today
    }

    fun isTomorrow(): Boolean {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val tomorrow = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        return date == tomorrow
    }
}
