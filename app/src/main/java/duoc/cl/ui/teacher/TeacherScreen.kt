package duoc.cl.ui.teacher

import androidx.compose.foundation.clickable
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
import duoc.cl.data.local.Course
import duoc.cl.data.local.LogbookEntity
import duoc.cl.data.local.Subject
import duoc.cl.data.local.User
import duoc.cl.ui.components.UserHeader
import duoc.cl.viewmodel.TeacherViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TeacherScreen(
    viewModel: TeacherViewModel,
    currentUser: User?,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit,
    onCreateLogbook: (Long, Long) -> Unit,
    onOpenLogbookDetail: (LogbookEntity) -> Unit
) {
    val courses by viewModel.courses.collectAsState(initial = emptyList())
    val subjects by viewModel.subjects.collectAsState(initial = emptyList())

    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }

    var logbookToDelete by remember { mutableStateOf<LogbookEntity?>(null) }

    val courseId = selectedCourse?.id ?: 0L
    val subjectId = selectedSubject?.id ?: 0L
    val logbooks by if (selectedCourse != null && selectedSubject != null) {
        viewModel.getLogbooks(courseId, subjectId).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel del Docente - Bitácoras") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedCourse != null && selectedSubject != null) {
                FloatingActionButton(
                    onClick = { onCreateLogbook(courseId, subjectId) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Crear Bitácora")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // User Session Header
            if (currentUser != null) {
                UserHeader(user = currentUser, onLogout = onLogout)
            }

            Text(
                text = "Seleccione Curso y Asignatura para gestionar las bitácoras mensuales:",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(text = "Curso:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                courses.forEach { course ->
                    FilterChip(
                        selected = selectedCourse == course,
                        onClick = { selectedCourse = course },
                        label = { Text(course.name) }
                    )
                }
            }

            Text(text = "Asignatura:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                subjects.forEach { subject ->
                    FilterChip(
                        selected = selectedSubject == subject,
                        onClick = { selectedSubject = subject },
                        label = { Text(subject.name) }
                    )
                }
            }

            HorizontalDivider()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (selectedCourse == null || selectedSubject == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Por favor seleccione un curso y una asignatura.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Bitácoras para ${selectedCourse!!.name} - ${selectedSubject!!.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (logbooks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "No hay bitácoras creadas. Presione '+' para crear una.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(logbooks) { logbook ->
                                    LogbookItemCard(
                                        logbook = logbook,
                                        onClick = { onOpenLogbookDetail(logbook) },
                                        onDelete = { logbookToDelete = logbook }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Deleting a Logbook
    logbookToDelete?.let { logbook ->
        AlertDialog(
            onDismissRequest = { logbookToDelete = null },
            icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar Bitácora") },
            text = {
                Text("¿Está seguro de que desea eliminar la bitácora '${logbook.title}'? Esta acción no se puede deshacer y eliminará todos los registros asociados de los estudiantes.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLogbook(logbook.id)
                        logbookToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { logbookToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun LogbookItemCard(
    logbook: LogbookEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = logbook.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(containerColor = if (logbook.isClosed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            text = if (logbook.isClosed) "Cerrada" else "Activa",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar Bitácora",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Periodo: ${logbook.month}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = logbook.purpose, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
    }
}
