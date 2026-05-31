package com.example.data

import androidx.room.*

@Entity(tableName = "virtual_files")
data class VirtualFile(
    @PrimaryKey val path: String,
    val content: String,
    val language: String,
    val isSystemFile: Boolean = false
)

@Entity(tableName = "terminal_logs")
data class TerminalLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val command: String,
    val output: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user" or "model"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val patchApplied: Boolean = false
)
