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
import net.spooncast.openmocker.lib.client.okhttp.OpenMockerInterceptor
import net.spooncast.openmocker.lib.control.ControlServer
import net.spooncast.openmocker.lib.control.ControlService
import net.spooncast.openmocker.lib.control.OpenMockerEventSink
import net.spooncast.openmocker.lib.control.SinkRegistry
import net.spooncast.openmocker.lib.data.repo.MemCacheRepoImpl
import net.spooncast.openmocker.lib.ui.OpenMockerActivity

object OpenMocker {

    private var controlServer: ControlServer? = null

    fun getInterceptor(): OpenMockerInterceptor {
        return OpenMockerInterceptor.Builder().build()
    }

    /**
     * 임베디드 제어 서버를 [port] 에서 시작한다(loopback 전용, opt-in). 이미 실행 중이면 멱등하게 무시한다.
     * 첫 호출에서 [ControlServer] 를 lazy 생성해 캐시하고, 캐시 저장소([MemCacheRepoImpl]) 와
     * [SinkRegistry] 를 직접 배선한다 — 내부 모델은 노출하지 않는다.
     */
    fun startControlServer(port: Int = ControlServer.DEFAULT_PORT) {
        val server = controlServer ?: ControlServer(
            ControlService(MemCacheRepoImpl.getInstance(), SinkRegistry)
        ).also { controlServer = it }
        server.start(port)
    }

    /** 제어 서버를 멈춘다. 실행 중이 아니면 멱등하게 무시한다. */
    fun stopControlServer() {
        controlServer?.stop()
    }

    /** 제어 서버의 `POST /inject/{id}` 로 주입 가능한 [sink] 를 등록한다(같은 id 는 last-wins). */
    fun registerSink(sink: OpenMockerEventSink) {
        SinkRegistry.register(sink)
    }

    /** 해당 [id] 의 sink 등록을 해제한다. */
    fun unregisterSink(id: String) {
        SinkRegistry.unregister(id)
    }

    fun show(context: Context) {
        val intent = Intent(context, OpenMockerActivity::class.java)
        context.startActivity(intent)
    }

    fun showNotification(activity: Activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        createNotificationChannel(activity)
        val notification = createNotification(activity)
        NotificationManagerCompat.from(activity).notify(NOTIFICATION_ID, notification)
    }

    fun hideNotification(activity: Activity) {
        NotificationManagerCompat.from(activity).cancel(NOTIFICATION_ID)
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
    private const val NOTIFICATION_ID = 20240911
}