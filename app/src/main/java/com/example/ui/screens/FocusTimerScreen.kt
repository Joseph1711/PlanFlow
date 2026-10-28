package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focus.FocusTimerManager
import com.example.ui.AppNavTab
import com.example.ui.PlanFlowViewModel

@Composable
fun FocusTimerScreen(
    viewModel: PlanFlowViewModel,
    modifier: Modifier = Modifier
) {
    // Intercept back key to return to Dashboard rather than closing the app
    BackHandler {
        viewModel.selectTab(AppNavTab.DASHBOARD)
    }

    val context = LocalContext.current
    val timerState by FocusTimerManager.timerState.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val pendingTasks = tasks.filter { it.status != "completed" }

    var selectedTaskId by remember { mutableStateOf<String?>(null) }
    val selectedTask = pendingTasks.find { it.id == selectedTaskId } ?: pendingTasks.firstOrNull()
    var showTaskDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        FocusTimerManager.syncWithClock()
    }

    DisposableEffect(Unit) {
        onDispose {
            // Keep running in background when leaving screen!
        }
    }

    val progress = if (timerState.totalSeconds > 0) {
        (1f - (timerState.secondsRemaining.toFloat() / timerState.totalSeconds)).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "focus_progress")

    val safeSecondsRemaining = timerState.secondsRemaining.coerceAtLeast(0)
    val minutes = safeSecondsRemaining / 60
    val seconds = safeSecondsRemaining % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("focus_timer_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Temporizador de Concentración",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Funciona en segundo plano al salir de la app",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Mode Selector Pills
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "work" to "Trabajo (25m)",
                "short_break" to "Pausa (5m)",
                "long_break" to "Descanso (15m)"
            ).forEach { (key, label) ->
                val isSelected = timerState.mode == key
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable {
                        val duration = when (key) {
                            "short_break" -> 5 * 60
                            "long_break" -> 15 * 60
                            else -> 25 * 60
                        }
                        FocusTimerManager.resetTimer(context)
                        FocusTimerManager.startTimer(
                            context = context,
                            mode = key,
                            totalSeconds = duration,
                            taskTitle = selectedTask?.title ?: "Sesión de Trabajo"
                        )
                    }
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Circular Countdown Display
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            CircularProgressIndicator(
                progress = { animatedProgress.coerceIn(0f, 1f) },
                modifier = Modifier.size(220.dp),
                strokeWidth = 12.dp,
                color = if (timerState.mode == "work") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeFormatted,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 2.sp
                )
                Text(
                    text = if (timerState.mode == "work") "TRABAJO PROFUNDO" else "TIEMPO DE DESCANSO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (timerState.mode == "work") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Timer Controls: Play/Pause, Reset, Skip
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { FocusTimerManager.resetTimer(context) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reiniciar")
            }

            Button(
                onClick = {
                    if (timerState.isRunning) {
                        FocusTimerManager.pauseTimer(context)
                    } else {
                        val duration = timerState.secondsRemaining.coerceAtLeast(1)
                        FocusTimerManager.startTimer(
                            context = context,
                            mode = timerState.mode,
                            totalSeconds = if (timerState.totalSeconds > 0) timerState.totalSeconds else duration,
                            taskTitle = selectedTask?.title ?: "Sesión de Trabajo"
                        )
                    }
                },
                modifier = Modifier
                    .size(68.dp)
                    .testTag("toggle_timer_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (timerState.isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (timerState.isRunning) "Pausar" else "Comenzar",
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = { FocusTimerManager.skipInterval(context) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Saltar intervalo")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Task selector dropdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Tarea asignada al temporizador:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTaskDropdown = true }
                    ) {
                        Text(
                            text = selectedTask?.title ?: "Seleccionar una tarea...",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showTaskDropdown,
                        onDismissRequest = { showTaskDropdown = false }
                    ) {
                        if (pendingTasks.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No hay tareas pendientes") },
                                onClick = { showTaskDropdown = false }
                            )
                        } else {
                            pendingTasks.forEach { task ->
                                DropdownMenuItem(
                                    text = { Text(task.title) },
                                    onClick = {
                                        selectedTaskId = task.id
                                        showTaskDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedTask != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { viewModel.toggleTaskCompletion(selectedTask) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Completar tarea", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Marcar tarea como completada")
                    }
                }
            }
        }

        // Sessions counter
        Text(
            text = "Sesiones completadas hoy: ${timerState.completedSessions}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
