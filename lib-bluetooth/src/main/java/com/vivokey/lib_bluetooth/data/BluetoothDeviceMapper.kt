package com.vivokey.lib_bluetooth.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.vivokey.lib_bluetooth.domain.models.Host

@SuppressLint("MissingPermission")
fun BluetoothDevice.toHost(): Host {
    return Host(
        name = name,
        address = address
    )
}