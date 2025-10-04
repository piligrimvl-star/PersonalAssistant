package com.example.personalassistant.data.repository

import com.example.personalassistant.data.dao.EventDao
import com.example.personalassistant.data.model.Event
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.Flow

class EventRepository(private val eventDao: EventDao) {

    fun getAllEvents(): Flow<List<Event>> = eventDao.getAllEvents()

    fun getEventsByDate(date: String): Flow<List<Event>> = eventDao.getEventsByDate(date)

    fun getActiveEvents(): Flow<List<Event>> = eventDao.getActiveEvents()

    fun getCompletedEvents(): Flow<List<Event>> = eventDao.getCompletedEvents()

    fun getTodayEvents(): Flow<List<Event>> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return eventDao.getEventsByDate(today)
    }

    fun getWeekEvents(): Flow<List<Event>> {
        val calendar = Calendar.getInstance()
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        return eventDao.getEventsInRange(startDate, endDate)
    }

    suspend fun insertEvent(event: Event): Long = eventDao.insertEvent(event)

    suspend fun updateEvent(event: Event) = eventDao.updateEvent(event)

    suspend fun deleteEvent(event: Event) = eventDao.deleteEvent(event)

    suspend fun deleteEventById(eventId: Long) = eventDao.deleteEventById(eventId)

    suspend fun markEventCompleted(eventId: Long) = eventDao.updateEventCompletion(eventId, true)

    suspend fun markEventIncomplete(eventId: Long) = eventDao.updateEventCompletion(eventId, false)

    fun searchEvents(query: String): Flow<List<Event>> = eventDao.searchEvents("%$query%")
}
