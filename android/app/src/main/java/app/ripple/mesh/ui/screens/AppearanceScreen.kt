package app.ripple.mesh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ripple.mesh.R
import app.ripple.mesh.ui.MeshViewModel
import app.ripple.mesh.ui.theme.AppTheme
import kotlin.math.roundToInt

data class FontOption(val id: String, val name: String, val family: FontFamily)

val AVAILABLE_FONTS = listOf(
    FontOption("system", "Default System", FontFamily.Default),
    FontOption("chiller", "Chiller / Cursive", FontFamily.Cursive),
    FontOption("monospace", "Monospace", FontFamily.Monospace),
    FontOption("serif", "Serif", FontFamily.Serif),
    FontOption("sans_serif", "Sans-Serif", FontFamily.SansSerif)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(vm: MeshViewModel, onBack: () -> Unit) {
    val currentThemeId by vm.themeMode.collectAsStateWithLifecycle()
    val selectedTheme = AppTheme.fromId(currentThemeId)
    var themeExpanded by remember { mutableStateOf(false) }

    val currentFontId by vm.fontFamily.collectAsStateWithLifecycle()
    val selectedFont = AVAILABLE_FONTS.firstOrNull { it.id == currentFontId } ?: AVAILABLE_FONTS.first()
    var fontExpanded by remember { mutableStateOf(false) }

    val currentFontScale by vm.fontScale.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            title = { Text(stringResource(R.string.appearance)) },
        )
    }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Theme Selector Section
            Text(stringResource(R.string.theme_setting), style = MaterialTheme.typography.titleMedium)

            ExposedDropdownMenuBox(
                expanded = themeExpanded,
                onExpandedChange = { themeExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = stringResource(selectedTheme.titleResId),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = themeExpanded,
                    onDismissRequest = { themeExpanded = false }
                ) {
                    AppTheme.entries.forEach { theme ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stringResource(theme.titleResId))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Box(Modifier.size(16.dp).background(theme.primaryColor, RoundedCornerShape(4.dp)))
                                        Box(Modifier.size(16.dp).background(theme.secondaryColor, RoundedCornerShape(4.dp)))
                                    }
                                }
                            },
                            onClick = {
                                vm.setThemeMode(theme.id)
                                themeExpanded = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            // Font & Typography Section
            Text(stringResource(R.string.font_setting), style = MaterialTheme.typography.titleMedium)

            // Font Family Selector
            Text("Font Family", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ExposedDropdownMenuBox(
                expanded = fontExpanded,
                onExpandedChange = { fontExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedFont.name,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = fontExpanded,
                    onDismissRequest = { fontExpanded = false }
                ) {
                    AVAILABLE_FONTS.forEach { font ->
                        DropdownMenuItem(
                            text = {
                                Text(font.name, fontFamily = font.family, style = MaterialTheme.typography.bodyLarge)
                            },
                            onClick = {
                                vm.setFontFamily(font.id)
                                fontExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Font Size Volume-like Slider Section (60% to 140%)
            val percentage = (currentFontScale * 100).roundToInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Font Size Scaling", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$percentage%" + if (percentage == 100) " (Default)" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.FormatSize,
                    contentDescription = "Small Font",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = currentFontScale,
                    onValueChange = { vm.setFontScale((it * 100).roundToInt() / 100f) },
                    valueRange = 0.60f..1.40f,
                    steps = 15,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.FormatSize,
                    contentDescription = "Large Font",
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // Real-Time Live Typography Preview Box
            Text("Typography Preview", style = MaterialTheme.typography.titleMedium)
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ripple Mesh Chat",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = selectedFont.family
                    )
                    Text(
                        "Quick brown fox jumps over the lazy dog. Off-grid mesh network operational with 2 active peers.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = selectedFont.family
                    )
                    Text(
                        "1:52 PM · Direct BLE Link (-68 dBm)",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = selectedFont.family,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
