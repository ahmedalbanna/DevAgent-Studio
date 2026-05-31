package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ChatMessage
import com.example.data.TerminalLog
import com.example.data.VirtualFile
import com.example.data.WorkspaceViewModel
import com.example.ui.components.CodeHighlighter
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    DevAgentDashboard(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun DevAgentDashboard(
    modifier: Modifier = Modifier,
    viewModel: WorkspaceViewModel = viewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // Retrieve database observations
    val files by viewModel.filesFlow.collectAsState()
    val terminalLogs by viewModel.terminalLogsFlow.collectAsState()
    val chatMessages by viewModel.chatMessagesFlow.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var newFileNameInput by remember { mutableStateOf("") }

    // Visual design color shorthand
    val darkBg = Color(0xFFFEF7FF)
    val panelBg = Color(0xFFFFFFFF)
    val neonTeal = Color(0xFF6750A4)
    val neonOrange = Color(0xFF6750A4)
    val mutedBorder = Color(0xFFE7E0EC)
    val dividerBorder = Color(0xFFCAC4D0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        // --- 1. HEADER SECTION (Geometric Balance Custom TopBar) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = panelBg),
            border = BorderStroke(1.dp, dividerBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Nebula Coder Circle terminal icon container
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(neonTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build, // representing terminal / tool symbol
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Column {
                            Text(
                                text = "Nebula Coder",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1D1B20)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFB1EEA3))
                                )
                                Text(
                                    text = "PROD-SERVER-01",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF49454F)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { /* Action */ },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share, 
                                contentDescription = "Settings Ethernet", 
                                tint = Color(0xFF49454F),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { /* More choices */ },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert, 
                                contentDescription = "More options", 
                                tint = Color(0xFF49454F),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // AI Model selector row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active LLM Intel:",
                        fontSize = 12.sp,
                        color = Color(0xFF49454F),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("gemini-3.5-flash", "gemini-3.1-pro-preview").forEach { model ->
                            val isSelected = viewModel.selectedModel == model
                            val label = if (model.contains("pro")) "Gemini Pro" else "Gemini Flash"
                            SuggestionChip(
                                onClick = { viewModel.selectedModel = model },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) Color(0xFFEADDFF) else Color(0xFFF3EDF7),
                                    labelColor = if (isSelected) Color(0xFF21005D) else Color(0xFF49454F)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0)
                                )
                            )
                        }
                    }
                }
            }
        }

        // --- 2. MAIN WORKSPACE TAB SELECTOR ---
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.Transparent,
            contentColor = neonTeal,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = neonTeal
                )
            },
            divider = {
                HorizontalDivider(color = dividerBorder)
            }
        ) {
            val tabs = listOf(
                Pair("Chat Code", Icons.Default.Send),
                Pair("Editor", Icons.Default.Edit),
                Pair("Terminal", Icons.Default.List),
                Pair("AI Debugger", Icons.Default.Warning)
            )
            tabs.forEachIndexed { index, pair ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    icon = { Icon(pair.second, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (activeTab == index) neonTeal else Color(0xFF49454F)) },
                    text = {
                        Text(
                            text = pair.first,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (activeTab == index) neonTeal else Color(0xFF49454F)
                        )
                    }
                )
            }
        }

        // --- 3. TAB VIEW BODY ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                0 -> ChatCoderTab(viewModel, chatMessages)
                1 -> CodeEditorTab(viewModel, files, onNewFileClicked = { showCreateFileDialog = true })
                2 -> TerminalEmulatorTab(viewModel, terminalLogs)
                3 -> AiDebuggerTab(viewModel)
            }
        }
    }

    // New File creation Dialog
    if (showCreateFileDialog) {
        Dialog(onDismissRequest = { showCreateFileDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = panelBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, dividerBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "touch: Generate Virtual File",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = neonTeal,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Define your file path inside template project workspace container (e.g. index.html, app.py).",
                        fontSize = 12.sp,
                        color = Color(0xFF49454F)
                    )

                    OutlinedTextField(
                        value = newFileNameInput,
                        onValueChange = { newFileNameInput = it },
                        placeholder = { Text("index.html", color = Color(0xFF938F99), fontSize = 13.sp) },
                        textStyle = TextStyle(color = Color(0xFF1D1B20), fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = neonTeal,
                            unfocusedBorderColor = dividerBorder,
                            focusedContainerColor = Color(0xFFF3EDF7),
                            unfocusedContainerColor = Color(0xFFF3EDF7)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newFileNameInput.isNotBlank()) {
                                viewModel.createNewFile(newFileNameInput.trim())
                                Toast.makeText(context, "File created and deployed!", Toast.LENGTH_SHORT).show()
                                newFileNameInput = ""
                                showCreateFileDialog = false
                            }
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCreateFileDialog = false }) {
                            Text("Abort", color = Color(0xFF49454F), fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = {
                                if (newFileNameInput.isNotBlank()) {
                                    viewModel.createNewFile(newFileNameInput.trim())
                                    Toast.makeText(context, "File created and deployed!", Toast.LENGTH_SHORT).show()
                                    newFileNameInput = ""
                                    showCreateFileDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = neonTeal, contentColor = Color.White)
                        ) {
                            Text("Create File", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 1: CONNECTED CHAT CODER WITH AUTOMATED CODE PATCHES
// ==========================================
@Composable
fun ChatCoderTab(viewModel: WorkspaceViewModel, messages: List<ChatMessage>) {
    var promptInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val neonTeal = Color(0xFF6750A4)
    val mutedBorder = Color(0xFFCAC4D0)
    val userBubbleBg = Color(0xFFEADDFF)
    val aiBubbleBg = Color(0xFFFFFFFF)
    val panelBg = Color(0xFFFFFFFF)

    // Autoscroll chat on append
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Quick suggestions row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val prompts = listOf("Build Web Server", "Fix MainActivity", "Help index.html")
            prompts.forEach { item ->
                SuggestionChip(
                    onClick = {
                        val fullPrompt = when (item) {
                            "Build Web Server" -> "Generate an update in server.js to add a robust POST endpoint inside express that receives telemetry json logs."
                            "Fix MainActivity" -> "Please implement the critical countdown counter inside MainActivity.kt and remove the TODO blocking gradle build compiles."
                            "Help index.html" -> "Generate a clean high-performance index.html file with beautiful real-time CSS countdown animations."
                            else -> item
                        }
                        promptInput = fullPrompt
                    },
                    label = { Text(item, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF6750A4)) },
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFF3EDF7))
                )
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(48.dp), tint = neonTeal.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "AI Coding Terminal Connected",
                            color = Color(0xFF1D1B20),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ask me to implement code, create files or fix bugs.\nYour instructions directly compile and patch the workspace db!",
                            color = Color(0xFF49454F),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            items(messages) { message ->
                val isUser = message.role == "user"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    // Chat Bubble
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 0.dp,
                            bottomEnd = if (isUser) 0.dp else 12.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = if (isUser) userBubbleBg else aiBubbleBg),
                        border = BorderStroke(1.dp, if (isUser) Color.Transparent else mutedBorder),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(vertical = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Header label (role name)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isUser) "DEVELOPER_SHELL" else "DEV_AGENT_AI",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isUser) Color(0xFF21005D) else Color(0xFF6750A4),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF49454F)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // Purge our file updates tags to clean the plain text message to prevent clutter
                            val cleanMsg = message.message
                                .replace(Regex("\\[FILE_UPDATE:.*?\\]"), "")
                                .replace("[END_FILE_UPDATE]", "")
                                .trim()

                            Text(
                                text = cleanMsg,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = if (isUser) Color(0xFF21005D) else Color(0xFF1D1B20),
                                lineHeight = 18.sp
                            )

                            // --- PARSIFIED AUTO-PATCH DETECTOR ---
                            if (!isUser) {
                                val patches = CodeHighlighter.extractFilePatches(message.message)
                                if (patches.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "⚡ RETAINED CODE PATCH SYSTEM",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = neonTeal,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    for (patch in patches) {
                                        val filePath = patch.first
                                        val fileCode = patch.second
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1B20)),
                                            border = BorderStroke(1.dp, Color(0xFF49454F)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 6.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFCFBCFF))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = filePath,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = Color(0xFFCFBCFF)
                                                        )
                                                    }

                                                    Text(
                                                        text = "Ready to patch",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF938F99),
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Snipped preview
                                                val lines = fileCode.trim().lines()
                                                val previewCode = lines.take(4).joinToString("\n") + if (lines.size > 4) "\n..." else ""
                                                Text(
                                                    text = previewCode,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFFE6E1E5),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFF1D1B20))
                                                        .padding(6.dp)
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                if (message.patchApplied) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFB1EEA3))
                                                        Text(
                                                            text = "PATCH APPLIED TO WORKSPACE",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFFB1EEA3),
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = {
                                                            viewModel.applyPatch(message.id, filePath, fileCode)
                                                            Toast.makeText(context, "$filePath patched recursively!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = neonTeal, contentColor = Color.White),
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                        modifier = Modifier.align(Alignment.End)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "DEPLOY & PATCH FILE",
                                                                fontSize = 10.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (viewModel.isSendingChat) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = neonTeal, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DevAgent is thinking and compiling structures...",
                            fontSize = 11.sp,
                            color = Color(0xFF49454F),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Send row (Geometric Balance Custom footer layout)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .background(Color(0xFFF3EDF7), RoundedCornerShape(24.dp))
                .border(BorderStroke(1.dp, mutedBorder), RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.clearChat() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Clear Chat", tint = Color(0xFF49454F))
            }

            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = { Text("Ask AI to generate index.html counter code...", color = Color(0xFF49454F), fontSize = 12.sp) },
                textStyle = TextStyle(color = Color(0xFF1D1B20), fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                maxLines = 4,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    if (promptInput.isNotBlank()) {
                        viewModel.sendChatMessage(promptInput.trim())
                        promptInput = ""
                    }
                },
                enabled = promptInput.isNotBlank() && !viewModel.isSendingChat,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (promptInput.isNotBlank() && !viewModel.isSendingChat) Color(0xFFEADDFF) else Color(0xFF49454F).copy(alpha = 0.15f))
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (promptInput.isNotBlank() && !viewModel.isSendingChat) Color(0xFF21005D) else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ==========================================
// TAB 2: ADVANCED SYNTAX HIGH-LIGHTED EDITOR W/ DRAWER
// ==========================================
@Composable
fun CodeEditorTab(
    viewModel: WorkspaceViewModel,
    files: List<VirtualFile>,
    onNewFileClicked: () -> Unit
) {
    val context = LocalContext.current
    val neonTeal = Color(0xFF6750A4)
    val mutedBorder = Color(0xFFCAC4D0)
    val activeFile = viewModel.activeFile

    // Drawer state (expand/collapsed list of files)
    var isFileDrawerOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Active file summary panel - light theme adaptation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFFFFF), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, mutedBorder), RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { isFileDrawerOpen = !isFileDrawerOpen }
                    .padding(4.dp)
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = null,
                    tint = neonTeal,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = activeFile?.path ?: "No Active File",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF1D1B20),
                    fontSize = 13.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = {
                        if (activeFile != null) {
                            viewModel.saveActiveFileDraft()
                            Toast.makeText(context, "File modifications compiled to virtual drive!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = neonTeal, contentColor = Color.White),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("SAVE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onNewFileClicked,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF3EDF7))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create", tint = Color(0xFF6750A4), modifier = Modifier.size(16.dp))
                }

                if (activeFile != null) {
                    IconButton(
                        onClick = { viewModel.deleteFile(activeFile) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFF5555).copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5555), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Files Sidebar drawer implementation
        AnimatedVisibility(
            visible = isFileDrawerOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                border = BorderStroke(1.dp, mutedBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(
                        text = "VIRTUAL DIRECTORY TREE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF49454F),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(6.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 160.dp)
                    ) {
                        items(files) { file ->
                            val isSelected = activeFile?.path == file.path
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) Color(0xFFEADDFF) else Color.Transparent)
                                    .clickable {
                                        viewModel.selectActiveFile(file)
                                        isFileDrawerOpen = false
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (isSelected) neonTeal else Color(0xFF49454F))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = file.path,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) Color(0xFF21005D) else Color(0xFF1D1B20)
                                    )
                                }

                                Text(
                                    text = file.language.uppercase(),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF49454F)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Real-time Code Editor Workspace text area
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1B20)),
            border = BorderStroke(1.dp, mutedBorder),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (activeFile != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Left gutter lines counter
                    val lines = viewModel.activeFileCodeDraft.lines()
                    val lineNumStr = lines.mapIndexed { index, _ -> "${index + 1}" }.joinToString("\n")

                    Row(modifier = Modifier.fillMaxSize()) {
                        // Lines numbers sidebar
                        Text(
                            text = lineNumStr,
                            color = Color(0xFF938F99),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .width(36.dp)
                                .background(Color(0xFF141218))
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                        )

                        // Real time Highlighted text editor
                        val visualTransformation = VisualTransformation { text ->
                            TransformedText(
                                text = CodeHighlighter.highlight(text.text, activeFile.language),
                                offsetMapping = OffsetMapping.Identity
                            )
                        }

                        OutlinedTextField(
                            value = viewModel.activeFileCodeDraft,
                            onValueChange = { viewModel.activeFileCodeDraft = it },
                            textStyle = TextStyle(
                                color = Color(0xFFE6E1E5),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            ),
                            visualTransformation = visualTransformation,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 4.dp),
                            singleLine = false
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "touch: Generate file descriptors to initiate coding",
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF49454F),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 3: RETRO TERMINAL SHELL SIMULATOR
// ==========================================
@Composable
fun TerminalEmulatorTab(viewModel: WorkspaceViewModel, logs: List<TerminalLog>) {
    var rawInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val context = LocalContext.current

    val consoleBg = Color(0xFF1D1B20)
    val neonTeal = Color(0xFFCFBCFF)
    val terminalGreen = Color(0xFFB1EEA3)
    val mutedBorder = Color(0xFFCAC4D0)
    val dividerBorder = Color(0xFF49454F)

    // Autoscroll terminal on logs updated
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            scrollState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // CLI Shortcuts Row (Geometric Balance suggestion layout)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ls", "gradle build", "npm start", "git status", "help").forEach { cmd ->
                SuggestionChip(
                    onClick = { viewModel.executeTerminalCommand(cmd) },
                    label = { Text(cmd, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF6750A4)) },
                    border = BorderStroke(1.dp, mutedBorder),
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFF3EDF7))
                )
            }
        }

        // Mon Monospace Shell log container
        Card(
            colors = CardDefaults.cardColors(containerColor = consoleBg),
            border = BorderStroke(1.dp, dividerBorder),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                items(logs) { log ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        if (log.command != "system init" && log.command != "file save") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "dev-user@sandbox:~/project$ ",
                                    color = Color(0xFF938F99),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = log.command,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        Text(
                            text = log.output,
                            color = if (log.command.contains("ai patch")) neonTeal else if (log.output.contains("FAILED") || log.output.contains("Error")) Color(0xFFFF8A80) else Color(0xFFE6E1E5),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )

                        // If a gradle build failed, give an AI diagnostic suggestions shortcut right inside logs!
                        if (log.output.contains("compileDebugKotlin FAILED") || log.output.contains("Compilation Error")) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    viewModel.debuggerLogsInput = log.output
                                    Toast.makeText(context, "Error trace fed into AI Debugger!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4), contentColor = Color.White),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PASTE IN AI DEBUGGER", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // CLI Prompt inputs Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(consoleBg, RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, dividerBorder), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "dev-user@sandbox$ ",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = terminalGreen,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = rawInput,
                onValueChange = { rawInput = it },
                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                placeholder = { Text("npm start", color = Color(0xFF938F99), fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    if (rawInput.isNotBlank()) {
                        viewModel.executeTerminalCommand(rawInput.trim())
                        rawInput = ""
                    }
                }),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    if (rawInput.isNotBlank()) {
                        viewModel.executeTerminalCommand(rawInput.trim())
                        rawInput = ""
                    }
                },
                enabled = rawInput.isNotBlank()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Run CLI", tint = if (rawInput.isNotBlank()) terminalGreen else Color(0xFF49454F))
            }
        }
    }
}

