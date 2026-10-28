package com.example

import com.example.data.model.ScheduleBlock
import com.example.data.model.Task
import com.example.data.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DayPlannerUnitTest {

    @Test
    fun testTaskPriorityWeights() {
        val urgent = Task(priority = "urgent")
        val high = Task(priority = "high")
        val medium = Task(priority = "medium")
        val low = Task(priority = "low")

        assertTrue(urgent.priorityWeight > high.priorityWeight)
        assertTrue(high.priorityWeight > medium.priorityWeight)
        assertTrue(medium.priorityWeight > low.priorityWeight)
        assertEquals(4, urgent.priorityWeight)
        assertEquals(1, low.priorityWeight)
    }

    @Test
    fun testTaskOverdueCalculation() {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val tomorrow = LocalDate.now().plusDays(1).toString()

        val overdueTask = Task(dueDate = yesterday, status = "pending")
        val futureTask = Task(dueDate = tomorrow, status = "pending")
        val completedYesterdayTask = Task(dueDate = yesterday, status = "completed")

        assertTrue(overdueTask.isOverdue)
        assertTrue(!futureTask.isOverdue)
        assertTrue(!completedYesterdayTask.isOverdue)
    }

    @Test
    fun testDeterministicTaskSorting() {
        val task1 = Task(id = "1", title = "Low priority", priority = "low", estimatedMinutes = 30)
        val task2 = Task(id = "2", title = "Urgent priority", priority = "urgent", estimatedMinutes = 45)
        val task3 = Task(id = "3", title = "High priority", priority = "high", estimatedMinutes = 60)

        val list = listOf(task1, task2, task3)
        val sorted = list.sortedByDescending { it.priorityWeight }

        assertEquals("Urgent priority", sorted[0].title)
        assertEquals("High priority", sorted[1].title)
        assertEquals("Low priority", sorted[2].title)
    }
}
