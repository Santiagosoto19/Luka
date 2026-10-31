package co.luka.capture

import co.luka.data.db.LukaDatabase
import co.luka.data.db.SourceEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Singleton
class MonitoredPackages @Inject constructor(
    database: LukaDatabase,
) {
    @Volatile private var names: Set<String> = emptySet()
    private val ready = CompletableDeferred<Unit>()

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                database.sourceDao().observeAll().collect { sources ->
                    names = monitoredPackageNames(sources)
                    if (!ready.isCompleted) ready.complete(Unit)
                }
            } catch (_: Exception) {
                if (!ready.isCompleted) ready.complete(Unit)
            }
        }
    }

    fun isMonitored(packageName: String): Boolean {
        if (!ready.isCompleted) {
            runBlocking { ready.await() }
        }
        return packageName in names
    }
}

internal fun monitoredPackageNames(sources: List<SourceEntity>): Set<String> = sources.mapNotNull { source ->
    if (!source.monitorNotifications || source.archived) null
    else source.packageName?.takeIf { it.isNotBlank() }
}.toSet()
