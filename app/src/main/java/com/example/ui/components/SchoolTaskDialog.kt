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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Task
import com.example.school.SchoolModeManager
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
fun SchoolTaskDialog(
    initialTask: Task?,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var priority by remember { mutableStateOf(initialTask?.priority ?: "high") }
    var schoolSubject by remember { mutableStateOf(initialTask?.schoolSubject?.ifBlank { "Matemáticas" } ?: "Matemáticas") }
    var schoolTaskType by remember { mutableStateOf(initialTask?.schoolTaskType ?: "deber") }
    var allowSimultaneous by remember { mutableStateOf(initialTask?.allowSimultaneous ?: false) }

    var dueDate by remember { mutableStateOf(initialTask?.dueDate?.ifBlank { LocalDate.now().toString() } ?: LocalDate.now().toString()) }

    val defaultStartTime = remember {
        val now = LocalTime.now()
        String.format(Locale.US, "%02d:00", now.hour.coerceIn(7, 21))
    }
    var dueTime by remember {
        mutableStateOf(
            initialTask?.scheduledStartTime?.ifBlank { initialTask.dueTime }?.ifBlank { defaultStartTime } ?: defaultStartTime
        )
    }
    var estimatedMinutes by remember { mutableIntStateOf(initialTask?.estimatedMinutes ?: 45) }

    val priorities = listOf("urgent" to "Urgente", "high" to "Alta", "medium" to "Media", "low" to "Baja")
    val durations = listOf(15, 30, 45, 60, 90, 120)

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
        val parsedTime = SchoolModeManager.parseTime(dueTime)
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("school_task_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (initialTask == null) "Nueva Tarea Escolar" else "Editar Actividad Escolar",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Modo Colegio",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Actividad o deber escolar *") },
                    placeholder = { Text("Ej. Ejercicios pág. 45-47, preparar exposición...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("school_task_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // School Subject Selector
                Text("Materia / Asignatura", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SchoolModeManager.SCHOOL_SUBJECTS.forEach { subj ->
                        val isSelected = schoolSubject == subj
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { schoolSubject = subj }
                        ) {
                            Text(
                                text = subj,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // School Task Type
                Text("Tipo de pendiente escolar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SchoolModeManager.SCHOOL_TASK_TYPES.take(3).forEach { (typeKey, typeLabel) ->
                        val isSelected = schoolTaskType == typeKey
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { schoolTaskType = typeKey }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(text = typeLabel, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Schedule: Date and Start Time
                Text("Horario programado en colegio", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Fecha") },
                        leadingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Fecha", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = dueTime,
                        onValueChange = { dueTime = it },
                        label = { Text("Hora inicio") },
                        leadingIcon = {
                            IconButton(onClick = { timePickerDialog.show() }) {
                                Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Hora", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Quick time chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("08:00", "10:00", "12:00", "14:00", "16:00").forEach { preset ->
                        val isSelected = dueTime == preset
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { dueTime = preset }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(preset, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Estimated Duration
                Text("Duración estimada", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    durations.take(5).forEach { dur ->
                        val isSelected = estimatedMinutes == dur
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { estimatedMinutes = dur }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                                Text("$dur min", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Priority
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
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) color else color.copy(alpha = 0.12f),
                            contentColor = if (isSelected) Color.White else color,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { priority = key }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SIMULTANEOUS ACTIVITY TOGGLE (Crucial Requirement)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Actividad simultánea",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Puede realizarse al mismo tiempo que otra actividad sin crear conflicto de horario.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = allowSimultaneous,
                            onCheckedChange = { allowSimultaneous = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Optional description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notas o detalles (opcional)") },
                    placeholder = { Text("Instrucciones del profesor, páginas, materiales...") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
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
                                val normalizedStart = SchoolModeManager.formatTime(SchoolModeManager.parseTime(dueTime))
                                val startLocal = SchoolModeManager.parseTime(normalizedStart)
                                val endLocal = startLocal.plusMinutes(estimatedMinutes.toLong())
                                val normalizedEnd = SchoolModeManager.formatTime(endLocal)

                                val taskId = initialTask?.id?.ifBlank { UUID.randomUUID().toString() } ?: UUID.randomUUID().toString()
                                val schoolTask = (initialTask ?: Task(id = taskId)).copy(
                                    id = taskId,
                                    title = title.trim(),
                                    description = description.trim(),
                                    priority = priority,
                                    category = "Colegio",
                                    schoolSubject = schoolSubject,
                                    schoolTaskType = schoolTaskType,
                                    isSchoolTask = true,
                                    allowSimultaneous = allowSimultaneous,
                                    dueDate = dueDate.trim(),
                                    dueTime = normalizedStart,
                                    scheduledStartTime = normalizedStart,
                                    scheduledEndTime = normalizedEnd,
                                    scheduledDate = dueDate.trim(),
                                    estimatedMinutes = estimatedMinutes
                                )
                                onSave(schoolTask)
                            }
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_school_task_button")
                    ) {
                        Text(if (initialTask == null) "Anotar en Horario Escolar" else "Guardar Cambios")
                    }
                }
            }
        }
    }
}
