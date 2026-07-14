package com.example.compiler

import java.util.regex.Pattern

object LaTeXToHtmlConverter {

    fun convert(texCode: String): String {
        var html = texCode

        // 1. Extract metadata
        var title = ""
        var author = ""
        var date = ""

        val titleMatcher = Pattern.compile("\\\\title\\{(.*?)\\}", Pattern.DOTALL).matcher(html)
        if (titleMatcher.find()) {
            title = titleMatcher.group(1).trim()
        }

        val authorMatcher = Pattern.compile("\\\\author\\{(.*?)\\}", Pattern.DOTALL).matcher(html)
        if (authorMatcher.find()) {
            author = authorMatcher.group(1).trim()
        }

        val dateMatcher = Pattern.compile("\\\\date\\{(.*?)\\}", Pattern.DOTALL).matcher(html)
        if (dateMatcher.find()) {
            date = dateMatcher.group(1).trim()
        }

        // Keep track of original text for math rendering but isolate document body
        val docStart = html.indexOf("\\begin{document}")
        if (docStart != -1) {
            html = html.substring(docStart + "\\begin{document}".length)
        }
        val docEnd = html.lastIndexOf("\\end{document}")
        if (docEnd != -1) {
            html = html.substring(0, docEnd)
        }

        // 2. Replacements for simple layout structures
        
        // Remove comments
        html = html.replace("(?m)^\\s*%\\s*.*$".toRegex(), "") // Full-line comments
        html = html.replace("(?m)(?<!\\\\)%.*$".toRegex(), "") // Inline comments (not escaped)

        // \maketitle
        val maketitleHtml = if (title.isNotEmpty()) {
            """
            <div class="maketitle">
                <h1 class="doc-title">$title</h1>
                ${if (author.isNotEmpty()) "<p class='doc-author'>$author</p>" else ""}
                ${if (date.isNotEmpty()) "<p class='doc-date'>$date</p>" else ""}
            </div>
            """.trimIndent()
        } else ""
        html = html.replace("\\maketitle", maketitleHtml)

        // \section{Title} -> Section Headers
        var sectionCounter = 1
        val sectionPattern = Pattern.compile("\\\\section\\s*\\{(.*?)\\}")
        val sectionMatcher = sectionPattern.matcher(html)
        val sectionSb = StringBuffer()
        while (sectionMatcher.find()) {
            val secName = sectionMatcher.group(1)
            sectionMatcher.appendReplacement(sectionSb, "<h2 class='section-title'>$sectionCounter. $secName</h2>")
            sectionCounter++
        }
        sectionMatcher.appendTail(sectionSb)
        html = sectionSb.toString()

        // \section*{Title} -> Unnumbered Section Headers
        html = html.replace("\\\\section\\*\\{(.*?)\\}".toRegex(), "<h2 class='section-title'>$1</h2>")

        // \subsection{Title} -> Subsection Headers
        var subsectionCounter = 1
        val subsectionPattern = Pattern.compile("\\\\subsection\\s*\\{(.*?)\\}")
        val subsectionMatcher = subsectionPattern.matcher(html)
        val subsectionSb = StringBuffer()
        while (subsectionMatcher.find()) {
            val subSecName = subsectionMatcher.group(1)
            subsectionMatcher.appendReplacement(subsectionSb, "<h3 class='subsection-title'>$subSecName</h3>")
            subsectionCounter++
        }
        subsectionMatcher.appendTail(subsectionSb)
        html = subsectionSb.toString()

        // \subsection*{Title} -> Unnumbered Subsection Headers
        html = html.replace("\\\\subsection\\*\\{(.*?)\\}".toRegex(), "<h3 class='subsection-title'>$1</h3>")

        // Text Styles (Bold, Italic, Typewriter, Underline)
        html = html.replace("\\\\textbf\\s*\\{(.*?)\\}".toRegex(), "<strong>$1</strong>")
        html = html.replace("\\\\textit\\s*\\{(.*?)\\}".toRegex(), "<em>$1</em>")
        html = html.replace("\\\\texttt\\s*\\{(.*?)\\}".toRegex(), "<code>$1</code>")
        html = html.replace("\\\\underline\\s*\\{(.*?)\\}".toRegex(), "<u>$1</u>")

        // Lists (Itemize and Enumerate)
        html = html.replace("\\\\begin\\{itemize\\}".toRegex(), "<ul class='itemize-list'>")
        html = html.replace("\\\\end\\{itemize\\}".toRegex(), "</ul>")
        html = html.replace("\\\\begin\\{enumerate\\}".toRegex(), "<ol class='enumerate-list'>")
        html = html.replace("\\\\end\\{enumerate\\}".toRegex(), "</ol>")
        
        // Items in lists
        html = html.replace("\\\\item".toRegex(), "<li>")

        // Spacing, breaks, and formatting
        html = html.replace("\\\\\\\\".toRegex(), "<br/>") // \\ -> line break
        html = html.replace("\\\\vspace\\{(.*?)\\}".toRegex(), "<div style='margin-top: $1;'></div>")
        
        // Handle Center environment
        html = html.replace("\\\\begin\\{center\\}".toRegex(), "<div class='center-align'>")
        html = html.replace("\\\\end\\{center\\}".toRegex(), "</div>")

        // Handle \hfill Sep 2021
        val hfillPattern = Pattern.compile("(?m)^(.*?)\\\\hfill\\s*(.*?)$")
        val hfillMatcher = hfillPattern.matcher(html)
        val hfillSb = StringBuffer()
        while (hfillMatcher.find()) {
            val left = hfillMatcher.group(1)
            val right = hfillMatcher.group(2)
            hfillMatcher.appendReplacement(hfillSb, "<div class='hfill-container'><span>$left</span><span>$right</span></div>")
        }
        hfillMatcher.appendTail(hfillSb)
        html = hfillSb.toString()

        // Clean up unhandled LaTeX commands to prevent "raw LaTeX" in output
        html = html.replace("\\\\usepackage\\{.*?\\}".toRegex(), "")
        html = html.replace("\\\\geometry\\{.*?\\}".toRegex(), "")

        // Build the full HTML template with KaTeX and beautiful CSS styles
        return buildFullHtmlDocument(html, title)
    }

