package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move
import com.softwire.dynamite.game.Round
import java.io.File
import kotlin.math.pow
import kotlin.math.floor
import kotlin.math.max

class MyBot : Bot {

    var roundTable = Array(25) {Array(5) {0} }

    override fun makeMove(gamestate: Gamestate): Move {
        // Are you debugging?
        // Put a breakpoint in this method to see when we make a move

        // W and D first 2 to confuse opponent lol
        val numRounds = gamestate.rounds.size
        if (numRounds == 0) return Move.W
        else if (numRounds == 1) return Move.D

        // Update roundTable
        updateRoundTable(gamestate.rounds[numRounds - 1], gamestate.rounds[numRounds - 2])

        // Decide whether to follow info from roundTable
        val prevRound = gamestate.rounds.last()
        val prevRoundIndex = roundToTableIndex(prevRound)
        val moveDistribution = roundTable[prevRoundIndex]

        val sampleSize = moveDistribution.sum()
        if (sampleSize >= 10) {
            // If >50% D, then probably D after draw
            if (moveDistribution[4] >= sampleSize / 2) {
                // Counter with W
                return Move.W
            }
            // See if a particular R/P/S is more frequent
            val rpsDistribution = moveDistribution.slice(0..2)
            val rpsSampleSize = rpsDistribution.sum()
            if (rpsSampleSize >= 10) {
                var rpsDistributionScaled = rpsDistribution.map{ it.toDouble() / rpsDistribution.sum() }
//                print(rpsDistributionScaled)
//                rpsDistributionScaled = rpsDistributionScaled.map{ (it).pow(4) }
//                rpsDistributionScaled = rpsDistributionScaled.map{ it / rpsDistributionScaled.sum() }
//                print(" scaled: ")
//                println(rpsDistributionScaled)
                if (rpsDistributionScaled.max()!! > 0.37) {
                    return moveToWin(
                        intToMove(rpsDistributionScaled.indices.maxBy { rpsDistributionScaled[it] }!!))
                }
                if (rpsDistributionScaled.min()!! < 0.29) {
                    return moveToLose(
                        intToMove(rpsDistributionScaled.indices.minBy { rpsDistributionScaled[it] }!!))
                }
            }
        }

        // Decide to throw water if dynamite draws many times
        val numDynamiteDraws = calculateDynamiteDraws(gamestate)
        val waterProbability = numDynamiteDraws.toDouble().pow(2)  / 100.0
        val waterRNG = Math.random()
        if (waterRNG < waterProbability) {
            return Move.W
        }

        // Decide if throw dynamite
        val dynamitesPlayed = numberOfDynamitesPlayed(gamestate)
        val (scoreP1, scoreP2) = calculateScores(gamestate)
        val movesUntilEnd = 1000 - max(scoreP1, scoreP2)

        val roundScore = calculateRoundScore(gamestate)
        val roundScoreMultiplier = calculateRoundScoreMultiplier(roundScore)

        var dynamiteProbability = (100.0 - dynamitesPlayed) / movesUntilEnd * roundScoreMultiplier
        // Make sure to use all dynamites
        if (100 - dynamitesPlayed == movesUntilEnd) {
            dynamiteProbability = 1.0
        }
//        println("$roundScoreMultiplier $dynamiteProbability")

        val dynamiteRNG = Math.random()
//        println("$dynamiteRNG $dynamiteProbability")
        if (dynamiteRNG < dynamiteProbability) {
            return Move.D
        }

        // Play random move
        val randomNumberBetween0And3 =
            floor(Math.random() * 3.0).toInt()
        val possibleMoves = arrayOf(Move.R, Move.P, Move.S)
        val randomMove = possibleMoves[randomNumberBetween0And3]

        return randomMove
    }

    private fun calculateDynamiteDraws(gamestate: Gamestate): Int {
        var num = 0
        for (round in gamestate.rounds.reversed()) {
            if (round.p1 == Move.D && round.p2 == Move.D) {
                num++
            } else break
        }
        return num
    }

