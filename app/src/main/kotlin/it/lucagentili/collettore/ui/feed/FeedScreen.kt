package it.lucagentili.collettore.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.lucagentili.collettore.data.EventEntity
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: FeedViewModel = viewModel()) {
  val events by viewModel.events.collectAsStateWithLifecycle()

  Scaffold(
    topBar = { TopAppBar(title = { Text("Collettore") }) },
  ) { padding ->
    if (events.isEmpty()) {
      EmptyFeed(Modifier.padding(padding).fillMaxSize())
    } else {
      EventList(events, Modifier.padding(padding).fillMaxSize())
    }
  }
}

@Composable
private fun EventList(events: List<EventEntity>, modifier: Modifier = Modifier) {
  val uriHandler = LocalUriHandler.current
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(events, key = { it.id }) { event ->
      Card(
        modifier =
          Modifier.fillMaxWidth().clickable(enabled = event.url.isNotBlank()) {
            uriHandler.openUri(event.url)
          },
        colors = CardDefaults.cardColors(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
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

@Composable
private fun EmptyFeed(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Nessuna modifica ricevuta", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Text(
      "Quando una sorgente monitorata cambia, l'evento compare qui.",
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}

private fun formatTime(epochMillis: Long): String =
  DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(epochMillis))
