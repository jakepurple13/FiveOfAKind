package com.programmersbox.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlin.random.nextInt

private const val DOT_LOOK = "●"

internal class Dice(value: Int = Random.nextInt(1..6), @Suppress("unused") val location: String) {
    var value by mutableIntStateOf(value)

    suspend fun roll(rollCount: Int = 5) {
        randomNumberAnimation(
            newValue = Random.nextInt(1..6),
            valueChange = { value = it },
            randomCount = rollCount
        )
    }

    @Composable
    fun ShowDice(
        useDots: Boolean,
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
    ) = if (useDots) DiceDots(this, modifier, onClick) else Dice(this, modifier, onClick)

    @Composable
    operator fun invoke(
        useDots: Boolean,
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
    ) = ShowDice(useDots, modifier, onClick)
}

@Composable
internal fun Dice(dice: Dice, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(7.dp),
        tonalElevation = 4.dp,
        enabled = dice.value != 0,
        border = BorderStroke(1.dp, contentColorFor(MaterialTheme.colorScheme.surface)),
        modifier = modifier.size(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (dice.value == 0) "" else dice.value.toString(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun DiceDots(dice: Dice, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(7.dp),
        tonalElevation = 4.dp,
        enabled = dice.value != 0,
        border = BorderStroke(1.dp, contentColorFor(MaterialTheme.colorScheme.surface)),
        modifier = modifier.size(56.dp)
    ) {
        when (dice.value) {
            1 -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                    Text(
                        DOT_LOOK,
                        textAlign = TextAlign.Center
                    )
                }
            }

            2 -> {
                Box(modifier = Modifier.padding(4.dp)) {
                    Text(DOT_LOOK, modifier = Modifier.align(Alignment.TopEnd), textAlign = TextAlign.Center)
                    Text(DOT_LOOK, modifier = Modifier.align(Alignment.BottomStart), textAlign = TextAlign.Center)
                }
            }

            3 -> {
                Box(modifier = Modifier.padding(4.dp)) {
                    Text(DOT_LOOK, modifier = Modifier.align(Alignment.TopEnd), textAlign = TextAlign.Center)
                    Text(DOT_LOOK, modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center)
                    Text(DOT_LOOK, modifier = Modifier.align(Alignment.BottomStart), textAlign = TextAlign.Center)
                }
            }

            4 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        Text(DOT_LOOK, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(DOT_LOOK, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                    Row(modifier = Modifier.weight(1f)) {
                        Text(DOT_LOOK, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(DOT_LOOK, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
            }

            5 -> {
                Box(modifier = Modifier.padding(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Text(DOT_LOOK, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(DOT_LOOK, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                    Text(DOT_LOOK, modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Text(DOT_LOOK, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(DOT_LOOK, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
            }

            6 -> {
                val fontSize = LocalTextStyle.current.fontSize
                val fontColor = LocalContentColor.current
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val spaceBetweenWidthDots = size.width / 3
                    val spaceBetweenHeightDots = size.height / 4
                    repeat(6) {
                        drawCircle(
                            color = fontColor,
                            radius = fontSize.toPx() / 4,
                            center = Offset(
                                spaceBetweenWidthDots * (it % 2 + 1),
                                spaceBetweenHeightDots * (it % 3 + 1)
                            )
                        )
                    }
                }
            }
        }
    }
}