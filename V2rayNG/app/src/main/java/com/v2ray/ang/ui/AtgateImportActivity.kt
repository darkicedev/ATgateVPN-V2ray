package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityAtgateImportBinding
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.fmt.VlessFmt
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AtgateImportActivity : BaseActivity() {

    private val binding by lazy { ActivityAtgateImportBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(binding.root)

        if (!MmkvManager.getSelectServer().isNullOrEmpty()) {
            startActivity(Intent(this, AtgateConnectActivity::class.java))
            finish()
            return
        }

        binding.buttonImport.setOnClickListener {
            val vlessLink = binding.inputVless.text?.toString()?.trim().orEmpty()
            if (vlessLink.isBlank()) {
                toastError(getString(R.string.atgate_import_error_empty))
                return@setOnClickListener
            }
            importVlessConfig(vlessLink)
        }
    }

    private fun importVlessConfig(link: String) {
        binding.buttonImport.isEnabled = false
        binding.progressIndicator.isVisible = true

        lifecycleScope.launch {
            val parsedConfig = withContext(Dispatchers.IO) {
                runCatching { VlessFmt.parse(link) }
            }

            binding.progressIndicator.isVisible = false
            binding.buttonImport.isEnabled = true

            val config = parsedConfig.getOrNull()
            if (parsedConfig.isFailure || config == null) {
                toastError(
                    if (parsedConfig.isFailure) {
                        getString(R.string.atgate_import_error_unknown)
                    } else {
                        getString(R.string.atgate_import_error_invalid)
                    }
                )
                return@launch
            }

            if (config.remarks.isBlank() || config.remarks.equals("none", ignoreCase = true)) {
                config.remarks = getString(R.string.atgate_profile_default_name)
            }

            withContext(Dispatchers.IO) {
                val guid = MmkvManager.encodeServerConfig("", config)
                MmkvManager.setSelectServer(guid)
            }

            toastSuccess(getString(R.string.atgate_import_success))
            startActivity(Intent(this@AtgateImportActivity, AtgateConnectActivity::class.java))
            finish()
        }
    }
}
