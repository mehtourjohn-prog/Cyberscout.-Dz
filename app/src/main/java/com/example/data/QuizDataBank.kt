package com.example.data

import com.example.model.AxisInfo
import com.example.model.Dialect
import com.example.model.GameInfo
import com.example.model.QuizQuestion
import com.example.model.ShuffledQuizQuestion

object QuizDataBank {
    val axes: List<AxisInfo> = listOf(
        AxisInfo(
            index = 0,
            icon = "🔐",
            nameDz = "حصن السرية",
            nameAr = "حصن السرية",
            descDz = "حماية كلمات السر والمعلومات الشخصية",
            descAr = "حماية كلمات المرور والبيانات الشخصية",
            videoQuery = "السلامة الرقمية حماية كلمات المرور والخصوصية للأطفال"
        ),
        AxisInfo(
            index = 1,
            icon = "🌲",
            nameDz = "غابة الغرباء",
            nameAr = "غابة الغرباء",
            descDz = "الخصوصية والهدرة مع الناس المجهولين",
            descAr = "الخصوصية والتواصل الآمن مع المجهولين",
            videoQuery = "التواصل الآمن مع الغرباء والخصوصية على الإنترنت"
        ),
        AxisInfo(
            index = 2,
            icon = "🛡️",
            nameDz = "درع الفتى",
            nameAr = "درع الفتى",
            descDz = "مواجهة التنمر والابتزاز الإلكتروني",
            descAr = "مواجهة التنمر والابتزاز الإلكتروني",
            videoQuery = "التنمر الإلكتروني والابتزاز الرقمي وكيفية الإبلاغ"
        ),
        AxisInfo(
            index = 3,
            icon = "🔎",
            nameDz = "غربال الأخبار",
            nameAr = "غربال الأخبار",
            descDz = "التثبّت من الإشاعات والأخبار الكاذبة",
            descAr = "التحقق من الشائعات والأخبار الزائفة",
            videoQuery = "التربية الإعلامية التحقق من الأخبار الزائفة والمصادر"
        ),
        AxisInfo(
            index = 4,
            icon = "⏳",
            nameDz = "ساعة الكشاف",
            nameAr = "ساعة الكشاف",
            descDz = "التوازن بين الشاشة وحياة الكشافة والخلاء",
            descAr = "التوازن بين الشاشة وحياة الكشافة والخلاء",
            videoQuery = "الصحة الرقمية إدارة وقت الشاشة والألعاب الإلكترونية"
        )
    )

    val games: List<GameInfo> = listOf(
        GameInfo(0, 0, "ركّب الشفرة", "ركّب الشفرة", "🔑"),
        GameInfo(1, 0, "مختبر كلمة المرور", "مختبر كلمة المرور", "🧪"),
        GameInfo(2, 0, "حصن الحساب", "حصن الحساب", "🏰"),
        GameInfo(3, 0, "صياد كلمات السر", "صياد كلمات السر", "🎣"),

        GameInfo(4, 1, "من خلف الشاشة؟", "من خلف الشاشة؟", "🕵️"),
        GameInfo(5, 1, "رسالة من مجهول", "رسالة من مجهول", "📩"),
        GameInfo(6, 1, "مغامرة الخصوصية", "مغامرة الخصوصية", "🧭"),
        GameInfo(7, 1, "حارس الدردشة", "حارس الدردشة", "💬"),

        GameInfo(8, 2, "تصرّف بصح", "تصرّف بصح", "⚡"),
        GameInfo(9, 2, "وسام الشجاعة الرقمية", "وسام الشجاعة الرقمية", "🏅"),
        GameInfo(10, 2, "أوقف الإساءة", "أوقف الإساءة", "🛑"),
        GameInfo(11, 2, "درع الصداقة", "درع الصداقة", "🤝"),

        GameInfo(12, 3, "كاشف الحقيقة", "كاشف الحقيقة", "🔍"),
        GameInfo(13, 3, "مختبر الأخبار", "مختبر الأخبار", "📰"),
        GameInfo(14, 3, "عين المحقق", "عين المحقق", "👁️"),
        GameInfo(15, 3, "غربال الشائعات", "غربال الشائعات", "⚖️"),

        GameInfo(16, 4, "تحدي 24 ساعة", "تحدي 24 ساعة", "⏱️"),
        GameInfo(17, 4, "ساعة الكشاف", "ساعة الكشاف", "🌙"),
        GameInfo(18, 4, "وازن يومك", "وازن يومك", "☀️"),
        GameInfo(19, 4, "مغامرة خارج الشاشة", "مغامرة خارج الشاشة", "🏕️")
    )

    fun questionsFor(gameIndex: Int, dialect: Dialect): List<QuizQuestion> {
        val safeIdx = gameIndex.coerceIn(0, 19)
        return if (dialect == Dialect.DZ) {
            if (safeIdx < 10) QuizDataDzPart1.games[safeIdx] else QuizDataDzPart2.games[safeIdx - 10]
        } else {
            if (safeIdx < 10) QuizDataArPart1.games[safeIdx] else QuizDataArPart2.games[safeIdx - 10]
        }
    }

    fun shuffledQuestionsFor(gameIndex: Int, dialect: Dialect): List<ShuffledQuizQuestion> {
        return questionsFor(gameIndex, dialect).map { q ->
            val indexed = q.options.mapIndexed { idx, text -> idx to text }.shuffled()
            val newCorrectIndex = indexed.indexOfFirst { it.first == q.correctIndex }
            ShuffledQuizQuestion(
                prompt = q.prompt,
                options = indexed.map { it.second },
                correctIndex = newCorrectIndex,
                explanation = q.explanation
            )
        }
    }
}
