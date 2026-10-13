package duoc.cl.ui

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import duoc.cl.data.local.LogbookEntity
import duoc.cl.ui.role.RoleSelectionScreen
import duoc.cl.ui.student.StudentScreen
import duoc.cl.ui.student.SubmitEntryScreen
import duoc.cl.ui.teacher.CreateLogbookScreen
import duoc.cl.ui.teacher.LogbookDetailScreen
import duoc.cl.ui.teacher.TeacherScreen
import duoc.cl.viewmodel.StudentViewModel
import duoc.cl.viewmodel.TeacherViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val teacherViewModel: TeacherViewModel = viewModel()
    val studentViewModel: StudentViewModel = viewModel()

    var activeLogbookForTeacher by remember { mutableStateOf<LogbookEntity?>(null) }
    var activeLogbookForStudent by remember { mutableStateOf<LogbookEntity?>(null) }

    NavHost(navController = navController, startDestination = "role_selection") {
        composable("role_selection") {
            RoleSelectionScreen(
                onSelectTeacher = { navController.navigate("teacher_dashboard") },
                onSelectStudent = { navController.navigate("student_dashboard") }
            )
        }
        composable("teacher_dashboard") {
            TeacherScreen(
                viewModel = teacherViewModel,
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
                    viewModel = studentViewModel,
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            }
        }
    }
}
