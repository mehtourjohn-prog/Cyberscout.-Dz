package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.QuizDataBank
import com.example.model.AppStrings
import com.example.model.Dialect
import com.example.model.DigitalBadge
import com.example.model.GameInfo
import com.example.model.GameProgress
import com.example.model.ScoutProfile

@Composable
fun HomeScreen(
    dialect: Dialect,
    profile: ScoutProfile?,
    progressList: List<GameProgress>,
    badgesList: List<DigitalBadge>,
    selectedAxisFilter: Int?,
    onSelectAxisFilter: (Int?) -> Unit,
    onStartAdventure: () -> Unit,
    onExploreBadges: () -> Unit,
    onStartGame: (Int) -> Unit
) {
    val strings = AppStrings.forDialect(dialect)
    val progressMap = remember(progressList) {
        progressList.associateBy { it.gameIndex.toInt() }
    }
    val completedCount = remember(progressList) {
        progressList.count { it.passed }
    }
    val totalPoints = remember(progressList, profile) {
        profile?.totalPoints?.toInt() ?: progressList.sumOf { it.bestScore.toInt() * 10 }
    }
    val badgesCount = remember(badgesList, progressList) {
        if (badgesList.isNotEmpty()) {
            badgesList.count { it.unlocked }
        } else {
            (0..4).count { ax -> progressList.count { it.axisIndex.toInt() == ax && it.passed } >= 4 }
        }
    }
    val featuredBadge = remember(badgesList) {
        badgesList.firstOrNull { it.isFeatured && it.unlocked }
    }
    val filteredGames = remember(selectedAxisFilter) {
        if (selectedAxisFilter == null) {
            QuizDataBank.games
        } else {
            QuizDataBank.games.filter { it.axisIndex == selectedAxisFilter }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 860.dp -> 3
            maxWidth >= 560.dp -> 2
            else -> 1
        }
        val chunkedGames = remember(filteredGames, columns) {
            filteredGames.chunked(columns)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HeroBannerCard(
                    strings = strings,
                    dialect = dialect,
                    scoutName = profile?.scoutName,
                    featuredBadge = featuredBadge,
                    onStartAdventure = onStartAdventure,
                    onExploreBadges = onExploreBadges
                )
            }

            item {
                StatsRow(
                    strings = strings,
                    completedCount = completedCount,
                    totalPoints = totalPoints,
                    badgesCount = badgesCount
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.secGamesTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = strings.secGamesDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedAxisFilter == null,
                            onClick = { onSelectAxisFilter(null) },
                            label = { Text(strings.allFilter) },
                            modifier = Modifier.testTag("filter_axis_all"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                    items(QuizDataBank.axes, key = { it.index }) { axis ->
                        FilterChip(
                            selected = selectedAxisFilter == axis.index,
                            onClick = { onSelectAxisFilter(axis.index) },
                            label = { Text("${axis.icon} ${axis.name(dialect)}") },
                            modifier = Modifier.testTag("filter_axis_${axis.index}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            items(chunkedGames, key = { row -> row.first().index }) { rowGames ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowGames.forEach { game ->
                        val axis = QuizDataBank.axes[game.axisIndex]
                        val prog = progressMap[game.index]
                        GameCard(
                            modifier = Modifier.weight(1f),
                            game = game,
                            axisName = "${axis.icon} ${axis.name(dialect)}",
                            progress = prog,
                            dialect = dialect,
                            strings = strings,
                            onClick = { onStartGame(game.index) }
                        )
                    }
                    repeat(columns - rowGames.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun HeroBannerCard(
    strings: AppStrings,
    dialect: Dialect,
    scoutName: String?,
    featuredBadge: DigitalBadge?,
    onStartAdventure: () -> Unit,
    onExploreBadges: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner_1794787073905),
                contentDescription = strings.heroTitle,
                modifier = Modifier
                    .matchParentSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = if (!scoutName.isNullOrBlank()) "${strings.heroTag} · $scoutName" else strings.heroTag,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.logo_cyberscout_official),
                        contentDescription = strings.appTitle,
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                if (featuredBadge != null) {
                    val featuredTitle = if (dialect == Dialect.DZ) featuredBadge.titleDz else featuredBadge.titleAr
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${featuredBadge.icon} ${strings.pinnedBadge}: $featuredTitle",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Text(
                    text = strings.heroTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = strings.heroDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStartAdventure,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_start_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.heroStart,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onExploreBadges,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_badges_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = strings.heroExplore,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(
    strings: AppStrings,
    completedCount: Int,
    totalPoints: Int,
    badgesCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatMiniCard(
                modifier = Modifier.weight(1f),
                value = "20",
                label = strings.lblGames,
                highlight = MaterialTheme.colorScheme.primary
            )
            StatMiniCard(
                modifier = Modifier.weight(1f),
                value = "$completedCount/20",
                label = strings.lblDone,
                highlight = MaterialTheme.colorScheme.primary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatMiniCard(
                modifier = Modifier.weight(1f),
                value = "$totalPoints",
                label = strings.lblPoints,
                highlight = MaterialTheme.colorScheme.secondary
            )
            StatMiniCard(
                modifier = Modifier.weight(1f),
                value = "$badgesCount/5",
                label = strings.lblBadges,
                highlight = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun StatMiniCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    highlight: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = highlight
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GameCard(
    modifier: Modifier = Modifier,
    game: GameInfo,
    axisName: String,
    progress: GameProgress?,
    dialect: Dialect,
    strings: AppStrings,
    onClick: () -> Unit
) {
    val bestScore = progress?.bestScore?.toInt() ?: 0
    val passed = progress?.passed == true
    val hasPlayed = progress != null

    Card(
        modifier = modifier.testTag("game_card_${game.index}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (passed) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = game.icon,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = axisName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "${game.index + 1}. ${game.title(dialect)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasPlayed) "${strings.bestScore}$bestScore/10" else strings.notStarted,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (passed) FontWeight.Bold else FontWeight.Normal
                )
                if (passed) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = strings.earnedBadge,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { (bestScore / 10f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("play_game_${game.index}"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (passed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                    contentColor = if (passed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (hasPlayed) Icons.Rounded.Replay else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hasPlayed) strings.replay else strings.startChallenge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
