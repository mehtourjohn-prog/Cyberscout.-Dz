package com.example.model

data class AxisInfo(
    val index: Int,
    val icon: String,
    val nameDz: String,
    val nameAr: String,
    val descDz: String,
    val descAr: String,
    val videoQuery: String
) {
    fun name(dialect: Dialect): String = if (dialect == Dialect.DZ) nameDz else nameAr
    fun desc(dialect: Dialect): String = if (dialect == Dialect.DZ) descDz else descAr
}

data class GameInfo(
    val index: Int,
    val axisIndex: Int,
    val titleDz: String,
    val titleAr: String,
    val icon: String
) {
    fun title(dialect: Dialect): String = if (dialect == Dialect.DZ) titleDz else titleAr
}

data class QuizQuestion(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class ShuffledQuizQuestion(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)
