package co.luka

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import co.luka.capture.PendingSettler
import co.luka.ui.access.rebindNotificationListener
import co.luka.worker.SummaryScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class LukaApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var summaryScheduler: SummaryScheduler
    @Inject lateinit var pendingSettler: PendingSettler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        summaryScheduler.ensureScheduled()
        rebindNotificationListener(this)
        appScope.launch { pendingSettler.run() }
    }
}
