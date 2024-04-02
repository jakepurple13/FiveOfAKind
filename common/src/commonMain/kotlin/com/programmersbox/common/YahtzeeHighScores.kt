package com.programmersbox.common

import io.realm.kotlin.MutableRealm
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.asFlow
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.Sort
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal const val HIGHSCORE_LIMIT = 15

internal class YahtzeeDatabase(name: String = Realm.DEFAULT_FILE_NAME) {
    private val realm by lazy {
        Realm.open(
            RealmConfiguration.Builder(
                setOf(
                    YahtzeeHighScores::class,
                    YahtzeeScoreItem::class,
                    YahtzeeScoreStat::class
                )
            )
                .schemaVersion(27)
                .name(name)
                .migration({ })
                //.deleteRealmIfMigrationNeeded()
                .build()
        )
    }

    private val yahtzeeHighScores: YahtzeeHighScores = realm.initDbBlocking { YahtzeeHighScores() }

    suspend fun addHighScore(scoreItem: YahtzeeScoreItem) {
        scoreItem.setScores()
        realm.write {
            copyToRealm(scoreItem)

            val scores = query<YahtzeeScoreItem>()
                .sort("totalScore", Sort.DESCENDING)
                .find()

            if (scores.size > HIGHSCORE_LIMIT) {
                scores.chunked(HIGHSCORE_LIMIT)
                    .drop(1)
                    .flatten()
                    .mapNotNull { findLatest(it) }
                    .forEach { delete(it) }
            }

            query<YahtzeeHighScores>()
                .first()
                .find()
                ?.let { updateScoreStats(it, scoreItem) }
        }
    }

    private fun updateScoreStats(yahtzeeHighScores: YahtzeeHighScores, scoreItem: YahtzeeScoreItem) {
        val scoreToStatMap = mapOf(
            scoreItem::ones to yahtzeeHighScores::ones,
            scoreItem::twos to yahtzeeHighScores::twos,
            scoreItem::threes to yahtzeeHighScores::threes,
            scoreItem::fours to yahtzeeHighScores::fours,
            scoreItem::fives to yahtzeeHighScores::fives,
            scoreItem::sixes to yahtzeeHighScores::sixes,
            scoreItem::threeKind to yahtzeeHighScores::threeKind,
            scoreItem::fourKind to yahtzeeHighScores::fourKind,
            scoreItem::fullHouse to yahtzeeHighScores::fullHouse,
            scoreItem::smallStraight to yahtzeeHighScores::smallStraight,
            scoreItem::largeStraight to yahtzeeHighScores::largeStraight,
            scoreItem::chance to yahtzeeHighScores::chance,
        )

        for ((scoreProperty, statProperty) in scoreToStatMap) {
            statProperty.get()?.let { updateStatIfNonZero(scoreProperty.get(), it) }
        }

        if (yahtzeeHighScores.yahtzee != null) {
            if (scoreItem.yahtzee != 0) {
                val yahtzeeScore = (scoreItem.yahtzee - 50) / 100
                yahtzeeHighScores.yahtzee!!.numberOfTimes++
                repeat(yahtzeeScore) {
                    yahtzeeHighScores.yahtzee!!.numberOfTimes++
                }
                yahtzeeHighScores.yahtzee!!.totalPoints += scoreItem.yahtzee
            }
        }
    }

    private fun updateStatIfNonZero(score: Int, stat: YahtzeeScoreStat) {
        if (score != 0) {
            stat.numberOfTimes++
            stat.totalPoints += score
        }
    }

    suspend fun removeHighScore(scoreItem: YahtzeeScoreItem) {
        realm.write { findLatest(scoreItem)?.let { delete(it) } }
    }

    fun getYahtzeeHighScores() = realm.query<YahtzeeScoreItem>()
        .sort("totalScore", Sort.DESCENDING)
        .asFlow()
        .mapNotNull { it.list }

    fun getYahtzeeStats() = yahtzeeHighScores
        .asFlow()
        .mapNotNull { it.obj }

