package com.d3vk0.wardriving.rf.village.mx

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.sentry.SentryEvent
import io.sentry.android.core.SentryAndroid

@HiltAndroidApp
class WardrivingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val dsn = BuildConfig.SENTRY_DSN
        if (dsn.isBlank()) return
        SentryAndroid.init(this) { options ->
            options.dsn = dsn
            options.isSendDefaultPii = false
            options.environment = "poc-v1-android"
            options.release = "wardriving-android@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
            options.setBeforeSend { event: SentryEvent, _ ->
                event.request?.headers?.remove("Authorization")
                event.request?.headers?.remove("authorization")
                event
            }
        }
    }
}
