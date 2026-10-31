package co.luka.capture

import co.luka.data.db.SourceEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class MonitoredPackagesTest {
    @Test
    fun onlyEnabledPackagesAreMonitored() {
        val sources = listOf(
            source("com.nequi.MobileApp", monitor = true),
            source("com.nu.production", monitor = false),
            source("  ", monitor = true),
            source(null, monitor = true),
            source("com.bancolombia.bancolombiaapp", monitor = true, archived = true),
        )
        assertEquals(setOf("com.nequi.MobileApp"), monitoredPackageNames(sources))
    }

    private fun source(packageName: String?, monitor: Boolean, archived: Boolean = false) = SourceEntity(
        slug = packageName ?: "cash",
        name = packageName ?: "Efectivo",
        kind = "app",
        packageName = packageName,
        parserId = null,
        color = "#000000",
        icon = "wallet",
        monitorNotifications = monitor,
        smsSender = null,
        sortOrder = 0,
        archived = archived,
    )
}
