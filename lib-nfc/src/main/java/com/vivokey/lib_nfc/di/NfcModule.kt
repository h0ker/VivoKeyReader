package com.vivokey.lib_nfc.di

import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcA
import com.vivokey.lib_nfc.data.IsodepControllerImpl
import com.vivokey.lib_nfc.data.NfcAControllerImpl
import com.vivokey.lib_nfc.domain.NfcController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NfcAController

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsodepController

@Module
@InstallIn(SingletonComponent::class)
abstract class NfcModule {

    companion object {
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
    }

    @Binds
    @Singleton
    @NfcAController
    abstract fun bindNfcAController(nfcAController: NfcAControllerImpl): NfcController

    @Binds
    @Singleton
    @IsodepController
    abstract fun bindIsodepController(isodepController: IsodepControllerImpl): NfcController

    class NfcControllerFactory @Inject constructor(
        @NfcAController private val nfcAControllerImpl: Provider<NfcController>,
        @IsodepController private val isodepControllerImpl: Provider<NfcController>
    ) {
        fun getController(tag: Tag): NfcController {
            return if(tag.techList.first() == NfcA::class.java.name) {
                nfcAControllerImpl.get()
            } else {
                isodepControllerImpl.get()
            }
        }
    }
}