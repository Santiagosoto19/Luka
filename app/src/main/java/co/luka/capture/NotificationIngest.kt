package co.luka.capture

import co.luka.data.repository.RoomIngestStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationIngest @Inject constructor(
    private val engine: IngestEngine,
    private val store: RoomIngestStore,
) {
    suspend fun onNotification(packageName: String, title: String, body: String, postedAt: Long) {
        engine.ingest(
            CaptureDraft(
                channel = "notification",
                packageName = packageName,
                sender = null,
                title = title,
                body = body,
                postedAt = postedAt,
            ),
            store,
        )
    }
}
