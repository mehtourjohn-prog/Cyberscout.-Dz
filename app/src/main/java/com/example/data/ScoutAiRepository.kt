package com.example.data

import android.util.Log
import com.example.model.Dialect
import com.example.model.ScoutProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "ScoutAiRepo"

data class AiStreamChunk(
    val accumulatedText: String,
    val suggestedGameIndex: Int = -1
)

class ScoutAiRepository {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    أنت "الكشاف أمين" (Scout Amin)، مرشد كشفي جزائري خبير في السلوك الرقمي الآمن وحماية الأطفال والمراهقين في أكاديمية CyberScout بالجزائر.
                    قواعد شخصيتك:
                    1. التزم دائماً باللهجة المطلوبة في السياق:
                       - إذا كانت اللهجة (dz): تكلم بالدارجة الجزائرية الأصيلة والمفهومة والمهذبة والمشجعة (مثلاً: "أهلاً خويا/أختي الكشاف"، "يعطيك الصحة"، "رد بالك"، "حذاري"، "فوجك"، "ما تخافش"، "خبر والديك ولا القائد").
                       - إذا كانت اللهجة (ar): تكلم بالعربية الفصحى الميسرة الدافئة والمناسبة للكشافين.
                    2. قدّم خطوات عملية واضحة وقصيرة ومطمئنة، مناسبة للأعمار من 8 إلى 15 سنة.
                    3. في حالات الابتزاز أو التهديد أو التحرش في الجزائر: طمئن الكشاف، امنعه من دفع أي مال أو إرسال صور، اطلب منه حفظ الأدلة (Screenshot)، وإبلاغ الوالدين أو القائد الكشفي فوراً، وأشر إلى أن الأمن الوطني والدرك الوطني في الجزائر لديهم خلايا متخصصة وسرية للجرائم المعلوماتية.
                    4. لا تطلب أبداً معلومات شخصية (اسم العائلة، عنوان، هاتف، كلمة مرور).
                    5. إذا كان هناك تحدي في الأكاديمية يناسب سؤال المستخدم، أضف في آخر ردك سطراً مستقلاً بصيغة [GAME:X] حيث X رقم اللعبة من 0 إلى 19:
                       0: ركّب الشفرة، 1: مختبر كلمة المرور، 2: حصن الحساب، 3: صياد كلمات السر،
                       4: من خلف الشاشة؟، 5: رسالة من مجهول، 6: مغامرة الخصوصية، 7: حارس الدردشة،
                       8: تصرّف بصح، 9: وسام الشجاعة الرقمية، 10: أوقف الإساءة (الابتزاز)، 11: درع الصداقة،
                       12: كاشف الحقيقة، 13: مختبر الأخبار، 14: عين المحقق (AI/Deepfake)، 15: غربال الشائعات،
                       16: تحدي 24 ساعة، 17: ساعة الكشاف، 18: وازن يومك، 19: مغامرة خارج الشاشة.
                    """.trimIndent()
                )
            }
        )
    }

    fun streamReply(
        userMessage: String,
        dialect: Dialect,
        profile: ScoutProfile?,
        recentHistory: List<Pair<String, String>>
    ): Flow<AiStreamChunk> = flow {
        val dialectPrompt = if (dialect == Dialect.DZ) {
            "اللهجة المطلوبة للرد: الدارجة الجزائرية (DZ)."
        } else {
            "اللهجة المطلوبة للرد: العربية الفصحى المبسطة (AR)."
        }
        val progressContext = if (profile != null) {
            "معلومات الكشاف: الاسم=${profile.scoutName}، الفئة=${profile.ageGroup}، التحديات المكتملة=${profile.completedCount}/20، النقاط=${profile.totalPoints}، الشارات=${profile.badgesCount}/5."
        } else {
            "كشاف زائر في الأكاديمية."
        }

        val chatHistory = recentHistory.takeLast(8).map { (role, text) ->
            content(role = if (role == "user") "user" else "model") {
                text(text)
            }
        }

        val enrichedUserPrompt = """
            [$dialectPrompt]
            [$progressContext]
            سؤال الكشاف: $userMessage
        """.trimIndent()

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                val chat = model.startChat(history = chatHistory)
                chat.sendMessageStream(enrichedUserPrompt).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        accumulated.append(chunkText)
                        val (cleanText, gameIdx) = parseGameTag(accumulated.toString())
                        emit(AiStreamChunk(cleanText, gameIdx))
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("لم يُرجع المساعد الذكي أي رد.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI chat timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            throw e
        }
    }

    companion object {
        private val GAME_TAG_REGEX = Regex("\\[GAME:(\\d{1,2})]")

        fun parseGameTag(raw: String): Pair<String, Int> {
            val match = GAME_TAG_REGEX.find(raw)
            val gameIdx = match?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..19 } ?: -1
            val cleaned = raw.replace(GAME_TAG_REGEX, "").trim()
            return cleaned to gameIdx
        }
    }
}
