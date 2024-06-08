package com.shub39.grit.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.shub39.grit.database.habit.DailyHabitStatus
import com.shub39.grit.database.habit.Habit
import com.shub39.grit.database.habit.HabitDatabase
import com.shub39.grit.notification.NotificationAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HabitViewModel(applicationContext: Context) : ViewModel() {

    private val habitDatabase = Room.databaseBuilder(
        applicationContext,
        HabitDatabase::class.java,
        "habit-database"
    ).build()
    private val habitDao = habitDatabase.habitDao()
    private val habitStatusDao = habitDatabase.dailyHabitStatusDao()
    private val _habits = MutableStateFlow(listOf<Habit>())
    private val scheduler = NotificationAlarmScheduler(applicationContext)

    val habits: StateFlow<List<Habit>> get() = _habits

    init {
        viewModelScope.launch {
            _habits.value = habitDao.getAllHabits()
            _habits.value.forEach {
                scheduler.cancel(it)
                scheduler.schedule(it)
            }
        }
    }

    fun addHabit(habit: Habit) {
        viewModelScope.launch {
            _habits.value += habit
            habitDao.insertHabit(habit)
            scheduler.schedule(habit)
        }
    }

    fun addStatusForHabit(status: DailyHabitStatus) {
        viewModelScope.launch {
            habitStatusDao.insertDailyStatus(status)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitDao.deleteHabit(habit)
            _habits.value -= habit
            scheduler.cancel(habit)
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            habitDao.updateHabit(habit)
            _habits.value = habitDao.getAllHabits()
            scheduler.cancel(habit)
            scheduler.schedule(habit)
        }
    }

    fun getStatusForHabit(id: String): List<DailyHabitStatus> {
        var status = listOf<DailyHabitStatus>()
        viewModelScope.launch {
            status = habitStatusDao.getDailyStatusForHabit(id)
        }
        return status
    }
}

