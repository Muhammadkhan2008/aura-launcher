package com.aura.launcher

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * SettingsPanel — chhota settings dialog.
 *
 * Abhi: app drawer ke columns (3-6) adjust kar sakte ho.
 * Sab kuch offline SharedPreferences mein save hota hai.
 */
@Composable
fun SettingsPanel(
    prefs: AuraPrefs,
    onClose: () -> Unit,
    onChanged: () -> Unit
) {
    var columns by remember { mutableStateOf(prefs.gridColumns) }

    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = Color(0xFF1B1730)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Aura Settings",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(20.dp))

                Text("App drawer columns: $columns", color = Color.White, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))

                // Columns slider (3 se 6)
                Slider(
                    value = columns.toFloat(),
                    onValueChange = { columns = it.toInt() },
                    valueRange = 3f..6f,
                    steps = 2   // 3,4,5,6
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClose) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        prefs.gridColumns = columns
                        onChanged()
                        onClose()
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
