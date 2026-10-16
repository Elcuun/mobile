package duoc.cl.ui.student

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
import duoc.cl.viewmodel.LogbookStatusFilter
import duoc.cl.viewmodel.StudentViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudentScreen(
    viewModel: StudentViewModel,
    currentUser: User?,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenLogbook: (LogbookEntity) -> Unit
) {
    val courses by viewModel.courses.collectAsState(initial = emptyList())
    val subjects by viewModel.subjects.collectAsState(initial = emptyList())
    val searchQuery by viewModel.logbookSearchQuery.collectAsState()
    val statusFilter by viewModel.logbookStatusFilter.collectAsState()

    val assignedCourse = remember(currentUser, courses) {
        currentUser?.courseId?.let { cid -> courses.firstOrNull { it.id == cid } }
    }

    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }

    LaunchedEffect(assignedCourse) {
        if (assignedCourse != null) {
            selectedCourse = assignedCourse
        }
    }

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
                title = { Text("Portal del Estudiante - Bitácoras") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // User Session Header
            if (currentUser != null) {
                UserHeader(user = currentUser, onLogout = onLogout)
            }

            // Course Display / Selection
            if (assignedCourse != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Class,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Curso Asignado",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = assignedCourse.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Seleccione su Curso:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
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
            }

            Text(
                text = "Seleccione una Asignatura para ver las bitácoras:",
                style = MaterialTheme.typography.bodyMedium
            )

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
                if (selectedCourse == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No hay curso asignado o seleccionado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (selectedSubject == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Por favor seleccione una asignatura para consultar bitácoras.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Bitácoras en ${selectedCourse!!.name} - ${selectedSubject!!.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Search by Title/Month & Filter by Status
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::setLogbookSearchQuery,
                            placeholder = { Text("Buscar por título o mes (ej: Marzo)...") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setLogbookSearchQuery("") }) {
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
                            LogbookStatusFilter.entries.forEach { filter ->
                                FilterChip(
                                    selected = statusFilter == filter,
                                    onClick = { viewModel.setLogbookStatusFilter(filter) },
                                    label = { Text(filter.label) }
                                )
                            }
                        }

                        if (logbooks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "No se encontraron bitácoras para los filtros aplicados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(logbooks) { logbook ->
                                    StudentLogbookCard(logbook = logbook, onClick = { onOpenLogbook(logbook) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentLogbookCard(logbook: LogbookEntity, onClick: () -> Unit) {
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = logbook.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Badge(containerColor = if (logbook.isClosed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        text = if (logbook.isClosed) "Cerrada" else "Abierta",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Periodo: ${logbook.month}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = logbook.purpose, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (logbook.isClosed) "Bitácora cerrada por el docente" else "Toca para registrar tu aprendizaje >",
                style = MaterialTheme.typography.bodySmall,
                color = if (logbook.isClosed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
