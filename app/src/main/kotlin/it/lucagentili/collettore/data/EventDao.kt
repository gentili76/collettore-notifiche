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

  @Query("SELECT * FROM events ORDER BY receivedAt DESC")
  fun observeAll(): Flow<List<EventEntity>>
}
