package com.example.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import java.util.regex.Pattern

object CodeHighlighter {
    // Elegant theme for syntax highlighting
    private val KeywordColor = Color(0xFFFF79C6) // Pink
    private val TypeColor = Color(0xFF8BE9FD) // Cyan
    private val ValVarColor = Color(0xFF50FA7B) // Green
    private val StringColor = Color(0xFFF1FA8C) // Yellow
    private val NumberColor = Color(0xFFBD93F9) // Purple
    private val CommentColor = Color(0xFF6272A4) // Slate Grey
    private val AnnotationColor = Color(0xFFFFB86C) // Orange
    private val DefaultColor = Color(0xFFE6EDF0) // Off-white

    fun highlight(code: String, language: String): AnnotatedString {
        return buildAnnotatedString {
            // Start by pushing default text
            append(code)
            
            // Apply Mono font to everything
            addStyle(
                style = SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    color = DefaultColor
                ),
                start = 0,
                end = code.length
            )

            try {
                // Compile regex patterns based on language rules
                val keywords = when (language.lowercase()) {
                    "kotlin" -> listOf(
                        "package", "import", "class", "interface", "fun", "val", "var",
                        "return", "override", "null", "true", "false", "this", "super",
                        "if", "else", "when", "for", "while", "do", "try", "catch", "throw",
                        "private", "public", "protected", "internal", "object", "companion", "init"
                    )
                    "javascript" -> listOf(
                        "const", "let", "var", "function", "return", "if", "else", "for",
                        "while", "do", "switch", "case", "break", "continue", "default",
                        "import", "export", "from", "class", "extends", "constructor", "new",
                        "this", "null", "undefined", "true", "false", "throw", "try", "catch", "require"
                    )
                    "html" -> listOf(
                        "doctype", "html", "head", "body", "title", "div", "span", "p", "h1",
                        "h2", "h3", "h4", "h5", "h6", "a", "img", "button", "input", "script",
                        "style", "link", "meta", "ul", "ol", "li"
                    )
                    "json" -> listOf("true", "false", "null")
                    else -> emptyList()
                }

                // Highlight Keywords
                if (keywords.isNotEmpty()) {
                    val keywordPattern = Pattern.compile("\\b(" + keywords.joinToString("|") + ")\\b")
                    val kMatcher = keywordPattern.matcher(code)
                    while (kMatcher.find()) {
                        addStyle(
                            style = SpanStyle(color = KeywordColor, fontWeight = FontWeight.Bold),
                            start = kMatcher.start(),
                            end = kMatcher.end()
                        )
                    }
                }

                // Highlight Strings ("..." or '...')
                val stringPattern = Pattern.compile("\".*?\"|'.*?'")
                val sMatcher = stringPattern.matcher(code)
                while (sMatcher.find()) {
                    addStyle(
                        style = SpanStyle(color = StringColor),
                        start = sMatcher.start(),
                        end = sMatcher.end()
                    )
                }

                // Highlight Single Line Comments (// ...)
                val commentPattern = Pattern.compile("//.*")
                val cMatcher = commentPattern.matcher(code)
                while (cMatcher.find()) {
                    addStyle(
                        style = SpanStyle(color = CommentColor),
                        start = cMatcher.start(),
                        end = cMatcher.end()
                    )
                }

                // Highlight numbers
                val numberPattern = Pattern.compile("\\b\\d+\\b")
                val nMatcher = numberPattern.matcher(code)
                while (nMatcher.find()) {
                    addStyle(
                        style = SpanStyle(color = NumberColor),
                        start = nMatcher.start(),
                        end = nMatcher.end()
                    )
                }

                // Highlight Kotlin Annotations (@...)
                if (language.lowercase() == "kotlin") {
                    val annotationPattern = Pattern.compile("@[a-zA-Z0-9_]+")
                    val aMatcher = annotationPattern.matcher(code)
                    while (aMatcher.find()) {
                        addStyle(
                            style = SpanStyle(color = AnnotationColor),
                            start = aMatcher.start(),
                            end = aMatcher.end()
                        )
                    }
                }

            } catch (e: Exception) {
                // Fallback graceful parsing if regex fails
            }
        }
    }

    fun extractFilePatches(text: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val regex = Regex("\\[FILE_UPDATE:([^\\]]+)\\]((?s).*?)\\[END_FILE_UPDATE\\]")
        val matches = regex.findAll(text)
        for (match in matches) {
            val path = match.groups[1]?.value?.trim() ?: continue
            val code = match.groups[2]?.value ?: ""
            result.add(Pair(path, code))
        }
        return result
    }
}
