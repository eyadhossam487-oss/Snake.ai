package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.game.SnakePreviewCanvas
import com.example.model.DailyClaimResult
import com.example.model.DailyRewardCatalog
import com.example.model.DailyRewardKind
import com.example.model.DailyRewardStatus
import com.example.model.DailyRewardTier
import com.example.model.SkinCatalog
import com.example.model.SnakePattern
import com.example.ui.theme.ArenaBorder
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.ArenaSurfaceElevated
import com.example.ui.theme.ArenaVoid
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlasmaMagenta
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextSecondary

@Composable
fun DailyRewardDialog(
    coins: Long,
    status: DailyRewardStatus,
    lastClaimResult: DailyClaimResult?,
    isBusy: Boolean,
    onClaim: () -> Unit,
    onDismiss: () -> Unit
) {
    val tiers = DailyRewardCatalog.tiers
    val nextTier = DailyRewardCatalog.tierForDay(status.nextRewardDay)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = ArenaSurface,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 500.dp)
                .border(1.5.dp, CyberCyan.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
                .testTag("daily_reward_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = SolarGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = SolarGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DAILY REWARDS",
                            style = MaterialTheme.typography.headlineMedium,
                            color = CyberCyan
                        )
                        Text(
                            text = "Log in once every 24h for coins & skins",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Surface(
                        color = SolarGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.border(1.dp, SolarGold.copy(alpha = 0.6f), RoundedCornerShape(50))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                                fontWeight = FontWeight.Bold,
                                color = SolarGold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Celebration card immediately after claiming
                if (lastClaimResult != null) {
                    val unlockedSkin = lastClaimResult.unlockedSkinId?.let { SkinCatalog.getById(it) }
                    Surface(
                        color = CyberCyan.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, CyberCyan.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                            .testTag("daily_claim_celebration")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DAY ${lastClaimResult.claimedDay} CLAIMED!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CyberCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (unlockedSkin != null) {
                                    "Earned +${lastClaimResult.coinsAwarded} Venom Coins & unlocked ${unlockedSkin.name}!"
                                } else {
                                    "Earned +${lastClaimResult.coinsAwarded} Venom Coins!"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (unlockedSkin != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                SnakePreviewCanvas(
                                    skinId = unlockedSkin.id,
                                    pattern = SnakePattern.NEON_CORE,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Cooldown / Streak Banner
                Surface(
                    color = if (status.canClaimNow) SolarGold.copy(alpha = 0.14f) else ArenaSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (status.canClaimNow) SolarGold.copy(alpha = 0.7f) else ArenaBorder,
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (status.canClaimNow) Icons.Default.AutoAwesome else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (status.canClaimNow) SolarGold else CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (status.canClaimNow) {
                                    "Day ${status.nextRewardDay} Ready!"
                                } else {
                                    "Next in ${DailyRewardCatalog.formatCountdown(status.remainingCooldownMs)}"
                                },
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (status.canClaimNow) SolarGold else CyberCyan
                            )
                        }
                        Text(
                            text = "STREAK ${status.currentStreak}/7",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Days 1..6 grid (2 per row)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    tiers.take(6).chunked(2).forEach { pair ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            pair.forEach { tier ->
                                val isClaimed = tier.day <= status.currentStreak
                                val isCurrentTarget = tier.day == status.nextRewardDay
                                DailyTierCard(
                                    tier = tier,
                                    isClaimed = isClaimed,
                                    isReadyNow = isCurrentTarget && status.canClaimNow,
                                    isUpNext = isCurrentTarget && !status.canClaimNow,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Day 7 Grand Jackpot Card
                    val day7 = tiers.last()
                    val day7Claimed = day7.day <= status.currentStreak
                    val day7Ready = day7.day == status.nextRewardDay && status.canClaimNow
                    val day7Skin = day7.skinId?.let { SkinCatalog.getById(it) }
                    Surface(
                        color = when {
                            day7Ready -> SolarGold.copy(alpha = 0.16f)
                            day7Claimed -> CyberCyan.copy(alpha = 0.10f)
                            else -> ArenaSurfaceElevated
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (day7Ready) 2.dp else 1.dp,
                                color = when {
                                    day7Ready -> SolarGold
                                    day7Claimed -> CyberCyan.copy(alpha = 0.6f)
                                    else -> PlasmaMagenta.copy(alpha = 0.45f)
                                },
                                shape = RoundedCornerShape(18.dp)
                            )
                            .testTag("daily_tier_card_7")
                    ) {
                        Box(
                            modifier = Modifier.background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        SolarGold.copy(alpha = 0.10f),
                                        PlasmaMagenta.copy(alpha = 0.08f)
                                    )
                                )
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                if (day7Skin != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(day7Skin.primaryColor)
                                            .border(3.dp, day7Skin.headColor, CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DAY 7 GRAND JACKPOT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SolarGold
                                    )
                                    Text(
                                        text = "${day7.title} Skin + ${day7.coinsAmount} Coins",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                TierStatusIcon(
                                    isClaimed = day7Claimed,
                                    isReadyNow = day7Ready
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onClaim,
                    enabled = status.canClaimNow && !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SolarGold,
                        contentColor = ArenaVoid
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("claim_daily_reward_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            color = ArenaVoid,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (status.canClaimNow) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CLAIM DAY ${nextTier.day}: ${nextTier.title.uppercase()}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    } else {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AVAILABLE IN ${DailyRewardCatalog.formatCountdown(status.remainingCooldownMs)}",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("close_daily_reward_button")
                ) {
                    Text("CLOSE")
                }
            }
        }
    }
}

@Composable
private fun DailyTierCard(
    tier: DailyRewardTier,
    isClaimed: Boolean,
    isReadyNow: Boolean,
    isUpNext: Boolean,
    modifier: Modifier = Modifier
) {
    val skin = tier.skinId?.let { SkinCatalog.getById(it) }
    val borderColor = when {
        isReadyNow -> SolarGold
        isClaimed -> CyberCyan.copy(alpha = 0.65f)
        isUpNext -> CyberCyan.copy(alpha = 0.35f)
        else -> ArenaBorder
    }
    val bgColor = when {
        isReadyNow -> SolarGold.copy(alpha = 0.14f)
        isClaimed -> CyberCyan.copy(alpha = 0.08f)
        else -> ArenaSurfaceElevated
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .border(if (isReadyNow) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .testTag("daily_tier_card_${tier.day}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "DAY ${tier.day}",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isReadyNow) SolarGold else TextSecondary
                )
                TierStatusIcon(isClaimed = isClaimed, isReadyNow = isReadyNow)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tier.kind == DailyRewardKind.SKIN && skin != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(skin.primaryColor)
                            .border(2.dp, skin.secondaryColor, CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = SolarGold,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tier.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (tier.kind == DailyRewardKind.SKIN) {
                            skin?.primaryColor ?: CyberCyan
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (tier.kind == DailyRewardKind.SKIN) {
                            "Unique Skin + ${tier.coinsAmount} Coins"
                        } else {
                            "Venom Coins"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TierStatusIcon(
    isClaimed: Boolean,
    isReadyNow: Boolean
) {
    when {
        isClaimed -> Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Claimed",
            tint = CyberCyan,
            modifier = Modifier.size(18.dp)
        )
        isReadyNow -> Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Ready to Claim",
            tint = SolarGold,
            modifier = Modifier.size(18.dp)
        )
        else -> Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked",
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}
