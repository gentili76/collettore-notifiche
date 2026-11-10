package it.lucagentili.collettore.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lucagentili.collettore.data.AppDatabase
import it.lucagentili.collettore.data.EventEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FeedViewModel(application: Application) : AndroidViewModel(application) {

  val events: StateFlow<List<EventEntity>> =
    AppDatabase.get(application)
      .eventDao()
      .observeAll()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
