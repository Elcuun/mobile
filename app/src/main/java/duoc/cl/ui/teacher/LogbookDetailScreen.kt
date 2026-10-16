package duoc.cl.ui.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import duoc.cl.data.local.LogbookEntity
import duoc.cl.data.local.LogbookEntryEntity
import duoc.cl.viewmodel.EntryStatusFilter
import duoc.cl.viewmodel.TeacherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookDetailScreen(
    logbook: LogbookEntity,
    viewModel: TeacherViewModel,
    onBack: () -> Unit,
    onEditLogbook: ((LogbookEntity) -> Unit)? = null
) {
    val entries by viewModel.getEntries(logbook.id).collectAsState(initial = emptyList())
    val searchQuery by viewModel.entrySearchQuery.collectAsState()
    val statusFilter by viewModel.entryStatusFilter.collectAsState()
    var selectedEntryForFeedback by remember { mutableStateOf<LogbookEntryEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(logbook.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.prepareEditLogbook(logbook)
                        onEditLogbook?.invoke(logbook)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Bitácora",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { viewModel.toggleCloseLogbook(logbook) }) {
                        Icon(
                            imageVector = if (logbook.isClosed) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Cerrar/Abrir Bitácora",
                            tint = if (logbook.isClosed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar Bitácora",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Propósito: ${logbook.purpose}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Periodo: ${logbook.month}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (logbook.isClosed) "Estado: Cerrada" else "Estado: Activa y Recibiendo Aportes",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (logbook.isClosed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Total Aportes Estudiantes: ${entries.size}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Text(text = "Aportes de Estudiantes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Search by Student Name / Content & Filter by Review Status
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setEntrySearchQuery,
                placeholder = { Text("Buscar por estudiante o contenido...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setEntrySearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Estado:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                EntryStatusFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { viewModel.setEntryStatusFilter(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }

            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No hay aportes que coincidan con la búsqueda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(entries) { entry ->
                        StudentEntryCard(
                            entry = entry,
                            onReviewClick = { selectedEntryForFeedback = entry }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar Bitácora") },
            text = { Text("¿Está seguro de que desea eliminar esta bitácora? Esta acción eliminará también todos los aportes asociados de los estudiantes.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLogbook(logbook.id) {
                            showDeleteConfirmDialog = false
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    selectedEntryForFeedback?.let { entry ->
        FeedbackDialog(
            entry = entry,
            onDismiss = { selectedEntryForFeedback = null },
            onSubmit = { feedback, isReviewed ->
                viewModel.updateFeedback(entry.id, feedback, isReviewed)
                selectedEntryForFeedback = null
            }
        )
    }
}

@Composable
fun StudentEntryCard(entry: LogbookEntryEntity, onReviewClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = entry.studentName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Badge(
                    containerColor = if (entry.isReviewed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (entry.isReviewed) "Revisado" else "Pendiente",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Contenidos: ${entry.contents}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Actividades: ${entry.activities}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Aprendizajes: ${entry.learnings}", style = MaterialTheme.typography.bodyMedium)
            if (entry.doubts.isNotBlank()) {
                Text(text = "Dudas: ${entry.doubts}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            if (!entry.feedback.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Retroalimentación Docente: ${entry.feedback}",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onReviewClick,
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Agregar Retroalimentación")
            }
        }
    }
}

@Composable
fun FeedbackDialog(
    entry: LogbookEntryEntity,
    onDismiss: () -> Unit,
    onSubmit: (String, Boolean) -> Unit
) {
    var feedbackText by remember { mutableStateOf(entry.feedback ?: "") }
    var isReviewed by remember { mutableStateOf(entry.isReviewed) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Retroalimentar a ${entry.studentName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    label = { Text("Comentarios de Retroalimentación") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(checked = isReviewed, onCheckedChange = { isReviewed = it })
                    Text(text = "Marcar como Revisado")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(feedbackText, isReviewed) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
