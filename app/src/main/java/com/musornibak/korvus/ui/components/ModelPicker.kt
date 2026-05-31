package com.musornibak.korvus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musornibak.korvus.data.model.ModelInfo
import com.musornibak.korvus.data.model.ModelRegistry
import com.musornibak.korvus.data.model.Provider
import com.musornibak.korvus.ui.theme.KorvusInkSoft
import com.musornibak.korvus.ui.theme.KorvusOrangeBg

@Composable
fun ModelPickerChip(
    selected: ModelInfo,
    onPick: (ModelInfo) -> Unit
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .background(KorvusOrangeBg, RoundedCornerShape(50))
            .clickable { open = true }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        ProviderIcon(model = selected, size = 26.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = selected.displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = KorvusInkSoft
        )
    }

    if (open) {
        ModelPickerSheet(
            currentId = selected.id,
            onDismiss = { open = false },
            onPick = {
                onPick(it)
                open = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerSheet(
    currentId: String,
    onDismiss: () -> Unit,
    onPick: (ModelInfo) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Модель",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "HF — стабильно, твой ключ. Pollinations — без лимитов, иногда падает.",
                style = MaterialTheme.typography.bodyMedium,
                color = KorvusInkSoft,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ProviderHeader("HuggingFace")
            ModelRegistry.ALL.filter { it.provider == Provider.HF }.forEach { m ->
                ModelRow(m, m.id == currentId) { onPick(m) }
            }
            Spacer(Modifier.height(14.dp))
            ProviderHeader("Pollinations")
            ModelRegistry.ALL.filter { it.provider == Provider.POLLINATIONS }.forEach { m ->
                ModelRow(m, m.id == currentId) { onPick(m) }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ProviderHeader(name: String) {
    Text(
        name,
        style = MaterialTheme.typography.labelMedium,
        color = KorvusInkSoft,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ModelRow(model: ModelInfo, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(vertical = 4.dp)
            .background(
                if (selected) KorvusOrangeBg else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(16.dp)
            )
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(PaddingValues(horizontal = 14.dp, vertical = 12.dp))
    ) {
        ProviderIcon(model = model, size = 42.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                model.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                model.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = KorvusInkSoft
            )
        }
    }
}
