package co.luka.capture

import android.app.Notification
import android.content.ComponentName
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LukaNotificationListener : NotificationListenerService() {
    @Inject lateinit var ingest: NotificationIngest
    @Inject lateinit var monitored: MonitoredPackages

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        ListenerHealth.connected.value = true
        val cutoff = System.currentTimeMillis() - RECENT_WINDOW_MS
        activeNotifications?.forEach { posted ->
            if (posted.postTime >= cutoff) handle(posted)
        }
    }

    override fun onListenerDisconnected() {
        ListenerHealth.connected.value = false
        requestRebind(ComponentName(this, LukaNotificationListener::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        handle(sbn)
    }

    override fun onDestroy() {
        ListenerHealth.connected.value = false
        scope.cancel()
        super.onDestroy()
    }

    private fun handle(sbn: StatusBarNotification) {
        if (sbn.isOngoing) return
        val packageName = sbn.packageName ?: return
        if (packageName == this.packageName) return
        if (!monitored.isMonitored(packageName)) {
            Log.i(TAG, "omitida $packageName")
            return
        }
        val text = notificationText(sbn.notification?.extras)
        val title = text.title
        val body = text.body
        if (title.isBlank() && body.isBlank()) {
            Log.i(TAG, "sin texto $packageName")
            return
        }
        val postedAt = sbn.postTime
        Log.i(TAG, "aviso $packageName ${title.take(80)} ${body.take(160)}")
        scope.launch {
            try {
                val outcome = ingest.onNotification(packageName, title, body, postedAt)
                Log.i(TAG, "resultado $packageName $outcome")
            } catch (error: Exception) {
                Log.e(TAG, "no se pudo registrar $packageName", error)
            }
        }
    }

    private companion object {
        const val TAG = "LukaCapture"
        const val RECENT_WINDOW_MS = 3 * 60 * 60 * 1000L
    }
}

internal data class NotificationText(val title: String, val body: String)

internal fun notificationText(extras: Bundle?): NotificationText {
    if (extras == null) return NotificationText("", "")
    val parts = linkedSetOf<String>()
    fun add(value: CharSequence?) {
        val text = value?.toString()?.trim().orEmpty()
        if (text.isNotEmpty()) parts += text
    }
    add(extras.getCharSequence(Notification.EXTRA_TITLE))
    add(extras.getCharSequence(Notification.EXTRA_TEXT))
    add(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
    add(extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
    add(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT))
    add(extras.getCharSequence(Notification.EXTRA_INFO_TEXT))
    extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { add(it) }
    @Suppress("DEPRECATION")
    val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
    messages?.forEach { item ->
        val bundle = item as? Bundle ?: return@forEach
        add(bundle.getCharSequence("text"))
    }
    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
    val body = parts.filter { it != title }.joinToString("\n")
    return NotificationText(title, body.ifBlank { title })
}