// ==========================================
// TAB 4: BUILT-IN INTEL AI DEBUGGER
// ==========================================
@Composable
fun AiDebuggerTab(viewModel: WorkspaceViewModel) {
    val context = LocalContext.current
    val neonTeal = Color(0xFF6750A4)
    val neonOrange = Color(0xFF6750A4)
    val mutedBorder = Color(0xFFCAC4D0)
    val dividerBorder = Color(0xFF49454F)
    val panelBg = Color(0xFFFFFFFF)

    val sampleTraceback = """
FATAL EXCEPTION: main
Process: com.aistudio.devagent.paxlqr, PID: 32014
java.lang.RuntimeException: Unable to start activity ComponentActivity:
at com.example.MainActivity.onCreate(MainActivity.kt:10)
Caused by: java.lang.NotImplementedError: TODO: Implement critical real-time counter
at com.example.MainActivity.onCreate(MainActivity.kt:10)
    """.trimIndent()

    val sampleNodeTraceback = """
Error: CRITICAL_DATABASE_CONNECTION_FAILED: Cannot reach Host pool (10.0.8.2)
    at /sandbox/project/server.js:18:11
    at Layer.handle [as handle_request] (/node_modules/express/lib/router/layer.js:95:5)
    at next (/node_modules/express/lib/router/route.js:144:13)
    """.trimIndent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = panelBg),
            border = BorderStroke(1.dp, mutedBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "⚙️ CRASH LOG TRACEBACK COMPILER",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = neonTeal
                )

                Text(
                    text = "Analyze console crash alerts with AI to generate automated remedies & repair patches.",
                    fontSize = 11.sp,
                    color = Color(0xFF49454F)
                )

                // Demo paste chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = { viewModel.debuggerLogsInput = sampleTraceback },
                        label = { Text("Traceback Demo1", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF6750A4)) },
                        border = BorderStroke(1.dp, mutedBorder),
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFF3EDF7))
                    )
                    SuggestionChip(
                        onClick = { viewModel.debuggerLogsInput = sampleNodeTraceback },
                        label = { Text("Express Demo2", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF6750A4)) },
                        border = BorderStroke(1.dp, mutedBorder),
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFF3EDF7))
                    )
                }

                // Error traces paste area
                OutlinedTextField(
                    value = viewModel.debuggerLogsInput,
                    onValueChange = { viewModel.debuggerLogsInput = it },
                    placeholder = { Text("Paste compilation outputs or Exception stack dumps here...", color = Color(0xFF938F99), fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    textStyle = TextStyle(color = Color(0xFF1D1B20), fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = neonTeal,
                        unfocusedBorderColor = mutedBorder,
                        focusedContainerColor = Color(0xFFF3EDF7),
                        unfocusedContainerColor = Color(0xFFF3EDF7)
                    ),
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )

                Button(
                    onClick = { viewModel.analyzeLogs() },
                    enabled = viewModel.debuggerLogsInput.isNotBlank() && !viewModel.isAnalyzingBug,
                    colors = ButtonDefaults.buttonColors(containerColor = neonTeal, contentColor = Color.White),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (viewModel.isAnalyzingBug) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DIAGNOSING BUG IN WORKSPACE...", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RUN AUTOMATED DIAGNOSTICS", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Diagnostics Outputs Card
        Card(
            colors = CardDefaults.cardColors(containerColor = panelBg),
            border = BorderStroke(1.dp, mutedBorder),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Text(
                    text = "📋 DIAGNOSTIC ARCHIVE REPORT",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = neonTeal,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFF3EDF7), RoundedCornerShape(6.dp))
                        .border(BorderStroke(1.dp, mutedBorder), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    val report = viewModel.debuggerResult
                    if (report == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Run diagnostic stream to construct reports.",
                                color = Color(0xFF938F99),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                // Diagnostic explanations
                                val cleanReport = report
                                    .replace(Regex("\\[FILE_UPDATE:.*?\\]"), "")
                                    .replace("[END_FILE_UPDATE]", "")
                                    .trim()

                                Text(
                                    text = cleanReport,
                                    color = Color(0xFF1D1B20),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    fontFamily = FontFamily.SansSerif
                                )

                                // Check for file update blocks
                                val debuggerPatches = CodeHighlighter.extractFilePatches(report)
                                if (debuggerPatches.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    for (patch in debuggerPatches) {
                                        val filePath = patch.first
                                        val hotCode = patch.second
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1B20)),
                                            border = BorderStroke(1.dp, dividerBorder),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "🔥 RESOLUTION PATCH AVAILABLE",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFFCFBCFF),
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Patch path: $filePath",
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFFE6E1E5)
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Button(
                                                    onClick = {
                                                        viewModel.applyDebuggerPatch(filePath, hotCode)
                                                        Toast.makeText(context, "$filePath patched and resolved!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = neonTeal, contentColor = Color.White),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier.align(Alignment.End),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("APPLY AI HOTFIX BUILD", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
