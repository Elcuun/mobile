package duoc.cl.data.repository

import android.content.Context
import android.content.SharedPreferences
import duoc.cl.data.local.Role
import duoc.cl.data.local.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repositorio encargado de la gestión y autenticación de usuarios (Docentes, Estudiantes y Apoderados).
 * Guarda las cuentas registradas y la sesión activa mediante SharedPreferences.
 */
class UserRepository private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        initDefaultUsersIfNeeded()
        restoreSession()
    }

    /**
     * Inicializa las cuentas demo en la aplicación si no se encuentran previamente guardadas.
     * Incluye al menos 8 cuentas ficticias (Docentes, Estudiantes y Apoderados).
     */
    private fun initDefaultUsersIfNeeded() {
        val users = getUsers()
        val defaultUsers = listOf(
            User(
                firstName = "María",
                lastName = "González",
                email = "docente@duoc.cl",
                password = "123456",
                role = Role.PROFESOR,
                courseId = null
            ),
            User(
                firstName = "Carlos",
                lastName = "Rodríguez",
                email = "docente2@duoc.cl",
                password = "123456",
                role = Role.PROFESOR,
                courseId = null
            ),
            User(
                firstName = "Ignacia",
                lastName = "Soto",
                email = "estudiante@duoc.cl",
                password = "123456",
                role = Role.ALUMNO,
                courseId = 1L // 5.º Básico A
            ),
            User(
                firstName = "Lucas",
                lastName = "Pérez",
                email = "estudiante2@duoc.cl",
                password = "123456",
                role = Role.ALUMNO,
                courseId = 4L // 8.º Básico A
            ),
            User(
                firstName = "Mateo",
                lastName = "Morales",
                email = "estudiante3@duoc.cl",
                password = "123456",
                role = Role.ALUMNO,
                courseId = 2L // 6.º Básico A
            ),
            User(
                firstName = "Sofía",
                lastName = "Castro",
                email = "estudiante4@duoc.cl",
                password = "123456",
                role = Role.ALUMNO,
                courseId = 3L // 7.º Básico A
            ),
            User(
                firstName = "Juan",
                lastName = "Soto",
                email = "apoderado@duoc.cl",
                password = "123456",
                role = Role.APODERADO,
                courseId = null
            ),
            User(
                firstName = "Ana",
                lastName = "Pérez",
                email = "apoderado2@duoc.cl",
                password = "123456",
                role = Role.APODERADO,
                courseId = null
            )
        )

        var changed = false
        for (defaultUser in defaultUsers) {
            if (users.none { it.email.equals(defaultUser.email, ignoreCase = true) }) {
                users.add(defaultUser)
                changed = true
            }
        }
        if (changed || users.isEmpty()) {
            saveUsers(users)
        }
    }

    /**
     * Obtiene la lista de usuarios registrados almacenada en SharedPreferences.
     */
    private fun getUsers(): MutableList<User> {
        val usersJson = prefs.getString(KEY_USERS, "[]") ?: "[]"
        val users = mutableListOf<User>()
        try {
            val jsonArray = JSONArray(usersJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val courseIdVal = if (obj.has("courseId") && !obj.isNull("courseId")) obj.optLong("courseId") else null
                users.add(
                    User(
                        id = obj.optString("id"),
                        firstName = obj.optString("firstName"),
                        lastName = obj.optString("lastName"),
                        email = obj.optString("email"),
                        password = obj.optString("password"),
                        role = Role.fromKey(obj.optString("role")),
                        courseId = courseIdVal
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return users
    }

    /**
     * Guarda la lista de usuarios en formato JSON en SharedPreferences.
     */
    private fun saveUsers(users: List<User>) {
        val jsonArray = JSONArray()
        for (user in users) {
            val obj = JSONObject().apply {
                put("id", user.id)
                put("firstName", user.firstName)
                put("lastName", user.lastName)
                put("email", user.email)
                put("password", user.password)
                put("role", user.role.name)
                put("courseId", user.courseId ?: JSONObject.NULL)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_USERS, jsonArray.toString()).apply()
    }

    /**
     * Restaura la sesión del usuario si existe un correo activo guardado en SharedPreferences.
     */
    private fun restoreSession() {
        val activeEmail = prefs.getString(KEY_LOGGED_IN_EMAIL, null)
        if (!activeEmail.isNullOrBlank()) {
            val user = getUsers().firstOrNull { it.email.equals(activeEmail, ignoreCase = true) }
            _currentUser.value = user
        }
    }

    /**
     * Registra un nuevo usuario en la aplicación validando que el correo no exista.
     */
    fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        role: Role,
        courseId: Long? = null
    ): Result<User> {
        val cleanEmail = email.trim().lowercase()
        val users = getUsers()

        if (users.any { it.email.lowercase() == cleanEmail }) {
            return Result.failure(Exception("El correo electrónico '$cleanEmail' ya está registrado."))
        }

        val newUser = User(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            email = cleanEmail,
            password = password,
            role = role,
            courseId = courseId
        )

        users.add(newUser)
        saveUsers(users)

        setSession(newUser)
        return Result.success(newUser)
    }

    /**
     * Inicia sesión con credenciales y verifica que el rol ingresado coincida con el perfil guardado.
     */
    fun login(
        email: String,
        password: String,
        role: Role
    ): Result<User> {
        val cleanEmail = email.trim().lowercase()
        val users = getUsers()

        val matchedUser = users.firstOrNull {
            it.email.lowercase() == cleanEmail && it.password == password
        }

        if (matchedUser == null) {
            return Result.failure(Exception("Correo electrónico o contraseña incorrectos."))
        }

        if (matchedUser.role != role) {
            val expectedRole = role.displayName
            return Result.failure(Exception("Esta cuenta no tiene perfil de $expectedRole."))
        }

        setSession(matchedUser)
        return Result.success(matchedUser)
    }

    /**
     * Establce el usuario en sesión activa.
     */
    private fun setSession(user: User) {
        prefs.edit().putString(KEY_LOGGED_IN_EMAIL, user.email).apply()
        _currentUser.value = user
    }

    /**
     * Cierra la sesión activa del usuario.
     */
    fun logout() {
        prefs.edit().remove(KEY_LOGGED_IN_EMAIL).apply()
        _currentUser.value = null
    }

    companion object {
        private const val PREFS_NAME = "app_user_prefs_v3"
        private const val KEY_USERS = "registered_users_v3"
        private const val KEY_LOGGED_IN_EMAIL = "logged_in_user_email_v3"

        @Volatile
        private var INSTANCE: UserRepository? = null

        fun getInstance(context: Context): UserRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = UserRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
