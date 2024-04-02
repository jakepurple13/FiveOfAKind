package com.programmersbox.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import moe.tlaster.precompose.flow.collectAsStateWithLifecycle
import moe.tlaster.precompose.navigation.BackHandler
import moe.tlaster.precompose.viewmodel.viewModel

internal typealias ScoreClick = () -> Unit

internal val Emerald = Color(0xFF2ecc71)
internal val Sunflower = Color(0xFFf1c40f)
internal val Alizarin = Color(0xFFe74c3c)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun YahtzeeScreen(
    vm: YahtzeeViewModel = viewModel(YahtzeeViewModel::class) { YahtzeeViewModel() },
    yahtzeeDatabase: YahtzeeDatabase = remember { YahtzeeDatabase() },
    settings: Settings,
) {
    val diceLook by settings.showDotsOnDice.flow.collectAsStateWithLifecycle(true)
    val isUsing24HourTime by settings.use24HourTime.flow.collectAsStateWithLifecycle(true)
    val scope = rememberCoroutineScope()

    val highScores by yahtzeeDatabase
        .getYahtzeeHighScores()
        .collectAsStateWithLifecycle(emptyList())

    val stats by yahtzeeDatabase
        .getYahtzeeStats()
        .collectAsStateWithLifecycle(YahtzeeHighScores())

    var newGameDialog by remember { mutableStateOf(false) }

    if (newGameDialog) {
        AlertDialog(
            onDismissRequest = { newGameDialog = false },
            title = { Text("Want to start a new game?") },
            text = { Text("You have ${vm.scores.totalScore} points. Are you sure?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.resetGame()
                        newGameDialog = false
                    }
                ) { Text("Yes") }
            },
            dismissButton = { TextButton(onClick = { newGameDialog = false }) { Text("No") } }
        )
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    BackHandler(drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    var statsDialog by remember { mutableStateOf(false) }

    if (statsDialog) {
        ModalBottomSheet(
            onDismissRequest = { statsDialog = false }
        ) { BottomSheetContent(stats) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("High Scores") },
                            actions = { Text(highScores.size.toString()) },
                            navigationIcon = {
                                TextButton(
                                    onClick = { statsDialog = true }
                                ) { Text("Stats") }
                            }
                        )
                    }
                ) { p ->
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = p
                    ) {
                        items(highScores) {
                            HighScoreItem(
                                item = it,
                                scaffoldState = drawerState,
                                isUsing24HourTime = isUsing24HourTime,
                                onDelete = { scope.launch { yahtzeeDatabase.removeHighScore(it) } },
                                modifier = Modifier.animateItemPlacement()
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Yahtzee") },
                    actions = {
                        /*TextButton(
                            onClick = {
                                HandType.entries.forEach {
                                    vm.scores.scoreList[it] = 10
                                }
                            }
                        ) { Text("Finish") }*/
                        TextButton(onClick = { newGameDialog = true }) { Text("New Game") }
                        TextButton(
                            onClick = { scope.launch { settings.use24HourTime.update(!isUsing24HourTime) } },
                        ) {
                            Crossfade(isUsing24HourTime) { target ->
                                Text(if (target) "24H" else "12H")
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Dice(1, "")
                            .ShowDice(diceLook, Modifier.size(40.dp)) {
                                scope.launch { settings.showDotsOnDice.update(!diceLook) }
                            }
                    }
                )
            },
            bottomBar = { BottomBarDiceRow(vm, diceLook) },
        ) { p ->
            Column(
                modifier = Modifier.padding(p),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    SmallScores(
                        smallScore = vm.scores.smallScore,
                        hand = vm.hand,
                        hasBonus = vm.scores.hasBonus,
                        isRolling = vm.rolling,
                        containsCheck = { vm.scores.scoreList.containsKey(it) },
                        scoreGet = { vm.scores.scoreList.getOrElse(it) { 0 } },
                        onOnesClick = vm::placeOnes,
                        onTwosClick = vm::placeTwos,
                        onThreesClick = vm::placeThrees,
                        onFoursClick = vm::placeFours,
                        onFivesClick = vm::placeFives,
                        onSixesClick = vm::placeSixes,
                        modifier = Modifier.weight(1f)
                    )
                    LargeScores(
                        largeScore = vm.scores.largeScore,
                        isRolling = vm.rolling,
                        hand = vm.hand,
                        isNotRollOneState = vm.state != YahtzeeState.RollOne,
                        containsCheck = { vm.scores.scoreList.containsKey(it) },
                        scoreGet = { vm.scores.scoreList.getOrElse(it) { 0 } },
                        canGetHand = {
                            when (it) {
                                HandType.ThreeOfAKind -> vm.scores.canGetThreeKind(vm.hand)
                                HandType.FourOfAKind -> vm.scores.canGetFourKind(vm.hand)
                                HandType.FullHouse -> vm.scores.canGetFullHouse(vm.hand)
                                HandType.SmallStraight -> vm.scores.canGetSmallStraight(vm.hand)
                                HandType.LargeStraight -> vm.scores.canGetLargeStraight(vm.hand)
                                HandType.Yahtzee -> vm.scores.canGetYahtzee(vm.hand)
                                else -> false
                            }
                        },
                        onThreeKindClick = vm::placeThreeOfKind,
                        onFourKindClick = vm::placeFourOfKind,
                        onFullHouseClick = vm::placeFullHouse,
                        onSmallStraightClick = vm::placeSmallStraight,
                        onLargeStraightClick = vm::placeLargeStraight,
                        onYahtzeeClick = vm::placeYahtzee,
                        onChanceClick = vm::placeChance,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Total Score: ${animateIntAsState(vm.scores.totalScore).value}")
            }
        }
    }

    if (vm.scores.isGameOver && vm.showGameOverDialog) {
        var showExplosion by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            val showExplosionCheck = if (HIGHSCORE_LIMIT > highScores.size) {
                true
            } else {
                vm.scores.totalScore >= (highScores.lastOrNull()?.totalScore ?: 0)
            }
            if (showExplosionCheck) {
                showExplosion = true
            }
            yahtzeeDatabase.addHighScore(
                YahtzeeScoreItem().apply {
                    ones = vm.scores.scoreList.getOrElse(HandType.Ones) { 0 }
                    twos = vm.scores.scoreList.getOrElse(HandType.Twos) { 0 }
                    threes = vm.scores.scoreList.getOrElse(HandType.Threes) { 0 }
                    fours = vm.scores.scoreList.getOrElse(HandType.Fours) { 0 }
                    fives = vm.scores.scoreList.getOrElse(HandType.Fives) { 0 }
                    sixes = vm.scores.scoreList.getOrElse(HandType.Sixes) { 0 }
                    threeKind = vm.scores.scoreList.getOrElse(HandType.ThreeOfAKind) { 0 }
                    fourKind = vm.scores.scoreList.getOrElse(HandType.FourOfAKind) { 0 }
                    fullHouse = vm.scores.scoreList.getOrElse(HandType.FullHouse) { 0 }
                    smallStraight = vm.scores.scoreList.getOrElse(HandType.SmallStraight) { 0 }
                    largeStraight = vm.scores.scoreList.getOrElse(HandType.LargeStraight) { 0 }
                    yahtzee = vm.scores.scoreList.getOrElse(HandType.Yahtzee) { 0 }
                    chance = vm.scores.scoreList.getOrElse(HandType.Chance) { 0 }
                }
            )
        }

        if (showExplosion) {
            Explosion()
            FountainConfetti()
        }

        AlertDialog(
            onDismissRequest = { vm.showGameOverDialog = false },
            title = { Text("Game Over") },
            text = { Text("You got a score of ${vm.scores.totalScore}") },
            confirmButton = { TextButton(onClick = vm::resetGame) { Text("Play Again") } },
            dismissButton = {
                TextButton(
                    onClick = {
                        vm.showGameOverDialog = false
                    }
                ) { Text("Stop Playing") }
            }
        )
    }

    var showParticles by remember(vm.scores.scoreList[HandType.Yahtzee]) {
        mutableStateOf(vm.scores.scoreList[HandType.Yahtzee] != null)
    }

    Crossfade(showParticles) { target ->
        if (target) {
            LaunchedEffect(Unit) {
                delay(3000)
                showParticles = false
            }
            FountainConfetti()
        }
    }
}

