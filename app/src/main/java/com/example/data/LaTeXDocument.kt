package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "latex_documents")
data class LaTeXDocument(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val code: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val category: String = "Custom",
    val isTemplate: Boolean = false
)
