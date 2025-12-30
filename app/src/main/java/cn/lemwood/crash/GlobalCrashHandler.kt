package cn.lemwood.crash

import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class GlobalCrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()
            
            Log.e("GlobalCrashHandler", "Uncaught Exception: $stackTrace")
            
            val intent = Intent(context, CrashActivity::class.java).apply {
                putExtra("stack_trace", stackTrace)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
            
            // 结束当前进程
            Process.killProcess(Process.myPid())
            exitProcess(10)
        } catch (e: Exception) {
            Log.e("GlobalCrashHandler", "Error in crash handler: ${e.message}")
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
