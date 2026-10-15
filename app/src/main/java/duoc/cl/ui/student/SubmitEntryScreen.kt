package duoc.cl.ui.student

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val scrollState = rememberScrollState()

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(currentUser) {
        if (currentUser != null && formState.studentName.isBlank()) {
            viewModel.onStudentNameChanged(currentUser.fullName)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            viewModel.setPhotoUri(tempPhotoUri.toString())
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val photoFile = File(context.cacheDir, "evidence_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
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
                title = { Text("Registrar Bitácora Mensual") },
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

            Text(text = "Recursos del Dispositivo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            val photoFile = File(context.cacheDir, "evidence_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                            tempPhotoUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (formState.photoUri != null) "Foto OK" else "Tomar Foto")
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
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (formState.latitude != null) "GPS OK" else "Obtener GPS")
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
                Icon(imageVector = Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (logbook.isClosed) "Bitácora Cerrada" else "Enviar Registro Mensual", fontWeight = FontWeight.Bold)
            }
        }
    }
}
