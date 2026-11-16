package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOutUser
import com.example.data.ScoutAiRepository
import com.example.data.ScoutRepository
import com.example.model.AppStrings
import com.example.model.Dialect
import com.example.security.configureAppCheck
import com.example.ui.AuthScreen
import com.example.ui.HomeScreen
import com.example.ui.LearningAndBadgesScreen
import com.example.ui.ProfileSheet
import com.example.ui.QuizDialog
import com.example.ui.SmartChatScreen
import com.example.ui.theme.CyberScoutTheme
import com.example.util.ScoutVoiceManager
import com.example.viewmodel.DataUiState
import com.example.viewmodel.ScoutViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

private enum class MainTab {
    GAMES,
    BADGES,
    CHAT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must initialize App Check before any Firebase SDK call or UI rendering
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            CyberScoutTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    CyberScoutApp()
                }
            }
        }
    }
}

@Composable
private fun CyberScoutApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val authState by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var authDialect by rememberSaveable { mutableStateOf(Dialect.DZ) }
    var authLoading by rememberSaveable { mutableStateOf(false) }
    var authError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { authError = null },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val currentUser = authState
    if (currentUser == null) {
        AuthScreen(
            dialect = authDialect,
            isLoading = authLoading,
            errorMessage = authError,
            onToggleDialect = {
                authDialect = if (authDialect == Dialect.DZ) Dialect.AR else Dialect.DZ
            },
            onGoogleSignIn = {
                authLoading = true
                authError = null
                onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                        authLoading = false
                        authError = null
                    },
                    onAuthError = { msg ->
                        authLoading = false
                        authError = msg
                    },
                    scope = scope,
                    onAuthCancelled = {
                        authLoading = false
                    }
                )
            }
        )
    } else {
        key(currentUser.uid) {
            val viewModel: ScoutViewModel = viewModel(
                key = currentUser.uid,
                factory = viewModelFactory {
                    initializer {
                        ScoutViewModel(
                            repository = ScoutRepository(context.applicationContext),
                            aiRepository = ScoutAiRepository(),
                            voiceManager = ScoutVoiceManager(context.applicationContext),
                            currentUserId = currentUser.uid,
                            defaultDisplayName = currentUser.displayName ?: "كشاف واعي"
                        )
                    }
                }
            )
            MainScoutScaffold(
                viewModel = viewModel,
                onSignOut = {
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScoutScaffold(
    viewModel: ScoutViewModel,
    onSignOut: () -> Unit
) {
    val dialect by viewModel.dialect.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val progressState by viewModel.progressState.collectAsStateWithLifecycle()
    val badgesState by viewModel.badgesState.collectAsStateWithLifecycle()
    val chatMessagesState by viewModel.chatMessagesState.collectAsStateWithLifecycle()
    val selectedAxisFilter by viewModel.selectedAxisFilter.collectAsStateWithLifecycle()
    val activeQuiz by viewModel.activeQuiz.collectAsStateWithLifecycle()
    val newlyEarnedBadge by viewModel.newlyEarnedBadge.collectAsStateWithLifecycle()
    val chatAiState by viewModel.chatAiState.collectAsStateWithLifecycle()
    val autoSpeakChat by viewModel.autoSpeakChat.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val isMuted by viewModel.voiceManager.isMuted.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsStateWithLifecycle()
    val activeVoiceLabel by viewModel.voiceManager.activeVoiceLabel.collectAsStateWithLifecycle()
    val availableVoices by viewModel.voiceManager.availableVoices.collectAsStateWithLifecycle()
    val selectedVoiceId by viewModel.voiceManager.selectedVoiceId.collectAsStateWithLifecycle()

    val strings = AppStrings.forDialect(dialect)
    var currentTab by rememberSaveable { mutableStateOf(MainTab.GAMES) }
    var showProfileSheet by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    val profile = (profileState as? DataUiState.Success)?.data
    val progressList = (progressState as? DataUiState.Success)?.data.orEmpty()
    val badgesList = (badgesState as? DataUiState.Success)?.data.orEmpty()
    val chatMessages = (chatMessagesState as? DataUiState.Success)?.data.orEmpty()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_cyberscout_official),
                                contentDescription = strings.appTitle,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(9.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CyberScout",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        FilledTonalButton(
                            onClick = { viewModel.toggleDialect() },
                            modifier = Modifier.testTag("top_dialect_toggle")
                        ) {
                            Text(
                                text = strings.dialectLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { viewModel.voiceManager.toggleMute() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("global_mute_button")
                        ) {
                            Icon(
                                imageVector = if (isMuted) {
                                    Icons.AutoMirrored.Rounded.VolumeOff
                                } else {
                                    Icons.AutoMirrored.Rounded.VolumeUp
                                },
                                contentDescription = if (isMuted) "تشغيل الصوت" else "كتم الصوت",
                                tint = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { showProfileSheet = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("open_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = strings.profileTitle,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentTab == MainTab.GAMES,
                            onClick = { currentTab = MainTab.GAMES },
                            icon = { Icon(Icons.Rounded.SportsEsports, contentDescription = strings.navGames) },
                            label = { Text(strings.navGames) },
                            modifier = Modifier.testTag("nav_games")
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.BADGES,
                            onClick = { currentTab = MainTab.BADGES },
                            icon = { Icon(Icons.Rounded.EmojiEvents, contentDescription = strings.navBadges) },
                            label = { Text(strings.navBadges) },
                            modifier = Modifier.testTag("nav_badges")
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.CHAT,
                            onClick = { currentTab = MainTab.CHAT },
                            icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = strings.navChat) },
                            label = { Text(strings.navChat) },
                            modifier = Modifier.testTag("nav_chat")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail {
                        NavigationRailItem(
                            selected = currentTab == MainTab.GAMES,
                            onClick = { currentTab = MainTab.GAMES },
                            icon = { Icon(Icons.Rounded.SportsEsports, contentDescription = strings.navGames) },
                            label = { Text(strings.navGames) },
                            modifier = Modifier.testTag("rail_games")
                        )
                        NavigationRailItem(
                            selected = currentTab == MainTab.BADGES,
                            onClick = { currentTab = MainTab.BADGES },
                            icon = { Icon(Icons.Rounded.EmojiEvents, contentDescription = strings.navBadges) },
                            label = { Text(strings.navBadges) },
                            modifier = Modifier.testTag("rail_badges")
                        )
                        NavigationRailItem(
                            selected = currentTab == MainTab.CHAT,
                            onClick = { currentTab = MainTab.CHAT },
                            icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = strings.navChat) },
                            label = { Text(strings.navChat) },
                            modifier = Modifier.testTag("rail_chat")
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (profileState is DataUiState.Loading && progressState is DataUiState.Loading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        when (currentTab) {
                            MainTab.GAMES -> {
                                HomeScreen(
                                    dialect = dialect,
                                    profile = profile,
                                    progressList = progressList,
                                    badgesList = badgesList,
                                    selectedAxisFilter = selectedAxisFilter,
                                    onSelectAxisFilter = { viewModel.setAxisFilter(it) },
                                    onStartAdventure = { viewModel.startFirstUnfinishedGame() },
                                    onExploreBadges = { currentTab = MainTab.BADGES },
                                    onStartGame = { viewModel.startGame(it) }
                                )
                            }
                            MainTab.BADGES -> {
                                LearningAndBadgesScreen(
                                    dialect = dialect,
                                    profile = profile,
                                    progressList = progressList,
                                    badgesList = badgesList,
                                    onSyncBadges = { viewModel.syncDigitalBadges() },
                                    onSetFeaturedBadge = { viewModel.setFeaturedBadge(it) },
                                    onPlayGameFromBadge = { gameIdx -> viewModel.startGame(gameIdx) },
                                    onShowMessage = { viewModel.showToast(it) }
                                )
                            }
                            MainTab.CHAT -> {
                                SmartChatScreen(
                                    dialect = dialect,
                                    messages = chatMessages,
                                    chatAiState = chatAiState,
                                    autoSpeakChat = autoSpeakChat,
                                    isSpeaking = isSpeaking,
                                    activeVoiceLabel = activeVoiceLabel,
                                    availableVoices = availableVoices,
                                    onSelectVoice = { viewModel.voiceManager.selectVoice(it) },
                                    onToggleAutoSpeak = { viewModel.toggleAutoSpeakChat() },
                                    onSpeakText = { viewModel.voiceManager.speak(it, dialect) },
                                    onStopSpeaking = { viewModel.voiceManager.stopSpeaking() },
                                    onSendMessage = { viewModel.sendChatMessage(it) },
                                    onClearChat = { viewModel.clearChatHistory() },
                                    onDismissError = { viewModel.dismissAiError() },
                                    onPlaySuggestedGame = { gameIdx -> viewModel.startGame(gameIdx) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val quiz = activeQuiz
    if (quiz != null) {
        QuizDialog(
            quiz = quiz,
            dialect = dialect,
            onSelectOption = { viewModel.answerCurrentQuestion(it) },
            onReadQuestion = { viewModel.readCurrentQuestion() },
            onNext = { viewModel.nextQuizStep() },
            onRetry = { viewModel.retryActiveQuiz() },
            onClose = { viewModel.closeQuiz() }
        )
    }

    if (showProfileSheet) {
        ProfileSheet(
            profile = profile,
            badgesList = badgesList,
            dialect = dialect,
            availableVoices = availableVoices,
            selectedVoiceId = selectedVoiceId,
            onSelectVoice = { viewModel.voiceManager.selectVoice(it) },
            onToggleDialect = { viewModel.toggleDialect() },
            onSaveProfile = { name, ageGroup -> viewModel.updateProfile(name, ageGroup) },
            onSignOut = onSignOut,
            onDismiss = { showProfileSheet = false }
        )
    }

    val earnedBadgePopup = newlyEarnedBadge
    if (earnedBadgePopup != null) {
        val badgeTitle = if (dialect == Dialect.DZ) earnedBadgePopup.titleDz else earnedBadgePopup.titleAr
        AlertDialog(
            onDismissRequest = { viewModel.dismissNewlyEarnedBadge() },
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.logo_cyberscout_official),
                    contentDescription = strings.appTitle,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Fit
                )
            },
            title = {
                Text(
                    text = strings.newlyEarnedTitle,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${earnedBadgePopup.icon} $badgeTitle",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${strings.badgeCodeLabel}: ${earnedBadgePopup.badgeCode}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissNewlyEarnedBadge()
                        viewModel.closeQuiz()
                        currentTab = MainTab.BADGES
                    }
                ) {
                    Text(strings.viewBadgesAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissNewlyEarnedBadge() }) {
                    Text(strings.closeQuiz)
                }
            }
        )
    }
}
