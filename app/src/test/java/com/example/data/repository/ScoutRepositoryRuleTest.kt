package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.ScoutRepository
import com.example.model.GameProgress
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ScoutRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureProfile_authenticatedOwner_createsAndObservesProfile() = runBlocking {
        val uid = signInTestUser("scout_${UUID.randomUUID()}@test.dz")
        val repo = ScoutRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureProfile(uid, "أمين الكشاف")
        }
        assertTrue(result.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeProfile(uid).first { it != null }
        }
        assertNotNull(profile)
        assertEquals("أمين الكشاف", profile?.scoutName)
        assertEquals("dz", profile?.dialect)
    }

    @Test
    fun saveGameAttempt_completingFullAxis_unlocksDigitalBadgeAndAllowsFeaturing() = runBlocking {
        val uid = signInTestUser("player_${UUID.randomUUID()}@test.dz")
        val repo = ScoutRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureProfile(uid, "نسر الأطلس").getOrThrow()
        }

        val accumulatedProgress = mutableListOf<GameProgress>()
        for (gameIdx in 0..2) {
            val res = withTimeout(DEFAULT_TIMEOUT_MS) {
                repo.saveGameAttempt(uid, gameIndex = gameIdx, axisIndex = 0, score = 8, currentProgress = accumulatedProgress)
            }
            val badgeOrNull = res.getOrThrow()
            assertEquals(null, badgeOrNull)
            accumulatedProgress.add(
                GameProgress(
                    docId = "game_$gameIdx",
                    userId = uid,
                    gameIndex = gameIdx.toLong(),
                    axisIndex = 0L,
                    bestScore = 8L,
                    attempts = 1L,
                    passed = true
                )
            )
        }

        // Passing the 4th game in axis 0 completes the axis and unlocks badge_0
        val fourthResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveGameAttempt(uid, gameIndex = 3, axisIndex = 0, score = 9, currentProgress = accumulatedProgress)
        }
        val unlockedBadge = fourthResult.getOrThrow()
        assertNotNull(unlockedBadge)
        assertTrue(unlockedBadge!!.unlocked)
        assertEquals(4L, unlockedBadge.passedGamesCount)
        assertEquals(330L, unlockedBadge.axisPoints)

        // Pin badge_0 as featured in Firestore
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.setFeaturedBadge(uid, axisIndex = 0).getOrThrow()
        }

        val badges = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeDigitalBadges(uid).first { list -> list.any { it.isFeatured } }
        }
        val badge0 = badges.first { it.axisIndex == 0L }
        assertTrue(badge0.unlocked)
        assertTrue(badge0.isFeatured)
    }

    @Test
    fun observeProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@test.dz")
        val aliceRepo = ScoutRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureProfile(aliceUid, "أمين").getOrThrow()
            aliceRepo.syncAllDigitalBadges(aliceUid, emptyList()).getOrThrow()
        }

        signInTestUser("bob_${UUID.randomUUID()}@test.dz")
        val bobRepo = ScoutRepository(firestore)

        var caughtProfile: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeProfile(aliceUid).first()
            }
        } catch (e: Exception) {
            caughtProfile = e
        }
        val profileEx = checkNotNull(caughtProfile?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $caughtProfile"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, profileEx.code)

        var caughtBadges: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeDigitalBadges(aliceUid).first()
            }
        } catch (e: Exception) {
            caughtBadges = e
        }
        val badgesEx = checkNotNull(caughtBadges?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $caughtBadges"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, badgesEx.code)
    }

    @Test
    fun observeDigitalBadges_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = ScoutRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeDigitalBadges("some_user_123").first()
            }
        } catch (e: Exception) {
            caught = e
        }
        val firestoreEx = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        assertFalse(false)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
