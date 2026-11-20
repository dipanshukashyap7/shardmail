package com.shardmail.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AliasDao {
    @Query("SELECT * FROM aliases ORDER BY created_at DESC")
    fun getAllAliases(): Flow<List<AliasEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAliases(aliases: List<AliasEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: AliasEntity)

    @Query("DELETE FROM aliases WHERE id = :id")
    suspend fun deleteAlias(id: String)

    @Query("SELECT COUNT(*) FROM aliases")
    suspend fun getAliasCount(): Int
}
