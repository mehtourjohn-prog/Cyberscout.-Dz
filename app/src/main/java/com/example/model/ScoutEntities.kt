package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class Dialect(val code: String) {
    DZ("dz"),
    AR("ar");

    companion object {
        fun fromCode(code: String?): Dialect = if (code == "ar") AR else DZ
    }
}

enum class AgeGroup(val code: String) {
    CUBS("cubs"),
    SCOUTS("scouts");

    companion object {
        fun fromCode(code: String?): AgeGroup = if (code == "cubs") CUBS else SCOUTS
    }
}

@IgnoreExtraProperties
data class ScoutProfile(
    val userId: String = "",
    val scoutName: String = "",
    val ageGroup: String = AgeGroup.SCOUTS.code,
    val dialect: String = Dialect.DZ.code,
    val totalPoints: Long = 0L,
    val completedCount: Long = 0L,
    val badgesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GameProgress(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val gameIndex: Long = 0L,
    val axisIndex: Long = 0L,
    val bestScore: Long = 0L,
    val attempts: Long = 0L,
    val passed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sender: String = "bot",
    val text: String = "",
    val dialect: String = Dialect.DZ.code,
    @get:PropertyName("isWarning")
    @field:PropertyName("isWarning")
    val isWarning: Boolean = false,
    val suggestedGameIndex: Long = -1L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DigitalBadge(
    @DocumentId val id: String = "",
    val userId: String = "",
    val axisIndex: Long = 0L,
    val badgeCode: String = "",
    val titleDz: String = "",
    val titleAr: String = "",
    val icon: String = "",
    val passedGamesCount: Long = 0L,
    val totalGamesInAxis: Long = 4L,
    val axisPoints: Long = 0L,
    val unlocked: Boolean = false,
    @get:PropertyName("isFeatured")
    @field:PropertyName("isFeatured")
    val isFeatured: Boolean = false,
    val earnedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

