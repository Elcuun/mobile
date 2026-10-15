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

class UserRepository private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        initDefaultUsersIfNeeded()
        restoreSession()
    }

    private fun initDefaultUsersIfNeeded() {
        val usersJson = prefs.getString(KEY_USERS, null)
        if (usersJson.isNull_or_empty_json()) {
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
                    firstName = "Ignacia",
                    lastName = "Soto",
                    email = "estudiante@duoc.cl",
                    password = "123456",
                    role = Role.ALUMNO,
                    courseId = 1L // 5.º Básico A
                )
            )
            saveUsers(defaultUsers)
        }
    }

    private fun String?.isNull_or_empty_json(): Boolean {
        return this.isNullOrBlank() || this == "[]"
    }

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

    private fun restoreSession() {
        val activeEmail = prefs.getString(KEY_LOGGED_IN_EMAIL, null)
        if (!activeEmail.isNullOrBlank()) {
            val user = getUsers().firstOrNull { it.email.equals(activeEmail, ignoreCase = true) }
            _currentUser.value = user
        }
    }

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

        // Set as logged in user
        setSession(newUser)
        return Result.success(newUser)
    }

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

    private fun setSession(user: User) {
        prefs.edit().putString(KEY_LOGGED_IN_EMAIL, user.email).apply()
        _currentUser.value = user
    }

    fun logout() {
        prefs.edit().remove(KEY_LOGGED_IN_EMAIL).apply()
        _currentUser.value = null
    }

    companion object {
        private const val PREFS_NAME = "app_user_prefs_v2"
        private const val KEY_USERS = "registered_users_v2"
        private const val KEY_LOGGED_IN_EMAIL = "logged_in_user_email_v2"

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
