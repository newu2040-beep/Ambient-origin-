package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMixDao {
    @Query("SELECT * FROM saved_mixes ORDER BY timestamp DESC")
    fun getAllMixes(): Flow<List<SavedMix>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMix(mix: SavedMix)

    @Query("DELETE FROM saved_mixes WHERE id = :id")
    suspend fun deleteMixById(id: Int)
}
