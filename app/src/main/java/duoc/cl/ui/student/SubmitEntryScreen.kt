package duoc.cl.ui.student

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.LocationServices
import duoc.cl.data.local.LogbookEntity
import duoc.cl.data.local.User
import duoc.cl.viewmodel.StudentViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitEntryScreen(
    logbook: LogbookEntity,
    currentUser: User?,
    viewModel: StudentViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val formState by viewModel.entryFormState.collectAsState()
    val isEditingEntry = viewModel.isEditingEntry
    val entries by viewModel.getEntriesForLogbook(logbook.id).collectAsState(initial = emptyList())
    val scrollState = rememberScrollState()

    var tempPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser) {
        if (currentUser != null && formState.studentName.isBlank()) {
            viewModel.onStudentNameChanged(currentUser.fullName)
        }
    }

    val studentEntries = remember(entries, currentUser, formState.studentName) {
        if (currentUser != null) {
            entries.filter { it.studentName.equals(currentUser.fullName, ignoreCase = true) }
        } else if (formState.studentName.isNotBlank()) {
            entries.filter { it.studentName.equals(formState.studentName, ignoreCase = true) }
        } else {
            entries
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && !tempPhotoUriString.isNullOrBlank()) {
            viewModel.setPhotoUri(tempPhotoUriString)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            viewModel.setPhotoUri(uri.toString())
        }
    }

    fun launchCamera() {
        try {
            val photoFile = File(context.cacheDir, "evidence_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempPhotoUriString = uri.toString()
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        viewModel.setLocation(loc.latitude, loc.longitude)
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditingEntry) "Editar Registro" else "Registrar Bitácora") },
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
            // Logbook Summary Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = logbook.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Propósito: ${logbook.purpose}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Instrucciones: ${logbook.instructions}", style = MaterialTheme.typography.bodySmall)
                    if (logbook.guidingQuestions.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Pregunta Guía: ${logbook.guidingQuestions}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Existing Submitted Entries Section
            if (studentEntries.isNotEmpty()) {
                Text(
                    text = "Tus Registros Enviados para esta Bitácora:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                studentEntries.forEach { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.studentName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Badge(
                                        containerColor = if (entry.isReviewed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            text = if (entry.isReviewed) "Revisado" else "Pendiente",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (!logbook.isClosed) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedButton(
                                            onClick = { viewModel.prepareEditEntry(entry) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "Editar", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Contenidos: ${entry.contents}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Actividades: ${entry.activities}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Aprendizajes: ${entry.learnings}", style = MaterialTheme.typography.bodySmall)
                            if (entry.doubts.isNotBlank()) {
                                Text(text = "Dudas: ${entry.doubts}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                            if (!entry.photoUri.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                PhotoPreview(photoUriString = entry.photoUri, maxHeight = 140.dp)
                            }
                            if (!entry.feedback.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Retroalimentación del Docente: ${entry.feedback}",
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()
            }

            // Entry Form Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditingEntry) "Editando Registro" else "Nuevo Registro de Bitácora",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (isEditingEntry) {
                    TextButton(onClick = { viewModel.prepareNewEntry(currentUser?.fullName ?: "") }) {
                        Text("Cancelar Edición")
                    }
                }
            }

            OutlinedTextField(
                value = formState.studentName,
                onValueChange = viewModel::onStudentNameChanged,
                label = { Text("Nombre y Apellido del Estudiante") },
                leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                isError = formState.studentNameError != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            AnimatedVisibility(visible = formState.studentNameError != null) {
                Text(text = formState.studentNameError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            OutlinedTextField(
                value = formState.contents,
                onValueChange = viewModel::onContentsChanged,
                label = { Text("Contenidos Trabajados en Clase") },
                leadingIcon = { Icon(imageVector = Icons.Default.Book, contentDescription = null) },
                isError = formState.contentsError != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            OutlinedTextField(
                value = formState.activities,
                onValueChange = viewModel::onActivitiesChanged,
                label = { Text("Actividades Realizadas") },
                leadingIcon = { Icon(imageVector = Icons.Default.Assignment, contentDescription = null) },
                isError = formState.activitiesError != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            OutlinedTextField(
                value = formState.learnings,
                onValueChange = viewModel::onLearningsChanged,
                label = { Text("Aprendizajes Obtenidos") },
                leadingIcon = { Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null) },
                isError = formState.learningsError != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            OutlinedTextField(
                value = formState.doubts,
                onValueChange = viewModel::onDoubtsChanged,
                label = { Text("Dudas o Dificultades (Opcional)") },
                leadingIcon = { Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Text(text = "Recursos del Dispositivo (Evidencia Foto / GPS)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            launchCamera()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Camara", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Galeria", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "GPS", style = MaterialTheme.typography.labelMedium)
                }
            }

            AnimatedVisibility(visible = formState.photoUri != null) {
                formState.photoUri?.let { uriStr ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Vista previa de imagen:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = { viewModel.setPhotoUri(null) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Quitar foto")
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        PhotoPreview(photoUriString = uriStr, maxHeight = 180.dp)
                    }
                }
            }

            AnimatedVisibility(visible = formState.latitude != null) {
                Text(
                    text = "Ubicación registrada: Lat ${formState.latitude}, Lon ${formState.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.submitEntry(logbook.id) { success ->
                        if (success) onSuccess()
                    }
                },
                enabled = formState.isFormValid && !logbook.isClosed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = if (isEditingEntry) Icons.Default.Save else Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (logbook.isClosed) "Bitácora Cerrada" else if (isEditingEntry) "Guardar Cambios" else "Enviar Registro Mensual",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PhotoPreview(
    photoUriString: String,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 180.dp
) {
    val context = LocalContext.current
    val bitmap = remember(photoUriString) {
        try {
            val uri = Uri.parse(photoUriString)
            if (uri.scheme == "content" || uri.scheme == "file") {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bm = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bm?.asImageBitmap()
            } else {
                val file = File(photoUriString)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    if (bitmap != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(maxHeight),
            shape = RoundedCornerShape(12.dp)
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = "Evidencia Fotográfica",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
