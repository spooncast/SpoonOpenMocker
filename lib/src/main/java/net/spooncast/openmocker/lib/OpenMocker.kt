package net.spooncast.openmocker.lib

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import net.spooncast.openmocker.lib.interceptor.OpenMockerInterceptor
import net.spooncast.openmocker.lib.ui.OpenMockerActivity

object OpenMocker {

    fun getInterceptor(): OpenMockerInterceptor {
        return OpenMockerInterceptor.Builder().build()
    }

    fun show(context: Context) {
        val intent = Intent(context, OpenMockerActivity::class.java)
        context.startActivity(intent)
    }

    fun notify(activity: Activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        createNotificationChannel(activity)
        val notification = createNotification(activity)
        NotificationManagerCompat.from(activity).notify(20240911, notification)
    }

    private fun createNotificationChannel(activity: Activity) {
        val notiChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Open Mocker", NotificationManager.IMPORTANCE_HIGH)
        NotificationManagerCompat.from(activity).createNotificationChannels(listOf(notiChannel))
    }

    private fun createNotification(activity: Activity): Notification {
        val intent = Intent(activity, OpenMockerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(activity, 20240911, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(activity, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(activity.getString(R.string.open_mocker))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }

    private const val NOTIFICATION_CHANNEL_ID = "open-mocker-channel"
}