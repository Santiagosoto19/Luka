package co.luka.capture

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
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

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        if (!monitored.isMonitored(packageName)) return
        val extras = sbn.notification?.extras
        val title = extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
        val big = extras?.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString()
        val body = big?.takeIf { it.isNotBlank() } ?: text
        if (title.isBlank() && body.isBlank()) return
        val postedAt = sbn.postTime
        scope.launch {
            ingest.onNotification(packageName, title, body, postedAt)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
