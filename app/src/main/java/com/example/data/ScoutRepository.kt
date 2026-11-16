package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AgeGroup
import com.example.model.ChatMessage
import com.example.model.Dialect
import com.example.model.DigitalBadge
import com.example.model.GameProgress
import com.example.model.ScoutProfile
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.math.max

class ScoutRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeProfile(userId: String): Flow<ScoutProfile?> {
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        ScoutProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
    }

    suspend fun ensureProfile(userId: String, defaultName: String): Result<Unit> {
        val docRef = db.collection("users").document(userId)
        return try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = defaultName.trim().ifBlank { "كشاف واعي" }.take(60)
                val payload = mapOf(
                    "userId" to userId,
                    "scoutName" to safeName,
                    "ageGroup" to AgeGroup.SCOUTS.code,
                    "dialect" to Dialect.DZ.code,
                    "totalPoints" to 0L,
                    "completedCount" to 0L,
                    "badgesCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProfileSettings(
        userId: String,
        scoutName: String? = null,
        ageGroup: String? = null,
        dialect: String? = null
    ): Result<Unit> {
        val docRef = db.collection("users").document(userId)
        return try {
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            scoutName?.trim()?.takeIf { it.isNotEmpty() }?.let {
                updates["scoutName"] = it.take(60)
            }
            ageGroup?.let { updates["ageGroup"] = it }
            dialect?.let { updates["dialect"] = it }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeGameProgress(userId: String): Flow<List<GameProgress>> {
        return db.collection("users").document(userId)
            .collection("gameProgress")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    GameProgress::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
    }

    fun observeDigitalBadges(userId: String): Flow<List<DigitalBadge>> {
        return db.collection("users").document(userId)
            .collection("badges")
            .orderBy("axisIndex", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DigitalBadge::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
    }

    /**
     * Saves a game attempt, updates the corresponding Firestore DigitalBadge document for the axis,
     * and returns the newly unlocked DigitalBadge if completing this game finished the axis (4/4).
     */
    suspend fun saveGameAttempt(
        userId: String,
        gameIndex: Int,
        axisIndex: Int,
        score: Int,
        currentProgress: List<GameProgress>
    ): Result<DigitalBadge?> {
        val gameId = "game_$gameIndex"
        val gameDocRef = db.collection("users").document(userId)
            .collection("gameProgress").document(gameId)
        val userDocRef = db.collection("users").document(userId)

        return try {
            val safeScore = score.coerceIn(0, 10)
            val existing = gameDocRef.get().await()
            val prevBest = (existing.getLong("bestScore") ?: 0L).toInt()
            val prevAttempts = (existing.getLong("attempts") ?: 0L).toInt()
            val newBest = max(prevBest, safeScore)
            val passed = newBest >= 7

            if (existing.exists()) {
                gameDocRef.update(
                    mapOf(
                        "bestScore" to newBest.toLong(),
                        "attempts" to (prevAttempts + 1).toLong(),
                        "passed" to passed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                gameDocRef.set(
                    mapOf(
                        "userId" to userId,
                        "gameIndex" to gameIndex.toLong(),
                        "axisIndex" to axisIndex.toLong(),
                        "bestScore" to newBest.toLong(),
                        "attempts" to 1L,
                        "passed" to passed,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Recompute progress map across all games
            val updatedByGame = currentProgress.associateBy { it.gameIndex.toInt() }.toMutableMap()
            updatedByGame[gameIndex] = GameProgress(
                docId = gameId,
                userId = userId,
                gameIndex = gameIndex.toLong(),
                axisIndex = axisIndex.toLong(),
                bestScore = newBest.toLong(),
                attempts = (prevAttempts + 1).toLong(),
                passed = passed
            )

            val allUpdatedProgress = updatedByGame.values.toList()
            val newlyUnlocked = upsertAxisDigitalBadge(
                userId = userId,
                axisIndex = axisIndex,
                progressList = allUpdatedProgress
            )

            val totalPoints = updatedByGame.values.sumOf { it.bestScore * 10L }
            val completedCount = updatedByGame.values.count { it.bestScore >= 7L }.toLong()
            val badgesCount = (0..4).count { ax ->
                updatedByGame.values.count { it.axisIndex.toInt() == ax && it.bestScore >= 7L } >= 4
            }.toLong()

            userDocRef.update(
                mapOf(
                    "totalPoints" to totalPoints.coerceIn(0L, 2000L),
                    "completedCount" to completedCount.coerceIn(0L, 20L),
                    "badgesCount" to badgesCount.coerceIn(0L, 5L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(newlyUnlocked)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, gameDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncAllDigitalBadges(
        userId: String,
        progressList: List<GameProgress>
    ): Result<Unit> {
        val badgesCol = db.collection("users").document(userId).collection("badges")
        return try {
            for (axis in QuizDataBank.axes) {
                upsertAxisDigitalBadge(userId, axis.index, progressList)
            }
            val badgesCount = (0..4).count { ax ->
                progressList.count { it.axisIndex.toInt() == ax && it.bestScore >= 7L } >= 4
            }.toLong()
            db.collection("users").document(userId).update(
                mapOf(
                    "badgesCount" to badgesCount.coerceIn(0L, 5L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, badgesCol.path)
            Result.failure(e)
        }
    }

    suspend fun setFeaturedBadge(userId: String, axisIndex: Int): Result<Unit> {
        val badgesCol = db.collection("users").document(userId).collection("badges")
        return try {
            val snap = badgesCol.get().await()
            val targetDocId = "badge_$axisIndex"
            val targetDoc = snap.documents.firstOrNull { it.id == targetDocId }
                ?: throw IllegalStateException("الشارة غير موجودة بعد")
            val isUnlocked = targetDoc.getBoolean("unlocked") == true
            if (!isUnlocked) {
                throw IllegalStateException("أكمل المحور (4/4 ألعاب) لفتح الشارة قبل تثبيتها")
            }

            val batch = db.batch()
            snap.documents.forEach { doc ->
                val shouldFeature = doc.id == targetDocId
                if ((doc.getBoolean("isFeatured") == true) != shouldFeature) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "isFeatured" to shouldFeature,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, badgesCol.path)
            Result.failure(e)
        }
    }

    private suspend fun upsertAxisDigitalBadge(
        userId: String,
        axisIndex: Int,
        progressList: List<GameProgress>
    ): DigitalBadge? {
        val axis = QuizDataBank.axes.getOrNull(axisIndex) ?: return null
        val badgeId = "badge_$axisIndex"
        val badgeRef = db.collection("users").document(userId)
            .collection("badges").document(badgeId)

        val axisGames = progressList.filter { it.axisIndex.toInt() == axisIndex }
        val passedGamesCount = axisGames.count { it.bestScore >= 7L }.coerceIn(0, 4)
        val axisPoints = axisGames.sumOf { it.bestScore * 10L }.coerceIn(0L, 400L)
        val unlockedNow = passedGamesCount >= 4
        val badgeCode = "CS-DZ-AX$axisIndex-${userId.take(6).uppercase(Locale.US)}"

        val existing = badgeRef.get().await()
        val wasUnlocked = existing.exists() && (existing.getBoolean("unlocked") == true)
        val finalUnlocked = wasUnlocked || unlockedNow
        val finalPassedCount = if (finalUnlocked) 4L else passedGamesCount.toLong()
        val finalFeatured = existing.getBoolean("isFeatured") == true && finalUnlocked

        if (!existing.exists()) {
            val payload = hashMapOf<String, Any?>(
                "userId" to userId,
                "axisIndex" to axisIndex.toLong(),
                "badgeCode" to badgeCode,
                "titleDz" to axis.nameDz,
                "titleAr" to axis.nameAr,
                "icon" to axis.icon,
                "passedGamesCount" to finalPassedCount,
                "totalGamesInAxis" to 4L,
                "axisPoints" to axisPoints,
                "unlocked" to finalUnlocked,
                "isFeatured" to false,
                "earnedAt" to if (finalUnlocked) FieldValue.serverTimestamp() else null,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            badgeRef.set(payload).await()
        } else {
            val existingEarnedAt = existing.getTimestamp("earnedAt")
            val updates = hashMapOf<String, Any?>(
                "badgeCode" to badgeCode,
                "titleDz" to axis.nameDz,
                "titleAr" to axis.nameAr,
                "icon" to axis.icon,
                "passedGamesCount" to finalPassedCount,
                "axisPoints" to axisPoints,
                "unlocked" to finalUnlocked,
                "isFeatured" to finalFeatured,
                "earnedAt" to when {
                    wasUnlocked && existingEarnedAt != null -> existingEarnedAt
                    finalUnlocked -> FieldValue.serverTimestamp()
                    else -> null
                },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            badgeRef.update(updates).await()
        }

        return if (!wasUnlocked && finalUnlocked) {
            badgeRef.get().await().toObject(
                DigitalBadge::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } else {
            null
        }
    }

    fun observeChatMessages(userId: String): Flow<List<ChatMessage>> {
        return db.collection("users").document(userId)
            .collection("chatMessages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
    }

    suspend fun addChatMessage(
        userId: String,
        sender: String,
        text: String,
        dialect: String,
        isWarning: Boolean = false,
        suggestedGameIndex: Int = -1
    ): Result<Unit> {
        val docRef = db.collection("users").document(userId)
            .collection("chatMessages").document()
        return try {
            val safeText = text.trim().take(8000).ifBlank { "..." }
            docRef.set(
                mapOf(
                    "userId" to userId,
                    "sender" to sender,
                    "text" to safeText,
                    "dialect" to dialect,
                    "isWarning" to isWarning,
                    "suggestedGameIndex" to suggestedGameIndex.toLong(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun clearChatMessages(userId: String): Result<Unit> {
        val colRef = db.collection("users").document(userId).collection("chatMessages")
        return try {
            val snap = colRef.get().await()
            if (!snap.isEmpty) {
                val batch = db.batch()
                snap.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colRef.path)
            Result.failure(e)
        }
    }
}
