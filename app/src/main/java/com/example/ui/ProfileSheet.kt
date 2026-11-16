package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AgeGroup
import com.example.model.AppStrings
import com.example.model.Dialect
import com.example.model.DigitalBadge
import com.example.model.ScoutProfile
import com.example.util.VoiceOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(
    profile: ScoutProfile?,
    badgesList: List<DigitalBadge>,
    dialect: Dialect,
    availableVoices: List<VoiceOption>,
    selectedVoiceId: String?,
    onSelectVoice: (String) -> Unit,
    onToggleDialect: () -> Unit,
    onSaveProfile: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = AppStrings.forDialect(dialect)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(profile?.scoutName) {
        mutableStateOf(profile?.scoutName ?: "كشاف واعي")
    }
    var ageGroup by remember(profile?.ageGroup) {
        mutableStateOf(profile?.ageGroup ?: AgeGroup.SCOUTS.code)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.logo_cyberscout_official),
                    contentDescription = strings.appTitle,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = strings.profileTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val earnedCount = badgesList.count { it.unlocked }
                    Text(
                        text = "${strings.lblBadges}: $earnedCount / 5",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                label = { Text(strings.scoutNameLabel) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_name_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Text(
                text = strings.ageGroupLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = ageGroup == AgeGroup.CUBS.code,
                    onClick = { ageGroup = AgeGroup.CUBS.code },
                    label = { Text(strings.ageCubs) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("age_cubs_chip")
                )
                FilterChip(
                    selected = ageGroup == AgeGroup.SCOUTS.code,
                    onClick = { ageGroup = AgeGroup.SCOUTS.code },
                    label = { Text(strings.ageScouts) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("age_scouts_chip")
                )
            }

            OutlinedButton(
                onClick = onToggleDialect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("profile_dialect_toggle"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Translate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = strings.dialectLabel, fontWeight = FontWeight.Bold)
            }

            if (availableVoices.isNotEmpty()) {
                Text(
                    text = strings.voiceActive,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableVoices.take(4).forEach { voice ->
                        FilterChip(
                            selected = voice.id == selectedVoiceId,
                            onClick = { onSelectVoice(voice.id) },
                            label = { Text(voice.label) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Button(
                onClick = {
                    onSaveProfile(name, ageGroup)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_profile_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = strings.saveProfile, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider()

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onSignOut()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(imageVector = Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = strings.signOut, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