    private fun intToMove(index: Int): Move {
        return when (index) {
            0 -> Move.R
            1 -> Move.P
            2 -> Move.S
            3 -> Move.W
            4 -> Move.D
            else -> Move.D
        }
    }
    private fun moveToInt(move: Move): Int {
        return when (move) {
            Move.R -> 0
            Move.P -> 1
            Move.S -> 2
            Move.W -> 3
            Move.D -> 4
        }
    }

    private fun moveToWin(move: Move): Move {
        return when(move) {
            Move.R -> Move.P
            Move.P -> Move.S
            Move.S -> Move.R
            else -> Move.D
        }
    }

    private fun moveToLose(move: Move): Move {
        return when(move) {
            Move.R -> Move.S
            Move.P -> Move.R
            Move.S -> Move.P
            else -> Move.W
        }
    }

    private fun roundToTableIndex(round: Round): Int {
        return round.p1.ordinal * 5 + round.p2.ordinal
    }

    private fun updateRoundTable(current: Round, previous: Round) {
        val index = roundToTableIndex(previous)
        roundTable[index][current.p2.ordinal]++
    }

    private fun calculateRoundScore(gamestate: Gamestate): Int {
        var roundScore = 1
        for (round in gamestate.rounds.reversed()) {
            if (calculateWinner(round.p1, round.p2) == 0) {
                roundScore++
            } else break
        }
//        if (gamestate.rounds.isNotEmpty())
//            println("${gamestate.rounds.last().p1} ${gamestate.rounds.last().p2}")
        return roundScore
    }

    private fun calculateRoundScoreMultiplier(roundScore: Int): Double {
        return 4.0.pow(roundScore - 1) - 0.9
    }

    private fun numberOfDynamitesPlayed(gamestate: Gamestate): Int {
        var dynamites = 0
        for (round in gamestate.rounds) {
            if (round.p1 == Move.D) {
                ++dynamites
            }
        }
        return dynamites
    }

    private fun calculateScores(gamestate: Gamestate): Pair<Int, Int> {
        var scoreP1 = 0
        var scoreP2 = 0
        var scoreRolledOver = 1
        for (round in gamestate.rounds) {
            when (calculateWinner(round.p1, round.p2)) {
                1 -> {
                    scoreP1 += scoreRolledOver
                    scoreRolledOver = 1
                }
                2 -> {
                    scoreP2 += scoreRolledOver
                    scoreRolledOver = 1
                }
                0 -> scoreRolledOver++
            }
        }
        return Pair(scoreP1, scoreP2)
    }

    private fun calculateWinner(move1: Move, move2: Move): Int {
        if (move1 == move2) return 0 // Draw
        when (move1) {
            Move.D -> {
                if (move2 == Move.W) return 2
                return 1
            }
            Move.W -> {
                if (move2 == Move.D) return 1
                return 2
            }
            Move.R -> {
                if (move2 == Move.S || move2 == Move.W) return 1
                return 2
            }
            Move.P -> {
                if (move2 == Move.R || move2 == Move.W) return 1
                return 2
            }
            Move.S -> {
                if (move2 == Move.P || move2 == Move.W) return 1
                return 2
            }
        }
    }

    private fun logGamestate(gamestate: Gamestate) {
        File("gamestateLog.txt").printWriter().use { out ->
            gamestate.rounds.forEach {
                out.println("${moveToText(it.p1)}-${moveToText(it.p2)} ${calculateWinner(it.p1, it.p2)}")
            }
        }
    }

    private fun moveToText(move: Move): String {
        return when (move) {
            Move.R -> "R"
            Move.P -> "P"
            Move.S -> "S"
            Move.D -> "D"
            Move.W -> "W"
        }
    }

    init {
        // Are you debugging?
        // Put a breakpoint on the line below to see when we start a new match
        println("Started new match")
    }
}