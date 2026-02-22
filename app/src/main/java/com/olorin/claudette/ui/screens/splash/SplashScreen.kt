package com.olorin.claudette.ui.screens.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olorin.claudette.LocalAppConfiguration
import com.olorin.claudette.ui.theme.ClaudetteBackground
import com.olorin.claudette.ui.theme.ClaudettePrimary
import com.olorin.claudette.ui.theme.ClaudettePrimaryLight
import kotlinx.coroutines.delay

private const val TITLE_FADE_DELAY_MS = 300L
private const val CURSOR_FADE_DELAY_MS = 500L
private const val SLOGAN_SLIDE_DELAY_MS = 900L
private const val FOOTER_FADE_DELAY_MS = 1400L
private const val AUTO_DISMISS_DELAY_MS = 2500L
private const val FADE_IN_DURATION_MS = 600
private const val CURSOR_BLINK_DURATION_MS = 530

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    val config = LocalAppConfiguration.current

    var showTitle by remember { mutableStateOf(false) }
    var showCursor by remember { mutableStateOf(false) }
    var showSlogan by remember { mutableStateOf(false) }
    var showFooter by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "cursorBlink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CURSOR_BLINK_DURATION_MS),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    LaunchedEffect(Unit) {
        delay(TITLE_FADE_DELAY_MS)
        showTitle = true
        delay(CURSOR_FADE_DELAY_MS - TITLE_FADE_DELAY_MS)
        showCursor = true
        delay(SLOGAN_SLIDE_DELAY_MS - CURSOR_FADE_DELAY_MS)
        showSlogan = true
        delay(FOOTER_FADE_DELAY_MS - SLOGAN_SLIDE_DELAY_MS)
        showFooter = true
        delay(AUTO_DISMISS_DELAY_MS - FOOTER_FADE_DELAY_MS)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClaudetteBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App name with cursor
            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn(animationSpec = tween(FADE_IN_DURATION_MS))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = config.splashAppName,
                        color = ClaudettePrimary,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    if (showCursor) {
                        Text(
                            text = config.splashCursorSymbol,
                            color = ClaudettePrimaryLight,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .alpha(cursorAlpha)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slogan slides in from bottom
            AnimatedVisibility(
                visible = showSlogan,
                enter = fadeIn(animationSpec = tween(FADE_IN_DURATION_MS)) +
                    slideInVertically(
                        animationSpec = tween(FADE_IN_DURATION_MS),
                        initialOffsetY = { it / 2 }
                    )
            ) {
                Text(
                    text = config.splashSlogan,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Footer at the bottom
        AnimatedVisibility(
            visible = showFooter,
            enter = fadeIn(animationSpec = tween(FADE_IN_DURATION_MS)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = config.splashFooterText,
                color = Color(0xFF666680),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
