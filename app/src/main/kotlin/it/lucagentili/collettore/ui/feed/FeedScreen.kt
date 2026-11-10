package it.lucagentili.collettore.ui.feed

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.lucagentili.collettore.data.EventEntity
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: FeedViewModel = viewModel()) {
  val unseen by viewModel.unseen.collectAsStateWithLifecycle()
  val seen by viewModel.seen.collectAsStateWithLifecycle()
  var tab by rememberSaveable { mutableIntStateOf(0) }

  Scaffold(
    topBar = { TopAppBar(title = { Text("Collettore") }) },
  ) { padding ->
    Column(modifier = Modifier.padding(padding).fillMaxSize()) {
      PrimaryTabRow(selectedTabIndex = tab) {
        Tab(
          selected = tab == 0,
          onClick = { tab = 0 },
          text = { Text("Nuove (${unseen.size})") },
        )
        Tab(
          selected = tab == 1,
          onClick = { tab = 1 },
          text = { Text("Viste (${seen.size})") },
        )
      }
      if (tab == 0) {
        EventList(
          events = unseen,
          emptyTitle = "Nessuna modifica nuova",
          emptyBody = "Quando una sorgente monitorata cambia, l'evento compare qui.",
          onOpen = viewModel::markSeen,
        )
      } else {
        EventList(
          events = seen,
          emptyTitle = "Nessun evento visto",
          emptyBody = "Gli eventi che apri compaiono qui.",
          onOpen = viewModel::markSeen,
        )
      }
    }
  }
}

@Composable
private fun EventList(
  events: List<EventEntity>,
  emptyTitle: String,
  emptyBody: String,
  onOpen: (EventEntity) -> Unit,
) {
  if (events.isEmpty()) {
    EmptyFeed(emptyTitle, emptyBody, Modifier.fillMaxSize())
    return
  }
  val uriHandler = LocalUriHandler.current
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(events, key = { it.id }) { event ->
      Card(
        modifier =
          Modifier.fillMaxWidth().clickable(enabled = event.url.isNotBlank()) {
            onOpen(event)
            uriHandler.openUri(event.url)
          },
        colors = CardDefaults.cardColors(),
      ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
          SiteIcon(event.url)
          Spacer(Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(event.body, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(
              formatTime(event.detectedAt * 1000),
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }
      }
    }
  }
}

private val faviconCache = LruCache<String, ImageBitmap>(64)

/** Favicon del dominio dell'evento, tramite il servizio favicon di Google. */
@Composable
private fun SiteIcon(url: String) {
  val host = remember(url) { Uri.parse(url).host }
  val icon by produceState<ImageBitmap?>(initialValue = null, host) {
    value = host?.takeIf { it.isNotBlank() }?.let { loadFavicon(it) }
  }
  val image = icon
  if (image == null) {
    // Nessuna favicon: cerchio con l'iniziale del dominio.
    Box(
      modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = host?.firstOrNull()?.uppercase().orEmpty(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimary,
      )
    }
  } else {
    Image(
      bitmap = image,
      contentDescription = null,
      modifier = Modifier.size(32.dp).clip(CircleShape),
    )
  }
}

private suspend fun loadFavicon(host: String): ImageBitmap? {
  faviconCache.get(host)?.let { return it }
  return withContext(Dispatchers.IO) {
    try {
        val connection =
          URL("https://www.google.com/s2/favicons?domain=$host&sz=64").openConnection()
            as HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        // 404: il sito non ha una favicon, si mostra il fallback.
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
          null
        } else {
          val bitmap = connection.inputStream.use { BitmapFactory.decodeStream(it) }
          bitmap?.asImageBitmap()?.also { faviconCache.put(host, it) }
        }
      } catch (e: Exception) {
        Log.w("Favicon", "errore per $host: $e")
        null
      }
  }
}

@Composable
private fun EmptyFeed(title: String, body: String, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium)
  }
}

private fun formatTime(epochMillis: Long): String =
  DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(epochMillis))
