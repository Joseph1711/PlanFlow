package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PlanFlowRepository
import com.example.ui.PlanFlowViewModel
import com.example.ui.screens.FocusTimerScreen
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @org.junit.Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
      com.google.firebase.FirebaseApp.initializeApp(
        context,
        com.google.firebase.FirebaseOptions.Builder()
          .setApplicationId("1:1234567890:android:abcdef")
          .setProjectId("planflow-test")
          .setApiKey("fakeApiKey1234567890abcdef")
          .build()
      )
    }
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PlanFlow", appName)
  }

  @Test
  fun `render FocusTimerScreen without crashing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = PlanFlowRepository(context)
    val viewModel = PlanFlowViewModel(repository, context)

    composeTestRule.setContent {
      FocusTimerScreen(viewModel = viewModel)
    }
  }

  @Test
  fun `tasks persist across simulated app restart`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository1 = PlanFlowRepository(context)

    val sampleTask = com.example.data.model.Task(
      title = "Comprar insumos",
      priority = "high",
      category = "Personal"
    )
    val taskId = repository1.createTask(sampleTask)

    // Simulate closing and reopening app by creating a new repository instance
    val repository2 = PlanFlowRepository(context)
    val tasks = repository2.observeTasks("guest_local")
    val firstList = tasks.first()

    org.junit.Assert.assertTrue(firstList.any { it.title == "Comprar insumos" })
  }

  @Test
  fun `autoPlanDay runs without crashing and respects user task hour`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = PlanFlowRepository(context)

    val fixedTask = com.example.data.model.Task(
      id = "task_fixed_1",
      title = "Reunión importante",
      dueTime = "15:30",
      estimatedMinutes = 30,
      priority = "urgent"
    )
    val flexibleTask = com.example.data.model.Task(
      id = "task_flex_1",
      title = "Revisar correos",
      dueTime = "",
      estimatedMinutes = 45,
      priority = "medium"
    )

    val prefs = com.example.data.model.UserPreferences(
      workStartHour = 8,
      workEndHour = 18,
      breakDurationMinutes = 15
    )

    // Call autoPlanDay in guest / unauthenticated state - MUST NOT CRASH
    val blocks = repository.autoPlanDay(listOf(fixedTask, flexibleTask), prefs)

    org.junit.Assert.assertNotNull(blocks)
    org.junit.Assert.assertTrue(blocks.isNotEmpty())

    // The fixed task MUST start at the user's selected hour (15:30)
    val fixedBlock = blocks.find { it.task.id == "task_fixed_1" }
    org.junit.Assert.assertNotNull(fixedBlock)
    org.junit.Assert.assertEquals("15:30", fixedBlock?.startTime)
  }

  @Test
  fun `SchoolModeManager resolves conflicts by prioritizing urgent tasks`() {
    val taskMedium = com.example.data.model.Task(
      id = "task_med",
      title = "Repasar Biología",
      scheduledStartTime = "12:00",
      estimatedMinutes = 60,
      priority = "medium",
      isSchoolTask = true,
      allowSimultaneous = false
    )
    val taskUrgent = com.example.data.model.Task(
      id = "task_urg",
      title = "Examen de Matemáticas",
      scheduledStartTime = "12:00",
      estimatedMinutes = 60,
      priority = "urgent",
      isSchoolTask = true,
      allowSimultaneous = false
    )

    val result = com.example.school.SchoolModeManager.organizeSchoolTasks(
      allTasks = listOf(taskMedium),
      taskToUpsert = taskUrgent
    )

    val organized = result.organizedTasks
    val mathTask = organized.first { it.id == "task_urg" }
    val bioTask = organized.first { it.id == "task_med" }

    // Urgent task must maintain the 12:00 slot
    org.junit.Assert.assertEquals("12:00", mathTask.scheduledStartTime)
    // Medium task must have been shifted forward to 13:00 to avoid overlap
    org.junit.Assert.assertEquals("13:00", bioTask.scheduledStartTime)
  }

  @Test
  fun `SchoolModeManager allows simultaneous activities without conflict`() {
    val task1 = com.example.data.model.Task(
      id = "task_sim_1",
      title = "Escuchar audiolibro de Historia",
      scheduledStartTime = "14:00",
      estimatedMinutes = 45,
      priority = "medium",
      isSchoolTask = true,
      allowSimultaneous = true
    )
    val task2 = com.example.data.model.Task(
      id = "task_sim_2",
      title = "Dibujar boceto de Arte",
      scheduledStartTime = "14:00",
      estimatedMinutes = 45,
      priority = "medium",
      isSchoolTask = true,
      allowSimultaneous = true
    )

    val result = com.example.school.SchoolModeManager.organizeSchoolTasks(
      allTasks = listOf(task1),
      taskToUpsert = task2
    )

    val organized = result.organizedTasks
    org.junit.Assert.assertEquals(2, organized.size)
    // Both simultaneous tasks remain at 14:00
    org.junit.Assert.assertEquals("14:00", organized[0].scheduledStartTime)
    org.junit.Assert.assertEquals("14:00", organized[1].scheduledStartTime)
  }

  @Test
  fun `SchoolModeManager extends duration and adjusts subsequent schedule`() {
    val task1 = com.example.data.model.Task(
      id = "task_t1",
      title = "Resolver Guía Física",
      scheduledStartTime = "10:00",
      estimatedMinutes = 30,
      priority = "high",
      isSchoolTask = true,
      allowSimultaneous = false
    )
    val task2 = com.example.data.model.Task(
      id = "task_t2",
      title = "Taller de Química",
      scheduledStartTime = "10:30",
      estimatedMinutes = 60,
      priority = "medium",
      isSchoolTask = true,
      allowSimultaneous = false
    )

    // Extend task1 by +30 minutes (now 60 min, ending at 11:00)
    val result = com.example.school.SchoolModeManager.extendTaskDuration(
      targetTask = task1,
      additionalMinutes = 30,
      allTasks = listOf(task1, task2)
    )

    val organized = result.organizedTasks
    val extendedTask1 = organized.first { it.id == "task_t1" }
    val shiftedTask2 = organized.first { it.id == "task_t2" }

    org.junit.Assert.assertEquals(60, extendedTask1.estimatedMinutes)
    org.junit.Assert.assertEquals("10:00", extendedTask1.scheduledStartTime)
    // Subsequent task must be shifted forward to 11:00
    org.junit.Assert.assertEquals("11:00", shiftedTask2.scheduledStartTime)
  }

  @Test
  fun `render SchoolModeScreen without crashing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = PlanFlowRepository(context)
    val viewModel = PlanFlowViewModel(repository, context)

    composeTestRule.setContent {
      com.example.ui.screens.SchoolModeScreen(
        viewModel = viewModel,
        onArriveHome = { viewModel.exitSchoolMode() }
      )
    }
  }
}


