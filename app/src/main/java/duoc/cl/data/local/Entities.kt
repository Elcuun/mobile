package duoc.cl.data.local

data class Course(
    val id: Long,
    val name: String
)

data class Subject(
    val id: Long,
    val name: String
)

data class LogbookEntity(
    val id: Long = System.currentTimeMillis(),
    val courseId: Long,
    val subjectId: Long,
    val month: String,
    val title: String,
    val purpose: String,
    val instructions: String,
    val guidingQuestions: String,
    val isClosed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class LogbookEntryEntity(
    val id: Long = System.currentTimeMillis(),
    val logbookId: Long,
    val studentName: String,
    val contents: String,
    val activities: String,
    val learnings: String,
    val doubts: String,
    val photoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val feedback: String? = null,
    val isReviewed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
