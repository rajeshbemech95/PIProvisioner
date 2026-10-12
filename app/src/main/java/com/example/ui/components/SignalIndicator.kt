package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError

@Composable
fun SignalIndicator(
    rssi: Int,
    modifier: Modifier = Modifier
) {
    // Determine level from 1 to 4
    val level = when {
        rssi >= -60 -> 4
        rssi >= -70 -> 3
        rssi >= -80 -> 2
        else -> 1
    }

    val activeColor = when (level) {
        4 -> EmeraldSuccess
        3 -> CyberCyan
        2 -> AmberWarning
        else -> RoseError
    }

    val inactiveColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Row(
        modifier = modifier.testTag("signal_indicator_$rssi"),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val heights = listOf(6.dp, 10.dp, 14.dp, 18.dp)
        heights.forEachIndexed { index, barHeight ->
            val isBarActive = (index + 1) <= level
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isBarActive) activeColor else inactiveColor)
            )
        }
    }
}
