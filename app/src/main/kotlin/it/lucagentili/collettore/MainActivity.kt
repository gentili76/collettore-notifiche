package it.lucagentili.collettore

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import it.lucagentili.collettore.messaging.EVENTS_TOPIC
import it.lucagentili.collettore.notifications.Notifications
import it.lucagentili.collettore.ui.feed.FeedScreen
import it.lucagentili.collettore.ui.theme.CollettoreNotificheTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    Notifications.ensureChannel(this)
    FirebaseMessaging.getInstance().subscribeToTopic(EVENTS_TOPIC)

    setContent {
      CollettoreNotificheTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          NotificationPermissionRequest()
          FeedScreen()
        }
      }
    }
  }
}

/** Android 13+: chiede il permesso per le notifiche al primo avvio. */
@Composable
private fun NotificationPermissionRequest() {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

  val context = androidx.compose.ui.platform.LocalContext.current
  val launcher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

  LaunchedEffect(Unit) {
    val granted =
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
  }
}
