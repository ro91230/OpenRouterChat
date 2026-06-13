package com.personal.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.personal.chat.ui.theme.SlateGray

@Composable
fun BottomHeavyLayout(
    modifier: Modifier = Modifier,
    topContent: @Composable BoxScope.() -> Unit,
    bottomInteractivePanel: @Composable BoxScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.67f)
        ) {
            topContent()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.33f)
                .background(SlateGray)
        ) {
            bottomInteractivePanel()
        }
    }
}
