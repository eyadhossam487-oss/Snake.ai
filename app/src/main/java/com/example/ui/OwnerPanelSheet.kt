package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.BotDifficulty
import com.example.model.OwnerCheatState
import com.example.ui.theme.ArenaBorder
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.ArenaSurfaceElevated
import com.example.ui.theme.ArenaVoid
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.PlasmaMagenta
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OwnerPanelSheet(
    isInArena: Boolean,
    cheatState: OwnerCheatState,
    currentBotDifficulty: BotDifficulty,
    currentUserLength: Long,
    onDismiss: () -> Unit,
    onToggleGodMode: () -> Unit = {},
    onTogglePermaSpeed: () -> Unit = {},
    onTogglePermaMagnet: () -> Unit = {},
    onToggleFreezeBots: () -> Unit = {},
    onAddLength: (Int) -> Unit = {},
    onSpawnFoodRain: () -> Unit = {},
    onEliminateAllBots: () -> Unit = {},
    onOverrideBotDifficulty: (BotDifficulty) -> Unit = {},
    onSetGlobalLeaderboardScore: (Long) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customLengthInput by remember(currentUserLength) {
        mutableStateOf(currentUserLength.coerceAtLeast(1000L).toString())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ArenaSurface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "Owner Crown",
                    tint = SolarGold,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "OWNER COMMAND VAULT",
                        style = MaterialTheme.typography.titleLarge,
                        color = SolarGold
                    )
                    Text(
                        text = "First-account exclusive cheats & cloud controls",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_owner_panel_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Owner Panel")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isInArena) {
                Text(
                    text = "LIVE ARENA CHEATS",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(10.dp))

                CheatToggleRow(
                    title = "God Mode (Invincible)",
                    subtitle = "Phase through enemy snakes & bounce safely off arena walls",
                    icon = Icons.Default.Shield,
                    checked = cheatState.godMode,
                    testTag = "owner_god_mode_switch",
                    onCheckedChange = { onToggleGodMode() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                CheatToggleRow(
                    title = "Speed Overdrive",
                    subtitle = "Permanent 2x hyper-speed with zero length drain",
                    icon = Icons.Default.Bolt,
                    checked = cheatState.permaSpeedOverdrive,
                    testTag = "owner_speed_switch",
                    onCheckedChange = { onTogglePermaSpeed() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                CheatToggleRow(
                    title = "Mega Coin Magnet",
                    subtitle = "Pull food orbs from 460m radius continuously",
                    icon = Icons.Default.AutoAwesome,
                    checked = cheatState.permaMegaMagnet,
                    testTag = "owner_magnet_switch",
                    onCheckedChange = { onTogglePermaMagnet() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                CheatToggleRow(
                    title = "Freeze All AI Bots",
                    subtitle = "Lock all bot snakes in place for easy eliminations",
                    icon = Icons.Default.PauseCircle,
                    checked = cheatState.freezeBots,
                    testTag = "owner_freeze_bots_switch",
                    onCheckedChange = { onToggleFreezeBots() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onAddLength(500) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SolarGold,
                            contentColor = ArenaVoid
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("owner_add_length_button")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+500 Length", style = MaterialTheme.typography.labelLarge)
                    }

                    Button(
                        onClick = onSpawnFoodRain,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = ArenaVoid
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("owner_food_rain_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Food Rain", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onEliminateAllBots,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonDanger),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("owner_smite_bots_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Eliminate All Bots (Drop Loot)", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "LIVE BOT AI DIFFICULTY",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BotDifficulty.entries.forEach { diff ->
                        val selected = (cheatState.botDifficultyOverride ?: currentBotDifficulty) == diff
                        FilterChip(
                            selected = selected,
                            onClick = { onOverrideBotDifficulty(diff) },
                            label = { Text(diff.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PlasmaMagenta.copy(alpha = 0.25f),
                                selectedLabelColor = PlasmaMagenta
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = ArenaBorder)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "CLOUD LEADERBOARD OVERRIDE",
                style = MaterialTheme.typography.labelLarge,
                color = SolarGold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Directly set your official Global Leaderboard max length.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = customLengthInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 7) {
                            customLengthInput = input
                        }
                    },
                    label = { Text("Custom Max Length") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("owner_custom_length_input")
                )
                Button(
                    onClick = {
                        val parsed = customLengthInput.toLongOrNull()?.coerceIn(25L, 9_999_999L) ?: 1000L
                        onSetGlobalLeaderboardScore(parsed)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SolarGold,
                        contentColor = ArenaVoid
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("owner_apply_length_button")
                ) {
                    Text("SET", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun CheatToggleRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = ArenaSurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (checked) SolarGold.copy(alpha = 0.65f) else ArenaBorder,
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) SolarGold else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ArenaVoid,
                    checkedTrackColor = SolarGold
                ),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
