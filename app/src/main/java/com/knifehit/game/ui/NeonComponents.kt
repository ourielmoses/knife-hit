package com.knifehit.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knifehit.game.i18n.GameFontFamily
import com.knifehit.game.render.toComposeColor

val NeonCyan = 0xFF00F5FF.toComposeColor()
val NeonMagenta = 0xFFFF2BD6.toComposeColor()
val NeonLime = 0xFF39FF14.toComposeColor()
val PanelBg = 0xE610141C.toComposeColor()

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = NeonCyan,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = 0xFF151A24.toComposeColor(),
            contentColor = accent,
            disabledContainerColor = 0xFF22262E.toComposeColor(),
            disabledContentColor = 0xFF66707A.toComposeColor(),
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            text = text,
            fontFamily = GameFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            style = TextStyle(shadow = Shadow(accent.copy(alpha = 0.65f), blurRadius = 12f)),
        )
    }
}

@Composable
fun NeonTitle(text: String, modifier: Modifier = Modifier, color: Color = NeonCyan) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = GameFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 42.sp,
        style = TextStyle(
            shadow = Shadow(color.copy(alpha = 0.85f), blurRadius = 22f),
        ),
    )
}

@Composable
fun OverlayScrim(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .background(0xCC070B14.toComposeColor())
            .padding(20.dp),
    ) { content() }
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .shadow(12.dp, RoundedCornerShape(18.dp))
            .background(PanelBg, RoundedCornerShape(18.dp))
            .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(18.dp),
    ) { content() }
}

@Composable
fun WalletChip(label: String, value: Int, color: Color, onClick: (() -> Unit)? = null) {
    Text(
        text = "$label $value",
        color = color,
        fontFamily = GameFontFamily,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(0x66101820.toComposeColor(), RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        fontSize = 14.sp,
    )
}
