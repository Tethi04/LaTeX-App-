package com.example.ui

import android.app.Application
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.LaTeXDocument
import com.example.data.LaTeXRepository
import com.example.compiler.LaTeXToHtmlConverter
import com.example.network.GeminiClient
import com.example.network.LaTeXCompilerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed interface CompilationState {
    object Idle : CompilationState
    object Loading : CompilationState
    data class Success(
        val pdfBytes: ByteArray?,
        val htmlContent: String,
        val isOffline: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    ) : CompilationState
    data class Error(val errorMessage: String) : CompilationState
}

sealed interface AiAssistantState {
    object Idle : AiAssistantState
    object Loading : AiAssistantState
    data class Success(val message: String, val codeToApply: String? = null) : AiAssistantState
    data class Error(val errorMessage: String) : AiAssistantState
}

class LaTeXViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LaTeXRepository

    val allDocuments: StateFlow<List<LaTeXDocument>>
    
    private val _currentDocument = MutableStateFlow<LaTeXDocument?>(null)
    val currentDocument: StateFlow<LaTeXDocument?> = _currentDocument.asStateFlow()

    private val _compilationState = MutableStateFlow<CompilationState>(CompilationState.Idle)
    val compilationState: StateFlow<CompilationState> = _compilationState.asStateFlow()

    private val _aiAssistantState = MutableStateFlow<AiAssistantState>(AiAssistantState.Idle)
    val aiAssistantState: StateFlow<AiAssistantState> = _aiAssistantState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = LaTeXRepository(database.latexDocumentDao())
        
        allDocuments = repository.allDocuments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Prepopulate standard LaTeX templates
        viewModelScope.launch(Dispatchers.IO) {
            repository.prepopulateTemplatesIfNeeded()
        }
    }

    fun selectDocument(document: LaTeXDocument) {
        _currentDocument.value = document
        _compilationState.value = CompilationState.Idle
        _aiAssistantState.value = AiAssistantState.Idle
    }

    fun clearSelectedDocument() {
        _currentDocument.value = null
        _compilationState.value = CompilationState.Idle
        _aiAssistantState.value = AiAssistantState.Idle
    }

    fun updateCurrentDocumentCode(newCode: String) {
        _currentDocument.value?.let { doc ->
            _currentDocument.value = doc.copy(code = newCode, updatedAt = System.currentTimeMillis())
        }
    }

    fun saveCurrentDocument(onSuccess: (() -> Unit)? = null) {
        val doc = _currentDocument.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (doc.id == 0) {
                val newId = repository.insert(doc)
                _currentDocument.value = doc.copy(id = newId.toInt())
            } else {
                repository.update(doc)
            }
            withContext(Dispatchers.Main) {
                onSuccess?.invoke()
            }
        }
    }

    fun createNewDocument(title: String, category: String, initialCode: String = "") {
        val defaultCode = if (initialCode.isNotEmpty()) initialCode else """\documentclass{article}
\title{$title}
\author{Author}
\date{\today}
\begin{document}
\maketitle

\section{Introduction}
Write your LaTeX document here. This app compiles both offline and online.

\end{document}"""

        val newDoc = LaTeXDocument(
            title = title,
            code = defaultCode,
            category = category,
            isTemplate = false
        )

        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.insert(newDoc)
            val insertedDoc = newDoc.copy(id = newId.toInt())
            withContext(Dispatchers.Main) {
                _currentDocument.value = insertedDoc
                _compilationState.value = CompilationState.Idle
                _aiAssistantState.value = AiAssistantState.Idle
            }
        }
    }

    fun deleteDocument(document: LaTeXDocument) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(document)
            if (_currentDocument.value?.id == document.id) {
                _currentDocument.value = null
            }
        }
    }

    fun compileOffline() {
        val doc = _currentDocument.value ?: return
        _compilationState.value = CompilationState.Loading
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val html = LaTeXToHtmlConverter.convert(doc.code)
                _compilationState.value = CompilationState.Success(
                    pdfBytes = null,
                    htmlContent = html,
                    isOffline = true
                )
            } catch (e: Exception) {
                _compilationState.value = CompilationState.Error(
                    "Offline Compilation Error: ${e.localizedMessage ?: e.message}"
                )
            }
        }
    }

    fun compileOnline() {
        val doc = _currentDocument.value ?: return
        _compilationState.value = CompilationState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val result = LaTeXCompilerService.compileOnline(doc.code)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { pdfBytes ->
                        val htmlFallback = LaTeXToHtmlConverter.convert(doc.code)
                        _compilationState.value = CompilationState.Success(
                            pdfBytes = pdfBytes,
                            htmlContent = htmlFallback,
                            isOffline = false
                        )
                    },
                    onFailure = { error ->
                        _compilationState.value = CompilationState.Error(
                            "Online Compilation Failed.\n\nLOG:\n${error.message}\n\nFalling back to offline preview..."
                        )
                    }
                )
            }
        }
    }

    // --- AI Assistant Logic via Gemini ---

    fun generateLaTexWithAi(userPrompt: String) {
        _aiAssistantState.value = AiAssistantState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val systemInstruction = """
                You are a senior LaTeX engineer. Generate full, valid, beautiful LaTeX code.
                Always include \documentclass, \begin{document}, headings, and \end{document}.
                Ensure that there is absolutely no markdown intro or outro explanation outside the LaTeX code itself, unless it is commented with `%`.
                Your entire response must be a valid, compilable LaTeX document.
            """.trimIndent()
            
            val response = GeminiClient.getAiLaTeXResponse(userPrompt, systemInstruction)
            
            withContext(Dispatchers.Main) {
                if (response.startsWith("Error") || response.startsWith("API Call Failed")) {
                    _aiAssistantState.value = AiAssistantState.Error(response)
                } else {
                    // Extract code block if AI wrapped it in ```latex ... ```
                    val cleanedCode = cleanCodeBlocks(response)
                    _aiAssistantState.value = AiAssistantState.Success(
                        message = "Successfully generated LaTeX template for you!",
                        codeToApply = cleanedCode
                    )
                }
            }
        }
    }

    fun fixLaTeXErrorsWithAi(compileErrorLog: String) {
        val currentCode = _currentDocument.value?.code ?: return
        _aiAssistantState.value = AiAssistantState.Loading
        
        viewModelScope.launch(Dispatchers.IO) {
            val prompt = """
                The following LaTeX code failed to compile.
                
                --- ERROR LOG ---
                $compileErrorLog
                
                --- ORIGINAL CODE ---
                $currentCode
                
                Please correct the syntax or structural errors in the original code. 
                Return the fully corrected, compilable LaTeX document. 
                Do not include raw chat explanations in markdown. Return ONLY the valid compilable LaTeX code.
            """.trimIndent()

            val systemInstruction = "You are a professional LaTeX debugging compiler assistant. Correct errors, clean up bad environments, and output valid, full LaTeX code."
            val response = GeminiClient.getAiLaTeXResponse(prompt, systemInstruction)

            withContext(Dispatchers.Main) {
                if (response.startsWith("Error") || response.startsWith("API Call Failed")) {
                    _aiAssistantState.value = AiAssistantState.Error(response)
                } else {
                    val cleanedCode = cleanCodeBlocks(response)
                    _aiAssistantState.value = AiAssistantState.Success(
                        message = "AI fixed the compilation errors successfully!",
                        codeToApply = cleanedCode
                    )
                }
            }
        }
    }

    fun clearAiState() {
        _aiAssistantState.value = AiAssistantState.Idle
    }

    private fun cleanCodeBlocks(rawText: String): String {
        var clean = rawText.trim()
        if (clean.startsWith("```latex")) {
            clean = clean.substringAfter("```latex")
        } else if (clean.startsWith("```tex")) {
            clean = clean.substringAfter("```tex")
        } else if (clean.startsWith("```")) {
            clean = clean.substringAfter("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.substringBeforeLast("```")
        }
        return clean.trim()
    }

    // --- Native PDF Printing/Saving ---

    fun exportPdfOffline(context: Context, htmlContent: String, title: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val printAdapter = webView.createPrintDocumentAdapter(title)
                    val jobName = "${title.replace(" ", "_")}_compiled"
                    printManager.print(
                        jobName, 
                        printAdapter, 
                        PrintAttributes.Builder().build()
                    )
                }
            }
            // Load HTML locally, including cdn script tag
            webView.loadDataWithBaseURL("https://cdn.jsdelivr.net/", htmlContent, "text/html", "UTF-8", null)
        }
    }

    fun saveDownloadedPdf(context: Context, pdfBytes: ByteArray, title: String): File? {
        val fileName = "${title.replace(" ", "_")}_online.pdf"
        val downloadsDir = context.getExternalFilesDir(null) ?: return null
        val pdfFile = File(downloadsDir, fileName)
        return try {
            FileOutputStream(pdfFile).use { fos ->
                fos.write(pdfBytes)
            }
            pdfFile
        } catch (e: Exception) {
            null
        }
    }
}
