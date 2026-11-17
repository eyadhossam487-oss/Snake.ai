package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.HapticsManager
import com.example.audio.SoundEffectsManager
import com.example.data.ViperRepository
import com.example.data.authStateFlow
import com.example.data.signOutUser
import com.example.model.BotDifficulty
import com.example.model.SnakePattern
import com.example.model.SoundEffect
import com.example.ui.ArenaScreen
import com.example.ui.AuthScreen
import com.example.ui.DailyRewardDialog
import com.example.ui.HomeScreen
import com.example.ui.LeaderboardScreen
import com.example.ui.OwnerPanelSheet
import com.example.ui.SkinStudioScreen
import com.example.ui.theme.ArenaBorder
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.ArenaVoid
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ViperArenaTheme
import com.example.viewmodel.ArenaSessionConfig
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.LobbyViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class LobbyTab(val label: String) {
    ARENA("Arena"),
    SKINS("Skins"),
    RANK("Rank")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ViperArenaTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener drives transition */ })
    } else {
        key(user.uid) {
            AuthenticatedSnakeShell(user = user)
        }
    }
}

@Composable
private fun AuthenticatedSnakeShell(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val lobbyViewModel: LobbyViewModel = viewModel(
        key = "lobby_${user.uid}",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                LobbyViewModel(
                    repository = ViperRepository(db),
                    currentUserId = user.uid,
                    userDisplayName = user.displayName ?: "Viper",
                    userEmail = user.email,
                    emailVerified = user.isEmailVerified
                )
            }
        }
    )

    val isOwner by lobbyViewModel.isOwner.collectAsStateWithLifecycle()
    val profileState by lobbyViewModel.userProfileState.collectAsStateWithLifecycle()
    val dailyRewardStatus by lobbyViewModel.dailyRewardStatus.collectAsStateWithLifecycle()
    val lastDailyClaimResult by lobbyViewModel.lastDailyClaimResult.collectAsStateWithLifecycle()
    val shouldAutoShowDailyDialog by lobbyViewModel.shouldAutoShowDailyDialog.collectAsStateWithLifecycle()
    val leaderboardState by lobbyViewModel.leaderboardState.collectAsStateWithLifecycle()
    val roomsState by lobbyViewModel.publicRoomsState.collectAsStateWithLifecycle()
    val actionMessage by lobbyViewModel.actionMessage.collectAsStateWithLifecycle()
    val isBusy by lobbyViewModel.isBusy.collectAsStateWithLifecycle()

    val gameViewModel: GameViewModel = viewModel(
        key = "game_${user.uid}_$isOwner",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                GameViewModel(
                    repository = ViperRepository(db),
                    currentUserId = user.uid,
                    isOwner = isOwner
                )
            }
        }
    )

    val activeSession by gameViewModel.activeSession.collectAsStateWithLifecycle()
    val snapshot by gameViewModel.snapshot.collectAsStateWithLifecycle()
    val cameraPos by gameViewModel.cameraPos.collectAsStateWithLifecycle()
    val ownerCheats by gameViewModel.ownerCheats.collectAsStateWithLifecycle()
    val isPaused by gameViewModel.isPaused.collectAsStateWithLifecycle()
    val matchSummary by gameViewModel.matchSummary.collectAsStateWithLifecycle()
    val activeDiff by gameViewModel.activeDifficulty.collectAsStateWithLifecycle()
    val activeBotCount by gameViewModel.activeBotCount.collectAsStateWithLifecycle()
    val gameSettings by gameViewModel.gameSettings.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(LobbyTab.ARENA) }
    var isArenaVisible by rememberSaveable { mutableStateOf(false) }
    var showOwnerSheet by remember { mutableStateOf(false) }
    var showDailyRewardDialog by remember { mutableStateOf(false) }

    val soundEffects = remember { SoundEffectsManager() }
    val hapticsManager = remember(context) { HapticsManager(context.applicationContext) }
    DisposableEffect(soundEffects, hapticsManager) {
        onDispose {
            soundEffects.release()
            hapticsManager.release()
        }
    }
    LaunchedEffect(gameSettings.soundEffectsEnabled) {
        soundEffects.enabled = gameSettings.soundEffectsEnabled
    }
    LaunchedEffect(gameSettings.hapticsEnabled) {
        hapticsManager.enabled = gameSettings.hapticsEnabled
    }
    LaunchedEffect(snapshot, isPaused, isArenaVisible) {
        if (isArenaVisible && !isPaused) {
            snapshot?.soundEvents?.forEach { effect ->
                soundEffects.play(effect)
            }
            snapshot?.hapticEvents?.forEach { haptic ->
                hapticsManager.trigger(haptic)
            }
        }
    }
    LaunchedEffect(lastDailyClaimResult) {
        if (lastDailyClaimResult != null) {
            soundEffects.play(SoundEffect.REWARD_CLAIM)
        }
    }

    LaunchedEffect(shouldAutoShowDailyDialog, activeSession) {
        if (shouldAutoShowDailyDialog && activeSession == null) {
            showDailyRewardDialog = true
            lobbyViewModel.dismissAutoDailyDialog()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        lobbyViewModel.clearMessage()
    }

    val profile = (profileState as? UiState.Success)?.data
    val playerName = profile?.displayName ?: (user.displayName ?: "Viper")
    val playerSkinId = profile?.selectedSkinId ?: "cyber_viper"
    val playerPattern = SnakePattern.fromRaw(profile?.selectedPattern)

    val currentSession = activeSession
    if (currentSession != null && isArenaVisible) {
        val room = currentSession.room
        val isHostOrSolo = room == null || room.hostId == user.uid

        ArenaScreen(
            snapshot = snapshot,
            cameraPos = cameraPos,
            room = room,
            activeDifficulty = activeDiff,
            activeBotCount = activeBotCount,
            gameSettings = gameSettings,
            isOwner = isOwner,
            ownerCheats = ownerCheats,
            isPaused = isPaused,
            matchSummary = matchSummary,
            isHostOrSolo = isHostOrSolo,
            onSteerAngle = gameViewModel::setSteeringAngle,
            onBoostChange = gameViewModel::setBoosting,
            onTogglePause = gameViewModel::togglePause,
            onUpdateSettings = gameViewModel::updateSettings,
            onDifficultyChange = gameViewModel::setBotDifficulty,
            onBotCountChange = gameViewModel::setBotCount,
            onRespawn = gameViewModel::respawn,
            onReturnToMainMenuKeepSession = {
                gameViewModel.pauseAndKeepSession()
                selectedTab = LobbyTab.ARENA
                isArenaVisible = false
            },
            onExitArena = {
                gameViewModel.leaveArena()
                isArenaVisible = false
            },
            onOpenOwnerPanel = {
                if (isOwner) {
                    soundEffects.play(SoundEffect.UI_CLICK)
                    showOwnerSheet = true
                }
            },
            onPlayClickSound = { soundEffects.play(SoundEffect.UI_CLICK) }
        )
    } else {
        val pausedSessionLabel = currentSession?.let { session ->
            session.room?.let { "${it.name} (${it.roomCode})" }
                ?: "Solo vs $activeBotCount ${activeDiff.label} Bots"
        }
        val pausedSessionLength = if (currentSession != null) snapshot?.playerSnake?.score else null

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = ArenaVoid,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = ArenaSurface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            LobbyTab.entries.forEach { tab ->
                                val selected = selectedTab == tab
                                val icon = when (tab) {
                                    LobbyTab.ARENA -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                                    LobbyTab.SKINS -> if (selected) Icons.Filled.Palette else Icons.Outlined.Palette
                                    LobbyTab.RANK -> if (selected) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        soundEffects.play(SoundEffect.UI_CLICK)
                                        selectedTab = tab
                                    },
                                    icon = { Icon(icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ArenaVoid,
                                        selectedTextColor = CyberCyan,
                                        indicatorColor = CyberCyan,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = ArenaSurface,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                        ) {
                            LobbyTab.entries.forEach { tab ->
                                val selected = selectedTab == tab
                                val icon = when (tab) {
                                    LobbyTab.ARENA -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                                    LobbyTab.SKINS -> if (selected) Icons.Filled.Palette else Icons.Outlined.Palette
                                    LobbyTab.RANK -> if (selected) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents
                                }
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        soundEffects.play(SoundEffect.UI_CLICK)
                                        selectedTab = tab
                                    },
                                    icon = { Icon(icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = ArenaVoid,
                                        selectedTextColor = CyberCyan,
                                        indicatorColor = CyberCyan,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        when (selectedTab) {
                            LobbyTab.ARENA -> HomeScreen(
                                profileState = profileState,
                                roomsState = roomsState,
                                dailyRewardStatus = dailyRewardStatus,
                                isOwner = isOwner,
                                isBusy = isBusy,
                                pausedSessionLabel = pausedSessionLabel,
                                pausedSessionLength = pausedSessionLength,
                                onResumePausedSession = {
                                    soundEffects.play(SoundEffect.UI_CLICK)
                                    gameViewModel.resumeExistingSession()
                                    isArenaVisible = true
                                },
                                onEndPausedSession = {
                                    soundEffects.play(SoundEffect.UI_CLICK)
                                    gameViewModel.leaveArena()
                                    isArenaVisible = false
                                },
                                onOpenDailyRewards = {
                                    soundEffects.play(SoundEffect.UI_CLICK)
                                    showDailyRewardDialog = true
                                },
                                onOpenSkinStudio = {
                                    soundEffects.play(SoundEffect.UI_CLICK)
                                    selectedTab = LobbyTab.SKINS
                                },
                                onOpenOwnerPanel = {
                                    if (isOwner) {
                                        soundEffects.play(SoundEffect.UI_CLICK)
                                        showOwnerSheet = true
                                    }
                                },
                                onDifficultySelected = lobbyViewModel::updatePreferredDifficulty,
                                onPlaySolo = { diff, bots ->
                                    val cfg = ArenaSessionConfig(
                                        room = null,
                                        botDifficulty = diff,
                                        botCount = bots,
                                        playerName = playerName,
                                        skinId = playerSkinId,
                                        pattern = playerPattern
                                    )
                                    gameViewModel.startArena(cfg)
                                    isArenaVisible = true
                                },
                                onCreateRoom = { name, diff, bots, maxPlayers ->
                                    lobbyViewModel.createMultiplayerRoom(
                                        name = name,
                                        botDifficulty = diff,
                                        botCount = bots,
                                        maxPlayers = maxPlayers
                                    ) { room ->
                                        val cfg = ArenaSessionConfig(
                                            room = room,
                                            botDifficulty = BotDifficulty.fromRaw(room.botDifficulty),
                                            botCount = room.botCount.toInt(),
                                            playerName = playerName,
                                            skinId = playerSkinId,
                                            pattern = playerPattern
                                        )
                                        gameViewModel.startArena(cfg)
                                        isArenaVisible = true
                                    }
                                },
                                onJoinRoom = { room ->
                                    lobbyViewModel.joinMultiplayerRoom(room.id) { joined ->
                                        val cfg = ArenaSessionConfig(
                                            room = joined,
                                            botDifficulty = BotDifficulty.fromRaw(joined.botDifficulty),
                                            botCount = joined.botCount.toInt(),
                                            playerName = playerName,
                                            skinId = playerSkinId,
                                            pattern = playerPattern
                                        )
                                        gameViewModel.startArena(cfg)
                                        isArenaVisible = true
                                    }
                                },
                                onJoinByCode = { code ->
                                    lobbyViewModel.joinRoomByCode(code) { joined ->
                                        val cfg = ArenaSessionConfig(
                                            room = joined,
                                            botDifficulty = BotDifficulty.fromRaw(joined.botDifficulty),
                                            botCount = joined.botCount.toInt(),
                                            playerName = playerName,
                                            skinId = playerSkinId,
                                            pattern = playerPattern
                                        )
                                        gameViewModel.startArena(cfg)
                                        isArenaVisible = true
                                    }
                                },
                                onOwnerCloseRoom = lobbyViewModel::ownerCloseRoom,
                                onSignOut = {
                                    gameViewModel.leaveArena()
                                    isArenaVisible = false
                                    signOutUser(credentialManager, scope)
                                }
                            )

                            LobbyTab.SKINS -> SkinStudioScreen(
                                profile = profile,
                                isOwner = isOwner,
                                isBusy = isBusy,
                                onSaveCustomization = { name, skinId, pattern, diff ->
                                    soundEffects.play(SoundEffect.UI_CLICK)
                                    lobbyViewModel.saveCustomization(name, skinId, pattern, diff)
                                },
                                onUnlockShopSkin = { skin ->
                                    soundEffects.play(SoundEffect.REWARD_CLAIM)
                                    lobbyViewModel.unlockShopSkin(skin)
                                },
                                onOpenDailyRewards = {
                                    soundEffects.play(SoundEffect.UI_CLICK)
                                    showDailyRewardDialog = true
                                },
                                onBackToHome = {
                                    soundEffects.play(SoundEffect.UI_CLICK)
                                    selectedTab = LobbyTab.ARENA
                                }
                            )

                            LobbyTab.RANK -> LeaderboardScreen(
                                leaderboardState = leaderboardState,
                                currentUserId = user.uid,
                                currentUserProfile = profile,
                                isOwner = isOwner,
                                onOwnerDeleteEntry = lobbyViewModel::ownerDeleteLeaderboardEntry,
                                onOpenOwnerPanel = {
                                    if (isOwner) {
                                        soundEffects.play(SoundEffect.UI_CLICK)
                                        showOwnerSheet = true
                                    }
                                },
                                onBackToHome = {
                                    soundEffects.play(SoundEffect.UI_CLICK)
                                    selectedTab = LobbyTab.ARENA
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDailyRewardDialog) {
        DailyRewardDialog(
            coins = profile?.coins ?: 0L,
            status = dailyRewardStatus,
            lastClaimResult = lastDailyClaimResult,
            isBusy = isBusy,
            onClaim = lobbyViewModel::claimDailyReward,
            onDismiss = {
                showDailyRewardDialog = false
                lobbyViewModel.clearDailyClaimResult()
            }
        )
    }

    if (showOwnerSheet && isOwner) {
        OwnerPanelSheet(
            isInArena = currentSession != null && isArenaVisible,
            cheatState = ownerCheats,
            currentBotDifficulty = activeDiff,
            currentUserLength = profile?.maxLength ?: 1000L,
            onDismiss = { showOwnerSheet = false },
            onToggleGodMode = gameViewModel::toggleGodMode,
            onTogglePermaSpeed = gameViewModel::togglePermaSpeed,
            onTogglePermaMagnet = gameViewModel::togglePermaMagnet,
            onToggleFreezeBots = gameViewModel::toggleFreezeBots,
            onAddLength = gameViewModel::addOwnerLength,
            onSpawnFoodRain = gameViewModel::spawnFoodRain,
            onEliminateAllBots = gameViewModel::eliminateAllBots,
            onOverrideBotDifficulty = gameViewModel::setOwnerBotDifficultyOverride,
            onSetGlobalLeaderboardScore = { newLen ->
                lobbyViewModel.ownerSetMyLeaderboardLength(
                    displayName = playerName,
                    skinId = playerSkinId,
                    pattern = playerPattern,
                    newLength = newLen,
                    newKills = profile?.totalKills ?: 0L
                )
            }
        )
    }
}
