package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LaTeXRepository(private val dao: LaTeXDocumentDao) {
    val allDocuments: Flow<List<LaTeXDocument>> = dao.getAllDocuments()

    suspend fun getDocumentById(id: Int): LaTeXDocument? = dao.getDocumentById(id)

    fun getDocumentByIdFlow(id: Int): Flow<LaTeXDocument?> = dao.getDocumentByIdFlow(id)

    suspend fun insert(document: LaTeXDocument): Long = dao.insertDocument(document)

    suspend fun update(document: LaTeXDocument) = dao.updateDocument(document)

    suspend fun delete(document: LaTeXDocument) = dao.deleteDocument(document)

    suspend fun deleteById(id: Int) = dao.deleteDocumentById(id)

    suspend fun prepopulateTemplatesIfNeeded() {
        if (dao.getCount() == 0) {
            // Add academic paper template
            dao.insertDocument(
                LaTeXDocument(
                    title = "Academic Paper Template",
                    code = """\documentclass{article}
\usepackage{amsmath}

\title{An Elegant Study of Math and Physics}
\author{A. Einstein}
\date{\today}

\begin{document}
\maketitle

\section{Introduction}
Welcome to your LaTeX compiler! This document compiles perfectly both online and offline.

\section{Mathematical Formulations}
Here is an example of a beautiful mathematical formulation:
\begin{equation}
E = m c^2
\end{equation}

And here is the Schrödinger equation in a quantum system:
\begin{equation}
i \hbar \frac{\partial}{\partial t} \Psi(x,t) = \left[ -\frac{\hbar^2}{2m}\frac{\partial^2}{\partial x^2} + V(x,t) \right] \Psi(x,t)
\end{equation}

\section{Conclusion}
Start editing this template to write your scientific documents!
\end{document}""",
                    category = "Academic",
                    isTemplate = true
                )
            )

            // Add professional CV template
            dao.insertDocument(
                LaTeXDocument(
                    title = "Professional Resume Template",
                    code = """\documentclass{article}

\begin{document}
\begin{center}
    {\Huge \textbf{John Doe}} \\
    \vspace{2mm}
    Software Engineer | Boston, MA | john.doe@email.com
\end{center}

\section*{Education}
\textbf{Boston University} \hfill Sep 2021 -- May 2025 \\
Bachelor of Science in Computer Science

\section*{Experience}
\textbf{Tech Solutions} -- Software Engineering Intern \hfill May 2024 -- Aug 2024
\begin{itemize}
    \item Developed key-value persistent modules for Android applications.
    \item Optimized vector rendering flows resulting in a 20% performance boost.
\end{itemize}

\section*{Skills}
\textbf{Languages:} Kotlin, Java, LaTeX, HTML/CSS \\
\textbf{Frameworks:} Jetpack Compose, Room Database
\end{document}""",
                    category = "Resume",
                    isTemplate = true
                )
            )

            // Add simple letter template
            dao.insertDocument(
                LaTeXDocument(
                    title = "Business Letter Template",
                    code = """\documentclass{letter}

\begin{document}
\textbf{Alex Morgan} \\
123 Creative Lane \\
Science City \\
\vspace{5mm}

\textbf{To: The LaTeX Compiler Team} \\
Silicon Valley \\
\vspace{5mm}

Dear Reader,

This letter was written and compiled entirely on my Android device using the offline LaTeX compiler.

The app supports live math previews, template selection, and exporting high-quality vector PDFs directly.

Sincerely, \\
Alex Morgan
\end{document}""",
                    category = "Letter",
                    isTemplate = true
                )
            )

            // Add Lab Report template
            val d = '$'
            dao.insertDocument(
                LaTeXDocument(
                    title = "Physics Lab Report",
                    code = """\documentclass{article}
\begin{document}
\title{Physics Lab: Gravity Acceleration}
\author{Your Name}
\date{\today}
\maketitle

\section{Objective}
To measure the acceleration due to Earth's gravity (${d}g${d}) using a simple pendulum.

\section{Theory}
The period ${d}T${d} of a simple pendulum is given by:
\[ T = 2\pi \sqrt{\frac{L}{g}} \]

By squaring both sides, we get:
\[ g = \frac{4\pi^2 L}{T^2} \]

\section{Results}
With ${d}L = 1.0${d} meter and ${d}T = 2.01${d} seconds, we calculate:
\[ g \approx 9.77 \text{ m/s}^2 \]
\end{document}""",
                    category = "Lab Report",
                    isTemplate = true
                )
            )
        }
    }
}
