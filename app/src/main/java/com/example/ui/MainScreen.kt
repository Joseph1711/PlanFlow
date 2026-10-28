package com.example.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.sp
import com.example.notification.NotificationHelper
import com.example.ui.components.QuickTaskDialog
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DayPlannerScreen
import com.example.ui.screens.FocusTimerScreen
import com.example.ui.screens.SchoolModeScreen
import com.example.ui.screens.StatsAndGoalsScreen
import com.example.ui.screens.TaskListScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PlanFlowViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val showTaskDialog by viewModel.showTaskDialog.collectAsState()
    val taskToEdit by viewModel.taskToEdit.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val isSchoolModeActive by viewModel.isSchoolModeActive.collectAsState()

    if (isSchoolModeActive) {
        SchoolModeScreen(
            viewModel = viewModel,
            onArriveHome = { viewModel.exitSchoolMode() }
        )
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Request POST_NOTIFICATIONS permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationHelper.createNotificationChannels(context)
        }
    }

    LaunchedEffect(Unit) {
        NotificationHelper.createNotificationChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    val configuration = LocalConfiguration.current
    val isExpandedWidth = configuration.screenWidthDp >= 600

    val navItems = listOf(
        AppNavTab.DASHBOARD to Icons.Default.Dashboard,
        AppNavTab.TASKS to Icons.Default.Checklist,
        AppNavTab.PLANNER to Icons.Default.AutoAwesome,
        AppNavTab.CALENDAR to Icons.Default.CalendarMonth,
        AppNavTab.FOCUS to Icons.Default.HourglassBottom,
        AppNavTab.STATS to Icons.Default.BarChart
    )

    Row(modifier = Modifier.fillMaxSize()) {
        // Navigation Rail on wider screens (Tablets / DeX / Foldables)
        if (isExpandedWidth) {
            NavigationRail(
                modifier = Modifier.testTag("nav_rail"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                navItems.forEach { (tab, icon) ->
                    NavigationRailItem(
                        selected = currentTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        icon = { Icon(imageVector = icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_planflow_logo),
                                contentDescription = "Logo de PlanFlow",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "PlanFlow",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    },
                    actions = {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { viewModel.enterSchoolMode() }
                                .testTag("btn_top_enter_school_mode")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "Activar Modo Colegio",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Colegio",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isExpandedWidth) {
                    NavigationBar(
                        modifier = Modifier.testTag("bottom_nav"),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { (tab, icon) ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                icon = { Icon(imageVector = icon, contentDescription = tab.title) },
                                label = { Text(tab.title) }
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.openCreateTaskDialog() },
                    modifier = Modifier.testTag("fab_create_task"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Crear tarea")
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    AppNavTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                    AppNavTab.TASKS -> TaskListScreen(viewModel = viewModel)
                    AppNavTab.PLANNER -> DayPlannerScreen(viewModel = viewModel)
                    AppNavTab.CALENDAR -> CalendarScreen(viewModel = viewModel)
                    AppNavTab.FOCUS -> FocusTimerScreen(viewModel = viewModel)
                    AppNavTab.STATS -> StatsAndGoalsScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Modal dialog for task creation/editing
    if (showTaskDialog) {
        QuickTaskDialog(
            initialTask = taskToEdit,
            onDismiss = { viewModel.closeTaskDialog() },
            onSave = { task -> viewModel.saveTask(task) }
        )
    }
}
