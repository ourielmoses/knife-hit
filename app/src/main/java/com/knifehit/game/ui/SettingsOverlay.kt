package com.knifehit.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.knifehit.game.i18n.LocalStrings
import com.knifehit.game.model.KnifeSkin
import com.knifehit.game.model.PlayerProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsOverlay(
    profile: PlayerProfile,
    ownedSkins: List<KnifeSkin>,
    onBack: () -> Unit,
    onAmbient: (Float) -> Unit,
    onSfx: (Float) -> Unit,
    onToggle: (PlayerProfile) -> Unit,
    onEquip: (String) -> Unit,
    onLanguage: (String) -> Unit,
    onClearSave: () -> Unit,
) {
    val s = LocalStrings.current
    var confirm1 by remember { mutableStateOf(false) }
    var confirm2 by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    OverlayScrim(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        ) {
            NeonButton(s.back, onBack)
            Spacer(Modifier.height(12.dp))
            Panel(Modifier.fillMaxWidth()) {
                Column {
                    Text(s.music, color = Color.White)
                    Slider(value = profile.ambientVolume, onValueChange = onAmbient)
                    Text(s.sfx, color = Color.White)
                    Slider(value = profile.sfxVolume, onValueChange = onSfx)
                    ToggleRow(s.haptics, profile.hapticsEnabled) {
                        onToggle(profile.copy(hapticsEnabled = it))
                    }
                    ToggleRow(s.particles, profile.particlesEnabled) {
                        onToggle(profile.copy(particlesEnabled = it))
                    }
                    ToggleRow(s.batterySaver, profile.batterySaver) {
                        onToggle(profile.copy(batterySaver = it))
                    }
                    ToggleRow(s.motion, profile.motionEnabled) {
                        onToggle(profile.copy(motionEnabled = it))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(s.language, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NeonButton(s.english, { onLanguage("en") }, enabled = profile.language != "en")
                        NeonButton(s.hebrew, { onLanguage("he") }, enabled = profile.language != "he")
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(s.favoriteKnife, color = Color.White)
                    if (ownedSkins.size <= 1) {
                        Text(s.favoriteHint, color = Color(0xFF9AA8B8))
                    } else {
                        ExposedDropdownMenuBox(expanded, { expanded = it }) {
                            TextField(
                                value = ownedSkins.firstOrNull { it.id == profile.equippedKnifeId }?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            )
                            ExposedDropdownMenu(expanded, { expanded = false }) {
                                ownedSkins.forEach { skin ->
                                    DropdownMenuItem(
                                        text = { Text(skin.name) },
                                        onClick = {
                                            expanded = false
                                            onEquip(skin.id)
                                        },
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    NeonButton(s.clearSave, { confirm1 = true }, accent = NeonMagenta)
                }
            }
        }
    }
    if (confirm1) {
        AlertDialog(
            onDismissRequest = { confirm1 = false },
            confirmButton = {
                TextButton(onClick = { confirm1 = false; confirm2 = true }) { Text(s.clearSave) }
            },
            dismissButton = { TextButton(onClick = { confirm1 = false }) { Text(s.back) } },
            title = { Text(s.clearSaveConfirm1) },
        )
    }
    if (confirm2) {
        AlertDialog(
            onDismissRequest = { confirm2 = false },
            confirmButton = {
                TextButton(onClick = { confirm2 = false; onClearSave() }) { Text(s.clearSave) }
            },
            dismissButton = { TextButton(onClick = { confirm2 = false }) { Text(s.back) } },
            title = { Text(s.clearSaveConfirm2) },
        )
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White)
        Switch(checked = value, onCheckedChange = onChange)
    }
}
