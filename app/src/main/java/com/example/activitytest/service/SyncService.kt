package com.example.activitytest.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.activitytest.R
import kotlinx.coroutines.*

class SyncService : Service() {

    companion object {
        const val ACTION_SYNC_FINISHED =
            "com.example.activitytest.ACTION_SYNC_FINISHED"

        const val EXTRA_SYNC_MESSAGE =
            "extra_sync_message"
    }

    private val serviceJob = SupervisorJob()


    // 使用协程调度器，避免主线程被阻塞
    private val serviceScope =
        CoroutineScope(Dispatchers.IO + serviceJob)   //协程调度器

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        serviceScope.launch {
            try {
                // 模拟网络同步耗时
                delay(3000)  //kotlin协程，不会阻塞线程，thread属于线程，会阻塞

                val broadcastIntent = Intent(
                    ACTION_SYNC_FINISHED
                ).apply {
                    setPackage(packageName)
                    putExtra(
                        EXTRA_SYNC_MESSAGE,
                        getString(R.string.sync_finished)
                    )
                }

                sendBroadcast(broadcastIntent)
            } finally {
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY  // 如果服务被杀死，则不重启
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        serviceJob.cancel()  //协程取消
        super.onDestroy()
    }
}