package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.data.LaTeXDocument
import com.example.ui.AiAssistantState
import com.example.ui.CompilationState
import com.example.ui.LaTeXViewModel
import com.example.ui.theme.MyApplicationTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: LaTeXViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: LaTeXViewModel) {
    val currentDoc by viewModel.currentDocument.collectAsState()
    val compilationState by viewModel.compilationState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (currentDoc == null) {
            DocumentListScreen(viewModel = viewModel)
        } else {
            if (compilationState is CompilationState.Success) {
                PreviewScreen(
                    viewModel = viewModel,
                    successState = compilationState as CompilationState.Success
                )
            } else {
                EditorScreen(viewModel = viewModel)
            }
        }
    }
}

// ==========================================
// 1. DOCUMENT LIST SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentListScreen(viewModel: LaTeXViewModel) {
    val documents by viewModel.allDocuments.collectAsState()
    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val savedDocs = documents.filter { !it.isTemplate }
    val templateDocs = documents.filter { it.isTemplate }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "L",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                            Text(
                                text = "LaTeX Studio",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    actions = {
                        IconButton(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "LaTeX Studio compiles offline & online PDFs. Created with Gemini.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Help Info",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .testTag("create_doc_fab")
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Document"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- Premium Header Banner ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI-Powered LaTeX Compiler",
                            color = Color(0xFFD0BCFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Compile offline instantly, sync online for standard LaTeX PDF, and fix errors with Gemini assistant.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // --- Toggle TabRow ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp)),
                indicator = { @Composable {} }, // Hide the default bottom line indicator
                divider = {} // Hide the default divider
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My Documents", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    modifier = Modifier.background(if (selectedTab == 0) Color(0xFFE8DEF8) else Color.Transparent),
                    selectedContentColor = Color(0xFF1D192B),
                    unselectedContentColor = Color(0xFF49454F)
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Templates", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    modifier = Modifier.background(if (selectedTab == 1) Color(0xFFE8DEF8) else Color.Transparent),
                    selectedContentColor = Color(0xFF1D192B),
                    unselectedContentColor = Color(0xFF49454F)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val docsToShow = if (selectedTab == 0) savedDocs else templateDocs

            if (docsToShow.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.NoteAdd,
                            contentDescription = "Empty Documents",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTab == 0) "No Documents Found" else "No Templates Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedTab == 0) "Create your very first LaTeX file or check out professional templates!" else "Initial setup in progress...",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        if (selectedTab == 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Create Document")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(docsToShow, key = { it.id }) { doc ->
                        DocumentItemCard(
                            document = doc,
                            onSelect = { viewModel.selectDocument(doc) },
                            onDelete = { viewModel.deleteDocument(doc) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var docTitle by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("Letter") }
        val categories = listOf("Letter", "Article", "CV/Resume", "Academic", "Lab Report")

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New LaTeX Document", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { docTitle = it },
                        label = { Text("Document Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("doc_title_input")
                    )

                    Text("Select Category:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            val isSelected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.1f
                                        )
                                    )
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (docTitle.isNotBlank()) {
                            viewModel.createNewDocument(docTitle, selectedCategory)
                            showCreateDialog = false
                        } else {
                            Toast.makeText(context, "Please enter a valid title", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DocumentItemCard(
    document: LaTeXDocument,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
    val formattedDate = sdf.format(Date(document.updatedAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("document_card_${document.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (document.isTemplate) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (document.isTemplate) Icons.Default.Layers else Icons.Default.Code,
                    contentDescription = null,
                    tint = if (document.isTemplate) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = document.category,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            if (!document.isTemplate) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Document",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. LATEX CODE EDITOR SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: LaTeXViewModel) {
    val document by viewModel.currentDocument.collectAsState()
    val aiState by viewModel.aiAssistantState.collectAsState()
    val context = LocalContext.current

    if (document == null) return

    var codeValue by remember(document?.id) {
        mutableStateOf(TextFieldValue(document!!.code))
    }

    var showAiAssistant by remember { mutableStateOf(false) }

    val onCodeChange: (TextFieldValue) -> Unit = { newValue ->
        codeValue = newValue
        viewModel.updateCurrentDocumentCode(newValue.text)
    }

    val insertSnippet: (String) -> Unit = { snippet ->
        val selection = codeValue.selection
        val text = codeValue.text
        val newText = StringBuilder(text)
            .replace(selection.start, selection.end, snippet)
            .toString()
        val newCursorPos = selection.start + snippet.length
        val updatedValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPos, newCursorPos)
        )
        codeValue = updatedValue
        viewModel.updateCurrentDocumentCode(newText)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = document!!.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF22C55E), CircleShape)
                                )
                                Text(
                                    text = "OFFLINE READY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelectedDocument() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.saveCurrentDocument {
                                    Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save Document",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showAiAssistant = true }) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Assistant",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LaTeXInsertToolbar(onInsertSnippet = insertSnippet)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Line numbers gutter
                    val lineCount = maxOf(1, codeValue.text.split("\n").size)
                    val lineNumbersText = (1..lineCount).joinToString("\n") { it.toString() }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(36.dp)
                            .background(Color(0xFFF7F2FA))
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Text(
                            text = lineNumbersText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFF49454F),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Vertical gutter divider boundary
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(Color(0xFFCAC4D0))
                    )

                    // Text editor field
                    OutlinedTextField(
                        value = codeValue,
                        onValueChange = onCodeChange,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .testTag("latex_code_editor"),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Color(0xFF1D1B20),
                            lineHeight = 20.sp
                        ),
                        placeholder = {
                            Text(
                                "\\documentclass{article}...",
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.None
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            // High Density Compilation Status Area
            val compState by viewModel.compilationState.collectAsState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE7E0EC))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (compState) {
                        is CompilationState.Idle -> {
                            Text(text = "⚡", fontSize = 12.sp, color = Color(0xFF49454F))
                            Text(
                                text = "Ready for compilation. Choose option below.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF49454F),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        is CompilationState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF6750A4)
                            )
                            Text(
                                text = "Compiling document source...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF49454F),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        is CompilationState.Error -> {
                            Text(text = "❌", fontSize = 11.sp, color = Color(0xFFB91C1C))
                            Text(
                                text = "Error: ${(compState as CompilationState.Error).errorMessage}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB91C1C),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        is CompilationState.Success -> {
                            Text(text = "✔", fontSize = 12.sp, color = Color(0xFF15803D))
                            Text(
                                text = "Compilation finished successfully.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF15803D),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.saveCurrentDocument {
                            viewModel.compileOffline()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("compile_offline_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0))
                ) {
                    Icon(imageVector = Icons.Default.Code, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Offline Preview", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        viewModel.saveCurrentDocument {
                            viewModel.compileOnline()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("compile_online_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compile (Online)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAiAssistant) {
        var aiPrompt by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                viewModel.clearAiState()
                showAiAssistant = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini LaTeX Assistant", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Describe what you want to write (e.g., 'Make a resume for a biology teacher' or 'Write standard Euler formula in LaTeX block math') and Gemini will generate full compilable code.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    OutlinedTextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        label = { Text("Describe code or template...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    when (val state = aiState) {
                        is AiAssistantState.Loading -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gemini is composing LaTeX...", fontSize = 13.sp)
                            }
                        }
                        is AiAssistantState.Success -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = state.message,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ready to insert ${state.codeToApply?.length ?: 0} characters of custom LaTeX code.",
                                    fontSize = 12.sp
                                )
                            }
                        }
                        is AiAssistantState.Error -> {
                            Text(
                                text = state.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (aiState is AiAssistantState.Success) {
                        val applyCode = (aiState as AiAssistantState.Success).codeToApply
                        Button(
                            onClick = {
                                if (applyCode != null) {
                                    onCodeChange(TextFieldValue(applyCode))
                                    viewModel.clearAiState()
                                    showAiAssistant = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text("Insert Code")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (aiPrompt.isNotBlank()) {
                                    viewModel.generateLaTexWithAi(aiPrompt)
                                }
                            },
                            enabled = aiPrompt.isNotBlank() && aiState !is AiAssistantState.Loading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Generate")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAiState()
                        showAiAssistant = false
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun LaTeXInsertToolbar(onInsertSnippet: (String) -> Unit) {
    val items = listOf(
        Pair("\\", "\\"),
        Pair("{ }", "{}"),
        Pair("[ ]", "[]"),
        Pair("$ $", "$$"),
        Pair("_", "_"),
        Pair("^", "^"),
        Pair("%", "%"),
        Pair("Section", "\\section{Section Name}\n"),
        Pair("Sub-section", "\\subsection{Subsection Name}\n"),
        Pair("Bold", "\\textbf{text}"),
        Pair("Italic", "\\textit{text}"),
        Pair("List Itemize", "\\begin{itemize}\n  \\item First item\n  \\item Second item\n\\end{itemize}\n"),
        Pair("List Enumerate", "\\begin{enumerate}\n  \\item First item\n  \\item Second item\n\\end{enumerate}\n"),
        Pair("Math Equation", "\\begin{equation}\n  E = m c^2\n\\end{equation}\n"),
        Pair("Underline", "\\underline{text}"),
        Pair("Center Text", "\\begin{center}\n  Centered Text\n\\end{center}\n"),
        Pair("Line Break", "\\\\\n")
    )

    Column {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3EDF7))
                .padding(vertical = 6.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(items) { item ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8DEF8))
                        .clickable { onInsertSnippet(item.second) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = item.first,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D192B)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFCAC4D0))
        )
    }
}

// ==========================================
// 3. COMPILE PREVIEW / RENDER SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    viewModel: LaTeXViewModel,
    successState: CompilationState.Success
) {
    val document by viewModel.currentDocument.collectAsState()
    val context = LocalContext.current

    if (document == null) return

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = document!!.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (successState.isOffline) Color(0xFFF59E0B) else Color(0xFF10B981),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (successState.isOffline) "Offline Preview" else "Online PDF Compiled",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (successState.isOffline) Color(0xFFF59E0B) else Color(0xFF10B981)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            viewModel.selectDocument(document!!)
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Edit"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.exportPdfOffline(
                                    context,
                                    successState.htmlContent,
                                    document!!.title
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Export PDF",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        if (successState.pdfBytes != null) {
                            IconButton(
                                onClick = {
                                    val savedFile = viewModel.saveDownloadedPdf(
                                        context,
                                        successState.pdfBytes,
                                        document!!.title
                                    )
                                    if (savedFile != null) {
                                        Toast.makeText(
                                            context,
                                            "Saved compiled PDF to app workspace!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        sharePdfFile(context, savedFile)
                                    } else {
                                        Toast.makeText(context, "Failed to save PDF.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Download Online PDF",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFCAC4D0))
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (successState.isOffline) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (successState.isOffline) "Offline Local Renderer Active" else "Full PDF ready for download",
                    fontSize = 12.sp,
                    color = if (successState.isOffline) Color(0xFF92400E) else Color(0xFF065F46),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (successState.isOffline) "TAP PDF TO PRINT" else "ONLINE SERVER",
                    fontSize = 10.sp,
                    color = if (successState.isOffline) Color(0xFFB45309) else Color(0xFF047857),
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .clickable {
                            viewModel.exportPdfOffline(
                                context,
                                successState.htmlContent,
                                document!!.title
                            )
                        }
                        .border(
                            1.dp,
                            if (successState.isOffline) Color(0xFFB45309) else Color(0xFF047857),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            LaTeXWebView(
                htmlContent = successState.htmlContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("preview_webview")
            )

            Button(
                onClick = { viewModel.selectDocument(document!!) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Return to Source Editor", fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun sharePdfFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share LaTeX Compiled PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "Sharing failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// ==========================================
// 4. WEBVIEW COMPOSE WRAPPER
// ==========================================

@Composable
fun LaTeXWebView(htmlContent: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            android.webkit.WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                webViewClient = android.webkit.WebViewClient()
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://cdn.jsdelivr.net/", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}
