package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.LeaderboardEntry
import com.example.model.SkinCatalog
import com.example.model.UserProfile
import com.example.ui.theme.ArenaBorder
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.ArenaSurfaceElevated
import com.example.ui.theme.ArenaVoid
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.LeaderboardViewData
import com.example.viewmodel.UiState

@Composable
fun LeaderboardScreen(
    leaderboardState: UiState<LeaderboardViewData>,
    currentUserId: String,
    currentUserProfile: UserProfile?,
    isOwner: Boolean,
    onOwnerDeleteEntry: (String) -> Unit,
    onOpenOwnerPanel: () -> Unit,
    onBackToHome: () -> Unit
) {
    BackHandler { onBackToHome() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaVoid),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "GLOBAL HALL OF FAME",
                        style = MaterialTheme.typography.headlineLarge,
                        color = CyberCyan
                    )
                    Text(
                        text = "Top 10 players ranked by maximum snake length",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                if (isOwner) {
                    IconButton(
                        onClick = onOpenOwnerPanel,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("leaderboard_owner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Owner Controls",
                            tint = SolarGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (leaderboardState) {
                is UiState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        CircularProgressIndicator(color = CyberCyan)
                    }
                }
                is UiState.Error -> {
                    Surface(
                        color = CrimsonDanger.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = leaderboardState.message,
                            color = CrimsonDanger,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val data = leaderboardState.data
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (data.topTen.isEmpty()) {
                            item {
                                Surface(
                                    color = ArenaSurfaceElevated,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, ArenaBorder, RoundedCornerShape(18.dp))
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = SolarGold,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No scores recorded yet",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            text = "Play a match in the arena to claim #1 on the global board!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(data.topTen, key = { _, e -> e.userId }) { idx, entry ->
                                LeaderboardRow(
                                    rank = idx + 1,
                                    entry = entry,
                                    isCurrentUser = entry.userId == currentUserId,
                                    isOwner = isOwner,
                                    onDelete = { onOwnerDeleteEntry(entry.userId) }
                                )
                            }
                        }
                    }

                    // Pinned "Your Global Rank" Card (explicitly shows player's rank if outside Top 10, or summary if inside)
                    val myRank = data.myRank
                    val myEntry = data.myEntry
                    val outsideTopTen = myRank == null || myRank > 10
                    Surface(
                        color = ArenaSurface,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.5.dp,
                                color = CyberCyan.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .testTag("your_rank_card")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (outsideTopTen) "YOUR GLOBAL RANK (OUTSIDE TOP 10)" else "YOUR GLOBAL RANK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = myEntry?.displayName ?: (currentUserProfile?.displayName ?: "You"),
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (myRank != null) {
                                        "Ranked #$myRank out of ${data.allEntriesCount} players"
                                    } else {
                                        "Finish a match to register your official rank"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (myRank != null) "#$myRank" else "--",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = CyberCyan
                                )
                                Text(
                                    text = "${myEntry?.maxLength ?: currentUserProfile?.maxLength ?: 25} len",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean,
    isOwner: Boolean,
    onDelete: () -> Unit
) {
    val skin = SkinCatalog.getById(entry.skinId)
    val rankColor = when (rank) {
        1 -> SolarGold
        2 -> Color(0xFFD9E2EC)
        3 -> Color(0xFFE08E45)
        else -> TextSecondary
    }

    Surface(
        color = if (isCurrentUser) CyberCyan.copy(alpha = 0.12f) else ArenaSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCurrentUser) 1.5.dp else 1.dp,
                color = if (isCurrentUser) CyberCyan else ArenaBorder,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = "#$rank",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = rankColor,
                modifier = Modifier.width(42.dp)
            )
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(skin.primaryColor)
                    .border(2.dp, skin.secondaryColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isCurrentUser) "${entry.displayName} (You)" else entry.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrentUser) CyberCyan else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${skin.name} • ${entry.totalKills} kills",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.maxLength}",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = rankColor
                )
                Text(
                    text = "MAX LEN",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            if (isOwner) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Entry",
                        tint = CrimsonDanger
                    )
                }
            }
        }
    }
}
