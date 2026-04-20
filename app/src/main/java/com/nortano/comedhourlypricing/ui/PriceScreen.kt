package com.nortano.comedhourlypricing.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.horologist.compose.layout.AppScaffold
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.nortano.comedhourlypricing.R
import com.nortano.comedhourlypricing.utils.getRelativeTime

val PriceTier.color: Color
    get() =
        when (this) {
            PriceTier.FREE -> Color(0xFF00E5FF) // Blue/Cyan for <= 0
            PriceTier.NORMAL -> Color(0xFF00E676) // Neon Green for 0 - 5.0
            PriceTier.ELEVATED -> Color(0xFFFFEA00) // Yellow for 5.0 - 9.9
            PriceTier.HIGH -> Color(0xFFFF1744) // Red for >= 10.0
            PriceTier.UNKNOWN -> Color.White
        }

@Composable
fun PriceScreen(viewModel: PriceViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PriceScreenContent(
        state = state,
        onRefresh = { viewModel.refresh() },
    )
}

@Composable
fun PriceScreenContent(
    state: PriceUiState,
    onRefresh: () -> Unit,
) {
    val priceTier = state.priceTier

    // Animation for the refresh icon
    val infiniteTransition = rememberInfiniteTransition(label = "RefreshTransition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "RefreshRotation",
    )

    AppScaffold {
        ScreenScaffold(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                // Main Content
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp),
                    // Shifts the center content up slightly
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Error Message (if any)
                    if (!state.errorMessage.isNullOrBlank()) {
                        Text(
                            text = stringResource(R.string.error_label),
                            color = MaterialTheme.colors.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }

                    // Massive Current Price
                    val displayPrice = state.priceText ?: stringResource(R.string.empty_price)
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = displayPrice,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = priceTier.color,
                            maxLines = 1,
                        )
                    }

                    // Unit label right beneath
                    Text(
                        text = stringResource(R.string.unit_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    // Hourly Average
                    val hourlyPrice = state.hourlyAvgPriceText ?: stringResource(R.string.empty_price)
                    Text(
                        text = stringResource(R.string.hourly_label, hourlyPrice),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )

                    // Time since last update
                    val context = LocalContext.current
                    val updatedText =
                        state.updatedAtMillis?.let { getRelativeTime(context, it) } ?: stringResource(R.string.no_data)
                    Text(
                        text = updatedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium, // Slightly bolder for readability
                        color = Color(0xFFAAAAAA), // Brighter gray
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                // Refresh Button at the bottom
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !state.isRefreshing) { onRefresh() }
                            .padding(8.dp), // clickable padding area
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.refresh_description),
                        tint = if (state.isRefreshing) priceTier.color else Color.Gray,
                        modifier =
                            Modifier
                                .size(24.dp)
                                .rotate(if (state.isRefreshing) rotation else 0f),
                    )
                }
            }
        }
    }
}
