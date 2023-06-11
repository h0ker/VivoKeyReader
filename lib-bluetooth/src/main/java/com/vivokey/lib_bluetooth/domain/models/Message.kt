package com.vivokey.lib_bluetooth.domain.models

enum class MessageType {
    SENT,
    RECEIVED,
    ERROR
}
data class Message(
    val bytes: ByteArray? = null,
    val exception: Throwable? = null,
    val type: MessageType
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (!bytes.contentEquals(other.bytes)) return false
        return type == other.type
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}
