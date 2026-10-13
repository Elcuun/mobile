# Plan de Implementación: Bitácora Mensual de Aprendizaje (Escuela San Bernardo)

Aplicación móvil en Jetpack Compose para fortalecer el registro y seguimiento del aprendizaje de estudiantes de 5.º a 8.º básico mediante una bitácora mensual por curso y asignatura, cumpliendo estrictamente con la pauta de evaluación (IE 2.1.1, IE 2.1.2, IE 2.2.1, IE 2.2.2, IE 2.3.1, IE 2.4.1).

## User Review Required

> [!IMPORTANT]
> La aplicación contará con dos perfiles seleccionables al iniciar (Vista Docente y Vista Estudiante) para probar de forma fluida todas las funcionalidades requeridas:
> - **Docente**: Crear y configurar bitácoras mensuales por curso y asignatura, supervisar participación, identificar estudiantes sin aportes, revisar registros y enviar retroalimentación, cerrar bitácoras y ver estadísticas.
> - **Estudiante**: Registrar su bitácora mensual (contenidos, actividades, aprendizajes, dudas), adjuntar evidencias usando la **Cámara** del dispositivo, registrar la ubicación geográfica de la actividad (uso de **Ubicación**), y consultar historial con retroalimentación del docente.

## Open Questions

- ¿Deseas que incluyamos datos de prueba iniciales (seed data) con cursos de 5.º a 8.º y asignaturas comunes (Matemáticas, Lenguaje, Ciencias, Historia) para facilitar la prueba inmediata? (Se incluirán por defecto).

## Proposed Changes

### Arquitectura y Base de Datos Local ([IE 2.3.1](file:///C:/Users/Duoc/AndroidStudioProjects/proyecto_mobile/app/src/main/java/duoc/cl))
- **Base de datos Room**: Entidades para Cursos (`Course`), Asignaturas (`Subject`), Bitácoras Mensuales (`Logbook`) y Entradas/Aportes de Estudiantes (`LogbookEntry`).
- **Repositorios y ViewModels**: Separación de responsabilidades (Clean Architecture ligera / MVVM con validaciones centralizadas).

### Interfaz de Usuario y Navegación ([IE 2.1.1](file:///C:/Users/Duoc/AndroidStudioProjects/proyecto_mobile/app/src/main/java/duoc/cl))
- **Material 3 & Edge-to-Edge**: Diseño coherente, jerárquico y accesible.
- **Navegación fluida**: Pantalla de selección de rol, Dashboard Docente, Formulario de Creación de Bitácora, Detalle de Curso/Asignatura, Vista de Estudiante, Formulario de Registro de Estudiante, Vista de Retroalimentación e Historial.

### Validaciones y Formularios ([IE 2.1.2](file:///C:/Users/Duoc/AndroidStudioProjects/proyecto_mobile/app/src/main/java/duoc/cl))
- **Validaciones visuales por campo**: Validación en tiempo real para creación de bitácoras y envío de entradas con mensajes de error claros e íconos.
- **Lógica centralizada de validación**: Estados desacoplados en ViewModels ([IE 2.2.1](file:///C:/Users/Duoc/AndroidStudioProjects/proyecto_mobile/app/src/main/java/duoc/cl)).

### Animaciones ([IE 2.2.2](file:///C:/Users/Duoc/AndroidStudioProjects/proyecto_mobile/app/src/main/java/duoc/cl))
- Animaciones de transición entre pantallas, `AnimatedVisibility` para formularios y estados de éxito/carga.

### Recursos del Dispositivo ([IE 2.4.1](file:///C:/Users/Duoc/AndroidStudioProjects/proyecto_mobile/app/src/main/java/duoc/cl))
1. **Cámara**: Captura de fotografía de evidencias o apuntes del estudiante (`ActivityResultContracts.TakePicture`).
2. **Ubicación (GPS)**: Obtención de la ubicación actual del dispositivo para certificar la ubicación del registro en San Bernardo (`FusedLocationProviderClient`).

## Verification Plan

### Automated Tests
- Pruebas unitarias de ViewModels y validadores.
- Pruebas de integración de base de datos Room.

### Manual Verification
- Desplegar en emulador o dispositivo físico.
- Probar el flujo de Docente (Crear bitácora, revisar aportes, dar feedback).
- Probar el flujo de Estudiante (Registrar bitácora, capturar foto con cámara, obtener ubicación GPS, enviar).
- Verificar validaciones en formularios y animaciones visuales.
