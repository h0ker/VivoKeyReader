package com.vivokey.lib_nfc.di

import android.content.Context
import android.nfc.NfcAdapter
import com.vivokey.lib_nfc.data.ApexControllerImpl
import com.vivokey.lib_nfc.domain.ApexController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NfcModule {

    @Singleton
    @Provides
    fun providesNfcAdapter(@ApplicationContext context: Context): NfcAdapter {
        return NfcAdapter.getDefaultAdapter(context)
    }

    @Singleton
    @Provides
    fun providesApplicationIOCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @Singleton
    @Provides
    fun providesApexController(): ApexController {
        return ApexControllerImpl(CoroutineScope(SupervisorJob() + Dispatchers.IO))
    }
}