package com.aiim.android

import android.app.Application
import com.aiim.chat.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * 应用入口类，使用Hilt进行依赖注入
 * 初始化Timber日志库
 */
@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化Timber日志库，仅在debug模式启用
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("AIIM Chat应用启动")
    }
}