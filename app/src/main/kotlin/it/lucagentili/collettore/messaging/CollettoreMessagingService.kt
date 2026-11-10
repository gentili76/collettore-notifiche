package it.lucagentili.collettore.messaging

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import it.lucagentili.collettore.data.AppDatabase
import it.lucagentili.collettore.data.EventEntity
import it.lucagentili.collettore.notifications.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Riceve gli eventi del worker. Il worker invia messaggi data-only, quindi
 * questo metodo viene chiamato sia in foreground sia in background.
 */
class CollettoreMessagingService : FirebaseMessagingService() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onMessageReceived(message: RemoteMessage) {
    val data = message.data
    val title = data["title"] ?: return
    val body = data["body"].orEmpty()
    val url = data["url"].orEmpty()
    val sourceId = data["source_id"].orEmpty()
    val detectedAt = data["ts"]?.toLongOrNull() ?: (System.currentTimeMillis() / 1000)

    val receivedAt = System.currentTimeMillis()
    val notificationId = (sourceId + detectedAt).hashCode()

    Notifications.ensureChannel(this)
    Notifications.show(this, notificationId, title, body, url)

    scope.launch {
      AppDatabase.get(applicationContext).eventDao().insert(
        EventEntity(
          sourceId = sourceId,
          title = title,
          body = body,
          url = url,
          detectedAt = detectedAt,
          receivedAt = receivedAt,
        ),
      )
    }
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }
}
