package co.luka.data.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import co.luka.R
import co.luka.domain.money.CopMoney
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAlerts @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        val channel = NotificationChannel(CHANNEL, "Avisos de Luka", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "Resúmenes y presupuestos"
        manager.createNotificationChannel(channel)
    }

    fun budget(category: String, spent: Long, limit: Long) {
        notify(
            id = (category.hashCode() and 0xffff) + 1000,
            title = "Presupuesto de $category",
            body = "Llevas ${CopMoney.format(spent)} de ${CopMoney.format(limit)}.",
        )
    }

    fun summary(id: Int, title: String, body: String) {
        notify(id, title, body)
    }

    private fun notify(id: Int, title: String, body: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
    }

    private companion object {
        const val CHANNEL = "luka_alerts"
    }
}
