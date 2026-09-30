package dev.tvdeck.onecontroller.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.tvdeck.onecontroller.R
import dev.tvdeck.onecontroller.core.service.OneControllerService
import dev.tvdeck.onecontroller.core.transport.TransportManager

class RemoteWidgetProvider : AppWidgetProvider() {
    companion object {
        const val ACTION_WIDGET_KEY = "dev.tvdeck.onecontroller.ACTION_WIDGET_KEY"
        const val EXTRA_KEYCODE = "extra_widget_keycode"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_KEY) {
            val keyCode = intent.getIntExtra(EXTRA_KEYCODE, 0)
            if (keyCode > 0) {
                val tm = TransportManager.getInstance(context)
                tm.sendNavigationKey(keyCode)
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_remote)

        fun createKeyIntent(keyCode: Int, requestCode: Int): PendingIntent {
            val intent = Intent(context, RemoteWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_KEY
                putExtra(EXTRA_KEYCODE, keyCode)
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        views.setOnClickPendingIntent(R.id.btn_widget_up, createKeyIntent(19, 101))
        views.setOnClickPendingIntent(R.id.btn_widget_down, createKeyIntent(20, 102))
        views.setOnClickPendingIntent(R.id.btn_widget_left, createKeyIntent(21, 103))
        views.setOnClickPendingIntent(R.id.btn_widget_right, createKeyIntent(22, 104))
        views.setOnClickPendingIntent(R.id.btn_widget_ok, createKeyIntent(23, 105))
        views.setOnClickPendingIntent(R.id.btn_widget_back, createKeyIntent(4, 106))
        views.setOnClickPendingIntent(R.id.btn_widget_home, createKeyIntent(3, 107))
        views.setOnClickPendingIntent(R.id.btn_widget_vol_up, createKeyIntent(24, 108))
        views.setOnClickPendingIntent(R.id.btn_widget_vol_down, createKeyIntent(25, 109))
        views.setOnClickPendingIntent(R.id.btn_widget_power, createKeyIntent(26, 110))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
