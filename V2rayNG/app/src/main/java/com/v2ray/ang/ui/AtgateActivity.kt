package com.v2ray.ang.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.net.VpnService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.V2RayServiceManager
import com.v2ray.ang.ui.atgate.AtgateEvent
import com.v2ray.ang.ui.atgate.AtgateScreen
import com.v2ray.ang.ui.atgate.AtgateViewModel
import com.v2ray.ang.ui.theme.ATGateTheme
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.v2ray.ang.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest

class AtgateActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val atgateViewModel: AtgateViewModel by viewModels()

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                startV2Ray()
            } else {
                atgateViewModel.onConnectionCancelled()
                atgateViewModel.showSnackbar(R.string.toast_permission_denied, isError = true)
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                atgateViewModel.showSnackbar(R.string.toast_permission_denied_notification)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }

        mainViewModel.startListenBroadcast()
        mainViewModel.initAssets(assets)
        atgateViewModel.onServiceStateChanged(mainViewModel.isRunning.value == true)
        atgateViewModel.syncSelectedProfile()
        mainViewModel.reloadServerList()

        mainViewModel.isRunning.observe(this) { running ->
            atgateViewModel.onServiceStateChanged(running)
        }
        mainViewModel.updateListAction.observe(this) {
            atgateViewModel.syncSelectedProfile()
        }
        mainViewModel.updateTestResultAction.observe(this) {
            atgateViewModel.onPingUpdated(it)
        }

        requestNotificationPermissionIfNeeded()

        setContent {
            ATGateTheme {
                val uiState by atgateViewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(atgateViewModel) {
                    atgateViewModel.events.collectLatest { event ->
                        when (event) {
                            is AtgateEvent.Snackbar -> {
                                snackbarHostState.showSnackbar(
                                    message = event.message,
                                    withDismissAction = false,
                                    duration = if (event.isError) SnackbarDuration.Long else SnackbarDuration.Short
                                )
                            }

                            is AtgateEvent.ProfileImported -> {
                                mainViewModel.reloadServerList()
                                atgateViewModel.syncSelectedProfile()
                            }
                        }
                    }
                }

                AtgateScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onToggleConnection = { handleToggleConnection() },
                    onOpenImport = { atgateViewModel.setShowImportSheet(true) },
                    onDismissImport = { atgateViewModel.setShowImportSheet(false) },
                    onImport = { link -> atgateViewModel.importVless(link) },
                    onSpeedTest = { handleSpeedTest() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.reloadServerList()
        atgateViewModel.syncSelectedProfile()
    }

    private fun handleToggleConnection() {
        val currentState = atgateViewModel.uiState.value
        if (currentState.connectionInProgress) {
            return
        }
        if (currentState.isRunning) {
            atgateViewModel.onConnectionAttemptStarted()
            V2RayServiceManager.stopVService(this)
            return
        }

        val guid = MmkvManager.getSelectServer()
        if (guid.isNullOrEmpty()) {
            atgateViewModel.onConnectionAttemptFailed(R.string.atgate_connect_error_no_profile)
            return
        }

        val prepareIntent = VpnService.prepare(this)
        atgateViewModel.onConnectionAttemptStarted()
        if (prepareIntent == null) {
            startV2Ray()
        } else {
            vpnPermissionLauncher.launch(prepareIntent)
        }
    }

    private fun startV2Ray() {
        val guid = MmkvManager.getSelectServer()
        if (guid.isNullOrEmpty()) {
            atgateViewModel.onConnectionAttemptFailed(R.string.atgate_connect_error_no_profile)
            return
        }
        V2RayServiceManager.startVService(this)
    }

    private fun handleSpeedTest() {
        val guid = MmkvManager.getSelectServer()
        if (guid.isNullOrEmpty()) {
            atgateViewModel.showSnackbar(R.string.atgate_connect_error_no_profile, isError = true)
            return
        }
        atgateViewModel.showSnackbar(R.string.atgate_speedtest_started)
        mainViewModel.testCurrentServerRealPing()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }
}
