package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LaTeXDocumentDao {
    @Query("SELECT * FROM latex_documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<LaTeXDocument>>

    @Query("SELECT * FROM latex_documents WHERE id = :id")
    suspend fun getDocumentById(id: Int): LaTeXDocument?

    @Query("SELECT * FROM latex_documents WHERE id = :id")
    fun getDocumentByIdFlow(id: Int): Flow<LaTeXDocument?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: LaTeXDocument): Long

    @Update
    suspend fun updateDocument(document: LaTeXDocument)

    @Delete
    suspend fun deleteDocument(document: LaTeXDocument)

    @Query("DELETE FROM latex_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Int)

    @Query("SELECT COUNT(*) FROM latex_documents")
    suspend fun getCount(): Int
}
