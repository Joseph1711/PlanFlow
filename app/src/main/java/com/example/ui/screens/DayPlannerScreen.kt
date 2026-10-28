package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.example.data.model.ScheduleBlock
import com.example.ui.AppNavTab
import com.example.ui.PlanFlowViewModel
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import com.example.ui.theme.PriorityUrgent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DayPlannerScreen(
    viewModel: PlanFlowViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.selectTab(AppNavTab.DASHBOARD)
    }

    val tasks by viewModel.tasks.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val scheduledBlocks by viewModel.scheduledBlocks.collectAsState()
    val isAutoPlanning by viewModel.isAutoPlanning.collectAsState()

    var showConfigHours by remember { mutableStateOf(false) }
    var workStart by remember { mutableIntStateOf(preferences.workStartHour) }
    var workEnd by remember { mutableIntStateOf(preferences.workEndHour) }
    var breakMins by remember { mutableIntStateOf(preferences.breakDurationMinutes) }

    val todayFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES"))
    val formattedDate = LocalDate.now().format(todayFormatter).replaceFirstChar { it.uppercase() }

    val pendingTasksCount = tasks.count { it.status != "completed" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("day_planner_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Title & Explainer Banner
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Planificación Automática",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Agenda del día basada en reglas y prioridades",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showConfigHours = !showConfigHours }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Configurar horarios",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 2. Working hours configuration drawer
        item {
            AnimatedVisibility(visible = showConfigHours) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Ajustar Disponibilidad Diaria",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Work start hour
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hora de inicio laboral:", fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { if (workStart > 6) workStart-- },
                                    modifier = Modifier.size(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("-") }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(String.format("%02d:00", workStart), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = { if (workStart < workEnd - 1) workStart++ },
                                    modifier = Modifier.size(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("+") }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Work end hour
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hora de fin laboral:", fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { if (workEnd > workStart + 1) workEnd-- },
                                    modifier = Modifier.size(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("-") }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(String.format("%02d:00", workEnd), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = { if (workEnd < 23) workEnd++ },
                                    modifier = Modifier.size(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("+") }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Break duration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pausa entre bloques:", fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { if (breakMins > 5) breakMins -= 5 },
                                    modifier = Modifier.size(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("-") }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("$breakMins min", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = { if (breakMins < 45) breakMins += 5 },
                                    modifier = Modifier.size(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("+") }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                viewModel.updatePreferences(
                                    preferences.copy(
                                        workStartHour = workStart,
                                        workEndHour = workEnd,
                                        breakDurationMinutes = breakMins
                                    )
                                )
                                showConfigHours = false
                                viewModel.triggerAutoPlanDay()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Guardar y Reorganizar Agenda")
                        }
                    }
                }
            }
        }

        // 3. Algorithm Rules Highlight Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Reglas",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reglas de optimización automáticas (Sin IA)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Prioridad Urgente > Alta > Media > Baja\n" +
                                "• Asignación sin solapamientos entre las ${preferences.workStartHour}:00 y las ${preferences.workEndHour}:00\n" +
                                "• Bloques de descanso de ${preferences.breakDurationMinutes} min tras 90 min de trabajo continuo\n" +
                                "• Avisos automáticos 10 minutos antes de comenzar cada bloque\n" +
                                "• Reorganización instantánea al cambiar disponibilidad",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.triggerAutoPlanDay() },
                        enabled = !isAutoPlanning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_planner_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isAutoPlanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Organizando día...")
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Planificar", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generar / Reorganizar Agenda Diaria")
                        }
                    }
                }
            }
        }

        // 4. Timeline Schedule Blocks Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Línea de Tiempo del Día (${scheduledBlocks.size} bloques)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (scheduledBlocks.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.triggerAutoPlanDay() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Replay, contentDescription = "Recalcular", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reorganizar", fontSize = 11.sp)
                    }
                }
            }
        }

        // Empty state if not generated yet
        if (scheduledBlocks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Calendario",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Agenda diaria no generada",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tienes $pendingTasksCount tareas pendientes. Pulsa 'Generar Agenda Diaria' para organizarlas automáticamente en horarios ideales.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(scheduledBlocks) { block ->
                val priorityColor = when (block.task.priority) {
                    "urgent" -> PriorityUrgent
                    "high" -> PriorityHigh
                    "medium" -> PriorityMedium
                    else -> PriorityLow
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time indicator column
                    Column(
                        modifier = Modifier.width(64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = block.startTime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(28.dp)
                                .background(
                                    if (block.isBreak) MaterialTheme.colorScheme.surfaceVariant
                                    else priorityColor
                                )
                        )
                        Text(
                            text = block.endTime,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Block Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (block.isBreak) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (block.isBreak) 0.dp else 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (block.isBreak) {
                                        Icon(
                                            imageVector = Icons.Default.Coffee,
                                            contentDescription = "Pausa",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    } else {
                                        Surface(
                                            shape = CircleShape,
                                            color = priorityColor,
                                            modifier = Modifier.size(8.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    Text(
                                        text = block.task.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (block.isBreak) "Pausa activa de ${block.durationMinutes} minutos para recargar energía"
                                    else "${block.task.category} • ${block.durationMinutes} minutos estimados",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!block.isBreak) {
                                IconButton(
                                    onClick = { viewModel.toggleTaskCompletion(block.task) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (block.task.status == "completed") Icons.Default.CheckCircle else Icons.Default.Check,
                                        contentDescription = "Completar",
                                        tint = if (block.task.status == "completed") MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
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
