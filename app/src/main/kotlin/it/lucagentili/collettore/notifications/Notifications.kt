package it.lucagentili.collettore.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import it.lucagentili.collettore.MainActivity

object Notifications {
  const val CHANNEL_ID = "eventi"

  /** Crea il canale di notifica (obbligatorio da Android 8). Idempotente. */
  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel =
      NotificationChannel(CHANNEL_ID, "Modifiche rilevate", NotificationManager.IMPORTANCE_DEFAULT)
        .apply { description = "Avvisi quando una sorgente monitorata cambia" }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  }

  /**
   * Mostra la notifica. Se il permesso POST_NOTIFICATIONS non è concesso
   * l'evento resta comunque nello storico, ma nessuna notifica viene mostrata.
   */
  fun show(context: Context, notificationId: Int, title: String, body: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      return
    }

    // Tap: apre l'app sulla tab "nuove" con l'ultimo evento evidenziato (non apre il sito).
    val openIntent =
      Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_NEWEST, true)
    val pending = PendingIntent.getActivity(
      context,
      notificationId,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_notify_chat)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setContentIntent(pending)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(notificationId, notification)
  }
}
