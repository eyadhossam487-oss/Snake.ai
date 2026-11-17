package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.game.SnakePreviewCanvas
import com.example.model.BotDifficulty
import com.example.model.SkinCatalog
import com.example.model.SnakePattern
import com.example.model.SnakeSkin
import com.example.model.UserProfile
import com.example.ui.theme.ArenaBorder
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.ArenaSurfaceElevated
import com.example.ui.theme.ArenaVoid
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkinStudioScreen(
    profile: UserProfile?,
    isOwner: Boolean,
    isBusy: Boolean,
    onSaveCustomization: (displayName: String, skinId: String, pattern: SnakePattern, difficulty: BotDifficulty) -> Unit,
    onUnlockShopSkin: (skinId: String) -> Unit,
    onOpenDailyRewards: () -> Unit,
    onBackToHome: () -> Unit
) {
    BackHandler { onBackToHome() }

    val availableSkins = remember(isOwner) { SkinCatalog.availableFor(isOwner) }
    val unlockedSkinIds = profile?.unlockedSkinIds ?: emptyList()
    val coins = profile?.coins ?: 0L
    var displayName by remember(profile?.displayName) {
        mutableStateOf(profile?.displayName ?: "Viper")
    }
    var selectedSkinId by remember(profile?.selectedSkinId) {
        mutableStateOf(profile?.selectedSkinId ?: "cyber_viper")
    }
    var selectedPattern by remember(profile?.selectedPattern) {
        mutableStateOf(SnakePattern.fromRaw(profile?.selectedPattern))
    }
    val preferredDiff = BotDifficulty.fromRaw(profile?.preferredDifficulty)
    val selectedSkin = SkinCatalog.getById(selectedSkinId)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaVoid),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SKIN STUDIO",
                            style = MaterialTheme.typography.headlineLarge,
                            color = CyberCyan
                        )
                        Text(
                            text = "Customize serpent colors, scale pattern & callsign",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Surface(
                        color = SolarGold.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .border(1.dp, SolarGold.copy(alpha = 0.55f), RoundedCornerShape(50))
                            .testTag("skin_studio_coins_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Venom Coins",
                                tint = SolarGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$coins",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelLarge,
                                color = SolarGold
                            )
                        }
                    }
                }
            }

            // Live Interactive Snake Preview
            item {
                Surface(
                    color = ArenaSurface,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, selectedSkin.primaryColor.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = selectedSkin.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = selectedSkin.primaryColor
                                )
                                Text(
                                    text = selectedSkin.tagline,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            if (selectedSkin.ownerOnly) {
                                Surface(
                                    color = SolarGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.WorkspacePremium,
                                            contentDescription = null,
                                            tint = SolarGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("OWNER", style = MaterialTheme.typography.labelSmall, color = SolarGold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        SnakePreviewCanvas(
                            skinId = selectedSkinId,
                            pattern = selectedPattern,
                            showCrown = isOwner || selectedSkin.ownerOnly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                        )
                    }
                }
            }

            // Nickname Input
            item {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { if (it.length <= 24) displayName = it },
                    label = { Text("Arena Callsign") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nickname_input")
                )
            }

            // Pattern Selector
            item {
                Text(
                    text = "SCALE PATTERN",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SnakePattern.entries.forEach { pat ->
                        FilterChip(
                            selected = selectedPattern == pat,
                            onClick = { selectedPattern = pat },
                            label = { Text(pat.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                selectedLabelColor = CyberCyan
                            ),
                            modifier = Modifier.testTag("pattern_chip_${pat.name.lowercase()}")
                        )
                    }
                }
            }

            // Skin Swatch Grid
            item {
                Text(
                    text = "SKIN PALETTES",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    availableSkins.chunked(2).forEach { pair ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            pair.forEach { skin ->
                                val isUnlocked = SkinCatalog.isSkinUnlocked(skin, unlockedSkinIds, isOwner)
                                SkinOptionCard(
                                    skin = skin,
                                    unlocked = isUnlocked,
                                    selected = skin.id == selectedSkinId,
                                    onClick = { selectedSkinId = skin.id },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Save / Unlock Action Button
            item {
                val isSelectedUnlocked = SkinCatalog.isSkinUnlocked(selectedSkin, unlockedSkinIds, isOwner)
                if (isSelectedUnlocked) {
                    Button(
                        onClick = {
                            onSaveCustomization(
                                displayName,
                                selectedSkinId,
                                selectedPattern,
                                preferredDiff
                            )
                        },
                        enabled = !isBusy && displayName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = ArenaVoid
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("save_customization_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                color = ArenaVoid,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SAVE LOADOUT", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else if (selectedSkin.unlockCostCoins > 0) {
                    val canAfford = coins >= selectedSkin.unlockCostCoins.toLong()
                    Button(
                        onClick = { onUnlockShopSkin(selectedSkin.id) },
                        enabled = !isBusy && canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SolarGold,
                            contentColor = ArenaVoid
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("unlock_skin_coins_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                color = ArenaVoid,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (canAfford) {
                                    "UNLOCK FOR ${selectedSkin.unlockCostCoins} COINS"
                                } else {
                                    "NEED ${selectedSkin.unlockCostCoins - coins} MORE COINS"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onOpenDailyRewards,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SolarGold,
                            contentColor = ArenaVoid
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("unlock_skin_daily_button")
                    ) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UNLOCK ON DAY ${selectedSkin.dailyRewardDay ?: 3} IN DAILY REWARDS",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkinOptionCard(
    skin: SnakeSkin,
    unlocked: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) skin.primaryColor.copy(alpha = 0.14f) else ArenaSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) skin.primaryColor else ArenaBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("skin_card_${skin.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(skin.primaryColor.copy(alpha = if (unlocked) 1f else 0.45f))
                    .border(3.dp, skin.secondaryColor, CircleShape)
            ) {
                if (!unlocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = ArenaVoid,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = skin.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = when {
                    skin.ownerOnly -> "Owner Exclusive"
                    !unlocked && skin.dailyRewardDay != null -> "Day ${skin.dailyRewardDay} Login Reward"
                    !unlocked && skin.unlockCostCoins > 0 -> "${skin.unlockCostCoins} Venom Coins"
                    else -> skin.tagline
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        skin.ownerOnly -> SolarGold
                        !unlocked -> SolarGold
                        else -> TextSecondary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
