package cn.lemwood

import android.app.Application
import cn.lemwood.crash.GlobalCrashHandler

class ServerSeeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化全局崩溃捕获
        GlobalCrashHandler(this)
    }
}