    suspend fun updateScores() {
        realm.write {
            realm.query<YahtzeeScoreItem>()
                .sort("totalScore", Sort.DESCENDING)
                .find()
                .forEach { it.setScores() }
        }
    }
}

private suspend inline fun <reified T : RealmObject> Realm.updateInfo(crossinline block: MutableRealm.(T?) -> Unit) {
    query(T::class).first().find()?.also { info ->
        write { block(findLatest(info)) }
    }
}

private inline fun <reified T : RealmObject> Realm.initDbBlocking(crossinline default: () -> T): T {
    val f = query(T::class).first().find()
    return f ?: writeBlocking { copyToRealm(default()) }
}

internal class YahtzeeHighScores : RealmObject {
    var ones: YahtzeeScoreStat? = YahtzeeScoreStat()
    var twos: YahtzeeScoreStat? = YahtzeeScoreStat()
    var threes: YahtzeeScoreStat? = YahtzeeScoreStat()
    var fours: YahtzeeScoreStat? = YahtzeeScoreStat()
    var fives: YahtzeeScoreStat? = YahtzeeScoreStat()
    var sixes: YahtzeeScoreStat? = YahtzeeScoreStat()

    var threeKind: YahtzeeScoreStat? = YahtzeeScoreStat()
    var fourKind: YahtzeeScoreStat? = YahtzeeScoreStat()
    var fullHouse: YahtzeeScoreStat? = YahtzeeScoreStat()
    var smallStraight: YahtzeeScoreStat? = YahtzeeScoreStat()
    var largeStraight: YahtzeeScoreStat? = YahtzeeScoreStat()
    var yahtzee: YahtzeeScoreStat? = YahtzeeScoreStat()
    var chance: YahtzeeScoreStat? = YahtzeeScoreStat()
}

internal class YahtzeeScoreStat : RealmObject {
    var numberOfTimes = 0
    var totalPoints = 0L
}

internal class YahtzeeScoreItem : RealmObject {
    @PrimaryKey
    var time: Long = Clock.System.now().toEpochMilliseconds()
    var ones: Int = 0
    var twos: Int = 0
    var threes: Int = 0
    var fours: Int = 0
    var fives: Int = 0
    var sixes: Int = 0
    var threeKind: Int = 0
    var fourKind: Int = 0
    var fullHouse: Int = 0
    var smallStraight: Int = 0
    var largeStraight: Int = 0
    var yahtzee: Int = 0
    var chance: Int = 0
    var smallScore = 0
    var largeScore = 0
    var totalScore = 0

    fun setScores() {
        smallScore = ones + twos + threes + fours + fives + sixes
        largeScore = threeKind + fourKind + fullHouse + smallStraight + largeStraight + yahtzee + chance
        totalScore = largeScore + smallScore + if (smallScore >= 63) 35 else 0
    }
}

fun RealmInstant.toInstant(): Instant {
    val sec: Long = this.epochSeconds
    // The value always lies in the range `-999_999_999..999_999_999`.
    // minus for timestamps before epoch, positive for after
    val nano: Int = this.nanosecondsOfSecond
    return if (sec >= 0) { // For positive timestamps, conversion can happen directly
        Instant.fromEpochSeconds(sec, nano.toLong())
    } else {
        // For negative timestamps, RealmInstant starts from the higher value with negative
        // nanoseconds, while Instant starts from the lower value with positive nanoseconds
        // TODO This probably breaks at edge cases like MIN/MAX
        Instant.fromEpochSeconds(sec - 1, 1_000_000 + nano.toLong())
    }
}

fun Instant.toRealmInstant(): RealmInstant {
    val sec: Long = this.epochSeconds
    // The value is always positive and lies in the range `0..999_999_999`.
    val nano: Int = this.nanosecondsOfSecond
    return if (sec >= 0) { // For positive timestamps, conversion can happen directly
        RealmInstant.from(sec, nano)
    } else {
        // For negative timestamps, RealmInstant starts from the higher value with negative
        // nanoseconds, while Instant starts from the lower value with positive nanoseconds
        // TODO This probably breaks at edge cases like MIN/MAX
        RealmInstant.from(sec + 1, -1_000_000 + nano)
    }
}