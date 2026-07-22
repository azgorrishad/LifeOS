package com.example.ui.notes

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.NoteEntity
import com.example.feature.dashboard.presentation.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesDialog(
    notes: List<NoteEntity>,
    onDismiss: () -> Unit,
    onAddNote: (String, String) -> Unit,
    onUpdateNote: (NoteEntity) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedNote by remember { mutableStateOf<NoteEntity?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }

    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) {
            notes
        } else {
            notes.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.content.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (isCreating) {
        NoteEditorDialog(
            note = null,
            onDismiss = { isCreating = false },
            onSave = { title, content ->
                onAddNote(title, content)
                isCreating = false
            }
        )
    } else if (isEditing && selectedNote != null) {
        NoteEditorDialog(
            note = selectedNote,
            onDismiss = { isEditing = false },
            onSave = { title, content ->
                onUpdateNote(selectedNote!!.copy(title = title, content = content))
                isEditing = false
                selectedNote = null
            }
        )
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LifeOS Notes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search & Add Note Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search notes...") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        FloatingActionButton(
                            onClick = { isCreating = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "New Note")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedNote != null) {
                        // Note Reader Mode
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { selectedNote = null }) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to list")
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(onClick = { isEditing = true }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit note", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = {
                                        onDeleteNote(selectedNote!!.id)
                                        selectedNote = null
                                    }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete note", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = selectedNote!!.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Text(
                                text = "Last updated: " + SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                                    .format(Date(selectedNote!!.updatedAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp)
                            ) {
                                MarkdownRenderer(content = selectedNote!!.content)
                            }
                        }
                    } else {
                        // Note List Mode
                        if (filteredNotes.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Notes,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "No notes yet" else "No matching notes found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (searchQuery.isBlank()) "Create a note to persist thoughts, checklists, or quick summaries." else "Try adjusting your keywords",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 32.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredNotes, key = { it.id }) { note ->
                                    Card(
                                        onClick = { selectedNote = note },
                                        modifier = Modifier.fillMaxWidth().animateItem(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = note.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(note.updatedAt)),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = note.content,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorDialog(
    note: NoteEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var isPreviewTab by remember { mutableStateOf(false) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (note == null) "Create Note" else "Edit Note",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Note Title", style = MaterialTheme.typography.titleMedium) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab selector for Edit vs Preview
                    TabRow(
                        selectedTabIndex = if (isPreviewTab) 1 else 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[if (isPreviewTab) 1 else 0]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        Tab(
                            selected = !isPreviewTab,
                            onClick = { isPreviewTab = false },
                            text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Edit Markdown")
                            }}
                        )
                        Tab(
                            selected = isPreviewTab,
                            onClick = { isPreviewTab = true },
                            text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Live Preview")
                            }}
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Content Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (isPreviewTab) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                MarkdownRenderer(content = if (content.isBlank()) "*No content written yet. Tap 'Edit Markdown' to add thoughts.*" else content)
                            }
                        } else {
                            OutlinedTextField(
                                value = content,
                                onValueChange = { content = it },
                                placeholder = { Text("Write content using # for titles, ** for bold, - for lists, etc...") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxSize(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onSave(title, content)
                                }
                            },
                            enabled = title.isNotBlank(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Note")
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun MarkdownRenderer(content: String) {
    val colorScheme = MaterialTheme.colorScheme
    val lines = remember(content) { content.split("\n") }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        var inCodeBlock = false
        val currentCodeBlock = StringBuilder()

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("```")) {
                if (inCodeBlock) {
                    // Render accumulated code block
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = currentCodeBlock.toString().trimEnd(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(12.dp)
                                .horizontalScroll(rememberScrollState())
                        )
                    }
                    currentCodeBlock.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                currentCodeBlock.append(line).append("\n")
                continue
            }

            when {
                trimmedLine.startsWith("# ") -> {
                    Text(
                        text = parseMarkdownInline(trimmedLine.substring(2), colorScheme),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                trimmedLine.startsWith("## ") -> {
                    Text(
                        text = parseMarkdownInline(trimmedLine.substring(3), colorScheme),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                trimmedLine.startsWith("### ") -> {
                    Text(
                        text = parseMarkdownInline(trimmedLine.substring(4), colorScheme),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.tertiary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                        Text(
                            text = parseMarkdownInline(trimmedLine.substring(2), colorScheme),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onBackground
                        )
                    }
                }
                else -> {
                    if (trimmedLine.isNotBlank()) {
                        Text(
                            text = parseMarkdownInline(line, colorScheme),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onBackground,
                            lineHeight = 20.sp
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        // Just in case code block is not closed
        if (inCodeBlock && currentCodeBlock.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = currentCodeBlock.toString().trimEnd(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(12.dp)
                        .horizontalScroll(rememberScrollState())
                )
            }
        }
    }
}

fun parseMarkdownInline(text: String, colorScheme: ColorScheme): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < text.length) {
        if (text.startsWith("**", i)) {
            val endIdx = text.indexOf("**", i + 2)
            if (endIdx != -1) {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                builder.append(text.substring(i + 2, endIdx))
                builder.pop()
                i = endIdx + 2
                continue
            }
        }
        if (text.startsWith("*", i)) {
            val endIdx = text.indexOf("*", i + 1)
            if (endIdx != -1) {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                builder.append(text.substring(i + 1, endIdx))
                builder.pop()
                i = endIdx + 1
                continue
            }
        }
        if (text[i] == '`') {
            val endIdx = text.indexOf('`', i + 1)
            if (endIdx != -1) {
                builder.pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        color = colorScheme.onSecondaryContainer
                    )
                )
                builder.append(text.substring(i + 1, endIdx))
                builder.pop()
                i = endIdx + 1
                continue
            }
        }
        builder.append(text[i].toString())
        i++
    }
    return builder.toAnnotatedString()
}

@Composable
fun NotesWidget(
    notes: List<NoteEntity>,
    onViewAll: () -> Unit,
    onAddNote: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Rounded.Notes,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LATEST NOTES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onViewAll,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("View all", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddNote() }
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.AddCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Capture a quick markdown note...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                notes.take(3).forEach { note ->
                    Card(
                        onClick = onViewAll,
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(note.updatedAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
