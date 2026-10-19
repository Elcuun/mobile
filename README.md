# Sistema de Bitácora Digital de Aprendizaje Mensual 📱📗

Aplicación móvil Android desarrollada en **Kotlin** con **Jetpack Compose**, **Material 3** y arquitectura **MVVM**.

Permite a **Docentes**, **Estudiantes** y **Apoderados** gestionar, registrar y consultar el progreso académico mensual a través de bitácoras de aprendizaje interactivas con soporte para capturas de foto (Cámara y Galería) y geolocalización GPS.

---

## 🎨 Tema de la Aplicación: Verde con Morado
El sistema utiliza una paleta de colores personalizada basada en **Verde (Primary)** y **Morado (Secondary)**, centralizada de forma modular en el paquete `duoc.cl.ui.theme`:
- **Color.kt**: Define los tokens de color primarios (`#2E7D32`), secundarios (`#6A1B9A`), contenedores y variantes para temas claro y oscuro.
- **Theme.kt**: Configura la paleta `Proyecto_mobileTheme` aplicando la combinación Verde-Morado de forma consistente en toda la aplicación.

---

## 👥 Cuentas Ficticias de Prueba (Credenciales Demo)

Para probar los diferentes roles y flujos de la aplicación, el sistema incluye por defecto las siguientes **8 cuentas ficticias**:

| Rol | Nombre | Correo Electrónico | Contraseña | Curso Asignado |
| :--- | :--- | :--- | :--- | :--- |
| 👨‍🏫 **Docente** | María González | `docente@duoc.cl` | `123456` | Todos los Cursos |
| 👨‍🏫 **Docente** | Carlos Rodríguez | `docente2@duoc.cl` | `123456` | Todos los Cursos |
| 🧑‍🎓 **Estudiante** | Ignacia Soto | `estudiante@duoc.cl` | `123456` | 5.º Básico A |
| 🧑‍🎓 **Estudiante** | Lucas Pérez | `estudiante2@duoc.cl` | `123456` | 8.º Básico A |
| 🧑‍🎓 **Estudiante** | Mateo Morales | `estudiante3@duoc.cl` | `123456` | 6.º Básico A |
| 🧑‍🎓 **Estudiante** | Sofía Castro | `estudiante4@duoc.cl` | `123456` | 7.º Básico A |
| 👨‍👩‍👧 **Apoderado** | Juan Soto | `apoderado@duoc.cl` | `123456` | Consulta (Ignacia Soto) |
| 👨‍👩‍👧 **Apoderado** | Ana Pérez | `apoderado2@duoc.cl` | `123456` | Consulta (Lucas Pérez) |

---

## 🌟 Funcionalidades Principales

### 1. Panel del Docente
- **Creación y Edición de Bitácoras:** Formulario completo para publicar y **editar** bitácoras mensuales especificando curso, asignatura, mes/año, propósito, instrucciones y preguntas guía.
- **Gestión de Estado:** Apertura y cierre de bitácoras (bloquea o habilita la recepción de aportes) y opción para eliminar bitácoras.
- **Revisión y Retroalimentación:** Visualización de los aportes enviados por los estudiantes con opción para ingresar comentarios de retroalimentación cualitativa y marcar el registro como "Revisado" o "Pendiente".

### 2. Portal del Estudiante
- **Consulta por Curso y Asignatura:** Muestra el curso asignado al perfil del estudiante y las bitácoras activas/cerradas.
- **Formulario de Registro de Bitácora:** Permite ingresar contenidos trabajados, actividades realizadas, aprendizajes obtenidos y dudas o dificultades.
- **Edición de Registros:** Opción para modificar o corregir registros previamente enviados mientras la bitácora permanezca abierta.
- **Captura de Evidencias (Cámara / Galería):**
  - Configuración segura de `FileProvider` (`${applicationId}.fileprovider`) con guardado de URI persistente en `rememberSaveable`.
  - Opción de tomar fotografía con la Cámara o seleccionar imágenes de la Galería.
  - **Vista Previa de Imagen:** Muestra una previsualización de la foto capturada en el formulario y en las tarjetas de registros guardados.
- **Geolocalización GPS:** Integración con `FusedLocationProviderClient` para adjuntar coordenadas de latitud y longitud.

### 3. Portal de Apoderados
- **Consulta en Modo Solo Lectura:** Permite a los padres y apoderados ingresar el nombre y correo del estudiante para revisar sus registros, evidencias fotográficas y la retroalimentación entregada por los profesores.

---

## 🔍 Búsqueda y Filtros Implementados

1. **Filtro por Curso y Asignatura:** Chips interactivos para filtrar el contenido por niveles académicos (5.º a 8.º Básico) y materias (Matemáticas, Lenguaje, Ciencias, Historia).
2. **Filtro por Mes:** Menú desplegable para filtrar bitácoras por periodos mensuales (Marzo 2025, Abril 2025, Mayo 2025, etc.).
3. **Filtro por Estado:**
   - **Bitácoras:** Chips para filtrar por `Todas`, `Activas` o `Cerradas`.
   - **Registros:** Chips para filtrar por `Todas`, `Pendientes` o `Revisadas`.
4. **Búsqueda Textual:** Campo de entrada para filtrar en tiempo real por título, nombre de estudiante o palabras clave de los contenidos.

---

## 🛠️ Tecnologías y Arquitectura

- **Lenguaje:** Kotlin
- **UI Framework:** Jetpack Compose + Material Design 3
- **Navegación:** Navigation Compose
- **Gestión de Estado y Reactividad:** ViewModel, StateFlow, Kotlin Coroutines
- **Persistencia Local:** SharedPreferences con serialización JSON para usuarios, bitácoras y registros.
- **Hardware & Multimedia:** CameraX / System Camera Intent (`ActivityResultContracts.TakePicture`), Android Photo Picker (`ActivityResultContracts.GetContent`), `FileProvider` y Google Play Services Location (`FusedLocationProviderClient`).

---

## 🚀 Instalación y Compilación

1. Clonar o abrir el repositorio en **Android Studio** (Ladybug o superior recomendado).
2. Asegurar que esté configurado **JDK 11** o superior.
3. Sincronizar el proyecto con Gradle.
4. Ejecutar el proyecto en un emulador Android o dispositivo físico:
   ```bash
   ./gradlew assembleDebug
   ```
