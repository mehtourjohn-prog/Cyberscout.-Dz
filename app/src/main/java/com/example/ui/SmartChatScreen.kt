package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.QuizDataBank
import com.example.model.AppStrings
import com.example.model.ChatMessage
import com.example.model.Dialect
import com.example.util.VoiceOption
import com.example.viewmodel.ChatAiState

@Composable
fun SmartChatScreen(
    dialect: Dialect,
    messages: List<ChatMessage>,
    chatAiState: ChatAiState,
    autoSpeakChat: Boolean,
    isSpeaking: Boolean,
    activeVoiceLabel: String,
    availableVoices: List<VoiceOption>,
    onSelectVoice: (String) -> Unit,
    onToggleAutoSpeak: () -> Unit,
    onSpeakText: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onDismissError: () -> Unit,
    onPlaySuggestedGame: (Int) -> Unit
) {
    val strings = AppStrings.forDialect(dialect)
    var inputText by rememberSaveable { mutableStateOf("") }
    var voiceMenuOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val isSending = chatAiState is ChatAiState.Loading || chatAiState is ChatAiState.Streaming

    LaunchedEffect(messages.size, chatAiState) {
        val totalItems = messages.size + 1
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Scout Amin header + voice controls
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_scout_amin_1794787073939),
                        contentDescription = strings.chatBotName,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.chatBotName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.chatBotSub,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = strings.clearChat,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        AssistChip(
                            onClick = { voiceMenuOpen = true },
                            label = {
                                Text(
                                    text = "${strings.voiceActive} $activeVoiceLabel",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.RecordVoiceOver,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("voice_picker_chip")
                        )
                        DropdownMenu(
                            expanded = voiceMenuOpen,
                            onDismissRequest = { voiceMenuOpen = false }
                        ) {
                            if (availableVoices.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (dialect == Dialect.DZ) "الصوت العربي الافتراضي"
                                            else "الصوت العربي الافتراضي"
                                        )
                                    },
                                    onClick = { voiceMenuOpen = false }
                                )
                            } else {
                                availableVoices.take(8).forEach { voice ->
                                    DropdownMenuItem(
                                        text = { Text(voice.label) },
                                        onClick = {
                                            onSelectVoice(voice.id)
                                            voiceMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    FilterChip(
                        selected = autoSpeakChat,
                        onClick = onToggleAutoSpeak,
                        label = {
                            Text(
                                text = if (autoSpeakChat) strings.autoSpeakOn else strings.autoSpeakOff,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("auto_speak_chip")
                    )
                }
            }
        }

        // Chat transcript
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "welcome_msg") {
                BotMessageBubble(
                    text = strings.welcomeChat,
                    isWarning = false,
                    suggestedGameIndex = -1,
                    dialect = dialect,
                    strings = strings,
                    isSpeaking = isSpeaking,
                    onSpeak = { onSpeakText(strings.welcomeChat) },
                    onStopSpeaking = onStopSpeaking,
                    onPlaySuggestedGame = onPlaySuggestedGame
                )
            }

            items(messages, key = { it.id.ifBlank { "${it.sender}_${it.text.hashCode()}" } }) { msg ->
                if (msg.sender == "user") {
                    UserMessageBubble(text = msg.text)
                } else {
                    BotMessageBubble(
                        text = msg.text,
                        isWarning = msg.isWarning,
                        suggestedGameIndex = msg.suggestedGameIndex.toInt(),
                        dialect = dialect,
                        strings = strings,
                        isSpeaking = isSpeaking,
                        onSpeak = { onSpeakText(msg.text) },
                        onStopSpeaking = onStopSpeaking,
                        onPlaySuggestedGame = onPlaySuggestedGame
                    )
                }
            }

            when (chatAiState) {
                is ChatAiState.Loading -> {
                    item(key = "ai_loading") {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (dialect == Dialect.DZ) "الكشاف أمين راهو يفكّر..." else "الكشاف أمين يكتب الرد...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                is ChatAiState.Streaming -> {
                    item(key = "ai_streaming") {
                        BotMessageBubble(
                            text = chatAiState.text,
                            isWarning = false,
                            suggestedGameIndex = chatAiState.suggestedGameIndex,
                            dialect = dialect,
                            strings = strings,
                            isSpeaking = isSpeaking,
                            onSpeak = { onSpeakText(chatAiState.text) },
                            onStopSpeaking = onStopSpeaking,
                            onPlaySuggestedGame = onPlaySuggestedGame
                        )
                    }
                }
                is ChatAiState.Error -> {
                    item(key = "ai_error") {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = chatAiState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onDismissError) {
                                    Text(text = strings.closeQuiz)
                                }
                            }
                        }
                    }
                }
                ChatAiState.Idle -> Unit
            }
        }

        // Quick-reply chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(strings.chips) { chip ->
                AssistChip(
                    onClick = {
                        if (!isSending) onSendMessage(chip)
                    },
                    label = {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    enabled = !isSending
                )
            }
        }

        // Message composer
        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(strings.chatPlaceholder) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input"),
                    maxLines = 3,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !isSending) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        }
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isSending) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(50.dp)
                        .testTag("chat_send_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = strings.chatSend
                    )
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun BotMessageBubble(
    text: String,
    isWarning: Boolean,
    suggestedGameIndex: Int,
    dialect: Dialect,
    strings: AppStrings,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onStopSpeaking: () -> Unit,
    onPlaySuggestedGame: (Int) -> Unit
) {
    val suggestedGame = QuizDataBank.games.getOrNull(suggestedGameIndex)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 340.dp),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isWarning) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isWarning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (dialect == Dialect.DZ) "تنبيه خصوصية!" else "تنبيه أمان!",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isWarning) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = if (isSpeaking) onStopSpeaking else onSpeak,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = if (isSpeaking) strings.stopReading else strings.readAloud,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSpeaking) strings.stopReading else strings.readAloud,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                if (suggestedGame != null) {
                    Button(
                        onClick = { onPlaySuggestedGame(suggestedGame.index) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("suggested_game_${suggestedGame.index}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${strings.playSuggestedGame}: ${suggestedGame.icon} ${suggestedGame.title(dialect)}",
                            style = MaterialTheme.typography.labelLarge,
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
