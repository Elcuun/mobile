package duoc.cl.ui

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import duoc.cl.data.local.LogbookEntity
import duoc.cl.data.local.Role
import duoc.cl.ui.apoderado.ApoderadoScreen
import duoc.cl.ui.auth.LoginScreen
import duoc.cl.ui.auth.RegisterScreen
import duoc.cl.ui.role.RoleSelectionScreen
import duoc.cl.ui.student.StudentScreen
import duoc.cl.ui.student.SubmitEntryScreen
import duoc.cl.ui.teacher.CreateLogbookScreen
import duoc.cl.ui.teacher.LogbookDetailScreen
import duoc.cl.ui.teacher.TeacherScreen
import duoc.cl.viewmodel.ApoderadoViewModel
import duoc.cl.viewmodel.AuthViewModel
import duoc.cl.viewmodel.StudentViewModel
import duoc.cl.viewmodel.TeacherViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val teacherViewModel: TeacherViewModel = viewModel()
    val studentViewModel: StudentViewModel = viewModel()
    val apoderadoViewModel: ApoderadoViewModel = viewModel()

    val currentUser by authViewModel.currentUser.collectAsState()

    var activeLogbookForTeacher by remember { mutableStateOf<LogbookEntity?>(null) }
    var activeLogbookForStudent by remember { mutableStateOf<LogbookEntity?>(null) }

    NavHost(navController = navController, startDestination = "role_selection") {
        composable("role_selection") {
            RoleSelectionScreen(
                onSelectTeacher = {
                    if (currentUser?.role == Role.PROFESOR) {
                        navController.navigate("teacher_dashboard")
                    } else {
                        navController.navigate("auth_login/${Role.PROFESOR.name}")
                    }
                },
                onSelectStudent = {
                    if (currentUser?.role == Role.ALUMNO) {
                        navController.navigate("student_dashboard")
                    } else {
                        navController.navigate("auth_login/${Role.ALUMNO.name}")
                    }
                },
                onSelectApoderado = {
                    navController.navigate("apoderado_dashboard")
                }
            )
        }

        composable("apoderado_dashboard") {
            ApoderadoScreen(
                viewModel = apoderadoViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "auth_login/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val roleStr = backStackEntry.arguments?.getString("role")
            val role = Role.fromKey(roleStr)
            LoginScreen(
                role = role,
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRegister = {
                    navController.navigate("auth_register/${role.name}")
                },
                onLoginSuccess = {
                    val destination = if (role == Role.PROFESOR) "teacher_dashboard" else "student_dashboard"
                    navController.navigate(destination) {
                        popUpTo("role_selection") { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = "auth_register/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val roleStr = backStackEntry.arguments?.getString("role")
            val role = Role.fromKey(roleStr)
            RegisterScreen(
                role = role,
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    val destination = if (role == Role.PROFESOR) "teacher_dashboard" else "student_dashboard"
                    navController.navigate(destination) {
                        popUpTo("role_selection") { inclusive = false }
                    }
                }
            )
        }

        composable("teacher_dashboard") {
            TeacherScreen(
                viewModel = teacherViewModel,
                currentUser = currentUser,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("role_selection") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                onCreateLogbook = { courseId, subjectId ->
                    navController.navigate("create_logbook/$courseId/$subjectId")
                },
                onOpenLogbookDetail = { logbook ->
                    activeLogbookForTeacher = logbook
                    navController.navigate("logbook_detail")
                }
            )
        }

        composable(
            route = "create_logbook/{courseId}/{subjectId}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.LongType },
                navArgument("subjectId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: 0L
            val subjectId = backStackEntry.arguments?.getLong("subjectId") ?: 0L
            CreateLogbookScreen(
                courseId = courseId,
                subjectId = subjectId,
                viewModel = teacherViewModel,
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable("logbook_detail") {
            activeLogbookForTeacher?.let { logbook ->
                LogbookDetailScreen(
                    logbook = logbook,
                    viewModel = teacherViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("student_dashboard") {
            StudentScreen(
                viewModel = studentViewModel,
                currentUser = currentUser,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("role_selection") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                onOpenLogbook = { logbook ->
                    activeLogbookForStudent = logbook
                    navController.navigate("submit_entry")
                }
            )
        }

        composable("submit_entry") {
            activeLogbookForStudent?.let { logbook ->
                SubmitEntryScreen(
                    logbook = logbook,
                    currentUser = currentUser,
                    viewModel = studentViewModel,
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            }
        }
    }
}
