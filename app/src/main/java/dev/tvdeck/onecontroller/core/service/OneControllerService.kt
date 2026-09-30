package dev.tvdeck.onecontroller.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.tvdeck.onecontroller.MainActivity
import dev.tvdeck.onecontroller.R
import dev.tvdeck.onecontroller.core.transport.TransportManager

class OneControllerService : Service() {
    companion object {
        const val CHANNEL_ID = "onecontroller_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_SEND_KEY = "dev.tvdeck.onecontroller.ACTION_SEND_KEY"
        const val EXTRA_KEY_CODE = "extra_key_code"

        fun start(context: Context) {
            val intent = Intent(context, OneControllerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OneControllerService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SEND_KEY) {
            val keyCode = intent.getIntExtra(EXTRA_KEY_CODE, 0)
            if (keyCode > 0) {
                val tm = TransportManager.getInstance(this)
                tm.sendNavigationKey(keyCode)
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OneController TV Remote",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Quick remote controls for your Android TV"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun createKeyPendingIntent(keyCode: Int, requestCode: Int): PendingIntent {
            val intent = Intent(this, OneControllerService::class.java).apply {
                action = ACTION_SEND_KEY
                putExtra(EXTRA_KEY_CODE, keyCode)
            }
            return PendingIntent.getService(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val tm = TransportManager.getInstance(this)
        val devName = tm.activeDevice.value?.name ?: "Android TV"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OneController: $devName")
            .setContentText("Connected & Ready")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_previous, "Back", createKeyPendingIntent(4, 1))
            .addAction(android.R.drawable.ic_media_play, "Play/Pause", createKeyPendingIntent(85, 2))
            .addAction(android.R.drawable.ic_input_add, "Vol+", createKeyPendingIntent(24, 3))
            .addAction(android.R.drawable.ic_delete, "Vol-", createKeyPendingIntent(25, 4))
            .build()
    }
}
