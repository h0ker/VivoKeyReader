package com.vivokey.lib_nfc.domain

data class ApduResult(
    val responseType: ApduResultType,
    val error: Exception? = null
)
