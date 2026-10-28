package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.PlanFlowRepository
import com.example.ui.AuthScreen
import com.example.ui.MainScreen
import com.example.ui.PlanFlowViewModel
import com.example.ui.PlanFlowViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent()
                }
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val authPrefs = remember { context.getSharedPreferences("planflow_auth_pref", android.content.Context.MODE_PRIVATE) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isGuestMode by remember { mutableStateOf(authPrefs.getBoolean("is_guest_mode", false)) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                authPrefs.edit().putBoolean("is_guest_mode", false).apply()
                isGuestMode = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null && !isGuestMode) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
                authPrefs.edit().putBoolean("is_guest_mode", false).apply()
                isGuestMode = false
            },
            onGuestMode = {
                authPrefs.edit().putBoolean("is_guest_mode", true).apply()
                isGuestMode = true
            }
        )
    } else {
        val repository = remember { PlanFlowRepository(context) }
        val viewModel: PlanFlowViewModel = viewModel(
            factory = PlanFlowViewModelFactory(repository, context)
        )
        MainScreen(viewModel = viewModel)
    }
}