@Composable
internal fun FountainConfetti() {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        CreateParticles(
            modifier = Modifier.matchParentSize(),
            x = with(LocalDensity.current) { maxWidth.toPx() / 2 },
            y = with(LocalDensity.current) { maxHeight.toPx() },
            velocity = Velocity(xDirection = 1f, yDirection = -15f, angle = PI, randomize = true),
            force = Force.Gravity(0.2f),
            acceleration = Acceleration(0f, -4f),
            particleSize = ParticleSize.RandomSizes(10..30),
            particleColor = ParticleColor.RandomColors(
                listOf(
                    Color.Yellow,
                    Color.Blue,
                    Color.Red,
                    Color.White,
                    Color.Magenta,
                    Color.Green
                )
            ),
            lifeTime = LifeTime(255f, 1f),
            emissionType = EmissionType.FlowEmission(maxParticlesCount = 300, emissionRate = 0.5f),
            durationMillis = 3 * 1000
        )
    }
}

@Composable
internal fun Explosion() {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        CreateParticles(
            modifier = Modifier.matchParentSize(),
            x = with(LocalDensity.current) { maxWidth.toPx() / 2 },
            y = -50f,
            velocity = Velocity(xDirection = 1f, yDirection = 1f, randomize = true),
            force = Force.Gravity(0.01f),
            acceleration = Acceleration(),
            particleSize = ParticleSize.RandomSizes(10..30),
            particleColor = ParticleColor.RandomColors(
                listOf(
                    Color.Yellow,
                    Color.Blue,
                    Color.Red,
                    Color.White,
                    Color.Magenta,
                    Color.Green
                )
            ),
            lifeTime = LifeTime(255f, 0.01f),
            emissionType = EmissionType.FlowEmission(maxParticlesCount = 300, emissionRate = 0.5f),
            durationMillis = 10 * 1000
        )
    }
}

