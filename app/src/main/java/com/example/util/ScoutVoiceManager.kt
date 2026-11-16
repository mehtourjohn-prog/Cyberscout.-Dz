package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.model.Dialect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

data class VoiceOption(
    val id: String,
    val label: String,
    val localeTag: String,
    val isAlgerianOrMaghrebi: Boolean
)

class ScoutVoiceManager(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var toneGenerator: ToneGenerator? = null

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceOption>>(emptyList())
    val availableVoices: StateFlow<List<VoiceOption>> = _availableVoices.asStateFlow()

    private val _selectedVoiceId = MutableStateFlow<String?>(null)
    val selectedVoiceId: StateFlow<String?> = _selectedVoiceId.asStateFlow()

    private val _activeVoiceLabel = MutableStateFlow("صوت عربي جزائري / مغاربي")
    val activeVoiceLabel: StateFlow<String> = _activeVoiceLabel.asStateFlow()

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
        } catch (e: Exception) {
            Log.w("ScoutVoice", "ToneGenerator unavailable", e)
        }

        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                setupProgressListener()
                loadArabicVoices()
            } else {
                Log.w("ScoutVoice", "TextToSpeech init failed with status=$status")
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }
        })
    }

    private fun loadArabicVoices() {
        val engine = tts ?: return
        val allVoices = try {
            engine.voices?.toList().orEmpty()
        } catch (e: Exception) {
            emptyList()
        }

        val arabicVoices = allVoices.filter {
            it.locale.language.equals("ar", ignoreCase = true)
        }.sortedWith(
            compareByDescending<Voice> { it.locale.country.equals("DZ", ignoreCase = true) }
                .thenByDescending { it.locale.country.uppercase() in setOf("TN", "MA", "LY") }
                .thenByDescending { !it.isNetworkConnectionRequired }
                .thenBy { it.name }
        )

        val options = arabicVoices.map { voice ->
            val country = voice.locale.country.uppercase()
            val badge = when (country) {
                "DZ" -> "🇩🇿 جزائري"
                "TN" -> "🇹🇳 مغاربي"
                "MA" -> "🇲🇦 مغاربي"
                "EG" -> "🇪🇬 عربي"
                "SA" -> "🇸🇦 عربي"
                else -> "🌍 عربي ($country)"
            }
            VoiceOption(
                id = voice.name,
                label = "$badge · ${voice.name.takeLast(14)}",
                localeTag = voice.locale.toLanguageTag(),
                isAlgerianOrMaghrebi = country in setOf("DZ", "TN", "MA")
            )
        }

        _availableVoices.value = options

        val bestVoice = arabicVoices.firstOrNull()
        if (bestVoice != null) {
            engine.voice = bestVoice
            _selectedVoiceId.value = bestVoice.name
            val bestOpt = options.first()
            _activeVoiceLabel.value = bestOpt.label
        } else {
            val dzLocale = Locale("ar", "DZ")
            val res = engine.setLanguage(dzLocale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale("ar"))
                _activeVoiceLabel.value = "صوت عربي (الافتراضي)"
            } else {
                _activeVoiceLabel.value = "🇩🇿 عربي جزائري (ar-DZ)"
            }
        }
    }

    fun selectVoice(voiceId: String) {
        val engine = tts ?: return
        val match = try {
            engine.voices?.firstOrNull { it.name == voiceId }
        } catch (e: Exception) {
            null
        }
        if (match != null) {
            engine.voice = match
            _selectedVoiceId.value = match.name
            _activeVoiceLabel.value = _availableVoices.value.firstOrNull { it.id == voiceId }?.label ?: match.name
        }
    }

    fun toggleMute() {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        if (newMuted) {
            stopSpeaking()
        }
    }

    fun speak(text: String, dialect: Dialect) {
        if (_isMuted.value || !ttsReady) return
        val engine = tts ?: return
        val cleaned = text
            .replace(Regex("[⭐🎖️🏅🔐🌲🛡️🔎⏳💬🇩🇿📖✓✕•*#_`~]"), " ")
            .replace("CyberScout", "سايبر سكوت")
            .replace("2FA", "المصادقة الثنائية")
            .replace("PDF", "بي دي إف")
            .replace("OTP", "كود التحقق")
            .trim()
        if (cleaned.isEmpty()) return

        // Slightly livelier pace for Algerian Darija scout narration
        engine.setPitch(if (dialect == Dialect.DZ) 1.03f else 1.0f)
        engine.setSpeechRate(if (dialect == Dialect.DZ) 0.97f else 0.95f)
        engine.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _isSpeaking.value = false
    }

    fun playSuccessChime() {
        if (_isMuted.value) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 160)
        } catch (_: Exception) {
        }
    }

    fun playWrongTone() {
        if (_isMuted.value) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 180)
        } catch (_: Exception) {
        }
    }

    fun playVictoryFanfare() {
        if (_isMuted.value) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 320)
        } catch (_: Exception) {
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            toneGenerator?.release()
        } catch (_: Exception) {
        }
    }
}
