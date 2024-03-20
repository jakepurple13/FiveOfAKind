package com.programmersbox.common

import io.realm.kotlin.MutableRealm
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.asFlow
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Ignore
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

private const val HIGHSCORE_LIMIT = 15

internal class YahtzeeDatabase(name: String = Realm.DEFAULT_FILE_NAME) {
    private val realm by lazy {
        Realm.open(
            RealmConfiguration.Builder(
                setOf(
                    YahtzeeHighScores::class,
                    YahtzeeScoreItem::class
                )
            )
                .schemaVersion(24)
                .name(name)
                .migration({ })
                //.deleteRealmIfMigrationNeeded()
                .build()
        )
    }

    private val yahtzeeHighScores: YahtzeeHighScores = realm.initDbBlocking { YahtzeeHighScores() }

    suspend fun addHighScore(scoreItem: YahtzeeScoreItem) {
        realm.updateInfo<YahtzeeHighScores> {
            it?.highScoresList?.add(scoreItem)
            val sorted = it?.highScoresList?.sortedByDescending { it.totalScore } ?: return@updateInfo
            if (sorted.size >= HIGHSCORE_LIMIT) {
                val expired = sorted.chunked(HIGHSCORE_LIMIT)
                    .drop(1)
                    .flatten()
                it?.highScoresList?.removeAll(expired)
            }
        }
    }

    suspend fun removeHighScore(scoreItem: YahtzeeScoreItem) {
        realm.updateInfo<YahtzeeHighScores> {
            it?.highScoresList?.remove(scoreItem)
        }
    }

    fun getYahtzeeHighScores() = yahtzeeHighScores
        .asFlow()
        .mapNotNull { it.obj }
        .mapNotNull { it.highScoresList.sortedByDescending { it.totalScore } }
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
    var highScoresList = realmListOf<YahtzeeScoreItem>()
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

    @Ignore
    val smallScore get() = ones + twos + threes + fours + fives + sixes

    @Ignore
    val largeScore get() = threeKind + fourKind + fullHouse + smallStraight + largeStraight + yahtzee + chance

    @Ignore
    val totalScore get() = largeScore + smallScore + if (smallScore >= 63) 35 else 0
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