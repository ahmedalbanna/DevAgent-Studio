package com.example.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.remote.Content
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.Part
import com.example.data.remote.RetrofitClient
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val dao = database.dao

    // Expose flows from Room
    val filesFlow: StateFlow<List<VirtualFile>> = dao.getAllFilesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val terminalLogsFlow: StateFlow<List<TerminalLog>> = dao.getRecentLogsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val chatMessagesFlow: StateFlow<List<ChatMessage>> = dao.getAllChatMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Active state variables
    var activeFile by mutableStateOf<VirtualFile?>(null)
    var selectedModel by mutableStateOf("gemini-3.5-flash")
    var isSendingChat by mutableStateOf(false)
    var isAnalyzingBug by mutableStateOf(false)
    var debuggerLogsInput by mutableStateOf("")
    var debuggerResult by mutableStateOf<String?>(null)

    // Draft code inside editor
    var activeFileCodeDraft by mutableStateOf("")

    init {
        // Pre-populate database with a premium workspace if empty
        viewModelScope.launch {
            val existing = dao.getAllFiles()
            if (existing.isEmpty()) {
                prepopulateWorkspace()
            }
            // Set first file as activeByDefault
            val first = dao.getAllFiles().firstOrNull()
            if (first != null) {
                selectActiveFile(first)
            }
        }
    }

    private suspend fun prepopulateWorkspace() {
        val files = listOf(
            VirtualFile(
                path = "MainActivity.kt",
                content = """package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material3.*
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: Implement critical real-time counter
        setContent {
            MyApplicationTheme {
                Text(text = "Hello DevAgent!")
            }
        }
    }
}
""",
                language = "kotlin"
            ),
            VirtualFile(
                path = "server.js",
                content = """const express = require('express');
const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json());

app.get('/api/status', (req, res) => {
    res.json({
        status: "online",
        uptime: process.uptime(),
        environment: "sandbox-terminal-server"
    });
});

// Trigger dynamic error for debugger testing
app.get('/api/crash', (req, res) => {
    throw new Error("CRITICAL_DATABASE_CONNECTION_FAILED: Cannot reach Host pool (10.0.8.2)");
});

app.listen(PORT, () => {
    console.log('Server is running on port ' + PORT);
});
""",
                language = "javascript"
            ),
            VirtualFile(
                path = "package.json",
                content = """{
  "name": "developer-sandbox-app",
  "version": "1.0.0",
  "description": "Virtual Linux backend express server",
  "main": "server.js",
  "dependencies": {
    "express": "^4.18.2"
  },
  "scripts": {
    "start": "node server.js",
    "test": "echo 'Error: no test specified' && exit 1"
  }
}""",
                language = "json"
            ),
            VirtualFile(
                path = "README.md",
                content = """# DevAgent Workspace Sandbox

Welcome to your secure AI-powered sandbox container environments.

## Features Available
- Monospace interactive CLI Terminal emulator.
- Structured AI chat-coding agent that directly patches codebase.
- Syntax highlighter file editor with manual coding changes.
- In-depth AI Debugger with diagnostics & stacktrace resolver.

## Terminal Commands
Try executing standard terminal shell actions:
- `ls` : List files in directory
- `cat server.js` : Display active server config
- `npm run start` / `npm start` : Simulate server background execution thread
- `gradle build` : Run Gradle compilation check
- `git status` : Review local file updates
- `clear` : Clean shell console
""",
                language = "markdown"
            )
        )
        for (file in files) {
            dao.insertFile(file)
        }
        dao.insertTerminalLog(
            TerminalLog(
                command = "system init",
                output = "Container booted successfully.\nVirtual Workspace synced inside local database.\nReady for developer command streams."
            )
        )
    }

    fun selectActiveFile(file: VirtualFile) {
        activeFile = file
        activeFileCodeDraft = file.content
    }

    fun saveActiveFileDraft() {
        val current = activeFile ?: return
        viewModelScope.launch {
            val updated = current.copy(content = activeFileCodeDraft)
            dao.insertFile(updated)
            activeFile = updated
            writeToTerminal("file save", "Saved package change for: ${current.path} (${activeFileCodeDraft.length} bytes loaded).")
        }
    }

    fun deleteFile(file: VirtualFile) {
        viewModelScope.launch {
            dao.deleteFile(file)
            writeToTerminal("rm ${file.path}", "Removed virtual descriptor path and garbage collected contents for ${file.path}")
            if (activeFile?.path == file.path) {
                val rem = dao.getAllFiles().firstOrNull()
                if (rem != null) selectActiveFile(rem) else activeFile = null
            }
        }
    }

    fun createNewFile(path: String) {
        if (path.isBlank()) return
        val ext = path.substringAfterLast('.', "")
        val lang = when (ext) {
            "kt", "kts" -> "kotlin"
            "js" -> "javascript"
            "json" -> "json"
            "md" -> "markdown"
            "html" -> "html"
            "css" -> "css"
            "py" -> "python"
            else -> "text"
        }
        viewModelScope.launch {
            val newFile = VirtualFile(path = path, content = "// New file created in editor\n", language = lang)
            dao.insertFile(newFile)
            selectActiveFile(newFile)
            writeToTerminal("touch $path", "Created empty descriptor mapping at $path in sandbox project workspace container.")
        }
    }

    // --- ChatGPT AI Coaching ---
    fun sendChatMessage(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            // Log user message
            val userMsg = ChatMessage(role = "user", message = prompt)
            dao.insertChatMessage(userMsg)
            isSendingChat = true

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    dao.insertChatMessage(
                        ChatMessage(
                            role = "model",
                            message = "⚠️ **Developer Secret Warning:** GEMINI_API_KEY is not configured inside AI Studio Secrets Panel yet.\nTo enable AI Coder reasoning, configure the GEMINI_API_KEY key in the secrets dashboard."
                        )
                    )
                    isSendingChat = false
                    return@launch
                }

                // Compile current workspace files info
                val files = dao.getAllFiles()
                val filesSummary = files.joinToString("\n") { "- ${it.path} (${it.language})" }

                val systemInstruction = """
                    You are DevAgent AI, a high-performance full-stack senior developer agent assisting the developer with their project codebase files.
                    The current files available in the active directory are:
                    $filesSummary
                    
                    When requested to write, create, or repair any code file, you MUST supply the modifications using structured XML-like blocks which the client app parses and applies automatically. The format MUST be exactly:
                    
                    [FILE_UPDATE:filename.ext]
                    the full content of the file goes here (no truncated parts or placeholders)
                    [END_FILE_UPDATE]
                    
                    You can include multiple FILE_UPDATE blocks in a single turn if modifying multiple files. Keep text feedback neat, professional, and explain clearly what features you implemented or resolved. Keep markdown code highlights clean.
                """.trimIndent()

                // Gather history conversation
                val dbHistory = dao.getAllChatMessagesFlow().stateIn(viewModelScope).value
                val apiContents = mutableListOf<Content>()

                // Map database messages to Gemini API format
                dbHistory.forEach { msg ->
                    apiContents.add(
                        Content(
                            role = if (msg.role == "user") "user" else "model",
                            parts = listOf(Part(text = msg.message))
                        )
                    )
                }

                // Add current prompt to contents
                apiContents.add(
                    Content(role = "user", parts = listOf(Part(text = prompt)))
                )

                val request = GenerateContentRequest(
                    contents = apiContents,
                    systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
                )

                val response = RetrofitClient.service.generateContent(selectedModel, apiKey, request)
                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Could not receive structured tokens from DevAgent engine. Connection timeout."

                val modelMsg = ChatMessage(role = "model", message = replyText)
                dao.insertChatMessage(modelMsg)

            } catch (e: Exception) {
                dao.insertChatMessage(ChatMessage(role = "model", message = "⚠️ **Error reaching AI server:** ${e.localizedMessage ?: "Connection reset."}"))
            } finally {
                isSendingChat = false
            }
        }
    }

    fun applyPatch(messageId: Long, filePath: String, patchContent: String) {
        viewModelScope.launch {
            val ext = filePath.substringAfterLast('.', "")
            val lang = when (ext) {
                "kt", "kts" -> "kotlin"
                "js" -> "javascript"
                "json" -> "json"
                "md" -> "markdown"
                "html" -> "html"
                "css" -> "css"
                "py" -> "python"
                else -> "text"
            }
            val updatedFile = VirtualFile(path = filePath, content = patchContent, language = lang)
            dao.insertFile(updatedFile)
            dao.updatePatchApplied(messageId, true)

            // Select it as active if applied
            selectActiveFile(updatedFile)
            writeToTerminal("ai patch $filePath", "Successfully patched and deployed AI modifications onto $filePath.")
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            dao.clearChatHistory()
        }
    }

    // --- Built-In AI Debugger Resolver ---
    fun analyzeLogs() {
        val logs = debuggerLogsInput
        if (logs.isBlank()) return
        viewModelScope.launch {
            isAnalyzingBug = true
            debuggerResult = null

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    debuggerResult = "⚠️ Cannot run debugger diagnostic: GEMINI_API_KEY is not configured."
                    isAnalyzingBug = false
                    return@launch
                }

                val files = dao.getAllFiles()
                val filesContext = files.joinToString("\n\n") { "--- FILE: ${it.path} ---\n${it.content}" }

                val debugPrompt = """
                    You are real-time AI debug scanner.
                    Analyze this error traceback:
                    $logs
                    
                    Here are the active files in client project repository:
                    $filesContext
                    
                    Diagnose the crash, tell the developer exactly which line and file causes it, explain the root cause, and write a [FILE_UPDATE:path_to_file] block with the correct patched source file. Keep diagnostics highly technical but concise.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = debugPrompt))))
                )

                val response = RetrofitClient.service.generateContent(selectedModel, apiKey, request)
                val diagnosticReply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "No diagnostics returned from code parser. Traceback is inconclusive."

                debuggerResult = diagnosticReply
            } catch (e: Exception) {
                debuggerResult = "Diagnostic failed: ${e.localizedMessage}"
            } finally {
                isAnalyzingBug = false
            }
        }
    }

    fun applyDebuggerPatch(filePath: String, patchContent: String) {
        viewModelScope.launch {
            val ext = filePath.substringAfterLast('.', "")
            val lang = when (ext) {
                "kt", "kts" -> "kotlin"
                "js" -> "javascript"
                "json" -> "json"
                "md" -> "markdown"
                "html" -> "html"
                "css" -> "css"
                "py" -> "python"
                else -> "text"
            }
            val updatedFile = VirtualFile(path = filePath, content = patchContent, language = lang)
            dao.insertFile(updatedFile)
            selectActiveFile(updatedFile)
            debuggerResult = "Applied updated hotfix onto $filePath successfully! Project rebuilt."
            writeToTerminal("ai patch $filePath", "Applied dynamic debugging hotfix onto file: $filePath.")
        }
    }

    // --- Monospace Terminal Emulator Simulation ---
    fun writeToTerminal(cmd: String, output: String) {
        viewModelScope.launch {
            val log = TerminalLog(command = cmd, output = output)
            dao.insertTerminalLog(log)
        }
    }

    fun executeTerminalCommand(inputText: String) {
        val rawCommand = inputText.trim()
        if (rawCommand.isBlank()) return

        viewModelScope.launch {
            val parts = rawCommand.split("\\s+".toRegex())
            val baseCmd = parts[0].lowercase()

            val responseOutput = when (baseCmd) {
                "help" -> """
                    DevAgent Sandbox Shell v1.0.1 (Pre-release)
                    Available CLI developer commands:
                    - `ls` : List virtual directory listing
                    - `cat <file>` : Print codebase file text
                    - `grep "<pattern>"` : Search regex across containers
                    - `git status` / `git diff` : Git descriptor tracker
                    - `gradle build` : Attempt standard Gradle compiler sync
                    - `npm run start` / `npm start` : Boot simulated Express server process
                    - `rm <file>` : Remove file
                    - `touch <file>` : Create a blank script file
                    - `clear` : Wipe command console history
                """.trimIndent()

                "ls" -> {
                    val list = dao.getAllFiles()
                    if (list.isEmpty()) {
                        "total 0\n(Empty directory workspace)"
                    } else {
                        var buffer = "total ${list.size * 4}\n"
                        val sdf = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
                        val nowStr = sdf.format(Date())
                        list.forEach { file ->
                            val byteLen = file.content.length
                            buffer += "-rw-r--r--  1 dev-user  staff  ${byteLen.toString().padEnd(6)} $nowStr ${file.path}\n"
                        }
                        buffer.trim()
                    }
                }

                "clear" -> {
                    dao.clearTerminalLogs()
                    ""
                }

                "cat" -> {
                    if (parts.size < 2) {
                        "cat: missing file operand. Usage: cat <filename>"
                    } else {
                        val path = parts[1]
                        val file = dao.getFileByPath(path)
                        if (file != null) {
                            file.content
                        } else {
                            "cat: $path: No such file or directory in workspace"
                        }
                    }
                }

                "touch" -> {
                    if (parts.size < 2) {
                        "touch: missing file operand. Usage: touch <filename>"
                    } else {
                        val path = parts[1]
                        createNewFile(path)
                        "Created $path"
                    }
                }

                "rm" -> {
                    if (parts.size < 2) {
                        "rm: missing operand. Usage: rm <filename>"
                    } else {
                        val path = parts[1]
                        val file = dao.getFileByPath(path)
                        if (file != null) {
                            deleteFile(file)
                            "rm: removed $path file context descriptor."
                        } else {
                            "rm: cannot remove '$path': No such file or directory in workspace"
                        }
                    }
                }

                "grep" -> {
                    if (parts.size < 2) {
                        "grep: missing search term. Usage: grep \"pattern\""
                    } else {
                        val pattern = rawCommand.substringAfter("grep").replace("\"", "").trim()
                        val list = dao.getAllFiles()
                        val results = list.mapNotNull { f ->
                            val matchingLines = f.content.lines().mapIndexedNotNull { index, line ->
                                if (line.contains(pattern, ignoreCase = true)) {
                                    "${f.path}:${index + 1}: ${line.trim()}"
                                } else null
                            }
                            if (matchingLines.isNotEmpty()) matchingLines.joinToString("\n") else null
                        }
                        if (results.isEmpty()) {
                            "grep: no occurrences of '$pattern' found."
                        } else {
                            results.joinToString("\n")
                        }
                    }
                }

                "git" -> {
                    if (parts.size < 2) {
                        "git: subcommand required. Try `git status` or `git diff`."
                    } else {
                        val sub = parts[1].lowercase()
                        if (sub == "status") {
                            """
                                On branch main
                                Your branch is up to date with 'origin/main'.

                                Untracked files:
                                  (use "git add <file>..." to include in what will be committed)
                                	.env.example
                                	terminal_workspace_configs.json

                                no changes added to commit (use "git add" and/or "git commit -a")
                            """.trimIndent()
                        } else if (sub == "diff") {
                            """
                                diff --git a/MainActivity.kt b/MainActivity.kt
                                index ec61bd2..3fbcde0 100644
                                --- a/MainActivity.kt
                                +++ b/MainActivity.kt
                                @@ -11,4 +11,4 @@
                                - Text(text = "Hello Android")
                                + Text(text = "Hello DevAgent OS Terminal Sandbox Version")
                            """.trimIndent()
                        } else {
                            "git: '$sub' is not a simulated command. Try `git status`."
                        }
                    }
                }

                "gradle" -> {
                    if (parts.size < 2 || parts[1].lowercase() != "build") {
                        "gradle: only `gradle build` tool task is supported."
                    } else {
                        // Scan for TODOs or Literal crash flags to fail build
                        val ktFile = dao.getFileByPath("MainActivity.kt")
                        if (ktFile != null && (ktFile.content.contains("TODO") || ktFile.content.contains("syntax error"))) {
                            """
                                Starting Gradle Daemon...
                                > Configure project :app
                                > Task :app:preBuild UP-TO-DATE
                                > Task :app:compileDebugKotlin FAILED
                                
                                FAILURE: Build failed with an exception.
                                
                                * What went wrong:
                                Execution failed for task ':app:compileDebugKotlin'.
                                > Compilation Error at /src/com/example/MainActivity.kt:10:
                                  Unresolved reference or unhandled FIXME stub: "TODO: Implement critical real-time counter"
                                  
                                * Try:
                                1. Copy this stacktrace trace.
                                2. Feed it into AI Debugger panel to automatically generate structural hotfix corrections.
                                
                                BUILD FAILED in 1.8s
                            """.trimIndent()
                        } else {
                            """
                                Starting Gradle Daemon...
                                > Configure project :app
                                > Task :app:preBuild UP-TO-DATE
                                > Task :app:compileDebugJavaWithJavac UP-TO-DATE
                                > Task :app:compileDebugKotlin SUCCESS
                                > Task :app:assembleDebug SUCCESS
                                
                                BUILD SUCCESSFUL in 2.1s
                                4 actionable tasks: 2 executed, 2 up-to-date
                            """.trimIndent()
                        }
                    }
                }

                "npm" -> {
                    val action = rawCommand.substringAfter("npm").trim()
                    if (action == "start" || action == "run start") {
                        val serverFile = dao.getFileByPath("server.js")
                        if (serverFile == null) {
                            "npm ERR! missing script file 'server.js' to boot server instance"
                        } else {
                            """
                                > developer-sandbox-app@1.0.0 start
                                > node server.js
                                
                                [Express Server Init] Database pool configured.
                                [Express Server Init] Loading routes...
                                [Express Server Init] GET /api/status - 200 OK
                                [Express Server Init] GET /api/crash - 500 FAIL
                                
                                Server is running on http://localhost:3000
                                (Keep command running... Press volume trigger to pause stream listener)
                            """.trimIndent()
                        }
                    } else if (action == "test") {
                        """
                            > developer-sandbox-app@1.0.0 test
                            > echo 'Error: no test specified' && exit 1
                            
                            Error: no test specified
                            npm ERR! Test failed. See above for details.
                        """.trimIndent()
                    } else {
                        "npm ERR! Unknown virtual target script action. Supported: `npm start`, `npm test`"
                    }
                }

                else -> "bash: $baseCmd: command not found in container sandbox"
            }

            if (responseOutput.isNotEmpty()) {
                dao.insertTerminalLog(
                    TerminalLog(command = rawCommand, output = responseOutput)
                )
            }
        }
    }
}
