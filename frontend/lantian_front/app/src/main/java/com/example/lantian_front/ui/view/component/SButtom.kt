package com.example.lantian_front.ui.view.component

import androidx.compose.material3.Button
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SCommonButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .wrapContentWidth()
            .height(48.dp),
        content = content
    )
}