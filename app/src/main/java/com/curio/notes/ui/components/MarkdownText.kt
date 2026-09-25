package com.curio.notes.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle

private val BoldPattern = Regex("\\*\\*(.+?)\\*\\*")

// Renders **bold** markers as real bold spans. Anything else (including
// unclosed markers) stays literal text. Pure and unit-tested.
fun String.toBoldAnnotated(): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (match in BoldPattern.findAll(this@toBoldAnnotated)) {
        append(this@toBoldAnnotated.substring(last, match.range.first))
        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
        append(match.groupValues[1])
        pop()
        last = match.range.last + 1
    }
    append(this@toBoldAnnotated.substring(last))
}

// Drop-in Text replacement for AI-generated content, which often arrives
// with **markers** despite the plain-text prompt rule.
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val annotated = remember(text) { text.toBoldAnnotated() }
    Text(
        text = annotated,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        modifier = modifier
    )
}
