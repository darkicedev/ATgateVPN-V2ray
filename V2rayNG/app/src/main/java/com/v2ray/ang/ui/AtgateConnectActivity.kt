package com.v2ray.ang.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityAtgateConnectBinding
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.V2RayServiceManager
import com.v2ray.ang.viewmodel.MainViewModel

class AtgateConnectActivity : BaseActivity() {

    private val binding by lazy { ActivityAtgateConnectBinding.inflate(layoutInflater) }
    private val mainViewModel: MainViewModel by viewModels()

    private val requestVpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                startV2Ray()
            } else {
                binding.connectProgress.isVisible = false
            }
        }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                toast(R.string.toast_permission_denied_notification)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(binding.root)

        setupViewModel()
        requestNotificationPermissionIfNeeded()

        binding.buttonConnect.setOnClickListener {
            if (mainViewModel.isRunning.value == true) {
                binding.connectProgress.isVisible = true
                V2RayServiceManager.stopVService(this)
            } else {
                attemptStartVpn()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.reloadServerList()
        updateUiState(mainViewModel.isRunning.value == true)
    }

    private fun setupViewModel() {
        mainViewModel.isRunning.observe(this) { isRunning ->
            updateUiState(isRunning)
        }
        mainViewModel.startListenBroadcast()
        mainViewModel.initAssets(assets)
    }

    private fun updateUiState(isRunning: Boolean) {
        val hasProfile = !MmkvManager.getSelectServer().isNullOrEmpty()
        binding.connectProgress.isVisible = false

        binding.buttonConnect.text = if (isRunning) {
            getString(R.string.atgate_connect_button_disconnect)
        } else {
            getString(R.string.atgate_connect_button_connect)
        }
        binding.buttonConnect.isEnabled = isRunning || hasProfile

        binding.statusText.text = when {
            isRunning -> getString(R.string.atgate_connect_status_connected)
            hasProfile -> getString(R.string.atgate_connect_status_ready)
            else -> getString(R.string.atgate_connect_status_disconnected)
        }
    }

    private fun attemptStartVpn() {
        val guid = MmkvManager.getSelectServer()
        if (guid.isNullOrEmpty()) {
            toastError(getString(R.string.atgate_connect_error_no_profile))
            return
        }

        val prepareIntent = VpnService.prepare(this)
        binding.connectProgress.isVisible = true
        if (prepareIntent == null) {
            startV2Ray()
        } else {
            requestVpnPermission.launch(prepareIntent)
        }
    }

    private fun startV2Ray() {
        if (MmkvManager.getSelectServer().isNullOrEmpty()) {
            toastError(getString(R.string.atgate_connect_error_no_profile))
            binding.connectProgress.isVisible = false
            return
        }
        V2RayServiceManager.startVService(this)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
