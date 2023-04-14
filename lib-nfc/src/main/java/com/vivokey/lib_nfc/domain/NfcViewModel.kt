package com.vivokey.lib_nfc.domain

import android.nfc.Tag

interface NfcViewModel {
    fun onTagScan(tag: Tag)
}