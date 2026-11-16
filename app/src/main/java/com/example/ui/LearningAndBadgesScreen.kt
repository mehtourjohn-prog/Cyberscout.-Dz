package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.QuizDataBank
import com.example.model.AppStrings
import com.example.model.AxisInfo
import com.example.model.Dialect
import com.example.model.DigitalBadge
import com.example.model.GameProgress
import com.example.model.ScoutProfile
import com.example.util.CertificatePdfGenerator
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun LearningAndBadgesScreen(
    dialect: Dialect,
    profile: ScoutProfile?,
    progressList: List<GameProgress>,
    badgesList: List<DigitalBadge>,
    onSyncBadges: () -> Unit,
    onSetFeaturedBadge: (Int) -> Unit,
    onPlayGameFromBadge: (Int) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val strings = AppStrings.forDialect(dialect)
    val badgesByAxis = remember(badgesList) { badgesList.associateBy { it.axisIndex.toInt() } }
    val completedCount = remember(progressList) { progressList.count { it.passed } }
    val earnedBadgesCount = remember(badgesList, progressList) {
        if (badgesList.isNotEmpty()) {
            badgesList.count { it.unlocked }
        } else {
            (0..4).count { ax -> progressList.count { it.axisIndex.toInt() == ax && it.passed } >= 4 }
        }
    }
    val canDownloadCert = completedCount >= 20
    var selectedAxisForDetail by remember { mutableStateOf<Int?>(null) }

    val safeProfile = profile ?: ScoutProfile(
        userId = "scout",
        scoutName = "كشاف واعي",
        completedCount = completedCount.toLong(),
        totalPoints = progressList.sumOf { it.bestScore * 10L },
        badgesCount = earnedBadgesCount.toLong()
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DigitalBadgesSummaryCard(
                dialect = dialect,
                strings = strings,
                earnedCount = earnedBadgesCount,
                featuredBadge = badgesList.firstOrNull { it.isFeatured && it.unlocked },
                onSyncBadges = onSyncBadges
            )
        }

        items(QuizDataBank.axes, key = { it.index }) { axis ->
            val firestoreBadge = badgesByAxis[axis.index]
            val passedInAxis = firestoreBadge?.passedGamesCount?.toInt()
                ?: progressList.count { it.axisIndex.toInt() == axis.index && it.passed }
            val unlocked = firestoreBadge?.unlocked ?: (passedInAxis >= 4)
            AxisBadgeCard(
                axis = axis,
                badge = firestoreBadge,
                passedCount = passedInAxis,
                unlocked = unlocked,
                dialect = dialect,
                strings = strings,
                onClick = { selectedAxisForDetail = axis.index }
            )
        }

        item {
            OutlinedButton(
                onClick = {
                    val result = CertificatePdfGenerator.generateAndShareBadgesReport(
                        context = context,
                        profile = safeProfile,
                        progress = progressList,
                        badges = badgesList,
                        dialect = dialect
                    )
                    if (result.isFailure) {
                        onShowMessage(result.exceptionOrNull()?.localizedMessage ?: "تعذر إنشاء تقرير PDF")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("download_badges_pdf_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Rounded.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = strings.btnAllBadgesPdf, fontWeight = FontWeight.Bold)
            }
        }

        item {
            CertificateCard(
                strings = strings,
                scoutName = safeProfile.scoutName,
                completedCount = completedCount,
                unlocked = canDownloadCert,
                onDownloadCertificate = {
                    val result = CertificatePdfGenerator.generateAndShareCertificate(
                        context = context,
                        profile = safeProfile,
                        progress = progressList,
                        dialect = dialect
                    )
                    if (result.isFailure) {
                        onShowMessage(result.exceptionOrNull()?.localizedMessage ?: "تعذر إنشاء الشهادة")
                    }
                }
            )
        }

        item {
            Column(
                modifier = Modifier.padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = strings.videosTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = strings.videosDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(QuizDataBank.axes, key = { "video_${it.index}" }) { axis ->
            VideoLibraryCard(
                axis = axis,
                dialect = dialect,
                watchLabel = strings.watchVideo,
                onOpenVideo = { query ->
                    val url = "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        onShowMessage(
                            if (dialect == Dialect.DZ) "ما لقيناش متصفح ولا يوتيوب على الجهاز"
                            else "تعذر فتح يوتيوب أو المتصفح على هذا الجهاز"
                        )
                    }
                }
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    val detailAxisIndex = selectedAxisForDetail
    if (detailAxisIndex != null) {
        val axis = QuizDataBank.axes.first { it.index == detailAxisIndex }
        val badge = badgesByAxis[detailAxisIndex] ?: DigitalBadge(
            id = "badge_$detailAxisIndex",
            userId = safeProfile.userId,
            axisIndex = detailAxisIndex.toLong(),
            badgeCode = "CS-DZ-AX$detailAxisIndex-${safeProfile.userId.take(6).uppercase(Locale.US)}",
            titleDz = axis.nameDz,
            titleAr = axis.nameAr,
            icon = axis.icon,
            passedGamesCount = progressList.count { it.axisIndex.toInt() == detailAxisIndex && it.passed }.toLong(),
            axisPoints = progressList.filter { it.axisIndex.toInt() == detailAxisIndex }.sumOf { it.bestScore * 10L },
            unlocked = progressList.count { it.axisIndex.toInt() == detailAxisIndex && it.passed } >= 4
        )
        BadgeDetailSheet(
            axis = axis,
            badge = badge,
            progressList = progressList,
            dialect = dialect,
            strings = strings,
            onPinFeatured = { onSetFeaturedBadge(axis.index) },
            onShareSinglePdf = {
                val res = CertificatePdfGenerator.generateAndShareSingleBadgeCertificate(
                    context = context,
                    profile = safeProfile,
                    badge = badge,
                    dialect = dialect
                )
                if (res.isFailure) {
                    onShowMessage(res.exceptionOrNull()?.localizedMessage ?: "تعذر إنشاء شهادة الشارة")
                }
            },
            onPlayGame = { gameIdx ->
                selectedAxisForDetail = null
                onPlayGameFromBadge(gameIdx)
            },
            onDismiss = { selectedAxisForDetail = null }
        )
    }
}

@Composable
private fun DigitalBadgesSummaryCard(
    dialect: Dialect,
    strings: AppStrings,
    earnedCount: Int,
    featuredBadge: DigitalBadge?,
    onSyncBadges: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_cyberscout_official),
                        contentDescription = strings.appTitle,
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.badgesTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$earnedCount / 5",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onSyncBadges,
                    modifier = Modifier.testTag("sync_badges_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = strings.syncBadges,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = strings.syncBadges, style = MaterialTheme.typography.labelMedium)
                }
            }

            Text(
                text = strings.badgesDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LinearProgressIndicator(
                progress = { (earnedCount / 5f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (featuredBadge != null) {
                val featTitle = if (dialect == Dialect.DZ) featuredBadge.titleDz else featuredBadge.titleAr
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${featuredBadge.icon} ${strings.pinnedBadge}: $featTitle",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AxisBadgeCard(
    axis: AxisInfo,
    badge: DigitalBadge?,
    passedCount: Int,
    unlocked: Boolean,
    dialect: Dialect,
    strings: AppStrings,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge_spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val mutedColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("badge_card_${axis.index}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (unlocked) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(66.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    if (unlocked) {
                        rotate(degrees = angle) {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    listOf(secondaryColor, primaryColor, secondaryColor)
                                ),
                                radius = radius,
                                style = Stroke(width = 6.dp.toPx())
                            )
                        }
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    secondaryColor.copy(alpha = 0.35f),
                                    primaryColor.copy(alpha = 0.15f)
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = radius * 0.85f
                            ),
                            radius = radius * 0.85f
                        )
                    } else {
                        drawCircle(
                            color = mutedColor.copy(alpha = 0.5f),
                            radius = radius * 0.88f,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
                Text(
                    text = axis.icon,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = axis.name(dialect),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (unlocked) strings.earnedBadge else "$passedCount/4",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (unlocked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = axis.desc(dialect),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LinearProgressIndicator(
                    progress = { (passedCount / 4f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (unlocked && badge != null) {
                            "${strings.badgeCodeLabel}: ${badge.badgeCode}"
                        } else {
                            strings.needBadge
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (badge?.isFeatured == true) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = strings.pinnedBadge,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BadgeDetailSheet(
    axis: AxisInfo,
    badge: DigitalBadge,
    progressList: List<GameProgress>,
    dialect: Dialect,
    strings: AppStrings,
    onPinFeatured: () -> Unit,
    onShareSinglePdf: () -> Unit,
    onPlayGame: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val axisGames = remember(axis.index) { QuizDataBank.games.filter { it.axisIndex == axis.index } }
    val progressByGame = remember(progressList) { progressList.associateBy { it.gameIndex.toInt() } }
    val earnedDateText = remember(badge.earnedAt) {
        badge.earnedAt?.toDate()?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.logo_cyberscout_official),
                    contentDescription = strings.appTitle,
                    modifier = Modifier.size(54.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.badgeDetailsTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${axis.icon} ${axis.name(dialect)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Surface(
                color = if (badge.unlocked) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (badge.unlocked) strings.earnedBadge else "${strings.badgeStatusLocked} (${badge.passedGamesCount}/4)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (badge.unlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${strings.badgePointsLabel}: ${badge.axisPoints} / 400",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (badge.unlocked) {
                        Text(
                            text = "${strings.badgeCodeLabel}: ${badge.badgeCode}" +
                                (earnedDateText?.let { "  •  $it" } ?: ""),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider()

            axisGames.forEach { game ->
                val prog = progressByGame[game.index]
                val passed = prog?.passed == true
                val score = prog?.bestScore?.toInt() ?: 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (passed) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${game.icon} ${game.title(dialect)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${strings.bestScore}$score/10",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { onPlayGame(game.index) },
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (passed) strings.replay else strings.startChallenge,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            if (badge.unlocked) {
                Button(
                    onClick = onPinFeatured,
                    enabled = !badge.isFeatured,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pin_featured_badge_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.PushPin, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (badge.isFeatured) strings.pinnedBadge else strings.pinBadge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onShareSinglePdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("share_single_badge_pdf_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = strings.shareSingleBadgePdf, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CertificateCard(
    strings: AppStrings,
    scoutName: String,
    completedCount: Int,
    unlocked: Boolean,
    onDownloadCertificate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (unlocked) Icons.Rounded.Verified else Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.certTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = strings.certDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (unlocked) strings.certReady(scoutName) else strings.certProgress(completedCount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            LinearProgressIndicator(
                progress = { (completedCount / 20f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Button(
                onClick = onDownloadCertificate,
                enabled = unlocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("download_cert_pdf_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Rounded.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = strings.btnCertPdf, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun VideoLibraryCard(
    axis: AxisInfo,
    dialect: Dialect,
    watchLabel: String,
    onOpenVideo: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_card_${axis.index}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${axis.icon} ${axis.name(dialect)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = axis.desc(dialect),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { onOpenVideo(axis.videoQuery) },
                modifier = Modifier.height(48.dp)
            ) {
                Text(text = watchLabel, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
