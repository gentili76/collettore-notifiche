package it.lucagentili.collettore.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lucagentili.collettore.data.AppDatabase
import it.lucagentili.collettore.data.EventEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedViewModel(application: Application) : AndroidViewModel(application) {

  private val dao = AppDatabase.get(application).eventDao()

  /** Eventi non ancora aperti: tab "nuove". */
  val unseen: StateFlow<List<EventEntity>> =
    dao.observeUnseen().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  /** Eventi già aperti: tab "viste". */
  val seen: StateFlow<List<EventEntity>> =
    dao.observeSeen().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  fun markSeen(event: EventEntity) {
    viewModelScope.launch { dao.markSeen(event.id) }
  }
}
