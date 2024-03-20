package com.programmersbox.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

internal enum class YahtzeeState { RollOne, RollTwo, RollThree, Stop }

private const val IS_NOT_DEBUG = true

internal class YahtzeeViewModel : ViewModel() {

    var rolling by mutableStateOf(false)

    var showGameOverDialog by mutableStateOf(true)

    var state by mutableStateOf(YahtzeeState.RollOne)

    val scores = YahtzeeScores()

    val hand = mutableStateListOf(
        Dice(0, location = "1"),
        Dice(0, location = "2"),
        Dice(0, location = "3"),
        Dice(0, location = "4"),
        Dice(0, location = "5")
    )

    val hold = mutableStateListOf<Dice>()

    fun reroll() {
        viewModelScope.launch {
            rolling = true
            (0 until hand.size).map { i ->
                async(Dispatchers.Unconfined) {
                    if (hand[i] !in hold) {
                        hand[i].roll()
                    }
                }
            }.awaitAll()
            rolling = false
            state = when (state) {
                YahtzeeState.RollOne -> YahtzeeState.RollTwo
                YahtzeeState.RollTwo -> YahtzeeState.RollThree
                YahtzeeState.RollThree -> YahtzeeState.Stop
                YahtzeeState.Stop -> YahtzeeState.RollOne
            }
        }
    }

    fun placeOnes() {
        if (hand.all { it.value == 0 } && IS_NOT_DEBUG) return
        scores.getSmall(hand, HandType.Ones)
        reset()
    }

    fun placeTwos() {
        if (hand.all { it.value == 0 } && IS_NOT_DEBUG) return
        scores.getSmall(hand, HandType.Twos)
        reset()
    }

    fun placeThrees() {
        if (hand.all { it.value == 0 } && IS_NOT_DEBUG) return
        scores.getSmall(hand, HandType.Threes)
        reset()
    }

    fun placeFours() {
        if (hand.all { it.value == 0 } && IS_NOT_DEBUG) return
        scores.getSmall(hand, HandType.Fours)
        reset()
    }

    fun placeFives() {
        if (hand.all { it.value == 0 } && IS_NOT_DEBUG) return
        scores.getSmall(hand, HandType.Fives)
        reset()
    }

    fun placeSixes() {
        if (hand.all { it.value == 0 } && IS_NOT_DEBUG) return
        scores.getSmall(hand, HandType.Sixes)
        reset()
    }

    fun placeThreeOfKind() {
        if (hand.all { it.value == 0 } && IS_NOT_DEBUG) return
        scores.getThreeOfAKind(hand)
        reset()
    }

    fun placeFourOfKind() {
        if (hand.all { it.value == 0 } && IS_NOT_DEBUG) return
        scores.getFourOfAKind(hand)
        reset()
    }

    fun placeFullHouse() {
        if (hand.all { it.value == 0 } && IS_NOT_DEBUG) return
        scores.getFullHouse(hand)
        reset()
    }

    fun placeSmallStraight() {
        if (hand.all { it.value == 0 } && IS_NOT_DEBUG) return
        scores.getSmallStraight(hand)
        reset()
    }

    fun placeLargeStraight() {
        if (hand.all { it.value == 0 } && IS_NOT_DEBUG) return
        scores.getLargeStraight(hand)
        reset()
    }

    fun placeYahtzee() {
        if (hand.all { it.value == 0 } && IS_NOT_DEBUG) return
        scores.getYahtzee(hand)
        reset()
    }

    fun placeChance() {
        if (hand.all { it.value == 0 } && IS_NOT_DEBUG) return
        scores.getChance(hand)
        reset()
    }

    private fun reset() {
        hold.clear()
        hand.forEach { it.value = 0 }
        state = YahtzeeState.RollOne
    }

    fun resetGame() {
        reset()
        scores.resetScores()
        showGameOverDialog = true
    }
}