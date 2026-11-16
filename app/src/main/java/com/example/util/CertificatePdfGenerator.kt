package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.QuizDataBank
import com.example.model.Dialect
import com.example.model.DigitalBadge
import com.example.model.GameProgress
import com.example.model.ScoutProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CertificatePdfGenerator {

    fun generateAndShareCertificate(
        context: Context,
        profile: ScoutProfile,
        progress: List<GameProgress>,
        dialect: Dialect
    ): Result<File> {
        return try {
            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            val cairo = ResourcesCompat.getFont(context, R.font.cairo) ?: Typeface.DEFAULT_BOLD
            val logo = loadOfficialLogo(context)

            drawCertificatePage(canvas, cairo, logo, profile, progress, dialect)
            pdf.finishPage(page)

            val outDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val file = File(outDir, "CyberScout_Certificate_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { pdf.writeTo(it) }
            pdf.close()

            openOrSharePdf(context, file, "شهادة الجدارة الرقمية - CyberScout Academy")
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateAndShareBadgesReport(
        context: Context,
        profile: ScoutProfile,
        progress: List<GameProgress>,
        badges: List<DigitalBadge>,
        dialect: Dialect
    ): Result<File> {
        return try {
            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            val cairo = ResourcesCompat.getFont(context, R.font.cairo) ?: Typeface.DEFAULT_BOLD
            val logo = loadOfficialLogo(context)

            drawBadgesReportPage(canvas, cairo, logo, profile, progress, badges, dialect)
            pdf.finishPage(page)

            val outDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val file = File(outDir, "CyberScout_Badges_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { pdf.writeTo(it) }
            pdf.close()

            openOrSharePdf(context, file, "تقرير شارات CyberScout Academy")
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateAndShareSingleBadgeCertificate(
        context: Context,
        profile: ScoutProfile,
        badge: DigitalBadge,
        dialect: Dialect
    ): Result<File> {
        return try {
            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            val cairo = ResourcesCompat.getFont(context, R.font.cairo) ?: Typeface.DEFAULT_BOLD
            val logo = loadOfficialLogo(context)

            drawSingleBadgePage(canvas, cairo, logo, profile, badge, dialect)
            pdf.finishPage(page)

            val outDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val file = File(outDir, "CyberScout_Badge_${badge.axisIndex}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { pdf.writeTo(it) }
            pdf.close()

            val badgeName = if (dialect == Dialect.DZ) badge.titleDz else badge.titleAr
            openOrSharePdf(context, file, "شارة رقمية: $badgeName - CyberScout Academy")
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun loadOfficialLogo(context: Context): Bitmap? {
        return try {
            BitmapFactory.decodeResource(context.resources, R.drawable.logo_cyberscout_official)
        } catch (_: Exception) {
            null
        }
    }

    private fun drawLogo(canvas: Canvas, logo: Bitmap?, rect: RectF) {
        if (logo != null) {
            canvas.drawBitmap(logo, null, rect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        }
    }

    private fun drawCertificatePage(
        canvas: Canvas,
        typeface: Typeface,
        logo: Bitmap?,
        profile: ScoutProfile,
        progress: List<GameProgress>,
        dialect: Dialect
    ) {
        canvas.drawColor(Color.parseColor("#FAFCF9"))

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 10f
            color = Color.parseColor("#064D27")
        }
        canvas.drawRoundRect(RectF(22f, 22f, 820f, 573f), 18f, 18f, borderPaint)

        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.parseColor("#D9A50A")
        }
        canvas.drawRoundRect(RectF(36f, 36f, 806f, 559f), 12f, 12f, goldPaint)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0D7B3E")
        }
        canvas.drawRoundRect(RectF(56f, 52f, 786f, 134f), 14f, 14f, headerPaint)
        drawLogo(canvas, logo, RectF(68f, 56f, 136f, 128f))

        drawCenteredText(
            canvas,
            "أكاديمية السلوك الرقمي الآمن · CyberScout Academy DZ",
            typeface,
            22f,
            Color.WHITE,
            66f,
            620
        )
        drawCenteredText(
            canvas,
            if (dialect == Dialect.DZ) "شهادة الجدارة الرقمية الكشفية" else "شهادة الجدارة في السلامة الرقمية",
            typeface,
            18f,
            Color.parseColor("#FBE40A"),
            102f,
            620
        )

        drawCenteredText(
            canvas,
            if (dialect == Dialect.DZ) "تشهد الأكاديمية بلي الكشاف البطل:" else "تشهد الأكاديمية بأن الكشاف المتميز:",
            typeface,
            18f,
            Color.parseColor("#476656"),
            175f,
            700
        )

        drawCenteredText(
            canvas,
            profile.scoutName.ifBlank { "كشاف واعي" },
            typeface,
            34f,
            Color.parseColor("#064D27"),
            215f,
            700
        )

        val ageLabel = if (profile.ageGroup == "cubs") "الأشبال والزهرات (8–11)" else "الفتيان والفتيات (12–15)"
        val summary = if (dialect == Dialect.DZ) {
            "كمّل بنجاح جميع تحديات الأمان الرقمي (20/20 لعبة) بمجموع ${profile.totalPoints} نقطة وحصد ${profile.badgesCount}/5 شارات رقمية ($ageLabel)."
        } else {
            "أتمّ بنجاح جميع تحديات السلامة الرقمية (20/20 لعبة) بمجموع ${profile.totalPoints} نقطة وحصل على ${profile.badgesCount}/5 شارات رقمية ($ageLabel)."
        }
        drawCenteredText(canvas, summary, typeface, 16f, Color.parseColor("#10301F"), 280f, 680)

        val earnedAxes = QuizDataBank.axes.filter { axis ->
            progress.count { it.axisIndex.toInt() == axis.index && it.passed } >= 4
        }
        val badgesLine = if (earnedAxes.isNotEmpty()) {
            earnedAxes.joinToString("  •  ") { "${it.icon} ${it.name(dialect)}" }
        } else {
            "🔐  •  🌲  •  🛡️  •  🔎  •  ⏳"
        }
        drawCenteredText(canvas, badgesLine, typeface, 15f, Color.parseColor("#0D7B3E"), 345f, 720)

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val verifyCode = "CS-DZ-${profile.userId.take(6).uppercase(Locale.US)}-${progress.size}"

        val sealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FBE40A")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(421f, 445f, 42f, sealPaint)
        canvas.drawCircle(421f, 445f, 36f, goldPaint)
        drawCenteredText(canvas, "★ معتمد ★", typeface, 13f, Color.parseColor("#064D27"), 436f, 120)

        drawCenteredText(
            canvas,
            "التاريخ: $dateStr    |    رمز التحقق: $verifyCode",
            typeface,
            13f,
            Color.parseColor("#587666"),
            510f,
            680
        )
    }

    private fun drawBadgesReportPage(
        canvas: Canvas,
        typeface: Typeface,
        logo: Bitmap?,
        profile: ScoutProfile,
        progress: List<GameProgress>,
        badges: List<DigitalBadge>,
        dialect: Dialect
    ) {
        canvas.drawColor(Color.parseColor("#F5FBF7"))
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 8f
            color = Color.parseColor("#0D7B3E")
        }
        canvas.drawRoundRect(RectF(24f, 24f, 818f, 571f), 16f, 16f, borderPaint)
        drawLogo(canvas, logo, RectF(46f, 36f, 114f, 104f))

        drawCenteredText(
            canvas,
            "تقرير الشارات الرقمية · CyberScout Academy DZ 🎖️",
            typeface,
            23f,
            Color.parseColor("#064D27"),
            46f,
            640
        )
        drawCenteredText(
            canvas,
            "الكشاف: ${profile.scoutName}  |  النقاط: ${profile.totalPoints}  |  التحديات المكتملة: ${profile.completedCount}/20",
            typeface,
            15f,
            Color.parseColor("#587666"),
            92f,
            660
        )

        val badgesByAxis = badges.associateBy { it.axisIndex.toInt() }
        var topY = 148f
        QuizDataBank.axes.forEach { axis ->
            val badgeDoc = badgesByAxis[axis.index]
            val passedCount = badgeDoc?.passedGamesCount?.toInt()
                ?: progress.count { it.axisIndex.toInt() == axis.index && it.passed }
            val unlocked = badgeDoc?.unlocked ?: (passedCount >= 4)
            val codeSuffix = badgeDoc?.badgeCode?.takeIf { it.isNotBlank() }?.let { " | $it" } ?: ""

            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (unlocked) Color.parseColor("#DFF8E9") else Color.parseColor("#FFFFFF")
            }
            val rect = RectF(66f, topY, 776f, topY + 66f)
            canvas.drawRoundRect(rect, 12f, 12f, cardPaint)
            val status = if (unlocked) {
                "✅ مكتسبة (4/4 ألعاب$codeSuffix)"
            } else {
                "⏳ قيد الإنجاز ($passedCount/4 ألعاب)"
            }
            drawCenteredText(
                canvas,
                "${axis.icon}  ${axis.name(dialect)}  —  $status",
                typeface,
                15f,
                if (unlocked) Color.parseColor("#064D27") else Color.parseColor("#587666"),
                topY + 20f,
                680
            )
            topY += 79f
        }
    }

    private fun drawSingleBadgePage(
        canvas: Canvas,
        typeface: Typeface,
        logo: Bitmap?,
        profile: ScoutProfile,
        badge: DigitalBadge,
        dialect: Dialect
    ) {
        canvas.drawColor(Color.parseColor("#F5FBF7"))
        val outerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 10f
            color = Color.parseColor("#064D27")
        }
        canvas.drawRoundRect(RectF(24f, 24f, 818f, 571f), 18f, 18f, outerBorder)

        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.parseColor("#D9A50A")
        }
        canvas.drawRoundRect(RectF(38f, 38f, 804f, 557f), 12f, 12f, goldPaint)

        drawLogo(canvas, logo, RectF(371f, 52f, 471f, 152f))

        drawCenteredText(
            canvas,
            "شهادة شارة رقمية معتمدة · CyberScout Academy DZ",
            typeface,
            21f,
            Color.parseColor("#064D27"),
            165f,
            700
        )

        val badgeTitle = if (dialect == Dialect.DZ) badge.titleDz else badge.titleAr
        drawCenteredText(
            canvas,
            "${badge.icon}  $badgeTitle",
            typeface,
            32f,
            Color.parseColor("#0D7B3E"),
            220f,
            700
        )

        drawCenteredText(
            canvas,
            "مُنحت هذه الشارة الرقمية للكشاف: ${profile.scoutName.ifBlank { "كشاف واعي" }}",
            typeface,
            20f,
            Color.parseColor("#10301F"),
            290f,
            700
        )

        drawCenteredText(
            canvas,
            "لإتمامه جميع تحديات المحور (4/4 ألعاب) بمجموع ${badge.axisPoints}/400 نقطة",
            typeface,
            16f,
            Color.parseColor("#476656"),
            345f,
            700
        )

        val earnedDate = badge.earnedAt?.toDate() ?: Date()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(earnedDate)
        drawCenteredText(
            canvas,
            "رمز التحقق الرقمي: ${badge.badgeCode}    |    تاريخ الاستحقاق: $dateStr",
            typeface,
            14f,
            Color.parseColor("#064D27"),
            455f,
            720
        )
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        typeface: Typeface,
        textSize: Float,
        color: Int,
        topY: Float,
        width: Int
    ) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = textSize
            this.color = color
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate((842f - width) / 2f, topY)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun openOrSharePdf(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
