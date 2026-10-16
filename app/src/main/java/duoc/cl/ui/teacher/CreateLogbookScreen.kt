package duoc.cl.ui.teacher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import duoc.cl.viewmodel.TeacherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLogbookScreen(
    courseId: Long,
    subjectId: Long,
    logbookId: Long = 0L,
    viewModel: TeacherViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val formState by viewModel.formState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(logbookId) {
        if (logbookId != 0L) {
            viewModel.loadAndPrepareEditLogbook(logbookId)
        } else {
            viewModel.prepareCreateLogbook()
        }
    }

    val isEditing = viewModel.isEditingLogbook

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Bitácora Mensual" else "Crear Bitácora Mensual") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isEditing) "Modifique la información de la bitácora mensual." else "Complete la información de la bitácora mensual para el curso y asignatura seleccionados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Month Field
            OutlinedTextField(
                value = formState.month,
                onValueChange = viewModel::onMonthChanged,
                label = { Text("Mes y Año (ej. Marzo 2025)") },
                leadingIcon = { Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null) },
                isError = formState.monthError != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            AnimatedVisibility(visible = formState.monthError != null) {
                Text(
                    text = formState.monthError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Title Field
            OutlinedTextField(
                value = formState.title,
                onValueChange = viewModel::onTitleChanged,
                label = { Text("Título de la Bitácora") },
                leadingIcon = { Icon(imageVector = Icons.Default.Title, contentDescription = null) },
                isError = formState.titleError != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            AnimatedVisibility(visible = formState.titleError != null) {
                Text(
                    text = formState.titleError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Purpose Field
            OutlinedTextField(
                value = formState.purpose,
                onValueChange = viewModel::onPurposeChanged,
                label = { Text("Propósito de Aprendizaje") },
                leadingIcon = { Icon(imageVector = Icons.Default.Flag, contentDescription = null) },
                isError = formState.purposeError != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )
            AnimatedVisibility(visible = formState.purposeError != null) {
                Text(
                    text = formState.purposeError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Instructions Field
            OutlinedTextField(
                value = formState.instructions,
                onValueChange = viewModel::onInstructionsChanged,
                label = { Text("Instrucciones para Estudiantes") },
                leadingIcon = { Icon(imageVector = Icons.Default.ListAlt, contentDescription = null) },
                isError = formState.instructionsError != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3
            )
            AnimatedVisibility(visible = formState.instructionsError != null) {
                Text(
                    text = formState.instructionsError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Guiding Questions Field
            OutlinedTextField(
                value = formState.guidingQuestions,
                onValueChange = viewModel::onGuidingQuestionsChanged,
                label = { Text("Preguntas Orientadoras") },
                leadingIcon = { Icon(imageVector = Icons.Default.Help, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.saveLogbook(courseId, subjectId) { success ->
                        if (success) onSuccess()
                    }
                },
                enabled = formState.isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Check,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "Guardar Cambios" else "Publicar Bitácora Mensual",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
