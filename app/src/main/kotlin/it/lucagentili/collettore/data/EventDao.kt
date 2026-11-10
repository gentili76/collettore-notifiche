package it.lucagentili.collettore.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(event: EventEntity): Long

  @Query("SELECT * FROM events WHERE seen = 0 ORDER BY receivedAt DESC")
  fun observeUnseen(): Flow<List<EventEntity>>

  @Query("SELECT * FROM events WHERE seen = 1 ORDER BY receivedAt DESC")
  fun observeSeen(): Flow<List<EventEntity>>

  @Query("UPDATE events SET seen = 1 WHERE id = :id")
  suspend fun markSeen(id: Long)
}