    private fun buildFullHtmlDocument(bodyHtml: String, title: String): String {
        val documentTitle = if (title.isNotEmpty()) title else "LaTeX Document"
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>$documentTitle</title>
            
            <!-- KaTeX Math Rendering (CDN for online, gracefully falls back to beautiful serif offline) -->
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css" crossorigin="anonymous">
            <script src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js" crossorigin="anonymous"></script>
            <script src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js" crossorigin="anonymous"></script>
            
            <style>
                body {
                    font-family: 'Georgia', 'Times New Roman', Times, serif;
                    line-height: 1.6;
                    color: #1a1a1a;
                    background-color: #ffffff;
                    margin: 0;
                    padding: 30px 20px;
                    max-width: 800px;
                    margin-left: auto;
                    margin-right: auto;
                }
                
                /* Title Area Styles */
                .maketitle {
                    text-align: center;
                    margin-bottom: 40px;
                    border-bottom: 1px solid #ddd;
                    padding-bottom: 20px;
                }
                .doc-title {
                    font-size: 2.2em;
                    font-weight: bold;
                    margin-bottom: 10px;
                    color: #2c3e50;
                }
                .doc-author {
                    font-size: 1.2em;
                    font-style: italic;
                    margin: 5px 0;
                    color: #555;
                }
                .doc-date {
                    font-size: 1.1em;
                    color: #777;
                    margin-top: 5px;
                }
                
                /* Headings */
                .section-title {
                    font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                    font-size: 1.5em;
                    color: #2c3e50;
                    border-bottom: 1px solid #eee;
                    padding-bottom: 5px;
                    margin-top: 30px;
                }
                .subsection-title {
                    font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                    font-size: 1.2em;
                    color: #34495e;
                    margin-top: 20px;
                }
                
                /* Content Styles */
                p {
                    margin-bottom: 15px;
                    text-align: justify;
                }
                
                /* Lists */
                ul.itemize-list, ol.enumerate-list {
                    padding-left: 20px;
                    margin-bottom: 20px;
                }
                li {
                    margin-bottom: 8px;
                }
                
                /* Code and inline formatting */
                code {
                    font-family: 'Courier New', Courier, monospace;
                    background-color: #f8f9fa;
                    padding: 2px 4px;
                    border-radius: 4px;
                    font-size: 0.9em;
                }
                
                /* Centering */
                .center-align {
                    text-align: center;
                    margin: 15px 0;
                }
                
                /* Right align / flex spacing for \hfill */
                .hfill-container {
                    display: flex;
                    justify-content: space-between;
                    margin-bottom: 5px;
                    width: 100%;
                }
                
                /* Offline mathematical fallback */
                .katex {
                    font-size: 1.1em;
                }
                
                /* Print styles */
                @media print {
                    body {
                        padding: 0;
                        margin: 0;
                        font-size: 12pt;
                        background: white;
                    }
                    .maketitle {
                        border-bottom: 1px solid #000;
                    }
                }
            </style>
        </head>
        <body>
            <div class="latex-content">
                $bodyHtml
            </div>
            
            <script>
                document.addEventListener("DOMContentLoaded", function() {
                    if (typeof renderMathInElement === "function") {
                        renderMathInElement(document.body, {
                            delimiters: [
                                {left: "$$", right: "$$", display: true},
                                {left: "$", right: "$", display: false},
                                {left: "\\(", right: "\\)", display: false},
                                {left: "\\[", right: "\\]", display: true},
                                {left: "\\begin{equation}", right: "\\end{equation}", display: true}
                            ],
                            throwOnError: false
                        });
                    } else {
                        console.log("KaTeX CDN failed to load (offline). Displaying standard math-serif fallback.");
                        var contentDiv = document.querySelector(".latex-content");
                        if (contentDiv) {
                            // Find all single '$' or equation elements and style them
                            var html = contentDiv.innerHTML;
                            // Basic inline math styling fallback via CSS replacement
                            html = html.replace(/\$([^\$]+)\$/g, '<span style="font-family: \'Times New Roman\', serif; font-style: italic; color: #2c3e50;">$1</span>');
                            contentDiv.innerHTML = html;
                        }
                    }
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}
