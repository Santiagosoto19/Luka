package co.luka.ui.access

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import co.luka.capture.LukaNotificationListener

fun notificationAccessEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
    val component = ComponentName(context, LukaNotificationListener::class.java).flattenToString()
    return enabled.split(":").any { it.equals(component, ignoreCase = true) }
}

fun openNotificationAccess(context: Context) {
    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