@Composable
internal fun BottomBarDiceRow(vm: YahtzeeViewModel, diceLooks: Boolean) {
    BottomAppBar {
        vm.hand.forEach { dice ->
            dice(
                useDots = diceLooks,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .weight(1f)
                    .aspectRatio(1f, true)
                    .border(
                        width = animateDpAsState(targetValue = if (dice in vm.hold) 4.dp else 0.dp).value,
                        color = animateColorAsState(targetValue = if (dice in vm.hold) Emerald else Color.Transparent).value,
                        shape = RoundedCornerShape(7.dp)
                    )
            ) { if (dice in vm.hold) vm.hold.remove(dice) else vm.hold.add(dice) }
        }

        IconButton(
            onClick = vm::reroll,
            modifier = Modifier.weight(1f),
            enabled = vm.state != YahtzeeState.Stop
        ) {
            Icon(
                Icons.Default.PlayArrow,
                null,
                tint = animateColorAsState(
                    when (vm.state) {
                        YahtzeeState.RollOne -> Emerald
                        YahtzeeState.RollTwo -> Sunflower
                        YahtzeeState.RollThree -> Alizarin
                        YahtzeeState.Stop -> LocalContentColor.current
                    }
                ).value
            )
        }
    }
}

@Composable
internal fun SmallScores(
    smallScore: Int,
    hand: List<Dice>,
    hasBonus: Boolean,
    isRolling: Boolean,
    containsCheck: (HandType) -> Boolean,
    scoreGet: (HandType) -> Int,
    onOnesClick: ScoreClick,
    onTwosClick: ScoreClick,
    onThreesClick: ScoreClick,
    onFoursClick: ScoreClick,
    onFivesClick: ScoreClick,
    onSixesClick: ScoreClick,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        val groupedCheck by remember {
            derivedStateOf {
                hand.groupingBy { it.value }
                    .eachCount()
                    .toList()
                    .sortedWith(compareBy({ it.second }, { it.first }))
                    .reversed()
                    .map { it.first }
            }
        }

        val highest = groupedCheck.elementAtOrNull(0)
        val medium = groupedCheck.elementAtOrNull(1)
        val lowest = groupedCheck.elementAtOrNull(2)

        fun canScore(value: Int) = highest == value || medium == value || lowest == value
        fun scoreColor(value: Int) = when {
            highest == value -> Emerald
            medium == value -> Sunflower
            lowest == value -> Alizarin
            else -> Color.Transparent
        }

        ScoreButton(
            category = "Ones",
            enabled = !containsCheck(HandType.Ones),
            score = scoreGet(HandType.Ones),
            canScore = canScore(1) && !isRolling,
            customBorderColor = scoreColor(1),
            onClick = onOnesClick
        )

        ScoreButton(
            category = "Twos",
            enabled = !containsCheck(HandType.Twos),
            score = scoreGet(HandType.Twos),
            canScore = canScore(2) && !isRolling,
            customBorderColor = scoreColor(2),
            onClick = onTwosClick
        )

        ScoreButton(
            category = "Threes",
            enabled = !containsCheck(HandType.Threes),
            score = scoreGet(HandType.Threes),
            canScore = canScore(3) && !isRolling,
            customBorderColor = scoreColor(3),
            onClick = onThreesClick
        )

        ScoreButton(
            category = "Fours",
            enabled = !containsCheck(HandType.Fours),
            score = scoreGet(HandType.Fours),
            canScore = canScore(4) && !isRolling,
            customBorderColor = scoreColor(4),
            onClick = onFoursClick
        )

        ScoreButton(
            category = "Fives",
            enabled = !containsCheck(HandType.Fives),
            score = scoreGet(HandType.Fives),
            canScore = canScore(5) && !isRolling,
            customBorderColor = scoreColor(5),
            onClick = onFivesClick
        )

        ScoreButton(
            category = "Sixes",
            enabled = !containsCheck(HandType.Sixes),
            score = scoreGet(HandType.Sixes),
            canScore = canScore(6) && !isRolling,
            customBorderColor = scoreColor(6),
            onClick = onSixesClick
        )

        AnimatedVisibility(hasBonus) {
            Text("+35 for >= 63")
        }

        if (smallScore >= 63) {
            val score by animateIntAsState(targetValue = smallScore)
            Text("Small Score: ${score + 35} ($score)")
        } else {
            Text("Small Score: ${animateIntAsState(smallScore).value}")
        }
    }
}

