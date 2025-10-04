package com.example.personalassistant.data.dao

import androidx.room.*
import com.example.personalassistant.data.model.Event
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY date, time") fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE date = :date ORDER BY time")
    fun getEventsByDate(date: String): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE isCompleted = 0 ORDER BY date, time")
    fun getActiveEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE isCompleted = 1 ORDER BY date DESC")
    fun getCompletedEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE date BETWEEN :startDate AND :endDate ORDER BY date, time")
    fun getEventsInRange(startDate: String, endDate: String): Flow<List<Event>>

    @Insert suspend fun insertEvent(event: Event): Long

    @Update suspend fun updateEvent(event: Event)

    @Delete suspend fun deleteEvent(event: Event)

    @Query("DELETE FROM events WHERE id = :eventId") suspend fun deleteEventById(eventId: Long)

    @Query("UPDATE events SET isCompleted = :isCompleted WHERE id = :eventId")
    suspend fun updateEventCompletion(eventId: Long, isCompleted: Boolean)

    @Query(
            "SELECT * FROM events WHERE title LIKE :query OR description LIKE :query ORDER BY date, time"
    )
    fun searchEvents(query: String): Flow<List<Event>>
}
