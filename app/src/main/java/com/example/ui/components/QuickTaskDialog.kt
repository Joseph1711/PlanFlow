package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SubTask
import com.example.data.model.Task
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import com.example.ui.theme.PriorityUrgent
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickTaskDialog(
    initialTask: Task?,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var priority by remember { mutableStateOf(initialTask?.priority ?: "medium") }
    var category by remember { mutableStateOf(initialTask?.category ?: "Trabajo") }
    var dueDate by remember { mutableStateOf(initialTask?.dueDate ?: LocalDate.now().toString()) }

    val defaultInitialTime = remember {
        val now = LocalTime.now()
        String.format(Locale.US, "%02d:00", (now.hour + 1) % 24)
    }
    var dueTime by remember { mutableStateOf(initialTask?.dueTime?.ifBlank { defaultInitialTime } ?: defaultInitialTime) }
    var estimatedMinutes by remember { mutableIntStateOf(initialTask?.estimatedMinutes ?: 30) }
    var reminderMinutesBefore by remember { mutableIntStateOf(initialTask?.reminderMinutesBefore ?: 15) }
    var recurrenceRule by remember { mutableStateOf(initialTask?.recurrenceRule ?: "none") }

    val datePickerDialog = remember(context, dueDate) {
        val parsedDate = try { LocalDate.parse(dueDate) } catch (e: Exception) { LocalDate.now() }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dueDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            parsedDate.year,
            parsedDate.monthValue - 1,
            parsedDate.dayOfMonth
        )
    }

    val timePickerDialog = remember(context, dueTime) {
        val parsedTime = try {
            val parts = dueTime.split(":")
            LocalTime.of(parts[0].toInt().coerceIn(0, 23), parts.getOrNull(1)?.toInt()?.coerceIn(0, 59) ?: 0)
        } catch (e: Exception) {
            LocalTime.of(12, 0)
        }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                dueTime = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
            },
            parsedTime.hour,
            parsedTime.minute,
            true
        )
    }

    val subtasks = remember {
        mutableStateListOf<SubTask>().apply {
            if (initialTask != null) addAll(initialTask.subtasks)
        }
    }
    var newSubtaskTitle by remember { mutableStateOf("") }

    val priorities = listOf("urgent" to "Urgente", "high" to "Alta", "medium" to "Media", "low" to "Baja")
    val categories = listOf("Trabajo", "Personal", "Estudio", "Salud", "Finanzas", "Hogar")
    val durations = listOf(15, 30, 45, 60, 90, 120)
    val reminders = listOf(5 to "5 min", 15 to "15 min", 30 to "30 min", 60 to "1 hora")
    val recurrences = listOf("none" to "Una vez", "daily" to "Diaria", "weekly" to "Semanal", "monthly" to "Mensual")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("task_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialTask == null) "Nueva Tarea" else "Editar Tarea",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre de la tarea *") },
                    placeholder = { Text("Ej. Terminar informe trimestral") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Description input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (opcional)") },
                    placeholder = { Text("Añade detalles, notas o instrucciones...") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_desc_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Priority Selector
                Text("Prioridad", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    priorities.forEach { (key, label) ->
                        val isSelected = priority == key
                        val color = when (key) {
                            "urgent" -> PriorityUrgent
                            "high" -> PriorityHigh
                            "medium" -> PriorityMedium
                            else -> PriorityLow
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) color else color.copy(alpha = 0.12f),
                            contentColor = if (isSelected) Color.White else color,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { priority = key }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selector
                Text("Categoría", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date and Time Row
                Text("Fecha y Hora Límite", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Fecha (AAAA-MM-DD)") },
                        leadingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Elegir fecha", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = dueTime,
                        onValueChange = { dueTime = it },
                        label = { Text("Hora (HH:mm)") },
                        leadingIcon = {
                            IconButton(onClick = { timePickerDialog.show() }) {
                                Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Elegir hora", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Quick date helpers
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { dueDate = LocalDate.now().toString() }) {
                        Text("Hoy", fontSize = 11.sp)
                    }
                    TextButton(onClick = { dueDate = LocalDate.now().plusDays(1).toString() }) {
                        Text("Mañana", fontSize = 11.sp)
                    }
                    TextButton(onClick = { dueDate = LocalDate.now().plusDays(7).toString() }) {
                        Text("En 7 días", fontSize = 11.sp)
                    }
                    TextButton(onClick = { datePickerDialog.show() }) {
                        Text("📅 Calendario", fontSize = 11.sp)
                    }
                }

                // Quick time chips
                Text("Horas frecuentes:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("09:00", "12:00", "15:00", "18:00", "21:00").forEach { presetTime ->
                        val isSelected = dueTime == presetTime
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { dueTime = presetTime }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
                                Text(text = presetTime, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.clickable { timePickerDialog.show() }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                            Text(text = "🕒 Reloj", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Estimated Duration Selector
                Text("Tiempo Estimado", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    durations.forEach { dur ->
                        val isSelected = estimatedMinutes == dur
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { estimatedMinutes = dur }
                        ) {
                            Text(
                                text = "$dur min",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reminder selector
                Text("Aviso / Recordatorio", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    reminders.forEach { (mins, label) ->
                        val isSelected = reminderMinutesBefore == mins
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { reminderMinutesBefore = mins }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(text = label, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Subtasks checklist manager
                Text("Subtareas / Pasos", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                subtasks.forEachIndexed { index, subtask ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${index + 1}.", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(subtask.title, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { subtasks.removeAt(index) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar subtarea", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Add subtask field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSubtaskTitle,
                        onValueChange = { newSubtaskTitle = it },
                        placeholder = { Text("Añadir paso...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newSubtaskTitle.isNotBlank()) {
                                subtasks.add(SubTask(id = UUID.randomUUID().toString(), title = newSubtaskTitle.trim(), isCompleted = false))
                                newSubtaskTitle = ""
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recurrence
                Text("Repetición Recurrente", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    recurrences.forEach { (rule, label) ->
                        val isSelected = recurrenceRule == rule
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { recurrenceRule = rule }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(text = label, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val normalizedTime = try {
                                    val trimmed = dueTime.trim().uppercase()
                                    val isPm = trimmed.contains("PM") || trimmed.contains("P.M.")
                                    val isAm = trimmed.contains("AM") || trimmed.contains("A.M.")
                                    val clean = trimmed.replace("PM", "").replace("AM", "").replace("P.M.", "").replace("A.M.", "").trim()
                                    val parts = clean.split(":")
                                    var hour = parts[0].trim().toIntOrNull() ?: 12
                                    val minute = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 0 else 0
                                    if (isPm && hour < 12) hour += 12
                                    if (isAm && hour == 12) hour = 0
                                    String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
                                } catch (e: Exception) {
                                    dueTime.trim()
                                }

                                val taskId = initialTask?.id?.ifBlank { UUID.randomUUID().toString() } ?: UUID.randomUUID().toString()
                                val newTask = (initialTask ?: Task(id = taskId)).copy(
                                    id = taskId,
                                    title = title.trim(),
                                    description = description.trim(),
                                    priority = priority,
                                    category = category,
                                    dueDate = dueDate.trim(),
                                    dueTime = normalizedTime,
                                    estimatedMinutes = estimatedMinutes,
                                    reminderMinutesBefore = reminderMinutesBefore,
                                    recurrenceRule = recurrenceRule,
                                    isRecurring = recurrenceRule != "none",
                                    subtasks = subtasks.toList()
                                )
                                onSave(newTask)
                            }
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.testTag("save_task_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (initialTask == null) "Crear Tarea" else "Guardar Cambios")
                    }
                }
            }
        }
    }
}
