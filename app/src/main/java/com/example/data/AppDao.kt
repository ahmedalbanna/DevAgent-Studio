package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- Virtual Files ---
    @Query("SELECT * FROM virtual_files ORDER BY path ASC")
    fun getAllFilesFlow(): Flow<List<VirtualFile>>

    @Query("SELECT * FROM virtual_files ORDER BY path ASC")
    suspend fun getAllFiles(): List<VirtualFile>

    @Query("SELECT * FROM virtual_files WHERE path = :path LIMIT 1")
    suspend fun getFileByPath(path: String): VirtualFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: VirtualFile)

    @Delete
    suspend fun deleteFile(file: VirtualFile)

    @Query("DELETE FROM virtual_files")
    suspend fun clearAllFiles()

    // --- Terminal Logs ---
    @Query("SELECT * FROM terminal_logs ORDER BY id DESC LIMIT 100")
    fun getRecentLogsFlow(): Flow<List<TerminalLog>>

    @Query("SELECT * FROM terminal_logs ORDER BY id ASC")
    suspend fun getAllTerminalLogs(): List<TerminalLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerminalLog(log: TerminalLog)

    @Query("DELETE FROM terminal_logs")
    suspend fun clearTerminalLogs()

    // --- Chat Messages ---
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessagesFlow(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    @Query("UPDATE chat_messages SET patchApplied = :applied WHERE id = :id")
    suspend fun updatePatchApplied(id: Long, applied: Boolean)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()
}
