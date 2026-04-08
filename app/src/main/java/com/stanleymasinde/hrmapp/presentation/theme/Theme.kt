package com.stanleymasinde.hrmapp.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

@Composable
fun HrmAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WearColorScheme,
        content = content
    )
}

@Preview
@Composable
fun HrmAppThemePreview() {
    HrmAppTheme {
        Text(text = "HrmApp Theme Preview")
    }
}
