package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiStreamChunk
import com.example.data.QuizDataBank
import com.example.data.ScoutAiRepository
import com.example.data.ScoutRepository
import com.example.model.AppStrings
import com.example.model.ChatMessage
import com.example.model.Dialect
import com.example.model.DigitalBadge
import com.example.model.GameProgress
import com.example.model.ScoutProfile
import com.example.model.ShuffledQuizQuestion
import com.example.util.ScoutVoiceManager
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

sealed interface ChatAiState {
    data object Idle : ChatAiState
    data object Loading : ChatAiState
    data class Streaming(val text: String, val suggestedGameIndex: Int = -1) : ChatAiState
    data class Error(val message: String) : ChatAiState
}

data class ActiveQuizState(
    val gameIndex: Int,
    val axisIndex: Int,
    val questions: List<ShuffledQuizQuestion>,
    val currentIndex: Int = 0,
    val selectedOption: Int? = null,
    val score: Int = 0,
    val isFinished: Boolean = false
)

class ScoutViewModel(
    private val repository: ScoutRepository,
    private val aiRepository: ScoutAiRepository,
    val voiceManager: ScoutVoiceManager,
    private val currentUserId: String,
    defaultDisplayName: String
) : ViewModel() {

    private val _dialect = MutableStateFlow(Dialect.DZ)
    val dialect: StateFlow<Dialect> = _dialect.asStateFlow()

    private val _selectedAxisFilter = MutableStateFlow<Int?>(null)
    val selectedAxisFilter: StateFlow<Int?> = _selectedAxisFilter.asStateFlow()

    private val _activeQuiz = MutableStateFlow<ActiveQuizState?>(null)
    val activeQuiz: StateFlow<ActiveQuizState?> = _activeQuiz.asStateFlow()

    private val _chatAiState = MutableStateFlow<ChatAiState>(ChatAiState.Idle)
    val chatAiState: StateFlow<ChatAiState> = _chatAiState.asStateFlow()

    private val _autoSpeakChat = MutableStateFlow(false)
    val autoSpeakChat: StateFlow<Boolean> = _autoSpeakChat.asStateFlow()

    private val _newlyEarnedBadge = MutableStateFlow<DigitalBadge?>(null)
    val newlyEarnedBadge: StateFlow<DigitalBadge?> = _newlyEarnedBadge.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var initialBadgeSyncTriggered = false

    val profileState: StateFlow<DataUiState<ScoutProfile?>> = repository.observeProfile(currentUserId)
        .map<ScoutProfile?, DataUiState<ScoutProfile?>> { profile ->
            if (profile != null) {
                _dialect.value = Dialect.fromCode(profile.dialect)
            }
            DataUiState.Success(profile)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing profile", root)
            emit(DataUiState.Error(root.message ?: "تعذر تحميل ملف الكشاف"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataUiState.Loading
        )

    val progressState: StateFlow<DataUiState<List<GameProgress>>> = repository.observeGameProgress(currentUserId)
        .map<List<GameProgress>, DataUiState<List<GameProgress>>> { DataUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing game progress", root)
            emit(DataUiState.Error(root.message ?: "تعذر تحميل التقدم"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataUiState.Loading
        )

    val badgesState: StateFlow<DataUiState<List<DigitalBadge>>> = repository.observeDigitalBadges(currentUserId)
        .map<List<DigitalBadge>, DataUiState<List<DigitalBadge>>> { DataUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing digital badges", root)
            emit(DataUiState.Error(root.message ?: "تعذر تحميل الشارات الرقمية"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataUiState.Loading
        )

    val chatMessagesState: StateFlow<DataUiState<List<ChatMessage>>> = repository.observeChatMessages(currentUserId)
        .map<List<ChatMessage>, DataUiState<List<ChatMessage>>> { DataUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing chat messages", root)
            emit(DataUiState.Error(root.message ?: "تعذر تحميل المحادثة"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataUiState.Loading
        )

    init {
        viewModelScope.launch {
            repository.ensureProfile(currentUserId, defaultDisplayName)
        }
        viewModelScope.launch {
            combine(profileState, progressState, badgesState) { prof, prog, badges ->
                Triple(prof, prog, badges)
            }.collect { (prof, prog, badges) ->
                if (!initialBadgeSyncTriggered &&
                    prof is DataUiState.Success && prof.data != null &&
                    prog is DataUiState.Success &&
                    badges is DataUiState.Success &&
                    badges.data.size < QuizDataBank.axes.size
                ) {
                    initialBadgeSyncTriggered = true
                    repository.syncAllDigitalBadges(currentUserId, prog.data)
                }
            }
        }
    }

    fun toggleDialect() {
        val next = if (_dialect.value == Dialect.DZ) Dialect.AR else Dialect.DZ
        _dialect.value = next
        viewModelScope.launch {
            repository.updateProfileSettings(currentUserId, dialect = next.code)
        }
    }

    fun setAxisFilter(axisIndex: Int?) {
        _selectedAxisFilter.value = axisIndex
    }

    fun startGame(gameIndex: Int) {
        val game = QuizDataBank.games.getOrNull(gameIndex) ?: return
        val questions = QuizDataBank.shuffledQuestionsFor(game.index, _dialect.value)
        _activeQuiz.value = ActiveQuizState(
            gameIndex = game.index,
            axisIndex = game.axisIndex,
            questions = questions
        )
    }

    fun startFirstUnfinishedGame() {
        val progress = (progressState.value as? DataUiState.Success)?.data.orEmpty()
        val passedIndices = progress.filter { it.passed }.map { it.gameIndex.toInt() }.toSet()
        val nextIdx = (0..19).firstOrNull { it !in passedIndices } ?: 0
        startGame(nextIdx)
    }

    fun answerCurrentQuestion(optionIndex: Int) {
        val quiz = _activeQuiz.value ?: return
        if (quiz.selectedOption != null || quiz.isFinished) return
        val currentQ = quiz.questions.getOrNull(quiz.currentIndex) ?: return
        val isCorrect = optionIndex == currentQ.correctIndex
        val newScore = if (isCorrect) quiz.score + 1 else quiz.score

        _activeQuiz.update {
            it?.copy(selectedOption = optionIndex, score = newScore)
        }

        val strings = AppStrings.forDialect(_dialect.value)
        if (isCorrect) {
            voiceManager.playSuccessChime()
            voiceManager.speak(strings.correctSpoken.random(), _dialect.value)
        } else {
            voiceManager.playWrongTone()
            voiceManager.speak("${strings.wrongSpokenPrefix}${currentQ.explanation}", _dialect.value)
        }
    }

    fun readCurrentQuestion() {
        val quiz = _activeQuiz.value ?: return
        val currentQ = quiz.questions.getOrNull(quiz.currentIndex) ?: return
        val speechText = buildString {
            append(currentQ.prompt)
            append(". ")
            currentQ.options.forEachIndexed { index, opt ->
                append("الخيار ${index + 1}: $opt. ")
            }
        }
        voiceManager.speak(speechText, _dialect.value)
    }

    fun nextQuizStep() {
        val quiz = _activeQuiz.value ?: return
        if (quiz.currentIndex + 1 < quiz.questions.size) {
            _activeQuiz.update {
                it?.copy(currentIndex = it.currentIndex + 1, selectedOption = null)
            }
        } else {
            _activeQuiz.update { it?.copy(isFinished = true) }
            val strings = AppStrings.forDialect(_dialect.value)
            if (quiz.score >= 7) {
                voiceManager.playVictoryFanfare()
            }
            voiceManager.speak(strings.finishSpoken(quiz.score), _dialect.value)

            val currentProgress = (progressState.value as? DataUiState.Success)?.data.orEmpty()
            viewModelScope.launch {
                val result = repository.saveGameAttempt(
                    userId = currentUserId,
                    gameIndex = quiz.gameIndex,
                    axisIndex = quiz.axisIndex,
                    score = quiz.score,
                    currentProgress = currentProgress
                )
                result.onSuccess { unlockedBadge ->
                    if (unlockedBadge != null) {
                        _newlyEarnedBadge.value = unlockedBadge
                        val badgeTitle = if (_dialect.value == Dialect.DZ) {
                            unlockedBadge.titleDz
                        } else {
                            unlockedBadge.titleAr
                        }
                        voiceManager.playVictoryFanfare()
                        voiceManager.speak(
                            "${strings.newlyEarnedTitle}: $badgeTitle",
                            _dialect.value
                        )
                    }
                }.onFailure { error ->
                    _toastMessage.value = error.localizedMessage ?: "تعذر حفظ النتيجة"
                }
            }
        }
    }

    fun dismissNewlyEarnedBadge() {
        _newlyEarnedBadge.value = null
    }

    fun syncDigitalBadges() {
        val currentProgress = (progressState.value as? DataUiState.Success)?.data.orEmpty()
        viewModelScope.launch {
            val res = repository.syncAllDigitalBadges(currentUserId, currentProgress)
            _toastMessage.value = if (res.isSuccess) {
                if (_dialect.value == Dialect.DZ) "تمت مزامنة الشارات الرقمية مع السحابة!" else "تمت مزامنة الشارات الرقمية بنجاح!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "تعذر مزامنة الشارات"
            }
        }
    }

    fun setFeaturedBadge(axisIndex: Int) {
        viewModelScope.launch {
            val res = repository.setFeaturedBadge(currentUserId, axisIndex)
            _toastMessage.value = if (res.isSuccess) {
                if (_dialect.value == Dialect.DZ) "تم تثبيت الشارة في ملفك!" else "تم تثبيت الشارة المميزة بنجاح!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "تعذر تثبيت الشارة"
            }
        }
    }

    fun retryActiveQuiz() {
        val quiz = _activeQuiz.value ?: return
        startGame(quiz.gameIndex)
    }

    fun closeQuiz() {
        voiceManager.stopSpeaking()
        _activeQuiz.value = null
    }

    fun toggleAutoSpeakChat() {
        _autoSpeakChat.value = !_autoSpeakChat.value
    }

    fun sendChatMessage(rawText: String) {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty() || _chatAiState.value is ChatAiState.Loading || _chatAiState.value is ChatAiState.Streaming) {
            return
        }

        val currentDialect = _dialect.value
        val strings = AppStrings.forDialect(currentDialect)
        val hasPii = containsSensitivePii(trimmed)
        val profile = (profileState.value as? DataUiState.Success)?.data
        val existingMessages = (chatMessagesState.value as? DataUiState.Success)?.data.orEmpty()
        val historyPairs = existingMessages.map { it.sender to it.text }

        viewModelScope.launch {
            repository.addChatMessage(
                userId = currentUserId,
                sender = "user",
                text = trimmed,
                dialect = currentDialect.code
            )

            if (hasPii) {
                repository.addChatMessage(
                    userId = currentUserId,
                    sender = "bot",
                    text = strings.piiWarning,
                    dialect = currentDialect.code,
                    isWarning = true,
                    suggestedGameIndex = 6
                )
                if (_autoSpeakChat.value) {
                    voiceManager.speak(strings.piiWarning, currentDialect)
                }
            }

            _chatAiState.value = ChatAiState.Loading
            var finalChunk = AiStreamChunk("")
            try {
                aiRepository.streamReply(
                    userMessage = trimmed,
                    dialect = currentDialect,
                    profile = profile,
                    recentHistory = historyPairs
                ).collect { chunk ->
                    finalChunk = chunk
                    _chatAiState.value = ChatAiState.Streaming(
                        text = chunk.accumulatedText,
                        suggestedGameIndex = chunk.suggestedGameIndex
                    )
                }

                if (finalChunk.accumulatedText.isNotBlank()) {
                    repository.addChatMessage(
                        userId = currentUserId,
                        sender = "bot",
                        text = finalChunk.accumulatedText,
                        dialect = currentDialect.code,
                        isWarning = false,
                        suggestedGameIndex = finalChunk.suggestedGameIndex
                    )
                    if (_autoSpeakChat.value) {
                        voiceManager.speak(finalChunk.accumulatedText, currentDialect)
                    }
                }
                _chatAiState.value = ChatAiState.Idle
            } catch (e: TimeoutCancellationException) {
                _chatAiState.value = ChatAiState.Error(
                    if (currentDialect == Dialect.DZ) {
                        "الكشاف أمين طول باش يجاوب (انتهى الوقت). عاود ابعث سؤالك!"
                    } else {
                        "انتهت مهلة الاتصال بالمساعد الذكي. يرجى المحاولة مرة أخرى."
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _chatAiState.value = ChatAiState.Error(
                    e.localizedMessage ?: if (currentDialect == Dialect.DZ) {
                        "صار مشكل في الاتصال بالشات الذكية."
                    } else {
                        "تعذر الاتصال بالمساعد الذكي."
                    }
                )
            }
        }
    }

    fun dismissAiError() {
        if (_chatAiState.value is ChatAiState.Error) {
            _chatAiState.value = ChatAiState.Idle
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChatMessages(currentUserId)
        }
    }

    fun updateProfile(scoutName: String, ageGroupCode: String) {
        viewModelScope.launch {
            val res = repository.updateProfileSettings(
                userId = currentUserId,
                scoutName = scoutName,
                ageGroup = ageGroupCode
            )
            _toastMessage.value = if (res.isSuccess) {
                if (_dialect.value == Dialect.DZ) "تم حفظ بيانات الكشاف!" else "تم حفظ بيانات الملف بنجاح!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "تعذر حفظ البيانات"
            }
        }
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }

    private fun containsSensitivePii(input: String): Boolean {
        val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
        val phoneRegex = Regex("(\\+?213|0)[567][0-9]{8}|\\b\\d{9,12}\\b")
        val passHint = Regex("(كلمة\\s*السر|password|mot\\s*de\\s*passe)\\s*[:=]?\\s*\\S+", RegexOption.IGNORE_CASE)
        return emailRegex.containsMatchIn(input) || phoneRegex.containsMatchIn(input) || passHint.containsMatchIn(input)
    }

    private companion object {
        const val TAG = "ScoutViewModel"
    }
}
