package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CloneDao {

    @Query("SELECT * FROM clones ORDER BY createdAt DESC")
    fun observeAllClones(): Flow<List<CloneEntity>>

    @Query("SELECT * FROM clones ORDER BY createdAt DESC")
    suspend fun getAllClonesOnce(): List<CloneEntity>

    @Query("SELECT * FROM clones WHERE id = :id LIMIT 1")
    fun observeCloneById(id: String): Flow<CloneEntity?>

    @Query("SELECT * FROM clones WHERE id = :id LIMIT 1")
    suspend fun getCloneById(id: String): CloneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClone(entity: CloneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClones(entities: List<CloneEntity>)

    @Update
    suspend fun updateClone(entity: CloneEntity)

    @Query("DELETE FROM clones WHERE id = :id")
    suspend fun deleteCloneById(id: String)

    @Query("DELETE FROM clones WHERE id IN (:ids)")
    suspend fun deleteClonesByIds(ids: List<String>)

    @Query("DELETE FROM clones")
    suspend fun deleteAllClones()
}
