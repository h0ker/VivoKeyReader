package com.vivokey.vivokeyreader.presentation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vivokey.lib_nfc.domain.NfcActivity
import com.vivokey.lib_nfc.domain.NfcViewModel
import com.vivokey.vivokeyreader.ui.theme.VivoKeyReaderTheme
import com.vivokey.vivokeyreader.domain.NavRoutes
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VivoKeyReaderActivity : NfcActivity() {

    @Inject lateinit var bluetoothAdapter: BluetoothAdapter

    private val viewModel: VivoKeyReaderViewModel by viewModels()

    private val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter.isEnabled

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { }

        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val canEnableBluetooth = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                result[Manifest.permission.BLUETOOTH_CONNECT] == true
            } else true

            if(canEnableBluetooth && !isBluetoothEnabled) {
                enableBluetoothLauncher.launch(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                )
            }
        }
        val permissionsToRequest = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_PRIVILEGED
            )
        }
        permissionLauncher.launch(permissionsToRequest)

        setContent {
            VivoKeyReaderTheme {

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.MainScreen.route
                ) {
                    composable(NavRoutes.MainScreen.route) {
                        VivoKeyReader(viewModel)
                    }
                }
            }
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        super.onTagDiscovered(tag)
        tag?.let {
            (viewModel as NfcViewModel).onTagScan(tag)
        }
    }

    override fun onBackPressed() {
        viewModel.onBack()
    }
}