@Composable
internal fun LargeScores(
    largeScore: Int,
    isRolling: Boolean,
    hand: List<Dice>,
    isNotRollOneState: Boolean,
    containsCheck: (HandType) -> Boolean,
    scoreGet: (HandType) -> Int,
    canGetHand: (HandType) -> Boolean,
    onThreeKindClick: ScoreClick,
    onFourKindClick: ScoreClick,
    onFullHouseClick: ScoreClick,
    onSmallStraightClick: ScoreClick,
    onLargeStraightClick: ScoreClick,
    onYahtzeeClick: ScoreClick,
    onChanceClick: ScoreClick,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        ScoreButton(
            category = "Three of a Kind",
            enabled = !containsCheck(HandType.ThreeOfAKind),
            score = scoreGet(HandType.ThreeOfAKind),
            canScore = canGetHand(HandType.ThreeOfAKind) && isNotRollOneState && !isRolling,
            onClick = onThreeKindClick
        )

        ScoreButton(
            category = "Four of a Kind",
            enabled = !containsCheck(HandType.FourOfAKind),
            score = scoreGet(HandType.FourOfAKind),
            canScore = canGetHand(HandType.FourOfAKind) && isNotRollOneState && !isRolling,
            onClick = onFourKindClick
        )

        ScoreButton(
            category = "Full House",
            enabled = !containsCheck(HandType.FullHouse),
            score = scoreGet(HandType.FullHouse),
            canScore = canGetHand(HandType.FullHouse) && isNotRollOneState && !isRolling,
            onClick = onFullHouseClick
        )

        ScoreButton(
            category = "Small Straight",
            enabled = !containsCheck(HandType.SmallStraight),
            score = scoreGet(HandType.SmallStraight),
            canScore = canGetHand(HandType.SmallStraight) && isNotRollOneState && !isRolling,
            onClick = onSmallStraightClick
        )

        ScoreButton(
            category = "Large Straight",
            enabled = !containsCheck(HandType.LargeStraight),
            score = scoreGet(HandType.LargeStraight),
            canScore = canGetHand(HandType.LargeStraight) && isNotRollOneState && !isRolling,
            onClick = onLargeStraightClick
        )

        ScoreButton(
            category = "Yahtzee",
            enabled = !containsCheck(HandType.Yahtzee) ||
                    canGetHand(HandType.Yahtzee) &&
                    hand.none { it.value == 0 },
            score = scoreGet(HandType.Yahtzee),
            canScore = canGetHand(HandType.Yahtzee) && isNotRollOneState && !isRolling,
            onClick = onYahtzeeClick
        )

        ScoreButton(
            category = "Chance",
            enabled = !containsCheck(HandType.Chance),
            score = scoreGet(HandType.Chance),
            onClick = onChanceClick
        )

        Text("Large Score: ${animateIntAsState(largeScore).value}")
    }
}

