package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.example.ui.AppNavTab
import com.example.ui.PlanFlowViewModel
import com.example.ui.components.TaskCard
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityUrgent
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: PlanFlowViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.selectTab(AppNavTab.DASHBOARD)
    }

    val tasks by viewModel.tasks.collectAsState()

    var selectedView by remember { mutableIntStateOf(0) } // 0: Diaria, 1: Semanal, 2: Mensual
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }

    val selectedDateStr = selectedDate.toString()
    val tasksForSelectedDate = tasks.filter { it.dueDate == selectedDateStr }

    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES"))

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar_screen")
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Calendario y Agenda",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // View Tabs (Diaria, Semanal, Mensual)
            TabRow(
                selectedTabIndex = selectedView,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedView == 0,
                    onClick = { selectedView = 0 },
                    text = { Text("Diaria", fontWeight = if (selectedView == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedView == 1,
                    onClick = { selectedView = 1 },
                    text = { Text("Semanal", fontWeight = if (selectedView == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedView == 2,
                    onClick = { selectedView = 2 },
                    text = { Text("Mensual", fontWeight = if (selectedView == 2) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // Subviews
        when (selectedView) {
            0 -> {
                // DAILY VIEW
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedDate = selectedDate.minusDays(1) }) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Día anterior")
                    }
                    Text(
                        text = selectedDate.format(dayFormatter).replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { selectedDate = selectedDate.plusDays(1) }) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Día siguiente")
                    }
                }
            }

            1 -> {
                // WEEKLY VIEW STRIP
                val startOfWeek = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedDate = selectedDate.minusWeeks(1) }) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Semana anterior")
                    }
                    Text(
                        text = "Semana del ${startOfWeek.dayOfMonth} al ${startOfWeek.plusDays(6).dayOfMonth} ${startOfWeek.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale("es", "ES"))}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    IconButton(onClick = { selectedDate = selectedDate.plusWeeks(1) }) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Semana siguiente")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Week days strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val daysOfWeekLabels = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
                    for (i in 0..6) {
                        val day = startOfWeek.plusDays(i.toLong())
                        val isSelected = day == selectedDate
                        val dayTasks = tasks.count { it.dueDate == day.toString() }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedDate = day }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(daysOfWeekLabels[i], fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(day.dayOfMonth.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (dayTasks > 0) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(5.dp)
                                    ) {}
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // MONTHLY CALENDAR GRID
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
                    }
                    Text(
                        text = currentYearMonth.format(monthFormatter).replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
                    }
                }

                // Month days grid (simple representation)
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        listOf("L", "M", "X", "J", "V", "S", "D").forEach { d ->
                            Text(d, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    val daysInMonth = currentYearMonth.lengthOfMonth()
                    val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value // 1..7
                    val totalSlots = ((daysInMonth + firstDayOfWeek - 1 + 6) / 7) * 7

                    for (week in 0 until (totalSlots / 7)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            for (col in 1..7) {
                                val dayNum = week * 7 + col - firstDayOfWeek + 1
                                if (dayNum in 1..daysInMonth) {
                                    val date = currentYearMonth.atDay(dayNum)
                                    val isSelected = date == selectedDate
                                    val dateTasks = tasks.count { it.dueDate == date.toString() }

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .clickable { selectedDate = date }
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = dayNum.toString(),
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (dateTasks > 0) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(3.dp)
                                                ) {}
                                            }
                                        }
                                    }
                                } else {
                                    Box(modifier = Modifier.size(34.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tasks for selected day Header & Move tools
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tareas de este día (${tasksForSelectedDate.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            TextButton(onClick = { viewModel.openCreateTaskDialog() }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir tarea", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Crear en esta fecha", fontSize = 11.sp)
            }
        }

        // Tasks list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (tasksForSelectedDate.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Sin tareas programadas para este día",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(tasksForSelectedDate, key = { index, task -> if (task.id.isNotBlank()) task.id else "cal_${index}_${task.title}" }) { _, task ->
                    Column {
                        TaskCard(
                            task = task,
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                            onToggleSubtask = { subtaskId -> viewModel.toggleSubtask(task, subtaskId) },
                            onEdit = { viewModel.openEditTaskDialog(task) },
                            onDelete = { viewModel.deleteTask(task.id) }
                        )

                        // Quick date mover buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                viewModel.saveTask(task.copy(dueDate = LocalDate.now().toString()))
                            }) {
                                Text("Mover a Hoy", fontSize = 11.sp)
                            }
                            TextButton(onClick = {
                                viewModel.saveTask(task.copy(dueDate = LocalDate.now().plusDays(1).toString()))
                            }) {
                                Text("Mover a Mañana", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}
