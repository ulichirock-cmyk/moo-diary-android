package com.moodiary.app.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.moodiary.app.MainActivity
import com.moodiary.app.R

/**
 * Keeps [DiaryMcpServer] listening while the switch on 我的 is on. A foreground
 * service is the only way a socket survives the app leaving the screen; the
 * notification it requires doubles as the reminder that the diary is reachable.
 * Declared as `specialUse` — `dataSync` is capped at six hours a day on Android 15.
 */
class DiaryMcpService : Service() {

    private var server: DiaryMcpServer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val settings = AiSettings(this)
        val repository = RoomDiaryRepository.get(this)
        if (server == null) {
            val created = DiaryMcpServer(
                port = DiaryMcpServer.DEFAULT_PORT,
                token = { settings.mcpToken },
                entries = { repository.entries.value },
            )
            if (runCatching { created.start() }.isFailure) {
                stopSelf()
                return START_NOT_STICKY
            }
            server = created
        }
        startInForeground()
        return START_STICKY
    }

    override fun onDestroy() {
        server?.stop()
        server = null
        super.onDestroy()
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.mcp_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val address = DiaryMcpServer.localAddress()?.let { "$it:${DiaryMcpServer.DEFAULT_PORT}" }
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_claude_mark)
            .setContentTitle(getString(R.string.mcp_notification_title))
            .setContentText(address ?: getString(R.string.mcp_no_network))
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL = "mcp"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            context.startForegroundService(Intent(context, DiaryMcpService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DiaryMcpService::class.java))
        }
    }
}