@Composable
internal fun ScoreButton(
    category: String,
    enabled: Boolean,
    canScore: Boolean = false,
    customBorderColor: Color = Emerald,
    score: Int,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(
            width = ButtonDefaults.outlinedButtonBorder.width,
            color = animateColorAsState(
                when {
                    canScore && enabled -> customBorderColor
                    enabled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                }
            ).value
        )
    ) { Text("$category: ${animateIntAsState(score).value}") }
}

@Composable
private fun HighScoreItem(
    item: YahtzeeScoreItem,
    scaffoldState: DrawerState,
    onDelete: () -> Unit,
    isUsing24HourTime: Boolean,
    modifier: Modifier = Modifier,
) {
    var deleteDialog by remember { mutableStateOf(false) }

    val time = remember(isUsing24HourTime) {
        val d = Instant.fromEpochMilliseconds(item.time).toLocalDateTime(TimeZone.currentSystemDefault())
        d.format(
            LocalDateTime.Format {
                monthName(MonthNames.ENGLISH_FULL)
                char(' ')
                dayOfMonth()
                char(' ')
                year()
                chars(", ")
                if (isUsing24HourTime) {
                    hour()
                    char(':')
                    minute()
                } else {
                    amPmHour()
                    char(':')
                    minute()
                    char(' ')
                    amPmMarker("AM", "PM")
                }
            }
        )
    }

    val smallScore = item.smallScore
    val largeScore = item.largeScore
    val totalScore = item.totalScore

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("Delete $totalScore at $time") },
            text = { Text("Are you sure?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        deleteDialog = false
                    }
                ) { Text("Yes") }
            },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("No") } }
        )
    }

    var showMore by remember(scaffoldState.targetValue) { mutableStateOf(false) }

    OutlinedCard(
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.background),
        modifier = modifier
    ) {
        ListItem(
            leadingContent = { IconButton(onClick = { deleteDialog = true }) { Icon(Icons.Default.Close, null) } },
            headlineContent = { Text("Score: $totalScore") },
            overlineContent = { Text("Time: $time") },
            trailingContent = {
                IconButton(
                    onClick = { showMore = !showMore },
                    modifier = Modifier.rotate(animateFloatAsState(targetValue = if (showMore) 180f else 0f).value)
                ) { Icon(Icons.Default.ArrowDropDown, null) }
            },
        )
        AnimatedVisibility(visible = showMore) {
            HorizontalDivider()
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("Ones: ${item.ones}")
                    Text("Twos: ${item.twos}")
                    Text("Threes: ${item.threes}")
                    Text("Fours: ${item.fours}")
                    Text("Fives: ${item.fives}")
                    Text("Sixes: ${item.sixes}")
                    if (smallScore >= 63) {
                        Text("+35 for >= 63")
                    }
                    val originalScore = if (smallScore >= 63) " ($smallScore)" else ""
                    Text("Small Score: ${if (smallScore >= 63) smallScore + 35 else smallScore}$originalScore")
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("Three of a Kind: ${item.threeKind}")
                    Text("Four of a Kind: ${item.fourKind}")
                    Text("Full House: ${item.fullHouse}")
                    Text("Small Straight: ${item.smallStraight}")
                    Text("Large Straight: ${item.largeStraight}")
                    Text("Yahtzee: ${item.yahtzee}")
                    Text("Chance: ${item.chance}")
                    Text("Large Score: $largeScore")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetContent(
    scores: YahtzeeHighScores,
) {
    TopAppBar(
        title = { Text("Stats") }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item { StatRow("Ones", scores.ones) }
        item { StatRow("Twos", scores.twos) }
        item { StatRow("Threes", scores.threes) }
        item { StatRow("Fours", scores.fours) }
        item { StatRow("Fives", scores.fives) }
        item { StatRow("Sixes", scores.sixes) }

        item { StatRow("Three of a Kind", scores.threeKind) }
        item { StatRow("Four of a Kind", scores.fourKind) }
        item { StatRow("Full House", scores.fullHouse) }
        item { StatRow("Small Straight", scores.smallStraight) }
        item { StatRow("Large Straight", scores.largeStraight) }
        item { StatRow("Yahtzee", scores.yahtzee) }
        item { StatRow("Chance", scores.chance) }
    }
}

@Composable
private fun StatRow(type: String, stat: YahtzeeScoreStat?) {
    ListItem(
        headlineContent = { Text(type) },
        supportingContent = {
            Column {
                Text("Times Counted: ${stat?.numberOfTimes}")
                Text("Total Points: ${stat?.totalPoints}")
            }
        }
    )
}