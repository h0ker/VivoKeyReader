package com.hoker.lib_utils.domain

enum class ConnectionStatus(val textStatus: String) {
    CONNECTED("Connected"),
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting")
}