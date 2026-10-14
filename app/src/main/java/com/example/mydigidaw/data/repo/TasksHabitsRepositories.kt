package com.example.mydigidaw.data.repo

import com.example.mydigidaw.data.db.HabitDao
import com.example.mydigidaw.data.db.HabitEntity
import com.example.mydigidaw.data.db.HabitLogEntity
import com.example.mydigidaw.data.db.TaskDao
import com.example.mydigidaw.data.db.TaskEntity
import com.example.mydigidaw.reminders.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TasksRepository(private val dao: TaskDao, private val reminders: ReminderScheduler) {
    val tasks: Flow<List<TaskEntity>> = dao.observeAll()

    suspend fun get(id: Long): TaskEntity? = dao.get(id)

    suspend fun save(task: TaskEntity): Long {
        val id = if (task.id == 0L) dao.insert(task) else task.id.also { dao.update(task) }
        reminders.scheduleTask(task.copy(id = id))
        return id
    }

    suspend fun setDone(id: Long, done: Boolean) {
        dao.setDone(id, done, if (done) System.currentTimeMillis() else null)
        dao.get(id)?.let(reminders::scheduleTask)
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
        reminders.cancel(ReminderScheduler.KIND_TASK, id)
    }

    suspend fun deleteDone() = dao.deleteDone()

    /** Mengembalikan tugas yang baru dihapus (untuk "Urungkan"). */
    suspend fun restore(task: TaskEntity) {
        dao.insert(task)
        reminders.scheduleTask(task)
    }
}

class HabitsRepository(private val dao: HabitDao, private val reminders: ReminderScheduler) {
    val habits: Flow<List<HabitEntity>> = dao.observeHabits()

    fun logsSince(fromDay: LocalDate): Flow<List<HabitLogEntity>> = dao.observeLogs(fromDay.toEpochDay())
    fun logsFor(habitId: Long): Flow<List<HabitLogEntity>> = dao.observeLogsFor(habitId)

    suspend fun get(id: Long): HabitEntity? = dao.get(id)

    suspend fun save(habit: HabitEntity): Long {
        val id = if (habit.id == 0L) dao.insert(habit) else habit.id.also { dao.update(habit) }
        reminders.scheduleHabit(habit.copy(id = id))
        return id
    }

    suspend fun delete(id: Long) {
        dao.deleteLogsFor(id)
        dao.delete(id)
        reminders.cancel(ReminderScheduler.KIND_HABIT, id)
    }

    suspend fun count(habitId: Long, day: LocalDate): Int = dao.log(habitId, day.toEpochDay())?.count ?: 0

    suspend fun setCount(habitId: Long, day: LocalDate, count: Int) {
        if (count <= 0) dao.deleteLog(habitId, day.toEpochDay())
        else dao.upsertLog(HabitLogEntity(habitId, day.toEpochDay(), count.coerceAtMost(99)))
    }

    suspend fun increment(habitId: Long, day: LocalDate) = setCount(habitId, day, count(habitId, day) + 1)

    suspend fun decrement(habitId: Long, day: LocalDate) = setCount(habitId, day, count(habitId, day) - 1)

    /** Tandai tuntas / batalkan untuk satu hari. */
    suspend fun toggle(habit: HabitEntity, day: LocalDate) {
        val current = count(habit.id, day)
        setCount(habit.id, day, if (current >= habit.targetPerDay) 0 else habit.targetPerDay)
    }
}
