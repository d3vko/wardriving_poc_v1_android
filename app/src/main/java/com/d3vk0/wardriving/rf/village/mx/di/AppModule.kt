package com.d3vk0.wardriving.rf.village.mx.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import androidx.room.Room
import com.d3vk0.wardriving.rf.village.mx.BuildConfig
import com.d3vk0.wardriving.rf.village.mx.core.ble.BleScanner
import com.d3vk0.wardriving.rf.village.mx.core.csv.CsvExportManager
import com.d3vk0.wardriving.rf.village.mx.core.domain.ApiConfig
import com.d3vk0.wardriving.rf.village.mx.core.local.WardrivingDao
import com.d3vk0.wardriving.rf.village.mx.core.local.WardrivingDatabase
import com.d3vk0.wardriving.rf.village.mx.core.location.LocationTracker
import com.d3vk0.wardriving.rf.village.mx.core.remote.ApiClientFactory
import com.d3vk0.wardriving.rf.village.mx.core.remote.WardrivingApiService
import com.d3vk0.wardriving.rf.village.mx.core.repository.AuthRepository
import com.d3vk0.wardriving.rf.village.mx.core.repository.UploadRepository
import com.d3vk0.wardriving.rf.village.mx.core.repository.WardrivingRepository
import com.d3vk0.wardriving.rf.village.mx.core.security.AuthTokenStore
import com.d3vk0.wardriving.rf.village.mx.core.settings.AppSettingsStore
import com.d3vk0.wardriving.rf.village.mx.core.telephony.TelephonyScanner
import com.d3vk0.wardriving.rf.village.mx.core.wifi.WifiScanner
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideApiConfig(): ApiConfig = ApiConfig(
        baseUrl = BuildConfig.API_BASE_URL,
        loginPath = BuildConfig.API_LOGIN_PATH,
        registerPath = BuildConfig.API_REGISTER_PATH,
        passwordRecoveryPath = BuildConfig.API_PASSWORD_RECOVERY_PATH,
        uploadPath = BuildConfig.API_UPLOAD_PATH,
        wifiBleUploadType = BuildConfig.API_UPLOAD_TYPE_WIFI_BLE,
        lteUploadType = BuildConfig.API_UPLOAD_TYPE_LTE,
    )

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WardrivingDatabase =
        Room.databaseBuilder(context, WardrivingDatabase::class.java, "wardriving.db")
            .addMigrations(WardrivingDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideDao(database: WardrivingDatabase): WardrivingDao = database.wardrivingDao()

    @Provides
    @Singleton
    fun provideTokenStore(@ApplicationContext context: Context) = AuthTokenStore(context)

    @Provides
    @Singleton
    fun provideApi(config: ApiConfig, tokenStore: AuthTokenStore): WardrivingApiService =
        ApiClientFactory { tokenStore.getToken() }.create(config)

    @Provides
    @Singleton
    fun provideSettings(@ApplicationContext context: Context) = AppSettingsStore(context)

    @Provides
    @Singleton
    fun provideWardrivingRepository(dao: WardrivingDao) = WardrivingRepository(dao)

    @Provides
    @Singleton
    fun provideAuthRepository(api: WardrivingApiService, config: ApiConfig, tokenStore: AuthTokenStore) =
        AuthRepository(api, config, tokenStore)

    @Provides
    @Singleton
    fun provideUploadRepository(api: WardrivingApiService, config: ApiConfig, dao: WardrivingDao) =
        UploadRepository(api, config, dao)

    @Provides
    @Singleton
    fun provideCsvExportManager(@ApplicationContext context: Context, dao: WardrivingDao) =
        CsvExportManager(context, dao)

    @Provides
    @Singleton
    fun provideLocationTracker(@ApplicationContext context: Context) =
        LocationTracker(LocationServices.getFusedLocationProviderClient(context))

    @Provides
    @Singleton
    fun provideWifiScanner(@ApplicationContext context: Context): WifiScanner {
        val manager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return WifiScanner(manager)
    }

    @Provides
    @Singleton
    fun provideBleScanner(@ApplicationContext context: Context): BleScanner {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        return BleScanner(bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter())
    }

    @Provides
    @Singleton
    fun provideTelephonyScanner(@ApplicationContext context: Context): TelephonyScanner {
        val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return TelephonyScanner(manager)
    }
}
