package com.vivokey.lib_nfc.domain

import android.nfc.Tag
import android.nfc.tech.Ndef
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer

enum class IsodepConnectionStatus {
    CONNECTED,
    DISCONNECTED
}

interface ApexController {

    val connectionStatus: StateFlow<IsodepConnectionStatus>
    fun connect(tag: Tag)
    fun close()
    fun issueApdu(cla: Byte, instruction: Byte, p1: Byte = 0, p2: Byte = 0, data: ByteBuffer.() -> Unit = {}): ByteBuffer
    fun transceive(data: ByteArray): ByteArray
    fun runSafe(tag: Tag, action: () -> Unit)
    fun getATR(): ByteArray?
    fun selectISD(): ByteArray?
}