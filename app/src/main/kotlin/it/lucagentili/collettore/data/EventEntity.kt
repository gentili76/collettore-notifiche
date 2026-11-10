package it.lucagentili.collettore.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Evento ricevuto via FCM: una modifica rilevata su una sorgente. */
@Entity(tableName = "events")
data class EventEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val sourceId: String,
  val title: String,
  val body: String,
  val url: String,
  /** Istante in cui il worker ha rilevato la modifica (epoch secondi). */
  val detectedAt: Long,
  /** Istante in cui l'app ha ricevuto il messaggio (epoch millisecondi). */
  val receivedAt: Long,
)
