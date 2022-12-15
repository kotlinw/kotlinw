package kotlinw.compose.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

@Composable
fun <T : Any, K : Any> ComboBox(
    label: String,
    options: List<T>,
    valueKey: K?,
    onValueChange: (T?) -> Unit,
    keyProvider: (T) -> K,
    emptyText: String = "",
    displayNameProvider: (Any?) -> String
) {
    Column(Modifier.padding(20.dp)) {
        var textFieldSize by remember { mutableStateOf(Size.Zero) }
        var expanded by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = options.firstOrNull { keyProvider(it) == valueKey }?.let { displayNameProvider(it) }
                ?: emptyText,
            readOnly = true,
            onValueChange = { },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    textFieldSize = coordinates.size.toSize()
                }
                .clickable {
                    expanded = true
                },
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    if (expanded)
                        Icons.Filled.KeyboardArrowUp
                    else
                        Icons.Filled.KeyboardArrowDown,
                    "contentDescription",
                    Modifier.clickable { expanded = !expanded }
                )
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
        ) {
            options.forEach { option ->
                key(keyProvider(option)) {
                    DropdownMenuItem(onClick = {
                        expanded = false
                        onValueChange(option)
                    }) {
                        Text(text = displayNameProvider(option))
                    }
                }
            }
        }
    }
}
