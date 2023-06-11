package com.vivokey.lib_nfc.domain

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
abstract class NfcActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    @Inject lateinit var nfcAdapter: NfcAdapter

    private val flags =
        NfcAdapter.FLAG_READER_NFC_A //or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

    private val options = Bundle()

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableReaderMode(this, this, flags, options)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
    }
}